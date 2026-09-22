import XCTest
import UIKit
import FBSDKCoreKit
@testable import AuthenticationPlugin

/**
 * @file FacebookSignInImplTests.swift
 * Behavioral tests for the iOS Facebook provider surface:
 * the pure `FacebookTokenMapper` slot/profile mapping,
 * the `FacebookErrorMapper` classification onto the shared ten-code set
 * (zero new codes), the `FacebookSignInImpl` orchestration through the injected
 * gateway/graph seams (single-flight surface, `.cancelled` → `USER_CANCELLED`,
 * `/me` profile fetch, `Settings.shared.appID` guard), and the `AuthenticationImpl`
 * facebook contracts (vault persistence via the injectable `persistFacebookSignIn`
 * hook — SPM test bundle has no host-app keychain entitlement, `-34018` —
 * `refreshToken('facebook')` exact rejection, `signOut('facebook')` no-throw).
 *
 * Written STRICTLY FIRST (RED): `FacebookSignInImpl`/`FacebookTokenMapper`/
 * `FacebookErrorMapper`/`FacebookSignInError`/`AuthenticationConfig.FacebookConfig`
 * and the `AuthenticationImpl` facebook hooks do not exist, and `FBSDKCoreKit`
 * is not yet a dependency — both test files fail to compile (`no such module`,
 * `cannot find '...' in scope`) until the GREEN slice lands.
 */
class FacebookSignInImplTests: XCTestCase {

    // SDK 18.x surface: `Settings` is a shared INSTANCE (`Settings.shared.appID`)
    // and its property setters validate SDK configuration in DEBUG builds
    // (`fatalError` when unconfigured). The headless SPM test process has no
    // `UIApplication`, so the SDK is initialized through the documented no-app
    // entry point exactly once (`hasInitializeBeenCalled` guard — idempotent);
    // dependencies are configured synchronously (CoreKitConfigurator) before any
    // first-frame deferral.
    private static let sdkInitialized: Void = {
        ApplicationDelegate.shared.initializeSDK()
    }()

    override func setUp() {
        super.setUp()
        _ = Self.sdkInitialized
    }

    // MARK: - FacebookTokenMapper: expiry (absolute epoch-seconds, floor parity)

    func testExpiresAtEpochSecondsFloorsFractionalSeconds() {
        // Android uses `getExpires().time / 1000` (floor); iOS receives seconds from
        // the SDK `Date` and must floor the same way.
        XCTAssertEqual(FacebookTokenMapper.expiresAtEpochSeconds(1700000123.9), 1700000123)
        XCTAssertEqual(FacebookTokenMapper.expiresAtEpochSeconds(1700000123.0), 1700000123)
    }

    func testExpiresAtEpochSecondsNilStaysNil() {
        XCTAssertNil(FacebookTokenMapper.expiresAtEpochSeconds(nil))
    }

    // MARK: - FacebookTokenMapper: permission sanitization

    func testSanitizePermissionsTrimsDropsBlanksAndDedupes() {
        XCTAssertEqual(
            FacebookTokenMapper.sanitizePermissions([" public_profile ", "", "email", "public_profile"]),
            ["public_profile", "email"]
        )
    }

    func testSanitizePermissionsNilStaysNil() {
        XCTAssertNil(FacebookTokenMapper.sanitizePermissions(nil))
    }

    // MARK: - FacebookTokenMapper: token slot mapping

    func testMapTokenFillsFacebookSlotsOnly() {
        let set = FacebookTokenMapper.mapToken(
            accessToken: "fb-at-1",
            userId: "12345",
            grantedPermissions: ["public_profile", "email"],
            declinedPermissions: ["user_photos"],
            expiresAt: 1700000123.0
        )
        XCTAssertEqual(set.accessToken, "fb-at-1")
        XCTAssertEqual(set.userId, "12345")
        XCTAssertEqual(set.grantedPermissions, ["public_profile", "email"])
        XCTAssertEqual(set.declinedPermissions, ["user_photos"])
        XCTAssertEqual(set.expiresAt, 1700000123)
        XCTAssertTrue(set.hasCredential)
    }

