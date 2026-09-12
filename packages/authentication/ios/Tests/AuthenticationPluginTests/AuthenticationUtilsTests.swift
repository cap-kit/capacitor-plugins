import AuthenticationServices
import XCTest
@testable import AuthenticationPlugin

/**
 * Compile-gate + behavioral tests for the iOS pure logic (AuthenticationUtils)
 * and error mapping (AuthenticationError).
 *
 * These run as a build-only gate: the test target must COMPILE under
 * `xcodebuild -scheme CapKitAuthentication`. The assertions verify real
 * behavior of the pure functions (mirroring the Android JUnit-proven contract).
 */
class AuthenticationUtilsTests: XCTestCase {

    // MARK: - PKCE verifier generation

    func testGenerateVerifierHasCorrectLengthAndUnreservedAlphabet() throws {
        let verifier = try AuthenticationUtils.generatePkceVerifier(length: 64)
        XCTAssertEqual(verifier.count, 64)
        let allowed = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        for scalar in verifier.unicodeScalars {
            XCTAssertTrue(allowed.contains(scalar), "Verifier char '\(scalar)' must be from the PKCE unreserved alphabet")
        }
    }

    func testGenerateVerifierThrowsOutsideRFC7636Range() {
        XCTAssertThrowsError(try AuthenticationUtils.generatePkceVerifier(length: 42))
        XCTAssertThrowsError(try AuthenticationUtils.generatePkceVerifier(length: 129))
    }

    func testGenerateVerifierIsRandomAcrossCalls() throws {
        let first = try AuthenticationUtils.generatePkceVerifier(length: 64)
        let second = try AuthenticationUtils.generatePkceVerifier(length: 64)
        XCTAssertNotEqual(first, second)
    }

    // MARK: - PKCE challenge

