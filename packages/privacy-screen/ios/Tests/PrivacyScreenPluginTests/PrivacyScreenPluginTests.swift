import XCTest
@testable import PrivacyScreenPlugin

/**
 Unit tests for the PrivacyScreen plugin's pure native logic.

 Covered headless (no window, no view hierarchy):
 - native version constant parity with package.json
 - native error code and message mapping
 - canonical error message strings (cross-platform parity lock)
 - blur-effect session-option validation and blur-style mapping
 - protection state transitions of the implementation

 The overlay presentation itself (key-window lookup, view hierarchy,
 snapshot timing, fade-out) is UI-bound and cannot be asserted in
 in-process tests; it is verified on-device via the example app. These
 tests intentionally never touch windows or views.
 */
final class PrivacyScreenPluginTests: XCTestCase {

    // MARK: - Version

    func testPluginVersionMatchesPackageJson() {
        XCTAssertEqual(PluginVersion.number, "8.0.0")
    }

    // MARK: - Error codes

    func testErrorCodesForEveryErrorCase() {
        XCTAssertEqual(PrivacyScreenError.unavailable("x").errorCode, "UNAVAILABLE")
        XCTAssertEqual(PrivacyScreenError.cancelled("x").errorCode, "CANCELLED")
        XCTAssertEqual(PrivacyScreenError.permissionDenied("x").errorCode, "PERMISSION_DENIED")
        XCTAssertEqual(PrivacyScreenError.initFailed("x").errorCode, "INIT_FAILED")
        XCTAssertEqual(PrivacyScreenError.invalidInput("x").errorCode, "INVALID_INPUT")
        XCTAssertEqual(PrivacyScreenError.unknownType("x").errorCode, "UNKNOWN_TYPE")
        XCTAssertEqual(PrivacyScreenError.notFound("x").errorCode, "NOT_FOUND")
        XCTAssertEqual(PrivacyScreenError.conflict("x").errorCode, "CONFLICT")
        XCTAssertEqual(PrivacyScreenError.timeout("x").errorCode, "TIMEOUT")
    }

    func testErrorMessagePassesThroughVerbatim() {
        let message = "custom native failure detail"
        XCTAssertEqual(PrivacyScreenError.invalidInput(message).message, message)
        XCTAssertEqual(PrivacyScreenError.timeout(message).message, message)
    }

    // MARK: - Canonical error messages (cross-platform parity lock)

    /**
     These literals MUST stay identical to the Android constants in
     PrivacyScreenErrorMessages (asserted on the Android side too).
     */
    func testCanonicalErrorMessagesMatchAndroid() {
        XCTAssertEqual(PrivacyScreenErrorMessages.unavailable, "Feature is unavailable on this device or configuration.")
        XCTAssertEqual(PrivacyScreenErrorMessages.cancelled, "Operation was cancelled.")
        XCTAssertEqual(PrivacyScreenErrorMessages.permissionDenied, "Required permission is denied or not granted.")
        XCTAssertEqual(PrivacyScreenErrorMessages.initFailed, "Native initialization failed.")
        XCTAssertEqual(PrivacyScreenErrorMessages.invalidInput, "Invalid or missing input parameters.")
        XCTAssertEqual(PrivacyScreenErrorMessages.unknownType, "Unsupported or invalid input type.")
        XCTAssertEqual(PrivacyScreenErrorMessages.notFound, "Requested resource not found.")
        XCTAssertEqual(PrivacyScreenErrorMessages.conflict, "Operation conflict with current state.")
        XCTAssertEqual(PrivacyScreenErrorMessages.timeout, "Operation timed out.")
        XCTAssertEqual(PrivacyScreenErrorMessages.internalError, "Internal error.")
        XCTAssertEqual(PrivacyScreenErrorMessages.unexpectedNativeError, "Unexpected native error in plugin operation.")
    }

    // MARK: - Blur-effect validation

    func testIsValidBlurEffectAcceptsDocumentedValues() {
        XCTAssertTrue(PrivacyScreenValidators.isValidBlurEffect("light"))
        XCTAssertTrue(PrivacyScreenValidators.isValidBlurEffect("dark"))
        XCTAssertTrue(PrivacyScreenValidators.isValidBlurEffect("none"))
    }

    func testIsValidBlurEffectRejectsNilAndUnknownValues() {
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect(nil))
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect("system"))
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect("vivid"))
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect(""))
    }

    func testIsValidBlurEffectIsCaseSensitive() {
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect("Light"))
        XCTAssertFalse(PrivacyScreenValidators.isValidBlurEffect("DARK"))
    }

    // MARK: - Blur-style mapping

    func testBlurStyleMapsLight() {
        XCTAssertEqual(
            PrivacyScreenUtils.blurStyle(for: "light"),
            .systemMaterialLight
        )
    }

    func testBlurStyleMapsDark() {
        XCTAssertEqual(
            PrivacyScreenUtils.blurStyle(for: "dark"),
            .systemMaterialDark
        )
    }

    func testBlurStyleFallsBackToSystemMaterial() {
        // `none` maps to the neutral system style (not the launch storyboard).
        XCTAssertEqual(
            PrivacyScreenUtils.blurStyle(for: "none"),
            .systemMaterial
        )
        // Unknown values default to the same neutral style.
        XCTAssertEqual(
            PrivacyScreenUtils.blurStyle(for: "anything-else"),
            .systemMaterial
        )
        // The internal default behaves like the fallback too.
        XCTAssertEqual(
            PrivacyScreenUtils.blurStyle(for: "system"),
            .systemMaterial
        )
    }

    // MARK: - Protection state

    func testProtectionStartsDisabled() {
        let impl = PrivacyScreenImpl()
        XCTAssertFalse(impl.isEnabled())
        XCTAssertEqual(impl.activeBlurEffect, "system")
    }

    func testEnableMarksProtectionEnabled() {
        let impl = PrivacyScreenImpl()
        impl.enable(blurEffect: "dark")
        XCTAssertTrue(impl.isEnabled())
        XCTAssertEqual(impl.activeBlurEffect, "dark")
    }

    func testDisableClearsProtection() {
        let impl = PrivacyScreenImpl()
        impl.enable(blurEffect: "light")
        impl.disable()
        XCTAssertFalse(impl.isEnabled())
    }

    func testEnableKeepsPreviousStyleWhenValueIsUnknown() {
        let impl = PrivacyScreenImpl()
        impl.enable(blurEffect: "dark")
        impl.enable(blurEffect: "vivid")
        XCTAssertTrue(impl.isEnabled())
        XCTAssertEqual(impl.activeBlurEffect, "dark")
    }

    func testEnableKeepsPreviousStyleWhenValueIsNil() {
        let impl = PrivacyScreenImpl()
        impl.enable(blurEffect: "light")
        impl.enable(blurEffect: nil)
        XCTAssertTrue(impl.isEnabled())
        XCTAssertEqual(impl.activeBlurEffect, "light")
    }
}
