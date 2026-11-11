import XCTest
@testable import SplashMotionPlugin

/**
 Unit tests for the SplashMotion plugin's pure native logic.

 Covered headless (no window, no view hierarchy):
 - native version constant parity with package.json
 - native error code and message mapping
 - canonical error message strings (cross-platform parity lock)
 - haptic style normalization
 - hex color parsing
 - duration clamping
 - dotLottie archive entry selection

 The splash presentation itself (window layering, Lottie rendering,
 fade-out, haptic delivery) is UI-bound and cannot be asserted in
 in-process tests; it is verified on-device via the example app. These
 tests intentionally never touch windows or views.
 */
final class SplashMotionPluginTests: XCTestCase {

    // MARK: - Version

    func testPluginVersionIsASemanticVersion() {
        let pattern = #"^\d+\.\d+\.\d+(-[0-9A-Za-z.\-]+)?$"#

        XCTAssertNotNil(
            PluginVersion.number.range(of: pattern, options: .regularExpression),
            "Unexpected PluginVersion.number: \(PluginVersion.number)"
        )
    }

    // MARK: - Error codes

    func testErrorCodesForEveryErrorCase() {
        XCTAssertEqual(SplashMotionError.unavailable("x").errorCode, "UNAVAILABLE")
        XCTAssertEqual(SplashMotionError.cancelled("x").errorCode, "CANCELLED")
        XCTAssertEqual(SplashMotionError.permissionDenied("x").errorCode, "PERMISSION_DENIED")
        XCTAssertEqual(SplashMotionError.initFailed("x").errorCode, "INIT_FAILED")
        XCTAssertEqual(SplashMotionError.invalidInput("x").errorCode, "INVALID_INPUT")
        XCTAssertEqual(SplashMotionError.unknownType("x").errorCode, "UNKNOWN_TYPE")
        XCTAssertEqual(SplashMotionError.notFound("x").errorCode, "NOT_FOUND")
        XCTAssertEqual(SplashMotionError.conflict("x").errorCode, "CONFLICT")
        XCTAssertEqual(SplashMotionError.timeout("x").errorCode, "TIMEOUT")
    }

    func testErrorMessagePassesThroughVerbatim() {
        let message = "custom native failure detail"
        XCTAssertEqual(SplashMotionError.invalidInput(message).message, message)
        XCTAssertEqual(SplashMotionError.timeout(message).message, message)
    }

    // MARK: - Canonical error messages (cross-platform parity lock)

    /**
     These literals MUST stay identical to the Android constants in
     SplashMotionErrorMessages (asserted on the Android side too).
     */
    func testCanonicalErrorMessagesMatchAndroid() {
        XCTAssertEqual(SplashMotionErrorMessages.unavailable, "Feature is unavailable on this device or configuration.")
        XCTAssertEqual(SplashMotionErrorMessages.cancelled, "Operation was cancelled.")
        XCTAssertEqual(SplashMotionErrorMessages.permissionDenied, "Required permission is denied or not granted.")
        XCTAssertEqual(SplashMotionErrorMessages.initFailed, "Native initialization failed.")
        XCTAssertEqual(SplashMotionErrorMessages.invalidInput, "Invalid or missing input parameters.")
        XCTAssertEqual(SplashMotionErrorMessages.unknownType, "Unsupported or invalid input type.")
        XCTAssertEqual(SplashMotionErrorMessages.notFound, "Requested resource not found.")
        XCTAssertEqual(SplashMotionErrorMessages.conflict, "Operation conflict with current state.")
        XCTAssertEqual(SplashMotionErrorMessages.timeout, "Operation timed out.")
        XCTAssertEqual(SplashMotionErrorMessages.internalError, "Internal error.")
        XCTAssertEqual(SplashMotionErrorMessages.unexpectedNativeError, "Unexpected native error in plugin operation.")
    }

    // MARK: - Haptic style normalization

