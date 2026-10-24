import XCTest
@testable import DevicePlugin

/**
 Basic functional tests for the Device plugin native implementation.

 These tests validate the core behavior of the implementation
 independently from the Capacitor bridge.
 */
class DevicePluginTests: XCTestCase {

    // MARK: - Device identity

    /**
     Returns the vendor identifier under the `identifier` key, matching the
     published `DeviceId` contract and the Android/web implementations.
     */
    func testGetIdReturnsIdentifierKey() throws {
        let implementation = DeviceImpl()
        implementation.identifierForVendorProvider = {
            UUID(uuidString: "E621E1F8-C36C-495A-93FC-0C247A3E6E5F")
        }

        let result = try implementation.getId()

        XCTAssertEqual(
            result["identifier"] as? String,
            "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
        )
        XCTAssertNil(result["uid"], "The legacy `uid` key must not be present")
    }

    /**
     Surfaces an unavailable error when the vendor identifier cannot be read,
     mirroring the Android implementation.
     */
    func testGetIdThrowsUnavailableWhenVendorIdentifierIsMissing() {
        let implementation = DeviceImpl()
        implementation.identifierForVendorProvider = { nil }

        XCTAssertThrowsError(try implementation.getId()) { error in
            guard let nativeError = error as? NativeError else {
                return XCTFail("Expected NativeError, received \(type(of: error))")
            }
            XCTAssertEqual(nativeError.errorCode, "UNAVAILABLE")
        }
    }
}
