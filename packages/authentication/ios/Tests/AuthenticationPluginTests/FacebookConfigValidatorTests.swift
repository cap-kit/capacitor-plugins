import XCTest
@testable import AuthenticationPlugin

/**
 * @file FacebookConfigValidatorTests.swift
 * Behavioral tests for the pure Facebook configuration validator (iOS), mirroring
 * the Android `FacebookConfigValidator` (cross-platform config parity gate).
 *
 * Written STRICTLY FIRST (RED): the validator type did not exist, so the test
 * target failed to compile with `cannot find 'FacebookConfigValidator' in scope`
 * (and facebook types in `AuthenticationConfig`) until the production file landed.
 */
class FacebookConfigValidatorTests: XCTestCase {

    // MARK: - Fixture

    private func makeConfig(
        appId: String = "123456789012345",
        clientToken: String? = nil,
        version: String = "v17.0",
        scopes: [String] = ["public_profile", "email"]
    ) -> AuthenticationConfig.FacebookConfig {
        AuthenticationConfig.FacebookConfig(
            facebookAppId: appId,
            facebookClientToken: clientToken,
            facebookVersion: version,
            scopes: scopes
        )
    }

    // MARK: - Validation semantics (Android parity)

    func testValidateAcceptsCompleteFacebookConfig() {
        XCTAssertNil(FacebookConfigValidator.validate(makeConfig()))
    }

    func testValidateRejectsAbsentConfigWithInitFailed() {
        let error = FacebookConfigValidator.validate(nil)
        XCTAssertEqual(error?.errorCode, "INIT_FAILED")
        XCTAssertEqual(error?.message, "Missing 'facebook' configuration; provide appId.")
    }

    func testValidateRejectsBlankAppIdWithAndroidMessage() {
        let error = FacebookConfigValidator.validate(makeConfig(appId: "  "))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "facebook.appId must not be blank.")
    }

    func testValidateRejectsEmptyAppIdWithAndroidMessage() {
        let error = FacebookConfigValidator.validate(makeConfig(appId: ""))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "facebook.appId must not be blank.")
    }

    func testValidateRejectsBlankClientTokenWithAndroidMessage() {
        let error = FacebookConfigValidator.validate(makeConfig(clientToken: "   "))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "facebook.clientToken must not be blank.")
    }

    func testValidateAllowsAbsentClientToken() {
        XCTAssertNil(FacebookConfigValidator.validate(makeConfig(clientToken: nil)))
    }

    func testValidateRejectsExplicitEmptyScopesWithAndroidMessage() {
        let error = FacebookConfigValidator.validate(makeConfig(scopes: []))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "facebook.scopes must not be empty.")
    }
}
