import Foundation
import Security

/**
 URLSession delegate responsible for performing
 SSL pinning during the TLS handshake phase.

 Responsibilities:
 - Intercept TLS authentication challenges
 - Evaluate the server trust object
 - Apply one of the supported pinning strategies
 - Return structured validation results

 Supported strategies (evaluation order):
 1. Excluded domain bypass
 2. Certificate-based pinning (anchor validation)
 3. Fingerprint-based pinning (leaf comparison)

 Forbidden:
 - Referencing Capacitor APIs
 - Throwing JavaScript-facing errors
 - Performing configuration lookups
 - Managing bridge lifecycle

 Architectural notes:
 - This class operates purely at the networking layer.
 - It is stateless beyond the injected configuration.
 - It must always call the completion handler exactly once.
 */
final class SSLPinningDelegate: NSObject, URLSessionDelegate, URLSessionTaskDelegate {

    // MARK: - Properties

    /**
     Tracks whether the completion handler has already been called.
     Used to prevent double-calling on timeout or other terminal errors.
     */
    private var hasCompleted = false

    /**
     Normalized expected fingerprints.

     All fingerprints are normalized at initialization time
     to guarantee deterministic comparison.
     */
    private let expectedFingerprints: [String]

    /**
     Optional pinned certificates used for certificate-based pinning.

     When present, the delegate switches to "cert" mode
     and replaces the system trust anchors with these certificates.
     */
    private let pinnedCertificates: [SecCertificate]?

    /**
     Lowercased list of excluded domains.

     Matching rules:
     - Exact hostname match
     - Subdomain match

     Example:
     - excluded: example.com
     - matches: example.com, api.example.com
     */
    private let excludedDomains: [String]

    /**
     Completion handler returning the raw result
     of the SSL pinning operation.

     The delegate must ALWAYS invoke this exactly once.
     */
    private let completion: ([String: Any]) -> Void

    /**
     Controls verbose native logging.

     Logging decisions are made outside this class.
     */
    private let verboseLogging: Bool

    // MARK: - Initialization

    /**
     Initializes the SSLPinningDelegate.

     - Parameters:
     - expectedFingerprints: Allowed SHA-256 fingerprints.
     - pinnedCertificates: Optional certificates for anchor-based validation.
     - excludedDomains: Hostnames that bypass SSL pinning.
     - completion: Completion handler returning structured result data.
     - verboseLogging: Enables verbose native logging.

     Design notes:
     - All inputs are normalized and stored.
     - No external configuration access is performed.
     */
    init(
        expectedFingerprints: [String],
        pinnedCertificates: [SecCertificate]? = nil,
        excludedDomains: [String] = [],
        completion: @escaping ([String: Any]) -> Void,
        verboseLogging: Bool,
        session: URLSession
    ) {
        self.expectedFingerprints =
            expectedFingerprints.map {
                SSLPinningUtils.normalizeFingerprint($0)
            }

        self.pinnedCertificates = pinnedCertificates
        self.excludedDomains =
            excludedDomains.map { $0.lowercased() }

        self.verboseLogging = verboseLogging

        let sessionRef = session
        self.completion = { result in
            sessionRef.invalidateAndCancel()
            completion(result)
        }
        super.init()
    }

    // MARK: - URLSessionDelegate

