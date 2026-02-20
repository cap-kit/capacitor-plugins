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

    static func normalizeFingerprint(_ fingerprint: String) -> String {
        fingerprint
            .replacingOccurrences(of: ":", with: "")
            .replacingOccurrences(of: " ", with: "")
            .lowercased()
    }

    static func isValidFingerprintFormat(_ fingerprint: String) -> Bool {
        let normalized = normalizeFingerprint(fingerprint)
        return normalized.count == 64 && normalized.allSatisfy { $0.isHexDigit }
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
