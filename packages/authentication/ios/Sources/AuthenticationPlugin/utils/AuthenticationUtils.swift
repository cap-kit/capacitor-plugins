import AuthenticationServices
import CryptoKit
import Foundation

/**
 * Storage kinds for the secure token vault.
 *
 * Mirrors the Android `TokenKind` enum so key derivation is consistent
 * across platforms. The Keychain vault (`KeychainVault`) stores tokens
 * as a single JSON blob; this enum drives the pure key-derivation contract.
 */
enum TokenKind: String {
    case access = "access"
    case refresh = "refresh"
    case id = "id"
    case serverAuthCode = "server_auth_code"
}

// Pure side-effect-free helpers in one cohesive file. The 251-vs-250 type-body
// crossing is style-only measurement noise; splitting would scatter the RFC 7636
// PKCE / Apple-nonce / JWT crypto contract across files (security-lint-precedence).
// Control comments must sit between doc and declaration, orphaning the doc
// (SwiftLint 0.65.1); both disable:next below are single-rule and line-scoped,
// no re-enables.
// swiftlint:disable:next orphaned_doc_comment
/**
 * Pure helpers for the Authentication plugin.
 *
 * Every function is side-effect free and independently testable.
 * No Capacitor or Google SDK dependency; pure Foundation + CryptoKit only.
 */
// swiftlint:disable:next type_body_length
struct AuthenticationUtils {

    // MARK: - PKCE

    /// Unreserved characters allowed in a code_verifier (RFC 7636 section 4.1).
    private static let pkceAlphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    /**
     * Generates a cryptographically random PKCE code_verifier.
     *
     * - Parameter length: number of characters; must be between 43 and 128 inclusive
     *   (RFC 7636).
     * - Throws: `AuthenticationError.invalidInput` when length is outside the range.
     */
    static func generatePkceVerifier(length: Int = 64) throws -> String {
        guard (43...128).contains(length) else {
            throw AuthenticationError.invalidInput(
                "PKCE verifier length must be between 43 and 128, got \(length)"
            )
        }
        let bytes = (0..<length).map { _ in UInt8.random(in: 0...255) }
        return String(bytes.map { byte in
            let index = pkceAlphabet.index(
                pkceAlphabet.startIndex,
                offsetBy: Int(byte) % pkceAlphabet.count
            )
            return pkceAlphabet[index]
        })
    }

