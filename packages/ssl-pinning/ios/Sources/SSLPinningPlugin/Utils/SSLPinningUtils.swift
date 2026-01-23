import Foundation
import CryptoKit
import Security

/// Utility helpers for SSL Pinning logic.
/// Pure Swift — no Capacitor dependency.
struct SSLPinningUtils {

    /// Validates and returns a HTTPS URL.
    static func httpsURL(from value: String) -> URL? {
        guard let url = URL(string: value),
              url.scheme?.lowercased() == "https" else {
            return nil
        }
        return url
    }

    /// Normalizes a fingerprint string (removes colons, lowercases).
    /// Example: "AA:BB:CC" → "aabbcc"
    static func normalizeFingerprint(_ value: String) -> String {
        value
            .replacingOccurrences(of: ":", with: "")
            .lowercased()
    }

    /// Computes the SHA-256 fingerprint from a SecCertificate.
    /// Output format: "aa:bb:cc:dd"
    static func sha256Fingerprint(from certificate: SecCertificate) -> String {
        let data = SecCertificateCopyData(certificate) as Data
        let hash = SHA256.hash(data: data)
        return hash
            .map { String(format: "%02x", $0) }
            .joined(separator: ":")
    }

    /// Extracts the leaf certificate from a SecTrust, handling iOS <15 and ≥15.
    static func leafCertificate(from trust: SecTrust) -> SecCertificate? {
        if #available(iOS 15.0, *) {
            if let chain = SecTrustCopyCertificateChain(trust) as? [SecCertificate] {
                return chain.first
            }
            return nil
        } else {
            return SecTrustGetCertificateAtIndex(trust, 0)
        }
    }
}