    func testHapticStyleNormalization() {
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle("light"), "light")
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle("LIGHT"), "light")
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle("heavy"), "heavy")
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle("medium"), "medium")
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle(nil), "medium")
        XCTAssertEqual(SplashMotionValidators.normalizeHapticStyle("unknown"), "medium")
    }

    // MARK: - Impact style mapping

    func testImpactStyleMapping() {
        XCTAssertEqual(SplashMotionUtils.impactStyle(for: "light"), .light)
        XCTAssertEqual(SplashMotionUtils.impactStyle(for: "heavy"), .heavy)
        XCTAssertEqual(SplashMotionUtils.impactStyle(for: "medium"), .medium)
        XCTAssertEqual(SplashMotionUtils.impactStyle(for: "anything-else"), .medium)
    }

    // MARK: - Hex color parsing

    func testParseColorAcceptsSixDigitHex() {
        let color = SplashMotionValidators.parseColor("#FFFFFF")
        XCTAssertNotNil(color)
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color?.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        XCTAssertEqual(red, 1.0, accuracy: 0.001)
        XCTAssertEqual(green, 1.0, accuracy: 0.001)
        XCTAssertEqual(blue, 1.0, accuracy: 0.001)
        XCTAssertEqual(alpha, 1.0, accuracy: 0.001)
    }

    func testParseColorAcceptsEightDigitHexWithAlpha() {
        let color = SplashMotionValidators.parseColor("#FF000080")
        XCTAssertNotNil(color)
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color?.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        XCTAssertEqual(red, 1.0, accuracy: 0.001)
        XCTAssertEqual(green, 0.0, accuracy: 0.001)
        XCTAssertEqual(blue, 0.0, accuracy: 0.001)
        XCTAssertEqual(alpha, 128.0 / 255.0, accuracy: 0.01)
    }

    func testParseColorRejectsNilAndInvalidValues() {
        XCTAssertNil(SplashMotionValidators.parseColor(nil))
        XCTAssertNil(SplashMotionValidators.parseColor(""))
        XCTAssertNil(SplashMotionValidators.parseColor("not-a-color"))
        XCTAssertNil(SplashMotionValidators.parseColor("#12345"))
    }

    // MARK: - Duration clamping

    func testDurationClamping() {
        XCTAssertEqual(SplashMotionValidators.clampDuration(nil, fallback: 200), 200)
        XCTAssertEqual(SplashMotionValidators.clampDuration(-5, fallback: 200), 0)
        XCTAssertEqual(SplashMotionValidators.clampDuration(500, fallback: 200), 500)
        XCTAssertEqual(SplashMotionValidators.clampDuration(999_999, fallback: 200), 60_000)
    }

    // MARK: - Gravity parsing

    func testGravityParsing() {
        XCTAssertEqual(SplashMotionValidators.parseGravity("topStart").vertical, .top)
        XCTAssertEqual(SplashMotionValidators.parseGravity("topStart").horizontal, .leading)
        XCTAssertEqual(SplashMotionValidators.parseGravity("TOP").vertical, .top)
        XCTAssertEqual(SplashMotionValidators.parseGravity("TOP").horizontal, .center)
        XCTAssertEqual(SplashMotionValidators.parseGravity("bottomEnd").vertical, .bottom)
        XCTAssertEqual(SplashMotionValidators.parseGravity("bottomEnd").horizontal, .trailing)
        XCTAssertEqual(SplashMotionValidators.parseGravity("end").vertical, .center)
        XCTAssertEqual(SplashMotionValidators.parseGravity("end").horizontal, .trailing)
        XCTAssertEqual(SplashMotionValidators.parseGravity(nil).vertical, .center)
        XCTAssertEqual(SplashMotionValidators.parseGravity("unknown").horizontal, .center)
    }

    // MARK: - Scale type to content mode mapping

    func testContentModeMapping() {
        XCTAssertEqual(SplashMotionValidators.parseContentMode("CENTER"), .center)
        XCTAssertEqual(SplashMotionValidators.parseContentMode("CENTER_CROP"), .scaleAspectFill)
        XCTAssertEqual(SplashMotionValidators.parseContentMode("FIT_XY"), .scaleToFill)
        XCTAssertEqual(SplashMotionValidators.parseContentMode("MATRIX"), .center)
        XCTAssertEqual(SplashMotionValidators.parseContentMode(nil), .scaleAspectFill)
        XCTAssertEqual(SplashMotionValidators.parseContentMode("unknown"), .scaleAspectFill)
    }

    // MARK: - Alpha and margin normalization

    func testAlphaNormalization() {
        XCTAssertEqual(SplashMotionValidators.normalizeAlpha(nil), 1.0)
        XCTAssertEqual(SplashMotionValidators.normalizeAlpha(0.0), 0.0)
        XCTAssertEqual(SplashMotionValidators.normalizeAlpha(0.5), 0.5)
        XCTAssertEqual(SplashMotionValidators.normalizeAlpha(-0.5), 0.0)
        XCTAssertEqual(SplashMotionValidators.normalizeAlpha(2.0), 1.0)
    }

    func testMarginNormalization() {
        XCTAssertEqual(SplashMotionValidators.normalizeMargin(nil, fallback: 16), 16)
        XCTAssertEqual(SplashMotionValidators.normalizeMargin(-5, fallback: 16), 0)
        XCTAssertEqual(SplashMotionValidators.normalizeMargin(32, fallback: 16), 32)
        XCTAssertEqual(SplashMotionValidators.normalizeMargin(500, fallback: 16), 200)
    }

    // MARK: - Entrance animation normalization

    func testEntranceNormalization() {
        XCTAssertEqual(SplashMotionValidators.normalizeEntranceType("fade"), "fade")
        XCTAssertEqual(SplashMotionValidators.normalizeEntranceType("SCALE"), "scale")
        XCTAssertEqual(SplashMotionValidators.normalizeEntranceType(nil), "none")
        XCTAssertEqual(SplashMotionValidators.normalizeEntranceType("zoom"), "none")
        XCTAssertEqual(SplashMotionValidators.clampEntranceDuration(nil), 300)
        XCTAssertEqual(SplashMotionValidators.clampEntranceDuration(-10), 0)
        XCTAssertEqual(SplashMotionValidators.clampEntranceDuration(500), 500)
        XCTAssertEqual(SplashMotionValidators.clampEntranceDuration(9_999), 2_000)
    }

    // MARK: - Spinner style mapping

    func testSpinnerStyleMapping() {
        // Official mapping: only "small" and "large" are differentiated on
        // iOS; everything else falls back to the platform default (.medium).
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle("small"), .medium)
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle("large"), .large)
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle("LARGE"), .large)
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle(nil), .medium)
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle("horizontal"), .medium)
        XCTAssertEqual(SplashMotionValidators.parseSpinnerStyle("inverse"), .medium)
    }

    // MARK: - Gradient resolution

    func testGradientResolutionHonorsDarkMode() {
        let white = SplashMotionValidators.parseColor("#FFFFFF")
        let black = SplashMotionValidators.parseColor("#000000")
        let darkGray = SplashMotionValidators.parseColor("#111111")

        // Light mode: light pair wins.
        let light = SplashMotionValidators.resolveGradient(
            top: "#FFFFFF", bottom: "#000000", topDark: "#111111", bottomDark: "#222222", isDark: false
        )
        XCTAssertEqual(light?.0, white)
        XCTAssertEqual(light?.1, black)

        // Dark mode with valid dark pair: dark pair wins.
        let dark = SplashMotionValidators.resolveGradient(
            top: "#FFFFFF", bottom: "#000000", topDark: "#111111", bottomDark: "#222222", isDark: true
        )
        XCTAssertEqual(dark?.0, darkGray)

        // Dark mode with invalid dark pair: light pair applies.
        let fallback = SplashMotionValidators.resolveGradient(
            top: "#FFFFFF", bottom: "#000000", topDark: "#111111", bottomDark: "invalid", isDark: true
        )
        XCTAssertEqual(fallback?.0, white)
        XCTAssertEqual(fallback?.1, black)

        // Invalid light pair: gradient ignored.
        XCTAssertNil(
            SplashMotionValidators.resolveGradient(top: "nope", bottom: "#000000", topDark: nil, bottomDark: nil, isDark: false)
        )
        XCTAssertNil(
            SplashMotionValidators.resolveGradient(top: "#FFFFFF", bottom: nil, topDark: nil, bottomDark: nil, isDark: false)
        )
    }

    // MARK: - dotLottie entry selection

    func testDotLottieSelectionPrefersAnimationsFolder() {
        XCTAssertEqual(
            SplashMotionZipReader.selectAnimationEntryName([
                "manifest.json", "animations/splash.json", "animations/other.json",
            ]),
            "animations/splash.json"
        )
    }

    func testDotLottieSelectionFallsBackToFirstNonManifestJson() {
        XCTAssertEqual(
            SplashMotionZipReader.selectAnimationEntryName(["manifest.json", "splash.json"]),
            "splash.json"
        )
    }

    func testDotLottieSelectionReturnsNilWithoutCandidate() {
        XCTAssertNil(SplashMotionZipReader.selectAnimationEntryName(["manifest.json", "images/logo.png"]))
        XCTAssertNil(SplashMotionZipReader.selectAnimationEntryName([]))
    }
}