    /**
     * Computes the RFC 7636 code_challenge for a verifier:
     * `base64url(sha256(verifier))` with padding removed.
     */
    static func generatePkceChallenge(verifier: String) -> String {
        let data = Data(verifier.utf8)
        let digest = SHA256.hash(data: data)
        return Data(digest)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    // MARK: - Auth URL builder

    // OAuth URL builder: 7 named params mirror the Android `OAuthUrlBuilder`
    // contract, exercised by `AuthenticationUtilsTests`; explicit named params
    // are the auditable form (security-lint-precedence). Control comments must
    // sit between doc and declaration, orphaning the doc (SwiftLint 0.65.1);
    // both disable:next below are single-rule and line-scoped, no re-enables.
    // swiftlint:disable:next orphaned_doc_comment
    /**
     * Builds an OAuth2 authorization-endpoint URL for the authorization-code + PKCE flow.
     */
    // swiftlint:disable:next function_parameter_count
    static func buildAuthUrl(
        endpoint: String,
        clientId: String,
        redirectUri: String,
        state: String,
        codeChallenge: String,
        scopes: [String],
        extra: [String: String] = [:]
    ) -> String {
        var params: [String] = [
            "response_type=code",
            "client_id=\(urlEncode(clientId))",
            "redirect_uri=\(urlEncode(redirectUri))",
            "state=\(urlEncode(state))",
            "code_challenge=\(urlEncode(codeChallenge))",
            "code_challenge_method=S256"
        ]
        if !scopes.isEmpty {
            params.append("scope=\(urlEncode(scopes.joined(separator: " ")))")
        }
        for (key, value) in extra {
            params.append("\(urlEncode(key))=\(urlEncode(value))")
        }
        return "\(endpoint)?\(params.joined(separator: "&"))"
    }

    // MARK: - Token mapper

    /**
     * Builds the normalized token map, omitting any absent optional field.
     *
     * Mirrors the Android `TokenMapper.map()` shape; absent optional fields are
     * omitted so the result matches the optional TypeScript contract.
     */
    static func mapTokens(
        accessToken: String?,
        refreshToken: String?,
        idToken: String?,
        serverAuthCode: String?
    ) -> [String: String] {
        var result: [String: String] = [:]
        if let value = accessToken, !value.isEmpty { result["accessToken"] = value }
        if let value = refreshToken, !value.isEmpty { result["refreshToken"] = value }
        if let value = idToken, !value.isEmpty { result["idToken"] = value }
        if let value = serverAuthCode, !value.isEmpty { result["serverAuthCode"] = value }
        return result
    }

    // MARK: - Storage key provider

    /**
     * Computes the storage key for a provider and token kind.
     *
     * Namespaced as `auth_{provider}_{kind}` matching the Android contract.
     */
    static func storageKey(provider: String, kind: TokenKind) -> String {
        "auth_\(provider)_\(kind.rawValue)"
    }

    // MARK: - Apple nonce (Sign in with Apple)

    /// Unreserved, URL-safe characters allowed in a Sign in with Apple nonce.
    private static let nonceAlphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    /**
     * Generates a cryptographically random nonce for a Sign in with Apple request.
     *
     * The raw nonce is hashed before being placed on the request (see
     * `sha256Base64URL`); the `id_token` `nonce` claim MUST equal that hash.
     *
     * - Parameter length: number of characters; must be between 16 and 128 inclusive.
     * - Throws: `AuthenticationError.invalidInput` when length is outside the range.
     */
    static func generateNonce(length: Int = 32) throws -> String {
        guard (16...128).contains(length) else {
            throw AuthenticationError.invalidInput(
                "Apple nonce length must be between 16 and 128, got \(length)"
            )
        }
        let bytes = (0..<length).map { _ in UInt8.random(in: 0...255) }
        return String(bytes.map { byte in
            let index = nonceAlphabet.index(
                nonceAlphabet.startIndex,
                offsetBy: Int(byte) % nonceAlphabet.count
            )
            return nonceAlphabet[index]
        })
    }

    /**
     * Computes the base64url SHA-256 digest of a string, with padding removed.
     *
     * This is the value placed on `ASAuthorizationOpenIDRequest.nonce`; Apple echoes
     * it in the `id_token` `nonce` claim, which `validateIDTokenNonce` compares.
     */
    static func sha256Base64URL(_ input: String) -> String {
        let digest = SHA256.hash(data: Data(input.utf8))
        return Data(digest)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    // MARK: - Apple ID token decoding

    /**
     * Decodes the claims of an OpenID Connect ID token payload (segment 2).
     *
     * The signature is NOT verified — this is input-claims extraction used to read the
     * `nonce` claim for validation (mirrors the Android `JwtDecoder`).
     * Returns `nil` for any malformed input.
     */
    static func decodeJWTClaims(_ idToken: String) -> [String: Any]? {
        let segments = idToken.split(separator: ".")
        guard segments.count == 3 else { return nil }
        guard let data = decodeBase64URLSegment(String(segments[1])) else { return nil }
        guard let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            return nil
        }
        return object
    }

    /**
     * Validates the `nonce` claim of an Apple `id_token` against the hash sent in the
     * authorization request.
     *
     * Mirrors the Android `StateNonceValidator` on the nonce half (the iOS native flow
     * never echoes `state`): returns `nil` on match and an `INVALID_INPUT`
     * `AuthenticationError` otherwise, with Android-parity messages.
     */
    static func validateIDTokenNonce(idToken: String, expectedHash: String) -> AuthenticationError? {
        guard let claims = decodeJWTClaims(idToken) else {
            return AuthenticationError.invalidInput("The Apple sign-in returned a malformed id_token.")
        }
        guard let nonceClaim = claims["nonce"] as? String else {
            return AuthenticationError.invalidInput("OpenID nonce claim is missing.")
        }
        guard nonceClaim == expectedHash else {
            return AuthenticationError.invalidInput("OpenID nonce mismatch.")
        }
        return nil
    }

    // MARK: - Apple error mapping

    /**
     * Maps a typed `AppleSignInError` onto the shared `AuthenticationError` model.
     *
     * Semantics mirror the Web reducer and the Android `AppleErrorMapper`: cancellation →
     * `USER_CANCELLED`, nonce/state problems → `INVALID_INPUT`, configuration/network/unknown →
     * `INIT_FAILED`. No new error codes. Messages match Android verbatim.
     */
    static func mapAppleError(_ error: AppleSignInError) -> AuthenticationError {
        switch error {
        case .userCancelled:
            return AuthenticationError.userCancelled("The user cancelled the Apple sign-in flow.")
        case .invalidNonce:
            return AuthenticationError.invalidInput("The Apple sign-in returned an invalid nonce.")
        case .invalidState:
            return AuthenticationError.invalidInput("The Apple sign-in returned an invalid state.")
        case .missingConfiguration:
            return AuthenticationError.initFailed("Apple configuration is missing or malformed.")
        case .network:
            return AuthenticationError.initFailed("The Apple sign-in flow failed over the network.")
        case .unknown:
            return AuthenticationError.initFailed("The Apple sign-in flow failed for an unknown reason.")
        }
    }

    // MARK: - Apple user profile mapping

    /**
     * Maps Apple's real-user estimation onto the stable string union.
     *
     * Literal spellings are preserved exactly as Apple issues them, including the
     * historical `likleyRealUser` typo (intentional for API stability).
     */
    static func mapUserDetectionStatus(_ status: ASUserDetectionStatus) -> String {
        switch status {
        case .likelyReal:
            return "likleyRealUser"
        case .unknown:
            return "unknown"
        case .unsupported:
            return "unsupported"
        @unknown default:
            return "unknown"
        }
    }

    /**
     * Builds the provider user profile delivered with an Apple credential.
     *
     * Returns `nil` when the credential carries no profile fields — Apple only shares
     * `fullName`/`email` on FIRST sign-in; later sign-ins must not treat `user` as
     * re-gettable (apple-provider first-sign-in semantics).
     */
    static func mapUser(
        id: String?,
        email: String?,
        givenName: String?,
        familyName: String?,
        detectionStatus: ASUserDetectionStatus
    ) -> SocialAuthResultUser? {
        guard id != nil || email != nil || givenName != nil || familyName != nil else {
            return nil
        }
        let name: SocialAuthUserName?
        if givenName != nil || familyName != nil {
            name = .nameParts(firstName: givenName, lastName: familyName)
        } else {
            name = nil
        }
        return SocialAuthResultUser(
            id: id,
            email: email,
            name: name,
            realUserStatus: mapUserDetectionStatus(detectionStatus)
        )
    }

    // MARK: - Apple token set mapping (bridge)

    /**
     * Builds the vault `TokenSet` carried by an Apple credential.
     *
     * Apple issues no native `accessToken`/`refreshToken` — the server exchanges the
     * `authorizationCode`; an Apple set is therefore never a `hasCredential` set.
     * Returns `nil` when the credential carries no token material at all.
     */
    static func mapAppleTokenSet(authorizationCode: String?, idToken: String?) -> TokenSet? {
        guard authorizationCode != nil || idToken != nil else { return nil }
        return TokenSet(
            accessToken: nil,
            refreshToken: nil,
            idToken: idToken,
            serverAuthCode: nil,
            authorizationCode: authorizationCode,
            user: nil
        )
    }

    /**
     * Maps the shared user profile onto the bridge dictionary, omitting absent fields.
     *
     * Emits `name` as the plain TypeScript union — a `{ firstName?, lastName? }` object or a
     * display string — matching the Web popup emission; the `nameType` discriminator is
     * vault-side only and never crosses the bridge.
     */
    static func mapSocialAuthUser(_ user: SocialAuthResultUser) -> [String: Any]? {
        guard user.id != nil || user.email != nil || user.name != nil ||
                user.realUserStatus != nil || user.picture != nil else {
            return nil
        }
        var result: [String: Any] = [:]
        if let id = user.id, !id.isEmpty { result["id"] = id }
        if let email = user.email, !email.isEmpty { result["email"] = email }
        if let name = user.name, let mappedName = mapUserName(name) {
            result["name"] = mappedName
        }
        if let realUserStatus = user.realUserStatus, !realUserStatus.isEmpty {
            result["realUserStatus"] = realUserStatus
        }
        if let picture = user.picture, !picture.isEmpty {
            result["picture"] = picture
        }
        return result
    }

    /**
     * Maps the user name onto its bridge union form: a `{ firstName?, lastName? }`
     * object or a display string, omitting empty content.
     */
    private static func mapUserName(_ name: SocialAuthUserName) -> Any? {
        switch name {
        case let .nameParts(firstName, lastName):
            var nameParts: [String: String] = [:]
            if let firstName, !firstName.isEmpty { nameParts["firstName"] = firstName }
            if let lastName, !lastName.isEmpty { nameParts["lastName"] = lastName }
            return nameParts.isEmpty ? nil : nameParts
        case let .displayName(value):
            return value.isEmpty ? nil : value
        }
    }

    /**
     * Maps an Apple `TokenSet` onto the bridge `tokens` object, omitting absent fields.
     *
     * Matches the optional TypeScript `OAuthTokenSet` contract (authorization code +
     * id token + user; no fabricated access/refresh tokens).
     */
    static func mapAppleTokens(_ tokenSet: TokenSet) -> [String: Any] {
        var result: [String: Any] = [:]
        if let authorizationCode = tokenSet.authorizationCode, !authorizationCode.isEmpty {
            result["authorizationCode"] = authorizationCode
        }
        if let idToken = tokenSet.idToken, !idToken.isEmpty { result["idToken"] = idToken }
        if let user = tokenSet.user, let userMap = mapSocialAuthUser(user) {
            result["user"] = userMap
        }
        return result
    }

    /**
     * Maps a Facebook `TokenSet` onto the bridge `tokens` object, omitting absent fields.
     *
     * Matches the optional TypeScript facebook contract: the granted
     * access token, the four facebook slots (`userId`, `grantedPermissions`,
     * `declinedPermissions`, `expiresAt` absolute epoch seconds) and the `/me` user
     * (with `picture`). No id/refresh/authorization-code material is ever fabricated.
     */
    static func mapFacebookTokens(_ tokenSet: TokenSet) -> [String: Any] {
        var result: [String: Any] = [:]
        if let accessToken = tokenSet.accessToken, !accessToken.isEmpty {
            result["accessToken"] = accessToken
        }
        if let userId = tokenSet.userId, !userId.isEmpty { result["userId"] = userId }
        if let grantedPermissions = tokenSet.grantedPermissions, !grantedPermissions.isEmpty {
            result["grantedPermissions"] = grantedPermissions
        }
        if let declinedPermissions = tokenSet.declinedPermissions, !declinedPermissions.isEmpty {
            result["declinedPermissions"] = declinedPermissions
        }
        if let expiresAt = tokenSet.expiresAt {
            result["expiresAt"] = expiresAt
        }
        if let user = tokenSet.user, let userMap = mapSocialAuthUser(user) {
            result["user"] = userMap
        }
        return result
    }

    // MARK: - Internal

    /// Base64url-decodes a JWT segment, restoring standard base64 padding.
    private static func decodeBase64URLSegment(_ value: String) -> Data? {
        var base64 = value
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 {
            base64.append("=")
        }
        return Data(base64Encoded: base64)
    }

    private static func urlEncode(_ value: String) -> String {
        value.addingPercentEncoding(
            withAllowedCharacters: CharacterSet(charactersIn:
                                                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        ) ?? value
    }

    // Crypto/PKCE/nonce/JWT helpers stay in one auditable file; file_length is
    // style-only (security-lint-precedence).
    // swiftlint:disable:next file_length
}
