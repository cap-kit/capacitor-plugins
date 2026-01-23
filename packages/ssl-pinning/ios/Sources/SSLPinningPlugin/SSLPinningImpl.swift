import Foundation
import Security

final class SSLPinningImpl {

    /// Cached configuration.
    private var config: SSLPinningConfig?

    /// Applies the given configuration.
    func applyConfig(_ config: SSLPinningConfig) {
        self.config = config
    }

    // MARK: - Single fingerprint

    /// Validates multiple SSL certificates for a given URL.
    func checkCertificate(
        urlString: String,
        fingerprintFromArgs: String?,
        completion: @escaping ([String: Any]) -> Void
    ) {
        let fingerprint =
            fingerprintFromArgs ??
            config?.fingerprint

        guard let expectedFingerprint = fingerprint else {
            completion([
                "fingerprintMatched": false,
                "error": "No fingerprint provided (args or config)",
                "errorCode": "UNAVAILABLE"
            ])
            return
        }

        performCheck(
            urlString: urlString,
            fingerprints: [expectedFingerprint],
            completion: completion
        )
    }

    // MARK: - Multiple fingerprints

    /// Validates multiple SSL certificates for a given URL.
    func checkCertificates(
        urlString: String,
        fingerprintsFromArgs: [String]?,
        completion: @escaping ([String: Any]) -> Void
    ) {
        let fingerprints =
            fingerprintsFromArgs ??
            config?.fingerprints

        guard let fingerprints, !fingerprints.isEmpty else {
            completion([
                "fingerprintMatched": false,
                "error": "No fingerprints provided (args or config)",
                "errorCode": "UNAVAILABLE"
            ])
            return
        }

        performCheck(
            urlString: urlString,
            fingerprints: fingerprints,
            completion: completion
        )
    }

    // MARK: - Shared implementation

    /// Performs the actual SSL pinning validation.
    ///
    /// This method:
    /// - Creates an ephemeral URLSession
    /// - Intercepts the TLS handshake via URLSessionDelegate
    /// - Extracts the server leaf certificate
    /// - Compares its SHA-256 fingerprint against the expected ones
    ///
    /// IMPORTANT:
    /// - The system trust chain is NOT evaluated
    /// - Only fingerprint matching determines acceptance
    private func performCheck(
        urlString: String,
        fingerprints: [String],
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let url = SSLPinningUtils.httpsURL(from: urlString) else {
            completion([
                "fingerprintMatched": false,
                "error": "Invalid HTTPS URL",
                "errorCode": "UNKNOWN_TYPE"
            ])
            return
        }

        let delegate = SSLPinningDelegate(
            expectedFingerprints: fingerprints,
            completion: completion,
            verboseLogging: config?.verboseLogging ?? false
        )

        let session = URLSession(
            configuration: .ephemeral,
            delegate: delegate,
            delegateQueue: nil
        )

        session.dataTask(with: url).resume()
    }
}
