import Foundation
import CryptoKit
import Security

/**
 Utility helpers for SSL pinning logic.

 Pure Swift utilities:
 - No Capacitor dependency
 - No side effects
 - Fully testable
 */
struct SSLPinningUtils {

    /**
     Validates and returns a HTTPS URL.

     Non-HTTPS URLs are explicitly rejected
     to prevent insecure usage.
     */
    static func httpsURL(from value: String) -> URL? {
        guard
            let url = URL(string: value),
            url.scheme?.lowercased() == "https"
        else {
            return nil
        }
        return url
    }

    /**
     Normalizes a fingerprint string by:
     - Removing colon separators
     - Converting to lowercase

     Example:
     "AA:BB:CC" → "aabbcc"
     */
    static func normalizeFingerprint(_ value: String) -> String {
        value
            .replacingOccurrences(of: ":", with: "")
            .lowercased()
    }

    /**
     Computes the SHA-256 fingerprint of a certificate.

     Output format:
     "aa:bb:cc:dd:..."
     */
    static func sha256Fingerprint(
        from certificate: SecCertificate
    ) -> String {
        let data =
            SecCertificateCopyData(certificate) as Data

        let hash =
            SHA256.hash(data: data)

        return hash
            .map { String(format: "%02x", $0) }
            .joined(separator: ":")
    }

    /**
     Extracts the leaf certificate from a SecTrust object.

     Handles both:
     - iOS 15+
     - Older fallback APIs (still safe on iOS 15 target)
     */
    static func leafCertificate(
        from trust: SecTrust
    ) -> SecCertificate? {
        if #available(iOS 15.0, *) {
            return
                (SecTrustCopyCertificateChain(trust)
                    as? [SecCertificate])?
                .first
        } else {
            return SecTrustGetCertificateAtIndex(trust, 0)
        }
    }

    /**
     Loads pinned certificates from app bundle.

     Certificates are expected to be in a 'certs' subdirectory.
     */
    static func loadPinnedCertificates(
        certFileNames: [String],
        subdirectory: String = "certs"
    ) -> [SecCertificate] {
        var certificates: [SecCertificate] = []

        for fileName in certFileNames {
            let parts = fileName.split(separator: ".")
            guard parts.count >= 2 else { continue }

            let name = String(parts.dropLast().joined(separator: "."))
            let ext = String(parts.last!)

            guard
                let url = Bundle.main.url(
                    forResource: name,
                    withExtension: ext,
                    subdirectory: subdirectory
                ),
                let data = try? Data(contentsOf: url),
                let cert = SecCertificateCreateWithData(nil, data as CFData)
            else {
                // Log error if file exists but certificate creation fails
                SSLPinningLogger.error("Failed to create certificate from file: \(fileName). Ensure it is a valid DER-encoded X.509 certificate.")
                continue
            }
            certificates.append(cert)
        }
        return certificates
    }
}
