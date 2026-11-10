import XCTest
import LocalAuthentication
import Security
@testable import BiometricsPlugin

/**
 Headless tests for the biometry-bound secure store.

 These cover the pure logic that runs without a device:
 - query construction (read / write / existence / delete)
 - access control mapping and creation
 - Keychain status to error mapping
 - existence semantics (`errSecInteractionNotAllowed` as present)

 Keychain-authenticated reads cannot run headless: the biometric
 prompt requires a device. They are verified on-device via the
 example app.
 */
final class BiometricsSecureStoreTests: XCTestCase {

    // MARK: - Identity

    func testServiceIsForkOwnedNotCapgo() {
        XCTAssertEqual(BiometricsSecureStore.service, "CapKitBiometricsSecureData")
        XCTAssertNotEqual(BiometricsSecureStore.service, "CapgoNativeBiometricSecureData")
    }

    func testAccountUsesCallerKey() {
        XCTAssertEqual(BiometricsSecureStore.account(for: "session.token"), "session.token")
    }

    // MARK: - Access control mapping

    func testAccessControlMapsBiometryAny() {
        XCTAssertEqual(BiometricsSecureStore.accessControlFlags(for: "biometryAny"), .biometryAny)
    }

    func testAccessControlMapsBiometryCurrentSet() {
        XCTAssertEqual(
            BiometricsSecureStore.accessControlFlags(for: "biometryCurrentSet"),
            .biometryCurrentSet
        )
    }

    func testAccessControlRejectsUnknownValue() {
        XCTAssertNil(BiometricsSecureStore.accessControlFlags(for: "password"))
    }

    func testAccessControlRejectsMissingValue() {
        XCTAssertNil(BiometricsSecureStore.accessControlFlags(for: nil))
    }

    func testMakeAccessControlSucceedsForBothFlags() throws {
        XCTAssertNotNil(try XCTUnwrap(BiometricsSecureStore.makeAccessControl(flags: .biometryAny)))
        XCTAssertNotNil(try XCTUnwrap(BiometricsSecureStore.makeAccessControl(flags: .biometryCurrentSet)))
    }

    // MARK: - Query construction

    func testReadQueryTargetsForkServiceWithAuthContext() throws {
        let context = LAContext()
        let query = BiometricsSecureStore.readQuery(key: "session.token", context: context)

        XCTAssertEqual(query[kSecClass as String] as? String, kSecClassGenericPassword as String)
        XCTAssertEqual(query[kSecAttrService as String] as? String, BiometricsSecureStore.service)
        XCTAssertEqual(query[kSecAttrAccount as String] as? String, "session.token")
        XCTAssertEqual(query[kSecMatchLimit as String] as? String, kSecMatchLimitOne as String)
        XCTAssertEqual(query[kSecReturnData as String] as? Bool, true)
        XCTAssertEqual(query[kSecReturnAttributes as String] as? Bool, true)

        let queriedContext = try XCTUnwrap(query[kSecUseAuthenticationContext as String] as? LAContext)
        XCTAssertTrue(queriedContext === context)
    }

    func testExistenceQueryDisablesInteractionAndNeverRequestsPayload() throws {
        let query = BiometricsSecureStore.existenceQuery(key: "session.token")

        XCTAssertEqual(query[kSecClass as String] as? String, kSecClassGenericPassword as String)
        XCTAssertEqual(query[kSecAttrService as String] as? String, BiometricsSecureStore.service)
        XCTAssertEqual(query[kSecAttrAccount as String] as? String, "session.token")
        XCTAssertEqual(query[kSecReturnAttributes as String] as? Bool, true)
        XCTAssertNil(query[kSecReturnData as String])

        let context = try XCTUnwrap(query[kSecUseAuthenticationContext as String] as? LAContext)
        XCTAssertTrue(context.interactionNotAllowed)
    }

    func testDeleteQueryTargetsOnlyIdentity() {
        let query = BiometricsSecureStore.deleteQuery(key: "session.token")

        XCTAssertEqual(query.count, 3)
        XCTAssertEqual(query[kSecClass as String] as? String, kSecClassGenericPassword as String)
        XCTAssertEqual(query[kSecAttrService as String] as? String, BiometricsSecureStore.service)
        XCTAssertEqual(query[kSecAttrAccount as String] as? String, "session.token")
    }

    func testWriteQueryCarriesValueAndAccessControl() throws {
        let access = try XCTUnwrap(BiometricsSecureStore.makeAccessControl(flags: .biometryAny))
        let valueData = Data("payload".utf8)
        let query = BiometricsSecureStore.writeQuery(key: "session.token", valueData: valueData, access: access)

        XCTAssertEqual(query[kSecClass as String] as? String, kSecClassGenericPassword as String)
        XCTAssertEqual(query[kSecAttrService as String] as? String, BiometricsSecureStore.service)
        XCTAssertEqual(query[kSecAttrAccount as String] as? String, "session.token")
        XCTAssertEqual(query[kSecValueData as String] as? Data, valueData)
        XCTAssertNotNil(query[kSecAttrAccessControl as String])
    }

    // MARK: - Existence semantics

    func testIndicatesItemExistsTreatsInteractionNotAllowedAsPresent() {
        XCTAssertTrue(BiometricsSecureStore.indicatesItemExists(status: errSecSuccess))
        XCTAssertTrue(BiometricsSecureStore.indicatesItemExists(status: errSecInteractionNotAllowed))
    }

    func testIndicatesItemExistsRejectsMissingAndFailedStatuses() {
        XCTAssertFalse(BiometricsSecureStore.indicatesItemExists(status: errSecItemNotFound))
        XCTAssertFalse(BiometricsSecureStore.indicatesItemExists(status: errSecAuthFailed))
        XCTAssertFalse(BiometricsSecureStore.indicatesItemExists(status: errSecUserCanceled))
    }

    // MARK: - Read status mapping

    func testReadErrorMapsMissingItemToNotFound() {
        let error = BiometricsSecureStore.readError(for: errSecItemNotFound)
        XCTAssertEqual(error?.errorCode, "NOT_FOUND")
        XCTAssertEqual(error?.message, BiometricsErrorMessages.secureDataNotFound)
    }

    func testReadErrorMapsUserCancelToCancelled() {
        XCTAssertEqual(BiometricsSecureStore.readError(for: errSecUserCanceled)?.errorCode, "CANCELLED")
    }

    func testReadErrorMapsAuthFailureToCancelled() {
        XCTAssertEqual(BiometricsSecureStore.readError(for: errSecAuthFailed)?.errorCode, "CANCELLED")
    }

    func testReadErrorMapsInteractionNotAllowedToUnavailable() {
        XCTAssertEqual(BiometricsSecureStore.readError(for: errSecInteractionNotAllowed)?.errorCode, "UNAVAILABLE")
    }

    func testReadErrorYieldsNilOnSuccess() {
        XCTAssertNil(BiometricsSecureStore.readError(for: errSecSuccess))
    }

    func testReadErrorMapsUnknownStatusToInitFailed() {
        XCTAssertEqual(BiometricsSecureStore.readError(for: errSecDecode)?.errorCode, "INIT_FAILED")
    }
}