    func testMapTokenKeepsNonApplicableFieldsNull() {
        // Facebook never issues id/refresh/authorization-code/server-auth-code material;
        // the slots stay absent, never empty strings.
        let set = FacebookTokenMapper.mapToken(accessToken: "fb-at-1")
        XCTAssertEqual(set.accessToken, "fb-at-1")
        XCTAssertNil(set.idToken)
        XCTAssertNil(set.refreshToken)
        XCTAssertNil(set.authorizationCode)
        XCTAssertNil(set.serverAuthCode)
        XCTAssertNil(set.userId)
        XCTAssertNil(set.grantedPermissions)
        XCTAssertNil(set.declinedPermissions)
        XCTAssertNil(set.expiresAt)
    }

    func testMapTokenSanitizesPermissionLists() {
        let set = FacebookTokenMapper.mapToken(
            accessToken: "fb-at-1",
            grantedPermissions: [" email ", "", "email"],
            declinedPermissions: ["  "]
        )
        XCTAssertEqual(set.grantedPermissions, ["email"])
        XCTAssertEqual(set.declinedPermissions, [])
    }

    // MARK: - FacebookTokenMapper: profile mapping

    func testMapProfileBuildsDisplayNameUserWithPicture() {
        let user = FacebookTokenMapper.mapProfile(
            id: "12345",
            name: "Grace Hopper",
            email: "grace@example.com",
            pictureUrl: "https://example.com/fb.jpg"
        )
        XCTAssertEqual(user.id, "12345")
        XCTAssertEqual(user.email, "grace@example.com")
        XCTAssertEqual(user.name, .displayName("Grace Hopper"))
        XCTAssertEqual(user.picture, "https://example.com/fb.jpg")
        XCTAssertNil(user.realUserStatus, "realUserStatus is NEVER set for facebook (Android parity)")
    }

    func testMapProfileBlankNameYieldsNilName() {
        let user = FacebookTokenMapper.mapProfile(id: "12345", name: "   ")
        XCTAssertNil(user.name)
        XCTAssertEqual(user.id, "12345")
    }

    // MARK: - FacebookTokenMapper: sign-in result

    func testMapSignInResultAttachesProfile() {
        let profile = FacebookTokenMapper.mapProfile(
            id: "12345",
            name: "Grace Hopper",
            email: "grace@example.com",
            pictureUrl: "https://example.com/fb.jpg"
        )
        let set = FacebookTokenMapper.mapSignInResult(
            accessToken: "fb-at-1",
            userId: "12345",
            grantedPermissions: ["public_profile", "email"],
            expiresAt: 1700000123.9,
            profile: profile
        )
        XCTAssertEqual(set.accessToken, "fb-at-1")
        XCTAssertEqual(set.userId, "12345")
        XCTAssertEqual(set.expiresAt, 1700000123, "Expiry is floored before attaching the profile")
        XCTAssertEqual(set.user, profile)
    }

    // MARK: - FacebookErrorMapper: typed categories (ten-code set, zero new codes)

    func testMapUserCancelledMapsExactMessage() {
        let error = FacebookErrorMapper.map(.userCancelled)
        XCTAssertEqual(error.errorCode, "USER_CANCELLED")
        XCTAssertEqual(error.message, "The user cancelled the Facebook sign-in flow.")
    }

    func testMapMissingConfigurationMapsInitFailed() {
        let error = FacebookErrorMapper.map(.missingConfiguration)
        XCTAssertEqual(error.errorCode, "INIT_FAILED")
        XCTAssertEqual(error.message, "Facebook configuration is missing or malformed.")
    }

    func testMapKeyHashMismatchMapsActionableInitFailed() {
        let error = FacebookErrorMapper.map(.keyHashMismatch)
        XCTAssertEqual(error.errorCode, "INIT_FAILED")
        XCTAssertEqual(error.message, "Facebook sign-in failed: verify key hash matches Facebook developer settings.")
    }

    func testMapGraphNetworkMapsUnavailable() {
        let error = FacebookErrorMapper.map(.graphNetwork)
        XCTAssertEqual(error.errorCode, "UNAVAILABLE")
        XCTAssertEqual(error.message, "The Facebook Graph request failed over the network.")
    }

    func testMapMalformedInputMapsInvalidInput() {
        let error = FacebookErrorMapper.map(.malformedInput)
        XCTAssertEqual(error.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error.message, "The Facebook sign-in returned malformed input.")
    }

    func testMapRefreshNotSupportedMapsExactMessage() {
        let error = FacebookErrorMapper.map(.refreshNotSupported)
        XCTAssertEqual(error.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error.message, "Facebook issues no refresh token")
    }

