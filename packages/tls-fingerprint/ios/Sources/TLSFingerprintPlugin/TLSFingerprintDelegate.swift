import Foundation
import Security

/**
 URLSession delegate responsible for performing
 TLS fingerprint validation during the TLS handshake phase.

 SECURITY MODEL:
 This plugin validates fingerprint equality only and does not
 enforce system trust evaluation.

 Responsibilities:
 - Intercept TLS authentication challenges
 - Evaluate the server trust object
 - Apply TLS fingerprint strategies
 - Return structured validation results

 Supported strategies (evaluation order):
 1. Excluded domain bypass
 2. Fingerprint-based validation (leaf comparison)

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
final class TLSFingerprintDelegate: NSObject, URLSessionDelegate, URLSessionTaskDelegate {

    // MARK: - Properties

    /**
     The URLSession that owns this delegate and executes the request.
     Set via setSession() after delegate initialization.
     */
    private var session: URLSession?

    /**
     Tracks whether the completion handler has already been called.
     Used to prevent double-calling on timeout or other terminal errors.
     */
    private var hasCompleted = false

    /**
     Lock for thread-safe completion tracking.
     */
    private let completionLock = NSLock()

    /**
     Normalized expected fingerprints.

     All fingerprints are normalized at initialization time
     to guarantee deterministic comparison.
     */
    private let expectedFingerprints: [String]

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
    private let completion: (TLSFingerprintResult) -> Void

    /**
     Controls verbose native logging.

     Logging decisions are made outside this class.
     */
    private let verboseLogging: Bool

    // MARK: - Initialization

    /**
     Initializes the TLSFingerprintDelegate.

     - Parameters:
     - expectedFingerprints: Allowed SHA-256 fingerprints.
     - excludedDomains: Hostnames that bypass SSL pinning.
     - completion: Completion handler returning structured result data.
     - verboseLogging: Enables verbose native logging.

     Design notes:
     - All inputs are normalized and stored.
     - No external configuration access is performed.
     - Session must be set via setSession() before use.
     */
    init(
        expectedFingerprints: [String],
        excludedDomains: [String] = [],
        completion: @escaping (TLSFingerprintResult) -> Void,
        verboseLogging: Bool
    ) {
        self.expectedFingerprints =
            expectedFingerprints.map {
                TLSFingerprintUtils.normalizeFingerprint($0)
            }

        self.excludedDomains =
            excludedDomains.map { $0.lowercased() }

        self.verboseLogging = verboseLogging

        self.completion = completion
        super.init()
    }

    /**
     Sets the URLSession that owns this delegate.

     Must be called before the session executes any task.
     The session will be invalidated when completion is called.
     */
    func setSession(_ session: URLSession) {
        self.session = session
    }

    // MARK: - Completion Handler

    /**
     Attempts to mark completion atomically.
     Returns true if this call successfully marked completion (no prior completion).
     Used by timeout handlers to prevent race conditions.
     */
    func trySetCompleted() -> Bool {
        completionLock.lock()
        defer { completionLock.unlock() }

        if hasCompleted {
            return false
        }
        hasCompleted = true
        return true
    }

    /**
     Helper method to complete the request and invalidate the session.
     Ensures session is invalidated exactly once.
     */
    private func completeWithResult(_ result: TLSFingerprintResult) {
        completionLock.lock()
        guard !hasCompleted else {
            completionLock.unlock()
            return
        }
        hasCompleted = true
        completionLock.unlock()

        session?.invalidateAndCancel()
        completion(result)
    }

    // MARK: - URLSessionDelegate

    /**
     Intercepts the TLS authentication challenge
     and applies the configured TLS fingerprint strategy.

     IMPORTANT SECURITY NOTES:

     - The delegate controls whether the connection proceeds.
     - If validation fails, the challenge is cancelled.
     - The completion handler MUST always be invoked.
     - Trust evaluation behavior depends on selected mode.

     This plugin validates fingerprint equality only and does not
     enforce system trust evaluation.

     Mode behavior:

     1. Excluded Mode
     - Bypasses fingerprint validation entirely.
     - Uses permissive trust handling.

     2. Fingerprint Mode
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
            completeWithResult(TLSFingerprintResult(
                fingerprintMatched: false,
                error: TLSFingerprintErrorMessages.internalError,
                errorCode: "INIT_FAILED"
            ))
            return
        }

        let host = challenge.protectionSpace.host.lowercased()

        // =========================================================
        // EXCLUDED DOMAIN MODE (bypass fingerprint validation)
        // =========================================================

        /**
         If the current host matches an excluded domain,
         fingerprint validation is bypassed.
         The connection uses a permissive trust manager.
         System trust chain is NOT explicitly evaluated.
         We still compute and return the actual fingerprint for parity.
         */
        if excludedDomains.contains(where: {
            host == $0 || host.hasSuffix("." + $0)
        }) {

            TLSFingerprintLogger.debug("TLSFingerprint excluded domain:", host)

            guard let certificate =
                    TLSFingerprintUtils.leafCertificate(from: trust)
            else {
                completionHandler(.cancelAuthenticationChallenge, nil)
                completeWithResult(TLSFingerprintResult(
                    fingerprintMatched: false,
                    error: TLSFingerprintErrorMessages.internalError,
                    errorCode: "INIT_FAILED"
                ))
                return
            }

            let actualFingerprint =
                TLSFingerprintUtils.normalizeFingerprint(
                    TLSFingerprintUtils.sha256Fingerprint(from: certificate)
                )

            completionHandler(.useCredential, URLCredential(trust: trust))

            completeWithResult(TLSFingerprintResult(
                actualFingerprint: actualFingerprint,
                fingerprintMatched: true,
                excludedDomain: true,
                mode: "excluded",
                error: TLSFingerprintErrorMessages.excludedDomain,
                errorCode: "EXCLUDED_DOMAIN"
            ))

            return
        }

        guard let certificate =
                TLSFingerprintUtils.leafCertificate(from: trust)
        else {
            completionHandler(.cancelAuthenticationChallenge, nil)
            completeWithResult(TLSFingerprintResult(
                fingerprintMatched: false,
                error: TLSFingerprintErrorMessages.internalError,
                errorCode: "INIT_FAILED"
            ))
            return
        }

        let actualFingerprint =
            TLSFingerprintUtils.normalizeFingerprint(
                TLSFingerprintUtils.sha256Fingerprint(from: certificate)
            )

        let matchedFingerprint =
            expectedFingerprints.first {
                $0 == actualFingerprint
            }

        let matched = matchedFingerprint != nil

        TLSFingerprintLogger.debug(
            "TLSFingerprint matched:",
            "\(matched)"
        )

        completionHandler(
            matched
                ? .useCredential
                : .cancelAuthenticationChallenge,
            matched
                ? URLCredential(trust: trust)
                : nil
        )

        completeWithResult(TLSFingerprintResult(
            actualFingerprint: actualFingerprint,
            fingerprintMatched: matched,
            matchedFingerprint: matchedFingerprint,
            mode: "fingerprint",
            error: matched ? "" : TLSFingerprintErrorMessages.pinningFailed,
            errorCode: matched ? "" : "PINNING_FAILED"
        ))
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
        guard trySetCompleted() else { return }

        if let error = error {
            let nsError = error as NSError

            // Timeout
            if nsError.code == NSURLErrorTimedOut {
                session.invalidateAndCancel()
                completion(TLSFingerprintResult(
                    fingerprintMatched: false,
                    error: TLSFingerprintErrorMessages.timeout,
                    errorCode: "TIMEOUT"
                ))
            }
            // Common network errors (non-timeout)
            else if nsError.domain == NSURLErrorDomain,
                    [NSURLErrorNotConnectedToInternet,
                     NSURLErrorNetworkConnectionLost,
                     NSURLErrorCannotFindHost,
                     NSURLErrorCannotConnectToHost].contains(nsError.code) {
                session.invalidateAndCancel()
                completion(TLSFingerprintResult(
                    fingerprintMatched: false,
                    error: TLSFingerprintErrorMessages.networkError,
                    errorCode: "NETWORK_ERROR"
                ))
            } else {
                // Fallback for other errors
                session.invalidateAndCancel()
                completion(TLSFingerprintResult(
                    fingerprintMatched: false,
                    error: TLSFingerprintErrorMessages.networkError,
                    errorCode: "NETWORK_ERROR"
                ))
            }
        }
    }
}
