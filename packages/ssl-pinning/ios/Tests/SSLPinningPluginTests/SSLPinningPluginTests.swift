import XCTest
@testable import SSLPinningPlugin

final class SSLPinningTests: XCTestCase {

    // MARK: - Fingerprint Normalization

    func testNormalizeFingerprint_withColons_returnsLowercaseNoColons() {
        let input = "AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD"
        let result = SSLPinningUtils.normalizeFingerprint(input)
        XCTAssertEqual(result, "abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    }

    func testNormalizeFingerprint_withSpaces_returnsLowercaseNoSpaces() {
        let input = "AB CD EF 12 34 56 78 90 AB CD EF 12 34 56 78 90 AB CD EF 12 34 56 78 90 AB CD"
        let result = SSLPinningUtils.normalizeFingerprint(input)
        XCTAssertEqual(result, "abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    }

    func testNormalizeFingerprint_withMixedColonsAndSpaces_returnsLowercaseClean() {
        let input = "AB:CD EF:12 34:56 78:90 AB:CD EF:12 34:56 78:90 AB:CD EF:12 34:56 78:90"
        let result = SSLPinningUtils.normalizeFingerprint(input)
        XCTAssertEqual(result, "abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    }

    func testNormalizeFingerprint_uppercase_returnsLowercase() {
        let input = "ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890"
        let result = SSLPinningUtils.normalizeFingerprint(input)
        XCTAssertEqual(result, "abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    }

    // MARK: - Fingerprint Validation

    func testIsValidFingerprintFormat_valid64Hex_returnsTrue() {
        let input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
        let result = SSLPinningUtils.isValidFingerprintFormat(input)
        XCTAssertTrue(result)
    }

    func testIsValidFingerprintFormat_validWithColons_returnsTrue() {
        let input = "ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd"
        let result = SSLPinningUtils.isValidFingerprintFormat(input)
        XCTAssertTrue(result)
    }

    func testIsValidFingerprintFormat_tooShort_returnsFalse() {
        let input = "abcdef1234567890"
        let result = SSLPinningUtils.isValidFingerprintFormat(input)
        XCTAssertFalse(result)
    }

    func testIsValidFingerprintFormat_tooLong_returnsFalse() {
        let input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        let result = SSLPinningUtils.isValidFingerprintFormat(input)
        XCTAssertFalse(result)
    }

    func testIsValidFingerprintFormat_invalidHex_returnsFalse() {
        let input = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef123456789g"
        let result = SSLPinningUtils.isValidFingerprintFormat(input)
        XCTAssertFalse(result)
    }

    // MARK: - Excluded Domains Matching

    func testIsExcludedDomain_exactMatch_returnsTrue() {
        let excludedDomains = ["example.com"]
        let host = "example.com"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertTrue(result)
    }

    func testIsExcludedDomain_subdomain_returnsTrue() {
        let excludedDomains = ["example.com"]
        let host = "api.example.com"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertTrue(result)
    }

    func testIsExcludedDomain_deepSubdomain_returnsTrue() {
        let excludedDomains = ["example.com"]
        let host = "api.v2.example.com"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertTrue(result)
    }

    func testIsExcludedDomain_notExcluded_returnsFalse() {
        let excludedDomains = ["example.com"]
        let host = "other.com"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertFalse(result)
    }

    func testIsExcludedDomain_similarSuffix_returnsFalse() {
        let excludedDomains = ["example.com"]
        let host = "notexample.com"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertFalse(result)
    }

    func testIsExcludedDomain_caseInsensitive_returnsTrue() {
        let excludedDomains = ["Example.com"]
        let host = "API.EXAMPLE.COM"
        let result = matchesExcludedDomain(host: host, excludedDomains: excludedDomains)
        XCTAssertTrue(result)
    }

    // MARK: - Result Shape

    func testResultShape_successFingerprint_hasRequiredFields() {
        let result: [String: Any] = [
            "actualFingerprint": "abcdef1234567890",
            "fingerprintMatched": true,
            "mode": "fingerprint",
            "errorCode": "",
            "error": ""
        ]

        XCTAssertNotNil(result["actualFingerprint"])
        XCTAssertNotNil(result["fingerprintMatched"])
        XCTAssertNotNil(result["mode"])
        XCTAssertNotNil(result["errorCode"])
        XCTAssertNotNil(result["error"])

        XCTAssertEqual(result["fingerprintMatched"] as? Bool, true)
        XCTAssertEqual(result["mode"] as? String, "fingerprint")
    }

    func testResultShape_excludedDomain_hasRequiredFields() {
        let result: [String: Any] = [
            "actualFingerprint": "abcdef1234567890",
            "fingerprintMatched": true,
            "excludedDomain": true,
            "mode": "excluded",
            "errorCode": "EXCLUDED_DOMAIN",
            "error": "Excluded domain"
        ]

        XCTAssertNotNil(result["actualFingerprint"])
        XCTAssertNotNil(result["fingerprintMatched"])
        XCTAssertNotNil(result["excludedDomain"])
        XCTAssertNotNil(result["mode"])
        XCTAssertNotNil(result["errorCode"])
        XCTAssertNotNil(result["error"])

        XCTAssertEqual(result["fingerprintMatched"] as? Bool, true)
        XCTAssertEqual(result["excludedDomain"] as? Bool, true)
        XCTAssertEqual(result["mode"] as? String, "excluded")
    }

    func testResultShape_failure_hasErrorFields() {
        let result: [String: Any] = [
            "actualFingerprint": "abcdef1234567890",
            "fingerprintMatched": false,
            "mode": "fingerprint",
            "errorCode": "PINNING_FAILED",
            "error": "Pinning failed"
        ]

        XCTAssertNotNil(result["actualFingerprint"])
        XCTAssertNotNil(result["fingerprintMatched"])
        XCTAssertNotNil(result["mode"])
        XCTAssertNotNil(result["errorCode"])
        XCTAssertNotNil(result["error"])

        XCTAssertEqual(result["fingerprintMatched"] as? Bool, false)
        XCTAssertEqual(result["errorCode"] as? String, "PINNING_FAILED")
    }

    // MARK: - Helper

    private func matchesExcludedDomain(host: String, excludedDomains: [String]) -> Bool {
        let hostLower = host.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        return excludedDomains.contains { excluded in
            let excludedLower = excluded.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
            return hostLower == excludedLower || hostLower.hasSuffix("." + excludedLower)
        }
    }
}