    /**
     Intercepts the TLS authentication challenge
     and applies the configured SSL pinning strategy.

     IMPORTANT SECURITY NOTES:

     - The delegate controls whether the connection proceeds.
     - If validation fails, the challenge is cancelled.
     - The completion handler MUST always be invoked.
     - Trust evaluation behavior depends on selected mode.

     Mode behavior:

     1. Excluded Mode
     - Bypasses pinning entirely.
     - Uses system trust.

     2. Certificate Mode
     - Replaces system trust anchors with pinned certificates.
     - Validates full chain using SecTrustEvaluateWithError.

     3. Fingerprint Mode
     - Does NOT evaluate system trust.
     - Compares only the leaf certificate fingerprint.
     */
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (
            URLSession.AuthChallengeDisposition,
            URLCredential?
        ) -> Void
    ) {

        guard let trust = challenge.protectionSpace.serverTrust else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            hasCompleted = true
            completion([
                "fingerprintMatched": false,
                "error": SSLPinningErrorMessages.internalError,
                "errorCode": "INIT_FAILED"
            ])
            return
        }

        let host = challenge.protectionSpace.host.lowercased()

        // =========================================================
        // EXCLUDED DOMAIN MODE (bypass pinning, use system trust)
        // =========================================================

        /**
         If the current host matches an excluded domain,
         SSL pinning is bypassed and system trust is used.
         */
        if excludedDomains.contains(where: {
            host == $0 || host.hasSuffix("." + $0)
        }) {

            SSLPinningLogger.debug("SSLPinning excluded domain:", host)

            hasCompleted = true
            completion([
                "fingerprintMatched": true,
                "excludedDomain": true,
                "mode": "excluded",
                "errorCode": "EXCLUDED_DOMAIN",
                "error": SSLPinningErrorMessages.excludedDomain
            ])

            completionHandler(.useCredential, URLCredential(trust: trust))
            return
        }

        // =========================================================
        // CERT MODE (only if pinnedCertificates is provided)
        // =========================================================

        /**
         Certificate-based pinning:

         - Replaces system trust anchors with pinned certificates.
         - The handshake succeeds only if the server chain
         can be validated against those anchors.
         */
        if let pinnedCerts = pinnedCertificates {

            SecTrustSetAnchorCertificates(trust, pinnedCerts as CFArray)
            SecTrustSetAnchorCertificatesOnly(trust, true)

            var error: CFError?
            let trusted = SecTrustEvaluateWithError(trust, &error)

            SSLPinningLogger.debug(
                "SSLPinning cert-mode trusted:",
                "\(trusted)"
            )

            if trusted {
                hasCompleted = true
                completion([
                    "fingerprintMatched": true,
                    "mode": "cert",
                    "errorCode": "",
                    "error": ""
                ])

                completionHandler(.useCredential, URLCredential(trust: trust))
            } else {
                hasCompleted = true
                completion([
                    "fingerprintMatched": false,
                    "errorCode": "TRUST_EVALUATION_FAILED",
                    "error": error?.localizedDescription ?? SSLPinningErrorMessages.pinningFailed
                ])

                completionHandler(.cancelAuthenticationChallenge, nil)
            }

            return
        }

        // =========================================================
        // FINGERPRINT MODE (unchanged behavior)
        // =========================================================

        /**
         Fingerprint-based pinning:

         - Extracts the leaf certificate.
         - Computes SHA-256 fingerprint.
         - Compares against normalized expected fingerprints.

         NOTE:
         - The system trust chain is NOT evaluated in this mode.
         - Only the leaf certificate fingerprint is considered.
         */
        guard let certificate =
                SSLPinningUtils.leafCertificate(from: trust)
        else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            hasCompleted = true
            completion([
                "fingerprintMatched": false,
                "error": SSLPinningErrorMessages.internalError,
                "errorCode": "INIT_FAILED"
            ])
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

        hasCompleted = true
        completion([
            "actualFingerprint": actualFingerprint,
            "fingerprintMatched": matched,
            "matchedFingerprint": matchedFingerprint as Any,
            "mode": "fingerprint",
            "errorCode": matched ? "" : "PINNING_FAILED",
            "error": matched ? "" : SSLPinningErrorMessages.pinningFailed
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

    // MARK: - URLSessionTaskDelegate

    /**
     Handles task-level events including timeouts.

     This method is called when the task completes, allowing us
     to detect and handle timeout errors that may occur before
     or during the authentication challenge.
     */
    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        guard !hasCompleted else { return }

        if let error = error {
            let nsError = error as NSError

            // Timeout
            if nsError.code == NSURLErrorTimedOut {
                hasCompleted = true
                completion([
                    "fingerprintMatched": false,
                    "errorCode": "TIMEOUT",
                    "error": SSLPinningErrorMessages.timeout
                ])
            }
            // Common network errors (non-timeout)
            else if nsError.domain == NSURLErrorDomain,
                    [NSURLErrorNotConnectedToInternet,
                     NSURLErrorNetworkConnectionLost,
                     NSURLErrorCannotFindHost,
                     NSURLErrorCannotConnectToHost].contains(nsError.code) {
                hasCompleted = true
                completion([
                    "fingerprintMatched": false,
                    "errorCode": "NETWORK_ERROR",
                    "error": SSLPinningErrorMessages.networkError
                ])
            } else {
                // Fallback for other errors
                hasCompleted = true
                completion([
                    "fingerprintMatched": false,
                    "errorCode": "NETWORK_ERROR",
                    "error": SSLPinningErrorMessages.networkError
                ])
            }
        }
    }
}
