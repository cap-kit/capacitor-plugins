import Foundation
import UIKit
import GoogleSignIn

// Three-provider (google/apple/facebook) bridge-decoupled facade; splitting would
// scatter the shared single-flight gate, per-provider vaults, and in-flight
// coordinators. type_body_length is style-only; explicit sequential control flow
// is preserved (security-lint-precedence). Annotation stack required: the
// control comment sits between class doc and declaration, orphaning the doc
// (SwiftLint 0.65.1); the orphan silence targets only the doc line, the
// type-body silence only the declaration. Both line-scoped, single-rule.
// swiftlint:disable:next orphaned_doc_comment
/**
 * Native iOS implementation for the Authentication plugin.
 *
 * This class contains pure platform logic and is isolated from the Capacitor bridge.
 * Architectural constraints:
 * - MUST NOT access CAPPluginCall.
 * - MUST NOT depend on Capacitor bridge APIs directly.
 * - MUST perform UI operations on the Main Thread.
 *
 * Google sign-in flow:
 * 1. Load `GIDClientID` from `Info.plist` (typed error if absent).
 * 2. Try `GIDSignIn.sharedInstance.restorePreviousSignIn` for silent zero-UX.
 * 3. If restore fails (or autoSelect is false), run interactive
 *    `GIDSignIn.sharedInstance.signIn(withPresenting:)`.
 * 4. Map the full token set (id + access + refresh + server auth code) and persist
 *    through `KeychainVault` (native-only; raw keys never reach JS).
 *
 * Result surface is a callback-based `SignInCallback` mirroring the Android
 * contract so the Plugin layer controls the bridge resolution.
 */
// swiftlint:disable:next type_body_length
@objc public final class AuthenticationImpl: NSObject {

    // MARK: - Result surface (decoupled from the Capacitor bridge)

    protocol SignInCallback {
        func onResult(_ tokens: TokenSet)
        func onError(_ error: AuthenticationError)
    }

    // MARK: - Properties

    /// Cached plugin configuration containing logging and behavioral flags.
    private var config: AuthenticationConfig?

    /// Per-provider Keychain vault for the `google` provider.
    private lazy var vault = KeychainVault(provider: "google")

    /// Per-provider Keychain vault for the `apple` provider (service `capkit.auth.apple`).
    private lazy var appleVault = KeychainVault(provider: "apple")

    /// Per-provider Keychain vault for the `facebook` provider (service `capkit.auth.facebook`).
    private lazy var facebookVault = KeychainVault(provider: "facebook")

    /// Persistence target for a successful Apple sign-in (validator correction).
    ///
    /// Defaults to the per-provider `apple` vault (`capkit.auth.apple`), mirroring
    /// Android `handleAppleSignInResult`'s `appleVault.store(bundle)`; the write is
    /// NON-blocking (`try?`) so a Keychain failure never fails the resolve — the
    /// authorization code is exchanged server-side. Tests replace this hook with an
    /// in-memory recorder: a bare SPM test bundle has no host-app keychain
    /// entitlement, so real Keychain calls fail with `errSecMissingEntitlement`
    /// (-34018) in the test process.
    lazy var persistAppleSignIn: (TokenSet) -> Void = { [weak self] tokenSet in
        guard let self = self else { return }
        try? self.appleVault.store(tokenSet)
    }

    /// Persistence target for a successful Facebook sign-in.
    ///
    /// Defaults to the per-provider `facebook` vault (`capkit.auth.facebook`),
    /// mirroring Android `handleFacebookSignInSuccess`'s `facebookVault.store`; the
    /// write is NON-blocking (`try?`) so a Keychain failure never fails the resolve.
    /// Tests replace this hook with an in-memory recorder (a bare SPM test bundle
    /// has no host-app keychain entitlement — real Keychain calls fail with
    /// -34018 in the test process).
    lazy var persistFacebookSignIn: (TokenSet) -> Void = { [weak self] tokenSet in
        guard let self = self else { return }
        try? self.facebookVault.store(tokenSet)
    }

