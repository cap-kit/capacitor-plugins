import Foundation
import Security
import CommonCrypto

struct SSLPinningUtils {
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

    // MARK: - Domain Matching

    /**
     Determines the effective certificate list for a given host.

     Matching rules (in order of precedence):
     1. Exact domain match (e.g., "api.example.com")
     2. Subdomain match - most specific wins (longest key)
     3. Fallback to global certs

     - Parameters:
     - host: The request host (e.g., "api.example.com")
     - certsByDomain: Per-domain certificate configuration
     - globalCerts: Global fallback certificates
     - Returns: The effective list of certificate file names
     */
    static func getEffectiveCerts(
        forHost host: String,
        certsByDomain: [String: [String]],
        globalCerts: [String]
    ) -> [String] {
        let hostLower = host.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)

        // 1. Try exact match
        if let exactMatch = certsByDomain[hostLower] {
            return exactMatch
        }

        // 2. Try subdomain match - find most specific (longest key)
        let matchingSubdomains = certsByDomain.keys
            .filter { key in
                let keyLower = key.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
                return hostLower == keyLower || hostLower.hasSuffix("." + keyLower)
            }
            .sorted { $0.count > $1.count }

        if let bestMatch = matchingSubdomains.first {
            return certsByDomain[bestMatch] ?? globalCerts
        }

        // 3. Fallback to global certs
        return globalCerts
    }

    // MARK: - Bundle Certificate Loading

    static func loadPinnedCertificates(certFileNames: [String]) -> [SecCertificate] {
        certFileNames.compactMap { fileName in
            let fileURL = URL(fileURLWithPath: fileName)
            let name = fileURL.deletingPathExtension().lastPathComponent
            let ext = fileURL.pathExtension.isEmpty ? "cer" : fileURL.pathExtension

            guard let path = Bundle.main.path(forResource: name, ofType: ext),
                  let data = try? Data(contentsOf: URL(fileURLWithPath: path)),
                  let cert = SecCertificateCreateWithData(nil, data as CFData) else {
                SSLPinningLogger.error("Failed to create certificate from file: \(fileName). Ensure it is a valid DER-encoded X.509 certificate.")
                return nil
            }

            return cert
        }
    }
}
