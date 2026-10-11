import XCTest
@testable import AuthenticationPlugin

/**
 * @file AuthenticationPluginTests.swift
 * Behavioral tests for the Apple additions to the plugin's native model:
 * `TokenSet` Codable round-trips carrying `authorizationCode`/`user`, the
* `SocialAuthResultUser` profile shape (with the vault-side `nameType`
 *  discriminator), a compile gate proving `AppleSignInImpl` exists and is
 *  constructible, and the apple-vault persistence contract: a successful Apple
 *  sign-in must persist its TokenSet to the per-provider `apple` vault (Android
 *  `appleVault.store(bundle)` parity — validator correction).
 */
class AuthenticationPluginTests: XCTestCase {

    // MARK: - TokenSet Codable with Apple fields

    func testTokenSetRoundTripsAppleAuthorizationCodeAndUser() throws {
        let set = TokenSet(
            idToken: "id-token",
            authorizationCode: "code-123",
            user: SocialAuthResultUser(
                id: "001234.aaa.bbb",
                email: "ada@example.com",
                name: .nameParts(firstName: "Ada", lastName: "Lovelace"),
                realUserStatus: "likleyRealUser"
            )
        )
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(set))
        XCTAssertEqual(decoded, set)
        XCTAssertEqual(decoded.authorizationCode, "code-123")
        XCTAssertEqual(decoded.user?.name, .nameParts(firstName: "Ada", lastName: "Lovelace"))
        XCTAssertEqual(decoded.user?.realUserStatus, "likleyRealUser")
    }

    func testTokenSetWithoutAppleFieldsRemainsGoogleShaped() throws {
        let set = TokenSet(accessToken: "at", refreshToken: "rt", idToken: "it", serverAuthCode: "sac")
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(set))
        XCTAssertEqual(decoded, set)
        XCTAssertNil(decoded.authorizationCode)
        XCTAssertNil(decoded.user)
        XCTAssertTrue(decoded.hasCredential)
    }

    func testUserNameRoundTripsDisplayName() throws {
        let user = SocialAuthResultUser(name: .displayName("Ada Lovelace"))
        let decoded = try JSONDecoder().decode(SocialAuthResultUser.self, from: JSONEncoder().encode(user))
        XCTAssertEqual(decoded.name, .displayName("Ada Lovelace"))
    }

    func testUserNameSerializesWithNameTypeDiscriminator() throws {
        let user = SocialAuthResultUser(name: .displayName("Ada Lovelace"))
        let data = try JSONEncoder().encode(user)
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let name = try XCTUnwrap(json["name"] as? [String: Any])
        XCTAssertEqual(name["nameType"] as? String, "displayName")
        XCTAssertEqual(name["value"] as? String, "Ada Lovelace")
    }

    func testUserNameNamePartsSerializationPreservesOnlyPresentParts() throws {
        let user = SocialAuthResultUser(name: .nameParts(firstName: "Ada", lastName: nil))
        let data = try JSONEncoder().encode(user)
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let name = try XCTUnwrap(json["name"] as? [String: Any])
        XCTAssertEqual(name["nameType"] as? String, "nameParts")
        XCTAssertEqual(name["firstName"] as? String, "Ada")
        XCTAssertNil(name["lastName"])
    }

    // MARK: - AppleSignInImpl compile gate

    // MARK: - facebook shared slots

    func testTokenSetRoundTripsFacebookSharedSlots() throws {
        let set = TokenSet(
            accessToken: "fb-at-1",
            userId: "12345",
            grantedPermissions: ["public_profile", "email"],
            declinedPermissions: ["user_photos"],
            expiresAt: 1700000000
        )
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(set))
        XCTAssertEqual(decoded, set)
        XCTAssertEqual(decoded.userId, "12345")
        XCTAssertEqual(decoded.grantedPermissions, ["public_profile", "email"])
        XCTAssertEqual(decoded.declinedPermissions, ["user_photos"])
        XCTAssertEqual(decoded.expiresAt, 1700000000)
    }

    func testTokenSetSharedSlotsRemainOptional() throws {
        // A google-shaped set without facebook slots decodes and re-encodes with all null.
        let set = TokenSet(accessToken: "at", refreshToken: "rt", idToken: "it", serverAuthCode: "sac")
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(set))
        XCTAssertEqual(decoded, set)
        XCTAssertNil(decoded.userId)
        XCTAssertNil(decoded.grantedPermissions)
        XCTAssertNil(decoded.declinedPermissions)
        XCTAssertNil(decoded.expiresAt)
    }

    func testLegacyTokenSetJSONDecodesWithNullSharedSlots() throws {
        // Pre-change JSON blob (no facebook slots) must decode without error.
        let legacyJSON = """
        {"accessToken":"at-1","refreshToken":"rt-1","idToken":"it-1","serverAuthCode":"sac-1"}
        """
        let decoded = try JSONDecoder().decode(TokenSet.self, from: Data(legacyJSON.utf8))
        XCTAssertEqual(decoded.accessToken, "at-1")
        XCTAssertNil(decoded.userId)
        XCTAssertNil(decoded.grantedPermissions)
        XCTAssertNil(decoded.declinedPermissions)
        XCTAssertNil(decoded.expiresAt)
    }

    func testSocialAuthResultUserPictureRoundTripsAndDefaultsNil() throws {
        let withPicture = SocialAuthResultUser(
            id: "12345",
            email: "grace@example.com",
            picture: "https://example.com/fb.jpg"
        )
        let decoded = try JSONDecoder().decode(
            SocialAuthResultUser.self,
            from: JSONEncoder().encode(withPicture)
        )
        XCTAssertEqual(decoded.picture, "https://example.com/fb.jpg")

        let withoutPicture = SocialAuthResultUser(id: "12345")
        let decodedPlain = try JSONDecoder().decode(
            SocialAuthResultUser.self,
            from: JSONEncoder().encode(withoutPicture)
        )
        XCTAssertNil(decodedPlain.picture)
    }

    func testAppleSignInImplIsConstructible() {
        let impl = AppleSignInImpl(scopes: ["name", "email"], nonce: "raw-nonce") { _ in }
        XCTAssertNotNil(impl)
    }

    // MARK: - AppleConfig configuration parity (Android AppleConfig.state)

    func testAppleConfigExposesOptionalStateForAndroidConfigParity() {
        // Android AppleConfig.kt carries `state: String?`; the iOS AppleConfig must
        // expose the same member (value injected via the `appleState` config key).
        let config = AuthenticationConfig.AppleConfig(
            clientId: "com.example.app",
            redirectURI: "https://app.example/callback",
            scopes: ["name", "email"],
            nonce: "raw-nonce",
            state: "st-1"
        )
        XCTAssertEqual(config.state, "st-1")
    }

    // MARK: - Apple sign-in vault persistence (Android parity)

    func testAppleSignInSuccessPersistsTokenSetToAppleVault() throws {
        // Validator-correction contract: a successful Apple sign-in must persist its
        // TokenSet to the per-provider `apple` vault (service `capkit.auth.apple`),
        // exactly like Android `handleAppleSignInResult` does `appleVault.store(bundle)`.
        // The persistence hook is recorded,
        // and the stored representation round-trips through the same JSON blob the
        // KeychainVault persists (JSONEncoder/Decoder) — reading the vault back must
        // return the same accessToken/authorizationCode/user the flow produced.
        //
        // NOTE: the hook is injected instead of writing the real Keychain because a
        // bare SPM test bundle has no host-app keychain entitlement — real SecItem
        // calls fail with errSecMissingEntitlement (-34018) in the test process
        // (documented in AuthenticationImpl.persistAppleSignIn).
        var tokenSet = try XCTUnwrap(
            AuthenticationUtils.mapAppleTokenSet(authorizationCode: "code-1", idToken: "id-token-1")
        )
        tokenSet.user = SocialAuthResultUser(
            id: "001234.aaa.bbb",
            email: "ada@example.com",
            name: .nameParts(firstName: "Ada", lastName: "Lovelace"),
            realUserStatus: "likleyRealUser"
        )
        let appleResult = AppleSignInImpl.AppleSignInResult(tokenSet: tokenSet, user: tokenSet.user)

        let impl = AuthenticationImpl()
        var persisted: TokenSet?
        impl.persistAppleSignIn = { persisted = $0 }

        let callback = CapturingSignInCallback()
        impl.handleAppleSignInSuccess(appleResult, callback: callback)

        XCTAssertEqual(callback.tokens, tokenSet, "The resolve must carry the produced token set")
        XCTAssertNil(callback.error)
        XCTAssertEqual(persisted, tokenSet, "The vault must receive the produced token set")

        // Vault-format round-trip: the JSON blob KeychainVault persists for the
        // `apple` service decodes back to the same accessToken/authorizationCode/user.
        let persistedSet = try XCTUnwrap(persisted, "The vault must have received the produced token set")
        let decoded = try JSONDecoder().decode(TokenSet.self, from: JSONEncoder().encode(persistedSet))
        XCTAssertEqual(decoded.accessToken, tokenSet.accessToken, "Apple issues no access token (code-flow)")
        XCTAssertEqual(decoded.authorizationCode, "code-1")
        XCTAssertEqual(decoded.idToken, "id-token-1")
        XCTAssertEqual(decoded.user, tokenSet.user)
    }
}

/// Spy over `AuthenticationImpl.SignInCallback`, capturing the resolved
/// outcome so tests can assert both the resolve and the vault side-effect
/// of a successful Apple sign-in (validator-correction contract).
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