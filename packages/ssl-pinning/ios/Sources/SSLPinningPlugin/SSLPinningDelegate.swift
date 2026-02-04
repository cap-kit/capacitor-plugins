import Foundation
import Security

/**
 URLSession delegate responsible for performing
 SSL pinning based on certificate fingerprint comparison.

 Responsibilities:
 - Intercept TLS authentication challenges
 - Extract the server leaf certificate
 - Compute its SHA-256 fingerprint
 - Compare it against the expected fingerprints

 Forbidden:
 - Referencing Capacitor APIs
 - Throwing JavaScript-facing errors
 - Performing configuration lookups
 */
final class SSLPinningDelegate: NSObject, URLSessionDelegate {

    // MARK: - Properties

    /**
     Normalized expected fingerprints.
     Normalization is performed once to ensure
     consistent comparison.
     */
    private let expectedFingerprints: [String]

    /**
     Completion handler returning the raw result
     of the SSL pinning operation.
     */
    private let completion: ([String: Any]) -> Void

    /**
     Controls verbose native logging.
     */
    private let verboseLogging: Bool

    // MARK: - Initialization

    /**
     Initializes the SSLPinningDelegate.

     - Parameters:
     - expectedFingerprints: Allowed SHA-256 fingerprints
     - verboseLogging: Enables verbose native logging
     - completion: Completion handler returning the result
     */
    init(
        expectedFingerprints: [String],
        verboseLogging: Bool,
        completion: @escaping ([String: Any]) -> Void
    ) {
        self.expectedFingerprints =
            expectedFingerprints.map {
                SSLPinningUtils.normalizeFingerprint($0)
            }
        self.verboseLogging = verboseLogging
        self.completion = completion
    }

    // MARK: - URLSessionDelegate

    /**
     Intercepts the TLS authentication challenge
     to perform manual SSL pinning.

     The connection is accepted or rejected solely
     based on fingerprint comparison.

     IMPORTANT:
     - The system trust chain is NOT evaluated
     - Only the leaf certificate fingerprint is checked
     */
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (
            URLSession.AuthChallengeDisposition,
            URLCredential?
        ) -> Void
    ) {

        guard
            let trust = challenge.protectionSpace.serverTrust,
            let certificate =
                SSLPinningUtils.leafCertificate(from: trust)
        else {
            SSLPinningLogger.error(
                "Failed to extract server certificate"
            )

            completion([
                "fingerprintMatched": false
            ])

            completionHandler(
                .cancelAuthenticationChallenge,
                nil
            )
            return
        }

        let actualFingerprint =
            SSLPinningUtils.normalizeFingerprint(
                SSLPinningUtils.sha256Fingerprint(from: certificate)
            )

        let matchedFingerprint =
            expectedFingerprints.first {
                $0 == actualFingerprint
            }

        let matched = matchedFingerprint != nil

        SSLPinningLogger.debug(
            "SSLPinning matched:",
            "\(matched)"
        )

        completion([
            "actualFingerprint": actualFingerprint,
            "fingerprintMatched": matched,
            "matchedFingerprint": matchedFingerprint as Any
        ])

        completionHandler(
            matched
                ? .useCredential
                : .cancelAuthenticationChallenge,
            matched
                ? URLCredential(trust: trust)
                : nil
        )
    }
}