    func testMapUnknownMapsInitFailed() {
        let error = FacebookErrorMapper.map(.unknown)
        XCTAssertEqual(error.errorCode, "INIT_FAILED")
        XCTAssertEqual(error.message, "The Facebook sign-in flow failed for an unknown reason.")
    }

    // MARK: - FacebookErrorMapper: raw SDK message classification

    func testIsKeyHashMismatchDetectsAndroidCode() {
        XCTAssertTrue(FacebookErrorMapper.isKeyHashMismatch("registration failed: 1349094"))
        XCTAssertFalse(FacebookErrorMapper.isKeyHashMismatch("graph error"))
        XCTAssertFalse(FacebookErrorMapper.isKeyHashMismatch(nil))
    }

    func testMapSdkMessageClassifiesKeyHashActionable() {
        let error = FacebookErrorMapper.mapSdkMessage("1349094")
        XCTAssertEqual(error.errorCode, "INIT_FAILED")
        XCTAssertEqual(error.message, "Facebook sign-in failed: verify key hash matches Facebook developer settings.")
    }

    func testMapSdkMessageBlankOrNilIsInvalidInput() {
        let blank = FacebookErrorMapper.mapSdkMessage("   ")
        let nilMessage = FacebookErrorMapper.mapSdkMessage(nil)
        XCTAssertEqual(blank.errorCode, "INVALID_INPUT")
        XCTAssertEqual(blank.message, "The Facebook SDK returned an empty error message.")
        XCTAssertEqual(nilMessage.errorCode, "INVALID_INPUT")
    }

    func testMapSdkMessagePassesRawMessageAsUnavailable() {
        let error = FacebookErrorMapper.mapSdkMessage("graph/178724899: (100) Invalid parameter")
        XCTAssertEqual(error.errorCode, "UNAVAILABLE")
        XCTAssertEqual(error.message, "graph/178724899: (100) Invalid parameter")
    }

    // MARK: - FacebookSignInImpl: configuration guard

    func testStartReturnsInitFailedWhenAppIdNotConfigured() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = nil
        defer { Settings.shared.appID = originalAppID }

        let gateway = FakeLoginGateway()
        let graph = FakeGraph()
        var completionFired = false
        let impl = FacebookSignInImpl(scopes: ["public_profile"], gateway: gateway, graph: graph) { _ in
            completionFired = true
        }

        let error = impl.start(presenting: UIViewController())

