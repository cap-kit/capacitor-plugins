import Foundation
import Security

final class SSLPinningDelegate: NSObject, URLSessionDelegate {

    /// Initializes the delegate with optional pinned certificates.
    private let expectedFingerprints: [String]

    /// Completion handler to return the result.
    private let completion: ([String: Any]) -> Void

    /// Verbose logging flag.
    private let verboseLogging: Bool

    /// Initializes the SSLPinningDelegate.
    init(
        expectedFingerprints: [String],
        completion: @escaping ([String: Any]) -> Void,
        verboseLogging: Bool
    ) {
        self.expectedFingerprints =
            expectedFingerprints.map {
                SSLPinningUtils.normalizeFingerprint($0)
            }
        self.completion = completion
        self.verboseLogging = verboseLogging
    }

    // MARK: - URLSessionDelegate

    /// Intercepts the TLS authentication challenge to perform
    /// manual SSL pinning based on certificate fingerprint.
    ///
    /// The connection is accepted or rejected solely based on
    /// fingerprint comparison, not on system trust evaluation.
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        // =========================================================
        // FINGERPRINT MODE (unchanged behavior)
        // =========================================================
        guard let trust = challenge.protectionSpace.serverTrust,
              let cert = SSLPinningUtils.leafCertificate(from: trust) else {

            completionHandler(.cancelAuthenticationChallenge, nil)
            completion([
                "fingerprintMatched": false,
                "error": "Unable to extract certificate",
                "errorCode": "INIT_FAILED"
            ])
            return
        }

        let actualFingerprint =
            SSLPinningUtils.normalizeFingerprint(
                SSLPinningUtils.sha256Fingerprint(from: cert)
            )

        let matchedFingerprint =
            expectedFingerprints.first {
                $0 == actualFingerprint
            }

        let matched = matchedFingerprint != nil

        SSLPinningLogger.debug("SSLPinning matched:", "\(matched)")

        completion([
            "actualFingerprint": actualFingerprint,
            "fingerprintMatched": matched,
            "matchedFingerprint": matchedFingerprint as Any
        ])

        completionHandler(
            matched ? .useCredential : .cancelAuthenticationChallenge,
            matched ? URLCredential(trust: trust) : nil
        )
    }
}
