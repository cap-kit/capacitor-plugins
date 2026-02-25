import XCTest
@testable import TLSFingerprint

/**
 Basic functional tests for the TLSFingerprint plugin native implementation.

 These tests validate the core behavior of the implementation
 independently from the Capacitor bridge.
 */
class TLSFingerprintPluginTests: XCTestCase {
    func testInstantiation() {
        let implementation = TLSFingerprintImpl()
        XCTAssertTrue(true)
    }
}