        XCTAssertEqual(error?.errorCode, "INIT_FAILED")
        XCTAssertEqual(
            error?.message,
            "Facebook is not configured. Set 'facebookAppId' (or Info.plist 'FacebookAppID')."
        )
        XCTAssertFalse(completionFired, "The completion must NOT fire when the flow never starts")
        XCTAssertFalse(gateway.didLaunch, "The SDK dialog must NOT be launched without an app id")
    }

    // MARK: - FacebookSignInImpl: launch surface

    func testStartReturnsNilAndLaunchesWhenConfigured() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let gateway = FakeLoginGateway()
        let impl = FacebookSignInImpl(scopes: ["public_profile"], gateway: gateway, graph: FakeGraph()) { _ in }
        let presenting = UIViewController()

        XCTAssertNil(impl.start(presenting: presenting))
        XCTAssertTrue(gateway.didLaunch)
        XCTAssertEqual(gateway.lastScopes, ["public_profile"])
        XCTAssertTrue(gateway.lastPresenting === presenting, "The resolved presenting view controller must be passed through")
    }

    func testEmptyScopesDefaultToPublicProfileAndEmail() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let gateway = FakeLoginGateway()
        let impl = FacebookSignInImpl(scopes: [], gateway: gateway, graph: FakeGraph()) { _ in }

        XCTAssertNil(impl.start(presenting: UIViewController()))
        XCTAssertEqual(gateway.lastScopes, ["public_profile", "email"])
    }

    // MARK: - FacebookSignInImpl: outcome mapping through the seams

    func testCancelledOutcomeMapsUserCancelled() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let impl = FacebookSignInImpl(
            scopes: ["public_profile"],
            gateway: FakeLoginGateway(outcome: .cancelled),
            graph: FakeGraph()
        ) { result in
            guard case let .failure(error) = result else {
                XCTFail("Expected a failure result")
                return
            }
            XCTAssertEqual(error.errorCode, "USER_CANCELLED")
            XCTAssertEqual(error.message, "The user cancelled the Facebook sign-in flow.")
        }

        XCTAssertNil(impl.start(presenting: UIViewController()))
    }

    func testSuccessOutcomeFetchesProfileAndMapsSlots() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let profile = FacebookTokenMapper.mapProfile(
            id: "12345",
            name: "Grace Hopper",
            email: "grace@example.com",
            pictureUrl: "https://example.com/fb.jpg"
        )
        let gateway = FakeLoginGateway(
            outcome: .success(
                accessToken: "fb-at-1",
                userId: "12345",
                granted: ["public_profile", "email"],
                declined: ["user_photos"],
                expiresAt: 1700000123.9
            )
        )
        let graph = FakeGraph(profileResult: .success(profile))
        let impl = FacebookSignInImpl(
            scopes: ["public_profile"],
            gateway: gateway,
            graph: graph
        ) { result in
            guard case let .success(tokens) = result else {
                XCTFail("Expected a success result")
                return
            }
            XCTAssertEqual(graph.lastToken, "fb-at-1", "The /me profile fetch must use the granted access token")
            XCTAssertEqual(tokens.accessToken, "fb-at-1")
            XCTAssertEqual(tokens.userId, "12345")
            XCTAssertEqual(tokens.grantedPermissions, ["public_profile", "email"])
            XCTAssertEqual(tokens.declinedPermissions, ["user_photos"])
            XCTAssertEqual(tokens.expiresAt, 1700000123, "Expiry is floored to whole seconds")
            XCTAssertEqual(tokens.user, profile)
            XCTAssertNil(tokens.idToken)
            XCTAssertNil(tokens.refreshToken)
            XCTAssertNil(tokens.authorizationCode)
            XCTAssertNil(tokens.serverAuthCode)
        }

        XCTAssertNil(impl.start(presenting: UIViewController()))
    }

    func testGraphFailureMapsUnavailable() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let gateway = FakeLoginGateway(
            outcome: .success(accessToken: "fb-at-1", userId: "12345", granted: nil, declined: nil, expiresAt: nil)
        )
        let graph = FakeGraph(profileResult: .failure(.unavailable("The Facebook Graph request failed over the network.")))
        let impl = FacebookSignInImpl(
            scopes: ["public_profile"],
            gateway: gateway,
            graph: graph
        ) { result in
            guard case let .failure(error) = result else {
                XCTFail("Expected a failure result")
                return
            }
            XCTAssertEqual(error.errorCode, "UNAVAILABLE")
            XCTAssertEqual(error.message, "The Facebook Graph request failed over the network.")
        }

        XCTAssertNil(impl.start(presenting: UIViewController()))
    }

    func testOutcomeFailurePassesMappedError() {
        let originalAppID = Settings.shared.appID
        Settings.shared.appID = "123456789012345"
        defer { Settings.shared.appID = originalAppID }

        let impl = FacebookSignInImpl(
            scopes: ["public_profile"],
            gateway: FakeLoginGateway(outcome: .failure(.initFailed("The Facebook sign-in flow failed for an unknown reason."))),
            graph: FakeGraph()
        ) { result in
            guard case let .failure(error) = result else {
                XCTFail("Expected a failure result")
                return
            }
            XCTAssertEqual(error.errorCode, "INIT_FAILED")
            XCTAssertEqual(error.message, "The Facebook sign-in flow failed for an unknown reason.")
        }

        XCTAssertNil(impl.start(presenting: UIViewController()))
    }

    // MARK: - Compile gate

    func testFacebookSignInImplIsConstructible() {
        let impl = FacebookSignInImpl(
            scopes: ["public_profile"],
            gateway: FakeLoginGateway(),
            graph: FakeGraph()
        ) { _ in }
        XCTAssertNotNil(impl)
    }

    // MARK: - AuthenticationImpl facebook contracts (Android parity)

    func testFacebookSignInSuccessPersistsTokenSetToFacebookVault() throws {
        // Validator-correction contract (apple parallel): a successful Facebook
        // sign-in must persist its TokenSet through the injectable hook (real
        // Keychain fails with -34018 in a bare SPM test bundle — no host-app
        // entitlement) and resolve the callback with the produced set. The stored
        // representation round-trips through the same JSON blob KeychainVault
        // persists; the vault service is `capkit.auth.facebook` (design
        // vault(provider:"facebook")).
        let tokenSet = FacebookTokenMapper.mapSignInResult(
            accessToken: "fb-at-1",
            userId: "12345",
            grantedPermissions: ["public_profile", "email"],
            declinedPermissions: ["user_photos"],
            expiresAt: 1700000123.0,
            profile: FacebookTokenMapper.mapProfile(
                id: "12345",
                name: "Grace Hopper",
                email: "grace@example.com",
                pictureUrl: "https://example.com/fb.jpg"
            )
        )

        let impl = AuthenticationImpl()
        var persisted: TokenSet?
        impl.persistFacebookSignIn = { persisted = $0 }

        let callback = CapturingSignInCallback()
        impl.handleFacebookSignInSuccess(tokenSet, callback: callback)

        XCTAssertEqual(callback.tokens, tokenSet, "The resolve must carry the produced token set")
        XCTAssertNil(callback.error)
        XCTAssertEqual(persisted, tokenSet, "The vault must receive the produced token set")

        let persistedSet = try XCTUnwrap(persisted, "The vault must have received the produced token set")
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(persistedSet))
        XCTAssertEqual(decoded.accessToken, "fb-at-1")
        XCTAssertEqual(decoded.userId, "12345")
        XCTAssertEqual(decoded.grantedPermissions, ["public_profile", "email"])
        XCTAssertEqual(decoded.declinedPermissions, ["user_photos"])
        XCTAssertEqual(decoded.expiresAt, 1700000123)
        XCTAssertEqual(decoded.user?.name, .displayName("Grace Hopper"))
        XCTAssertEqual(decoded.user?.picture, "https://example.com/fb.jpg")
        XCTAssertNil(decoded.refreshToken, "Facebook issues no refresh token")
    }

    func testRefreshTokenFacebookThrowsExactMessage() {
        let impl = AuthenticationImpl()
        XCTAssertThrowsError(try impl.refreshToken(provider: "facebook")) { error in
            guard let authError = error as? AuthenticationError else {
                XCTFail("Expected an AuthenticationError, got \(error)")
                return
            }
            XCTAssertEqual(authError.errorCode, "INVALID_INPUT")
            XCTAssertEqual(authError.message, "Facebook issues no refresh token")
        }
    }

    func testSignOutFacebookClearsVaultWithoutThrowing() {
        // signOut('facebook') = SDK logout + vault delete; the non-throwing vault
        // delete must never surface an error to the bridge.
        let impl = AuthenticationImpl()
        XCTAssertNoThrow(try impl.signOut(provider: "facebook"))
    }
}