    /// The Google Sign-In SDK shared instance.
    private let gidSignIn = GIDSignIn.sharedInstance

    /// Single-flight gate: prevents concurrent sign-in flows.
    private var signInInProgress = false

    /// Retained coordinator for an in-flight native Apple flow.
    ///
    /// `ASAuthorizationController` holds weak references to its delegate and
    /// presentation-context provider, so the coordinator must stay alive for the
    /// whole flow; it is released on completion.
    private var activeAppleSignIn: AppleSignInImpl?

    /// Retained coordinator for an in-flight native Facebook flow.
    ///
    /// Holds the coordinator for the whole flow so the injected SDK bindings and
    /// the once-guarded completion stay alive until the outcome lands; released on
    /// completion (pattern mirror of `activeAppleSignIn`).
    private var activeFacebookSignIn: FacebookSignInImpl?

    // MARK: - Initialization

    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     * Applies static plugin configuration.
     *
     * MUST be called exactly once from the Plugin bridge layer during `load()`.
     */
    func applyConfig(_ config: AuthenticationConfig) {
        precondition(
            self.config == nil,
            "AuthenticationImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        AuthenticationLogger.verbose = config.verboseLogging

        AuthenticationLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - GIDClientID loader

    /**
     * Loads the Google client ID from `Info.plist` (`GIDClientID` key).
     *
     * - Throws: `AuthenticationError.initFailed` when the key is absent.
     */
    private func loadGIDClientID() throws -> String {
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
              !clientID.isEmpty else {
            throw AuthenticationError.initFailed(
                "GIDClientID is missing from Info.plist. Add the Google client ID to your Info.plist."
            )
        }
        return clientID
    }

    // MARK: - Facade: initialize

    func initialize(provider: String?) throws {
        if provider == "apple" {
            _ = try requireAppleConfig()
            return
        }
        if provider == "facebook" {
            _ = try requireFacebookConfig()
            return
        }
        guard provider == "google" else {
            throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
        }
        // Validate that GIDClientID is present at init time (fail-fast).
        _ = try loadGIDClientID()
    }

    // MARK: - Facade: signIn

    /**
     * Signs the user in with Google (interactive or silent restore).
     *
     * This method MUST be called from the main thread (GIDSignIn presents a UIKit modal).
     *
     * - Parameter provider: the provider key (`"google"`).
     * - Parameter autoSelect: when true, attempts silent `restorePreviousSignIn`
     *   first (zero-UX) before falling back to the interactive flow.
     * - Parameter callback: typed result surface (bridge decoupled).
     */
    func signIn(provider: String?, autoSelect: Bool?, callback: SignInCallback) {
        guard provider == "google" else {
            if provider == "apple" {
                signInApple(callback: callback)
                return
            }
            guard provider == "facebook" else {
                callback.onError(.invalidInput("Unsupported provider: \(provider ?? "nil")"))
                return
            }
            signInFacebook(callback: callback)
            return
        }
        guard !signInInProgress else {
            callback.onError(.conflict("A Google sign-in is already in progress."))
            return
        }

        signInInProgress = true
        do {
            let clientID = try loadGIDClientID()
            gidSignIn.configuration = GIDConfiguration(clientID: clientID)
        } catch let error as AuthenticationError {
            signInInProgress = false
            callback.onError(error)
            return
        } catch {
            signInInProgress = false
            callback.onError(.initFailed(error.localizedDescription))
            return
        }

        let shouldAttemptRestore = autoSelect ?? config?.googleAutoSelect ?? false
        if shouldAttemptRestore {
            gidSignIn.restorePreviousSignIn { [weak self] user, _ in
                guard let self = self else { return }
                if let user = user {
                    self.finishSignIn(user: user, callback: callback)
                    return
                }
                // Restore failed; fall back to interactive (no autoSelect flag so it doesn't loop).
                self.startInteractive(callback: callback)
            }
        } else {
            startInteractive(callback: callback)
        }
    }

    // MARK: - Facade: signOut / logout

    func signOut(provider: String?) throws {
        guard provider == "google" else {
            if provider == "apple" {
                appleVault.delete()
                return
            }
            guard provider == "facebook" else {
                throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
            }
            // The SDK logout is static — LoginManager() is stateless
            // enough that no instance needs to be retained across calls.
            FacebookSignInImpl.logOut()
            facebookVault.delete()
            return
        }
        gidSignIn.signOut()
        vault.delete()
    }

    func logout(provider: String?) throws {
        try signOut(provider: provider)
    }

    // MARK: - Facade: getCurrentAccessToken

    func getCurrentAccessToken(provider: String?) throws -> String? {
        guard provider == "google" else {
            if provider == "apple" {
                // Apple issues no native access token; the server exchanges the
                // authorization code. Resolve null (honest absence, not unimplemented).
                return try appleVault.read()?.accessToken
            }
            guard provider == "facebook" else {
                throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
            }
            return try facebookVault.read()?.accessToken
        }
        return try vault.read()?.accessToken
    }

    // MARK: - Facade: refreshToken

    /**
     * Refreshes the access token via the Google token endpoint using the
     * stored refresh token. Uses URLSession.
     *
     * - Throws: `AuthenticationError.invalidInput` when no refresh token is stored.
     */
    func refreshToken(provider: String?) throws -> TokenSet {
        guard provider == "google" else {
            if provider == "apple" {
                // Apple issues no refresh token; the authorization code is exchanged
                // server-side. Web-parity message.
                throw AuthenticationError.invalidInput("Apple issues no refresh token")
            }
            guard provider == "facebook" else {
                throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
            }
            // Facebook issues no refresh token — shared constant, exact Android/Web wording.
            throw AuthenticationError.invalidInput(FacebookErrorMapper.refreshNotSupportedMessage)
        }

        let existing = try vault.read()
        guard let refreshToken = existing?.refreshToken, !refreshToken.isEmpty else {
            throw AuthenticationError.invalidInput("No refresh token available")
        }
        let clientID = try loadGIDClientID()

        // Synchronous endpoint refresh: the semaphore parks the calling thread while
        // the URLSession task runs; every closure path assigns `refreshResult` before
        // signalling, so it is guaranteed populated once `wait()` returns.
        let semaphore = DispatchSemaphore(value: 0)
        var refreshResult: Result<TokenSet, Error>?

        let request = Self.makeRefreshRequest(clientID: clientID, refreshToken: refreshToken)

        URLSession.shared.dataTask(with: request) { [vault] data, _, error in
            defer { semaphore.signal() }
            if let error = error {
                refreshResult = .failure(
                    AuthenticationError.initFailed("Token refresh request failed: \(error.localizedDescription)")
                )
                return
            }
            guard let data = data else {
                refreshResult = .failure(AuthenticationError.initFailed("Token refresh failed: invalid response"))
                return
            }
            do {
                let merged = try Self.parseRefreshResponse(data, existing: existing)
                try? vault.store(merged)
                refreshResult = .success(merged)
            } catch {
                refreshResult = .failure(error)
            }
        }.resume()

        semaphore.wait()

        guard let refreshResult else {
            throw AuthenticationError.initFailed("Token refresh failed: no response")
        }
        return try Self.resolveRefreshResult(refreshResult)
    }

    // MARK: - Private: Google token-endpoint helpers

    /**
     * Builds the Google token-endpoint refresh request (URLSession on iOS).
     */
    private static func makeRefreshRequest(clientID: String, refreshToken: String) -> URLRequest {
        let body = [
            "grant_type": "refresh_token",
            "client_id": clientID,
            "refresh_token": refreshToken
        ].map { "\($0.key)=\($0.value)" }.joined(separator: "&")

        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = body.data(using: .utf8)
        return request
    }

    /**
     * Parses the Google token-endpoint response, mapping each failure arm onto the
     * shared `AuthenticationError` surface (messages unchanged).
     *
     * Pure extraction of the response-handling step: the sequential guards are
     * preserved verbatim so the credential path stays explicit and auditable.
     */
    private static func parseRefreshResponse(_ data: Data, existing: TokenSet?) throws -> TokenSet {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw AuthenticationError.initFailed("Token refresh failed: invalid response")
        }
        if let error = json["error"] as? String {
            let desc = json["error_description"] as? String ?? "Token endpoint error: \(error)"
            throw AuthenticationError.initFailed(desc)
        }
        guard let accessToken = json["access_token"] as? String else {
            throw AuthenticationError.initFailed("Token refresh failed: no access token")
        }
        return TokenSet(
            accessToken: accessToken,
            refreshToken: existing?.refreshToken,
            idToken: json["id_token"] as? String ?? existing?.idToken,
            serverAuthCode: existing?.serverAuthCode
        )
    }

    /**
     * Maps the token-endpoint outcome onto the facade's throwing surface.
     */
    private static func resolveRefreshResult(_ result: Result<TokenSet, Error>) throws -> TokenSet {
        switch result {
        case .success(let tokens):
            return tokens
        case .failure(let error):
            if let authError = error as? AuthenticationError {
                throw authError
            }
            throw AuthenticationError.initFailed(error.localizedDescription)
        }
    }

    // MARK: - Facade: Restore credentials (iOS unavailable, NOT unimplemented)

    func createRestoreCredential(options: [String: Any]?) throws -> [String: Any] {
        // Restore (Zero Tap) is Android-only; iOS reports available=false (NOT unimplemented).
        return ["available": false]
    }

    func getRestoreCredential(provider: String?) throws -> [String: Any] {
        guard provider == "google" else {
            throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
        }
        return ["available": false]
    }

    func clearRestoreCredential(provider: String?) throws -> [String: Any] {
        guard provider == "google" else {
            throw AuthenticationError.invalidInput("Unsupported provider: \(provider ?? "nil")")
        }
        return ["available": false]
    }

    // MARK: - Private: Apple configuration + flow helpers

    /**
     * Validates the Apple configuration at init time (fail-fast).
     *
     * Mirrors the Android `AppleConfigResolver` + `AppleConfigValidator`: an absent
     * `apple` block is `INIT_FAILED`; blank `clientId` or blank-but-present
     * `redirectURI` are `INVALID_INPUT`, with Android-parity messages. The native
     * iOS flow consumes `clientId` at runtime (`redirectURI` is a Web/Android
     * artifact, optional — blank-if-present only) but both are validated for
     * cross-platform configuration parity.
     */
    private func requireAppleConfig() throws -> AuthenticationConfig.AppleConfig {
        guard let apple = config?.apple else {
            throw AuthenticationError.initFailed("Missing 'apple' configuration; provide clientId.")
        }
        if let error = AppleConfigValidator.validate(apple) {
            throw error
        }
        return apple
    }

    /**
     * Starts the native Sign in with Apple flow.
     *
     * MUST be called from the main thread (`ASAuthorizationController` presents a
     * sheet). A fresh nonce is generated unless the configuration provides one; the
     * request carries its SHA-256 hash and the `id_token` `nonce` claim is validated
     * against it. Single-flight reuses the shared `signInInProgress` gate.
     */
    private func signInApple(callback: SignInCallback) {
        guard !signInInProgress else {
            callback.onError(.conflict("A sign-in is already in progress."))
            return
        }
        do {
            let appleConfig = try requireAppleConfig()
            let nonce = try (appleConfig.nonce ?? AuthenticationUtils.generateNonce())
            signInInProgress = true
            let appleSignIn = AppleSignInImpl(scopes: appleConfig.scopes, nonce: nonce) { [weak self] result in
                guard let self = self else { return }
                self.activeAppleSignIn = nil
                self.signInInProgress = false
                switch result {
                case let .success(appleResult):
                    self.handleAppleSignInSuccess(appleResult, callback: callback)
                case let .failure(error):
                    callback.onError(error)
                }
            }
            activeAppleSignIn = appleSignIn
            guard appleSignIn.start() else {
                activeAppleSignIn = nil
                signInInProgress = false
                callback.onError(.initFailed("Apple sign-in could not be presented."))
                return
            }
        } catch let error as AuthenticationError {
            callback.onError(error)
        } catch {
            callback.onError(.initFailed(error.localizedDescription))
        }
    }

    /**
     * Applies a successful Apple sign-in (Android `handleAppleSignInResult`
     * success-arm parity, `vault(provider:"apple")`): attaches the shared profile to the token set
     * and persists it to the per-provider `apple` vault (`capkit.auth.apple`) so
     * `getCurrentAccessToken('apple')`/`signOut` operate on the written vault.
     *
     * The vault write is deliberately NON-blocking (`try?`): a Keychain failure
     * must never fail the resolve — the authorization code is exchanged
     * server-side and the sign-in result still resolves with the tokens.
     */
    func handleAppleSignInSuccess(_ appleResult: AppleSignInImpl.AppleSignInResult, callback: SignInCallback) {
        var tokenSet = appleResult.tokenSet
        tokenSet.user = appleResult.user
        persistAppleSignIn(tokenSet)
        callback.onResult(tokenSet)
    }

    // MARK: - Private: Facebook configuration + flow helpers

    /**
     * Validates the Facebook configuration at init time (fail-fast).
     *
     * Mirrors the Android `FacebookConfigResolver` + `FacebookConfigValidator`
     * (config parity gate): an absent `facebook` block is
     * `INIT_FAILED`; blank `facebookAppId` or blank-but-present
     * `facebookClientToken` (and an explicitly empty scope set) are
     * `INVALID_INPUT`, with Android-parity messages. The resolved scopes default
     * to `public_profile email` in the config reader.
     */
    private func requireFacebookConfig() throws -> AuthenticationConfig.FacebookConfig {
        guard let facebook = config?.facebook else {
            throw AuthenticationError.initFailed("Missing 'facebook' configuration; provide appId.")
        }
        if let error = FacebookConfigValidator.validate(facebook) {
            throw error
        }
        return facebook
    }

    /**
     * Starts the native Facebook sign-in flow:
     * resolves the presenting view controller, launches the SDK dialog through the
     * injected bindings, and on completion persists the mapped `TokenSet` to the
     * per-provider `facebook` vault before resolving the callback.
     *
     * MUST be called from the main thread (`LoginManager` presents a UIKit modal).
     * Single-flight reuses the shared `signInInProgress` gate; a configuration
     * failure or a non-presentable window resolves the error without launching.
     */
    private func signInFacebook(callback: SignInCallback) {
        guard !signInInProgress else {
            callback.onError(.conflict("A sign-in is already in progress."))
            return
        }
        do {
            let facebookConfig = try requireFacebookConfig()
            guard let presentingVC = topViewController() else {
                callback.onError(.initFailed("No presenting view controller available."))
                return
            }
            signInInProgress = true
            let facebookSignIn = FacebookSignInImpl(
                scopes: facebookConfig.scopes,
                gateway: FacebookLoginGatewaySDK(),
                graph: FacebookGraphRequestSDK()
            ) { [weak self] result in
                guard let self = self else { return }
                self.activeFacebookSignIn = nil
                self.signInInProgress = false
                switch result {
                case let .success(tokenSet):
                    self.handleFacebookSignInSuccess(tokenSet, callback: callback)
                case let .failure(error):
                    callback.onError(error)
                }
            }
            activeFacebookSignIn = facebookSignIn
            if let error = facebookSignIn.start(presenting: presentingVC) {
                // The flow never started (e.g. `Settings.appID` unset): release the
                // coordinator and resolve the returned error — completion was NOT
                // invoked by the impl (single-fire surface).
                activeFacebookSignIn = nil
                signInInProgress = false
                callback.onError(error)
            }
        } catch let error as AuthenticationError {
            callback.onError(error)
        } catch {
            callback.onError(.initFailed(error.localizedDescription))
        }
    }

    /**
     * Applies a successful Facebook sign-in (Android `handleFacebookSignInSuccess`
     * parity, data-flow `vault(provider:"facebook")`): the
     * impl already attached the `/me` profile to the set, so the handler persists
     * it to the per-provider `facebook` vault (`capkit.auth.facebook`) so
     * `getCurrentAccessToken('facebook')`/`signOut` operate on the written vault.
     *
     * The vault write is deliberately NON-blocking (`try?`): a Keychain failure
     * must never fail the resolve — the sign-in result still resolves with the
     * produced tokens.
     */
    func handleFacebookSignInSuccess(_ tokenSet: TokenSet, callback: SignInCallback) {
        persistFacebookSignIn(tokenSet)
        callback.onResult(tokenSet)
    }

    // MARK: - Private: GIDSignIn flow helpers

    private func startInteractive(callback: SignInCallback) {
        guard let presentingVC = topViewController() else {
            signInInProgress = false
            callback.onError(.initFailed("No presenting view controller available."))
            return
        }

        gidSignIn.signIn(withPresenting: presentingVC) { [weak self] result, error in
            guard let self = self else { return }
            if let error = error {
                self.signInInProgress = false
                callback.onError(self.mapGIDError(error))
                return
            }
            guard let user = result?.user else {
                self.signInInProgress = false
                callback.onError(.initFailed("Google sign-in returned no user."))
                return
            }
            self.finishSignIn(user: user, callback: callback)
        }
    }

    private func finishSignIn(user: GIDGoogleUser, callback: SignInCallback) {
        let tokenSet = mapGIDUser(user)
        try? vault.store(tokenSet)
        signInInProgress = false
        callback.onResult(tokenSet)
    }

    private func mapGIDError(_ error: Error) -> AuthenticationError {
        guard let gidError = error as? GIDSignInError else {
            return .initFailed("Google sign-in failed: \(error.localizedDescription)")
        }
        switch gidError.code {
        case .canceled:
            return .userCancelled("Google sign-in was cancelled.")
        case .hasNoAuthInKeychain:
            return .userCancelled("No Google account was available.")
        case .keychain:
            return .initFailed("Google sign-in keychain error: \(gidError.localizedDescription)")
        default:
            return .initFailed("Google sign-in failed: \(gidError.localizedDescription)")
        }
    }

    /**
     * Maps a `GIDGoogleUser` to the vault's `TokenSet`.
     *
     * On interactive sign-in the `GIDGoogleUser` carries:
     * - `profile.email` and `profile.name` (profile data)
     * - `idToken` (OpenID Connect)
     * - `accessToken` (OAuth2)
     * - `refreshToken` (OAuth2)
     *
     * The server auth code is not directly surfaced by GIDSignIn and is deferred
     * to a backend exchange from the refresh token (no in-plugin exchange).
     */
    private func mapGIDUser(_ user: GIDGoogleUser) -> TokenSet {
        return TokenSet(
            accessToken: tokenString(user.accessToken),
            refreshToken: tokenString(user.refreshToken),
            idToken: tokenString(user.idToken),
            serverAuthCode: nil
        )
    }

    private func tokenString(_ token: GIDToken?) -> String? {
        guard let token = token else { return nil }
        return token.tokenString.isEmpty ? nil : token.tokenString
    }

    // MARK: - Private: UIKit helpers

    /**
     * Returns the topmost view controller to present the GIDSignIn modal from.
     */
    private func topViewController() -> UIViewController? {
        let rootVC: UIViewController?
        if #available(iOS 15.0, *) {
            rootVC = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }?
                .rootViewController
        } else {
            rootVC = UIApplication.shared.keyWindow?.rootViewController
        }
        guard var top = rootVC else { return nil }
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }

    // Three-provider bridge-decoupled facade (google/apple/facebook, single-flight
    // gate, per-provider vaults, in-flight coordinators); splitting across files
    // would scatter the shared flow state. file_length is style-only; control flow
    // and auditability are unaffected (security-lint-precedence).
    // swiftlint:disable:next file_length
}