    func testGenerateChallengeIsDeterministicBase64UrlSha256() {
        // Known vector: SHA-256 of the RFC 7636 example verifier
        // `dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk`
        let verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        let challenge = AuthenticationUtils.generatePkceChallenge(verifier: verifier)
        // base64url(SHA-256(verifier)) without padding
        XCTAssertEqual(challenge, "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
        // Challenge must not carry trailing '=' padding
        XCTAssertFalse(challenge.hasSuffix("="))
    }

    func testGenerateChallengeReturnsDistinctValuesForDifferentVerifiers() {
        let a = AuthenticationUtils.generatePkceChallenge(verifier: "verifier-one")
        let b = AuthenticationUtils.generatePkceChallenge(verifier: "verifier-two")
        XCTAssertNotEqual(a, b)
    }

    // MARK: - Auth URL builder

    func testBuildAuthUrlIncludesStateAndPkce() {
        let url = AuthenticationUtils.buildAuthUrl(
            endpoint: "https://accounts.google.com/o/oauth2/v2/auth",
            clientId: "client@apps.googleusercontent.com",
            redirectUri: "https://app.example/callback",
            state: "state-value",
            codeChallenge: "challenge-value",
            scopes: ["openid", "email", "profile"]
        )
        XCTAssertTrue(url.hasPrefix("https://accounts.google.com/o/oauth2/v2/auth?"))
        XCTAssertTrue(url.contains("response_type=code"))
        XCTAssertTrue(url.contains("client_id=client%40apps.googleusercontent.com"))
        XCTAssertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.example%2Fcallback"))
        XCTAssertTrue(url.contains("scope=openid%20email%20profile"))
        XCTAssertTrue(url.contains("state=state-value"))
        XCTAssertTrue(url.contains("code_challenge=challenge-value"))
        XCTAssertTrue(url.contains("code_challenge_method=S256"))
    }

    func testBuildAuthUrlEncodesSpaceAsPercent20() {
        let url = AuthenticationUtils.buildAuthUrl(
            endpoint: "https://example.com/auth",
            clientId: "id",
            redirectUri: "https://app.example/cb",
            state: "s",
            codeChallenge: "c",
            scopes: ["a", "b", "c"]
        )
        XCTAssertTrue(url.contains("scope=a%20b%20c"), "Scopes must be space-delimited and %20-encoded")
        XCTAssertFalse(url.contains("scope=a+b+c"), "URL encoding must not use the + space form")
    }

    // MARK: - Token mapper

    func testTokenMapOmitsAbsentOptionalFields() {
        let map = AuthenticationUtils.mapTokens(
            accessToken: "at",
            refreshToken: nil,
            idToken: "it",
            serverAuthCode: nil
        )
        XCTAssertEqual(map["accessToken"], "at")
        XCTAssertEqual(map["idToken"], "it")
        XCTAssertNil(map["refreshToken"])
        XCTAssertNil(map["serverAuthCode"])
    }

    func testTokenMapIncludesAllFieldsWhenPresent() {
        let map = AuthenticationUtils.mapTokens(
            accessToken: "at",
            refreshToken: "rt",
            idToken: "it",
            serverAuthCode: "sac"
        )
        XCTAssertEqual(map["accessToken"], "at")
        XCTAssertEqual(map["refreshToken"], "rt")
        XCTAssertEqual(map["idToken"], "it")
        XCTAssertEqual(map["serverAuthCode"], "sac")
    }

    // MARK: - Storage key provider

    func testStorageKeyNamespacedByProviderAndKind() {
        XCTAssertEqual(
            AuthenticationUtils.storageKey(provider: "google", kind: .access),
            "auth_google_access"
        )
        XCTAssertEqual(
            AuthenticationUtils.storageKey(provider: "google", kind: .refresh),
            "auth_google_refresh"
        )
        XCTAssertEqual(
            AuthenticationUtils.storageKey(provider: "google", kind: .id),
            "auth_google_id"
        )
        XCTAssertEqual(
            AuthenticationUtils.storageKey(provider: "google", kind: .serverAuthCode),
            "auth_google_server_auth_code"
        )
    }

    func testStorageKeyIsolatedPerProvider() {
        XCTAssertNotEqual(
            AuthenticationUtils.storageKey(provider: "google", kind: .access),
            AuthenticationUtils.storageKey(provider: "microsoft", kind: .access)
        )
    }

    // MARK: - Error code mapping

    func testUserCancelledMapsToUSER_CANCELLEDErrorCode() {
        let error = AuthenticationError.userCancelled("Google sign-in was cancelled.")
        XCTAssertEqual(error.errorCode, "USER_CANCELLED")
    }

    func testErrorCodesMatchJSParityContract() {
        XCTAssertEqual(AuthenticationError.initFailed("x").errorCode, "INIT_FAILED")
        XCTAssertEqual(AuthenticationError.invalidInput("x").errorCode, "INVALID_INPUT")
        XCTAssertEqual(AuthenticationError.unavailable("x").errorCode, "UNAVAILABLE")
        XCTAssertEqual(AuthenticationError.cancelled("x").errorCode, "CANCELLED")
        XCTAssertEqual(AuthenticationError.timeout("x").errorCode, "TIMEOUT")
    }

    // MARK: - Apple nonce (Sign in with Apple)

    func testGenerateNonceHasExpectedLengthAndSafeAlphabet() throws {
        let nonce = try AuthenticationUtils.generateNonce(length: 32)
        XCTAssertEqual(nonce.count, 32)
        let allowed = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
        for scalar in nonce.unicodeScalars {
            XCTAssertTrue(allowed.contains(scalar), "Nonce char '\(scalar)' must be URL-safe alphanumeric")
        }
    }

    func testGenerateNonceThrowsOutsideSupportedRange() {
        XCTAssertThrowsError(try AuthenticationUtils.generateNonce(length: 15))
        XCTAssertThrowsError(try AuthenticationUtils.generateNonce(length: 129))
    }

    func testGenerateNonceIsRandomAcrossCalls() throws {
        let first = try AuthenticationUtils.generateNonce()
        let second = try AuthenticationUtils.generateNonce()
        XCTAssertNotEqual(first, second)
    }

    // MARK: - Apple nonce hashing

    func testSha256Base64URLMatchesPkceChallengeVector() {
        // base64url(SHA-256(verifier)) — same vector as the PKCE challenge test
        XCTAssertEqual(
            AuthenticationUtils.sha256Base64URL("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        )
    }

    // MARK: - Apple ID token claim decoding

    func testDecodeJWTClaimsExtractsNonceClaim() throws {
        let idToken = Self.makeIDToken(nonceClaim: "hashed-nonce", extra: ["email": "ada@example.com"])
        let claims = try XCTUnwrap(AuthenticationUtils.decodeJWTClaims(idToken))
        XCTAssertEqual(claims["nonce"] as? String, "hashed-nonce")
        XCTAssertEqual(claims["email"] as? String, "ada@example.com")
    }

    func testDecodeJWTClaimsReturnsNilForMalformedTokens() {
        XCTAssertNil(AuthenticationUtils.decodeJWTClaims("not-a-jwt"))
        XCTAssertNil(AuthenticationUtils.decodeJWTClaims("header.payload"))
        XCTAssertNil(AuthenticationUtils.decodeJWTClaims(""))
    }

    // MARK: - Apple ID token nonce validation

    func testValidateIDTokenNonceAcceptsMatchingClaim() throws {
        let hash = AuthenticationUtils.sha256Base64URL("raw-nonce-value")
        let idToken = Self.makeIDToken(nonceClaim: hash)
        XCTAssertNil(AuthenticationUtils.validateIDTokenNonce(idToken: idToken, expectedHash: hash))
    }

    func testValidateIDTokenNonceRejectsMismatchedClaim() throws {
        let idToken = Self.makeIDToken(nonceClaim: "attacker-injected-nonce")
        let error = AuthenticationUtils.validateIDTokenNonce(
            idToken: idToken,
            expectedHash: AuthenticationUtils.sha256Base64URL("issued-nonce")
        )
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "OpenID nonce mismatch.")
    }

    func testValidateIDTokenNonceRejectsMissingClaim() throws {
        let idToken = Self.makeIDToken(nonceClaim: nil)
        let error = AuthenticationUtils.validateIDTokenNonce(
            idToken: idToken,
            expectedHash: AuthenticationUtils.sha256Base64URL("issued-nonce")
        )
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "OpenID nonce claim is missing.")
    }

    func testValidateIDTokenNonceRejectsMalformedToken() {
        let error = AuthenticationUtils.validateIDTokenNonce(idToken: "not-a-jwt", expectedHash: "hash")
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
    }

    // MARK: - Apple error mapping

    func testMapAppleErrorCancellationMapsToUSER_CANCELLED() {
        let error = AuthenticationUtils.mapAppleError(.userCancelled)
        XCTAssertEqual(error.errorCode, "USER_CANCELLED")
        XCTAssertEqual(error.message, "The user cancelled the Apple sign-in flow.")
    }

    func testMapAppleErrorNonceAndStateMapToINVALID_INPUT() {
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.invalidNonce).errorCode, "INVALID_INPUT")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.invalidNonce).message, "The Apple sign-in returned an invalid nonce.")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.invalidState).errorCode, "INVALID_INPUT")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.invalidState).message, "The Apple sign-in returned an invalid state.")
    }

    func testMapAppleErrorRemainingFailuresMapToINIT_FAILED() {
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.missingConfiguration).errorCode, "INIT_FAILED")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.missingConfiguration).message, "Apple configuration is missing or malformed.")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.network).errorCode, "INIT_FAILED")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.network).message, "The Apple sign-in flow failed over the network.")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.unknown).errorCode, "INIT_FAILED")
        XCTAssertEqual(AuthenticationUtils.mapAppleError(.unknown).message, "The Apple sign-in flow failed for an unknown reason.")
    }

    // MARK: - Apple user profile mapping

    func testMapUserDetectionStatusPreservesAppleSpellings() {
        XCTAssertEqual(AuthenticationUtils.mapUserDetectionStatus(.likelyReal), "likleyRealUser")
        XCTAssertEqual(AuthenticationUtils.mapUserDetectionStatus(.unknown), "unknown")
        XCTAssertEqual(AuthenticationUtils.mapUserDetectionStatus(.unsupported), "unsupported")
    }

    func testMapUserReturnsNilWhenNoProfileFieldsAreShared() {
        XCTAssertNil(
            AuthenticationUtils.mapUser(
                id: nil,
                email: nil,
                givenName: nil,
                familyName: nil,
                detectionStatus: .unknown
            )
        )
    }

    func testMapUserPopulatesNamePartsAndRealUserStatus() throws {
        let user = try XCTUnwrap(
            AuthenticationUtils.mapUser(
                id: "001234.aaa.bbb",
                email: "ada@example.com",
                givenName: "Ada",
                familyName: "Lovelace",
                detectionStatus: .likelyReal
            )
        )
        XCTAssertEqual(user.id, "001234.aaa.bbb")
        XCTAssertEqual(user.email, "ada@example.com")
        XCTAssertEqual(user.name, .nameParts(firstName: "Ada", lastName: "Lovelace"))
        XCTAssertEqual(user.realUserStatus, "likleyRealUser")
    }

    func testMapUserOmitsNameWhenNoNamePartsAreShared() throws {
        let user = try XCTUnwrap(
            AuthenticationUtils.mapUser(
                id: "001234.aaa.bbb",
                email: nil,
                givenName: nil,
                familyName: nil,
                detectionStatus: .unsupported
            )
        )
        XCTAssertNil(user.name)
        XCTAssertEqual(user.realUserStatus, "unsupported")
    }

    // MARK: - Apple token set mapping

    func testMapAppleTokenSetBuildsCredentialSet() throws {
        let set = try XCTUnwrap(
            AuthenticationUtils.mapAppleTokenSet(authorizationCode: "code-123", idToken: "id-token")
        )
        XCTAssertEqual(set.authorizationCode, "code-123")
        XCTAssertEqual(set.idToken, "id-token")
        XCTAssertNil(set.accessToken)
        XCTAssertNil(set.refreshToken)
        XCTAssertNil(set.serverAuthCode)
        XCTAssertFalse(set.hasCredential, "Apple issues no native access token")
    }

    func testMapAppleTokenSetReturnsNilWithoutCredentialMaterial() {
        XCTAssertNil(AuthenticationUtils.mapAppleTokenSet(authorizationCode: nil, idToken: nil))
    }

    // MARK: - Apple bridge dictionary mapping

    func testMapSocialAuthUserEmitsPlainNameWithoutDiscriminator() throws {
        let user = SocialAuthResultUser(
            id: "001234.aaa.bbb",
            email: nil,
            name: .nameParts(firstName: "Ada", lastName: "Lovelace"),
            realUserStatus: "likleyRealUser"
        )
        let dict = try XCTUnwrap(AuthenticationUtils.mapSocialAuthUser(user))
        XCTAssertEqual(dict["id"] as? String, "001234.aaa.bbb")
        XCTAssertNil(dict["email"])
        let name = try XCTUnwrap(dict["name"] as? [String: String])
        XCTAssertEqual(name["firstName"], "Ada")
        XCTAssertEqual(name["lastName"], "Lovelace")
        XCTAssertNil(name["nameType"], "Bridge emission must not carry the vault discriminator")
        XCTAssertEqual(dict["realUserStatus"] as? String, "likleyRealUser")
    }

    func testMapSocialAuthUserReturnsNilForEmptyProfile() {
        XCTAssertNil(AuthenticationUtils.mapSocialAuthUser(SocialAuthResultUser()))
    }

    func testMapAppleTokensOmitsAbsentFields() throws {
        let set = try XCTUnwrap(AuthenticationUtils.mapAppleTokenSet(authorizationCode: "code", idToken: nil))
        let tokens = AuthenticationUtils.mapAppleTokens(set)
        XCTAssertEqual(tokens["authorizationCode"] as? String, "code")
        XCTAssertNil(tokens["idToken"])
        XCTAssertNil(tokens["user"])
    }

    func testMapAppleTokensIncludesUserWhenPresent() throws {
        var set = try XCTUnwrap(AuthenticationUtils.mapAppleTokenSet(authorizationCode: "code", idToken: "it"))
        set.user = SocialAuthResultUser(name: .displayName("Ada Lovelace"))
        let tokens = AuthenticationUtils.mapAppleTokens(set)
        XCTAssertEqual(tokens["idToken"] as? String, "it")
        let user = try XCTUnwrap(tokens["user"] as? [String: Any])
        XCTAssertEqual(user["name"] as? String, "Ada Lovelace")
    }

    // MARK: - Test helpers

    /// Builds a synthetic JWT with the given nonce claim (never signature-verified).
    private static func makeIDToken(nonceClaim: String?, extra: [String: Any] = [:]) -> String {
        var payload = extra
        if let nonceClaim {
            payload["nonce"] = nonceClaim
        }
        let jsonData = (try? JSONSerialization.data(withJSONObject: payload)) ?? Data("{}".utf8)
        let body = String(data: jsonData, encoding: .utf8) ?? "{}"
        return "\(base64URLEncode(#"{"alg":"none"}"#)).\(base64URLEncode(body)).signature"
    }

    private static func base64URLEncode(_ value: String) -> String {
        Data(value.utf8)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}