// MARK: - Seams (speak only the plugin's own protocol types, never the SDK)

/// Fake over the `FacebookSignInGateway` seam: records launch parameters and fires
/// the configured outcome synchronously (LoginManager delivers on the main queue;
/// the fakes keep the flow tests deterministic without expectations).
private final class FakeLoginGateway: FacebookSignInGateway {
    private(set) var didLaunch = false
    private(set) var lastScopes: [String]?
    private(set) var lastPresenting: UIViewController?

    private let outcome: FacebookLoginOutcome

    init(outcome: FacebookLoginOutcome = .cancelled) {
        self.outcome = outcome
    }

    func launchLogin(
        scopes: [String],
        presenting viewController: UIViewController,
        completion: @escaping (FacebookLoginOutcome) -> Void
    ) {
        didLaunch = true
        lastScopes = scopes
        lastPresenting = viewController
        completion(outcome)
    }

    func logOut() {}
}

/// Fake over the `FacebookGraphRequesting` seam: answers the configured `/me`
/// profile result synchronously with the granted access token recorded.
private final class FakeGraph: FacebookGraphRequesting {
    private(set) var lastToken: String?

    private let result: Result<SocialAuthResultUser, AuthenticationError>

    init(profileResult: Result<SocialAuthResultUser, AuthenticationError> = .success(SocialAuthResultUser(id: "12345"))) {
        self.result = profileResult
    }

    func fetchProfile(
        token: String,
        completion: @escaping (Result<SocialAuthResultUser, AuthenticationError>) -> Void
    ) {
        lastToken = token
        completion(result)
    }
}

/// Spy over `AuthenticationImpl.SignInCallback`, capturing the resolved outcome
/// so tests can assert both the resolve and the vault side-effect of a successful
/// Facebook sign-in (mirror of the apple persistence contract test).
private final class CapturingSignInCallback: AuthenticationImpl.SignInCallback {
    private(set) var tokens: TokenSet?
    private(set) var error: AuthenticationError?

    func onResult(_ tokens: TokenSet) {
        self.tokens = tokens
    }

    func onError(_ error: AuthenticationError) {
        self.error = error
    }
}
