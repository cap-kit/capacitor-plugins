import XCTest
@testable import AuthenticationPlugin

/**
 * @file AppleConfigValidatorTests.swift
 * Behavioral tests for the pure Apple configuration validator (iOS), mirroring
 * the Android `AppleConfigValidator` (cross-platform config
 * parity gate).
 *
 * Written STRICTLY FIRST (RED): the validator type did not exist, so the test
 * target failed to compile with `cannot find 'AppleConfigValidator' in scope`
 * until the production file landed.
 */
class AppleConfigValidatorTests: XCTestCase {

    // MARK: - Fixture

    private func makeConfig(
        clientId: String = "com.example.app",
        redirectURI: String? = "https://app.example/callback",
        scopes: [String] = ["name", "email"],
        nonce: String? = nil,
        state: String? = nil
    ) -> AuthenticationConfig.AppleConfig {
        AuthenticationConfig.AppleConfig(
            clientId: clientId,
            redirectURI: redirectURI,
            scopes: scopes,
            nonce: nonce,
            state: state
        )
    }

    // MARK: - Validation semantics (Android parity)

    func testValidateAcceptsCompleteAppleConfig() {
        XCTAssertNil(AppleConfigValidator.validate(makeConfig()))
    }

    func testValidateRejectsBlankClientIdWithAndroidMessage() {
        let error = AppleConfigValidator.validate(makeConfig(clientId: "  "))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "apple.clientId must not be blank.")
    }

    func testValidateRejectsBlankRedirectURIWithAndroidMessage() {
        let error = AppleConfigValidator.validate(makeConfig(redirectURI: ""))
        XCTAssertEqual(error?.errorCode, "INVALID_INPUT")
        XCTAssertEqual(error?.message, "apple.redirectURI must not be blank.")
    }

    func testValidateAllowsAbsentRedirectURIForAndroidParity() {
        // Android AppleConfigValidator checks redirectURI blank-IF-present (nil
        // passes). The native Sign in with Apple flow never uses redirectURI —
        // it is a Web/Android artifact — so iOS must not require it.
        XCTAssertNil(AppleConfigValidator.validate(makeConfig(redirectURI: nil)))
    }

    func testValidateIgnoresNonceAndStateValues() {
        XCTAssertNil(AppleConfigValidator.validate(makeConfig(nonce: "n", state: "st-1")))
    }
}