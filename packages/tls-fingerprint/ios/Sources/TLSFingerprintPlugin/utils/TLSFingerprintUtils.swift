import Foundation
import Security
import CommonCrypto

struct TLSFingerprintUtils {
    // MARK: - URL Helpers

    static func httpsURL(from urlString: String) -> URL? {
        guard let url = URL(string: urlString), url.scheme?.lowercased() == "https" else {
            return nil
        }
        return url
    }

    // MARK: - Fingerprint Normalization

    /**
     Normalizes a fingerprint string by:
     - Removing colon separators
     - Removing all whitespace
     - Converting to lowercase

     Example:
     "AA:BB:CC" → "aabbcc"
     "AA BB CC" → "aabbcc"
     */
    static func normalizeFingerprint(_ fingerprint: String) -> String {
        fingerprint
            .replacingOccurrences(of: ":", with: "")
            .replacingOccurrences(of: " ", with: "")
            .lowercased()
    }

    /**
     Validates that a fingerprint string is a valid SHA-256 hex format.

     Valid fingerprint:
     - Exactly 64 hexadecimal characters (after normalization)
     - Contains only [a-f0-9]
     */
    static func isValidFingerprintFormat(_ fingerprint: String) -> Bool {
        let normalized = normalizeFingerprint(fingerprint)
        return normalized.count == 64 && normalized.allSatisfy { $0.isHexDigit }
    }

    /**
     Validates a fingerprint and returns an error message if invalid, or nil if valid.
     */
    static func validateFingerprint(_ fingerprint: String) -> String? {
        let trimmed = fingerprint.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return "Fingerprint cannot be blank"
        }
        let normalized = normalizeFingerprint(fingerprint)
        if normalized.count != 64 {
            return "Invalid fingerprint: must be 64 hex characters"
        }
        if !normalized.allSatisfy({ $0.isHexDigit }) {
            return "Invalid fingerprint: must contain only hex characters [a-f0-9]"
        }
        return nil
    }

    // MARK: - Certificate Helpers

    static func sha256Fingerprint(from certificate: SecCertificate) -> String {
        let certData = SecCertificateCopyData(certificate) as Data
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        certData.withUnsafeBytes { bytes in
            _ = CC_SHA256(bytes.baseAddress, CC_LONG(certData.count), &hash)
        }
        return hash.map { String(format: "%02x", $0) }.joined()
    }

    static func leafCertificate(from trust: SecTrust) -> SecCertificate? {
        guard SecTrustGetCertificateCount(trust) > 0 else { return nil }
        return SecTrustGetCertificateAtIndex(trust, 0)
    }
}
