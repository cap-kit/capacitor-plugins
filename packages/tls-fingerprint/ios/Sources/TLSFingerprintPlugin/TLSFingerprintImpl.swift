import Foundation

/**
 Native iOS implementation for the TLSFingerprint plugin.

 Responsibilities:
 - Perform platform-specific operations
 - Throw typed TLSFingerprintError values on failure

 Forbidden:
 - Accessing CAPPluginCall
 - Referencing Capacitor APIs
 - Constructing JS payloads
 */
@objc public final class TLSFingerprintImpl: NSObject {

    // MARK: - Constants

    private static let timeoutSeconds: TimeInterval = 10

    // MARK: - Properties

    /**
     Static plugin configuration.

     Injected once during plugin initialization.
     Must not be mutated after being set.
     */
    private var config: TLSFingerprintConfig?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.

     Responsibilities:
     - Store immutable configuration
     - Configure runtime logging behavior
     */
    func applyConfig(_ config: TLSFingerprintConfig) {
        precondition(
            self.config == nil,
            "TLSFingerprintImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        TLSFingerprintLogger.verbose = config.verboseLogging

        TLSFingerprintLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Single fingerprint

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using a single SHA-256 fingerprint.

     Resolution order:
     1. Runtime fingerprint argument
     2. Static configuration fingerprint

     - Throws: `TLSFingerprintError.unavailable`
     if no fingerprint is available.
     */
    func checkCertificate(
        urlString: String,
        fingerprintFromArgs: String?
    ) async throws -> TLSFingerprintResult {

        let fingerprint =
            fingerprintFromArgs ??
            config?.fingerprint

        guard let expectedFingerprint = fingerprint else {
            throw TLSFingerprintError.unavailable(
                TLSFingerprintErrorMessages.noFingerprintsProvided
            )
        }

        return try await performCheck(
            urlString: urlString,
            fingerprints: [expectedFingerprint]
        )
    }

    // MARK: - Multiple fingerprints

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using multiple allowed SHA-256 fingerprints.

     A match is considered valid if ANY provided
     fingerprint matches the server certificate.

     - Throws: `TLSFingerprintError.unavailable`
     if no fingerprints are available.
     */
    func checkCertificates(
        urlString: String,
        fingerprintsFromArgs: [String]?
    ) async throws -> TLSFingerprintResult {

        let fingerprints =
            fingerprintsFromArgs ??
            config?.fingerprints

        guard let fps = fingerprints, !fps.isEmpty else {
            throw TLSFingerprintError.unavailable(
                TLSFingerprintErrorMessages.noFingerprintsProvided
            )
        }

        return try await performCheck(
            urlString: urlString,
            fingerprints: fps
        )
    }

    // MARK: - Shared implementation

    /**
     Performs SSL fingerprint validation for a given HTTPS URL.

     Evaluation order:

     1. Excluded domains → bypass validation entirely.
     2. Fingerprint-based validation.

     This method uses async/await and wraps the URLSession
     delegate flow inside a continuation.

     - Throws:
     - `TLSFingerprintError.unknownType`
     - `TLSFingerprintError.initFailed`
     */
    private func performCheck(
        urlString: String,
        fingerprints: [String]
    ) async throws -> TLSFingerprintResult {

        guard let url = TLSFingerprintUtils.httpsURL(from: urlString) else {
            throw TLSFingerprintError.unknownType(
                TLSFingerprintErrorMessages.invalidUrlMustBeHttps
            )
        }

        let host = url.host?.lowercased() ?? ""

        // -----------------------------------------------------------------------
        // EXCLUDED DOMAIN MODE
        // -----------------------------------------------------------------------

        /**
         If the request host matches an excluded domain,
         we still perform the connection to retrieve the actual fingerprint
         for parity with Android behavior.

         The connection uses a permissive trust manager.
         System trust chain is NOT explicitly evaluated.
         */
        if isExcludedDomain(host) {
            TLSFingerprintLogger.debug("TLSFingerprint excluded domain:", host)

            return try await withCheckedThrowingContinuation { continuation in
                let configuration = URLSessionConfiguration.ephemeral
                configuration.timeoutIntervalForRequest = Self.timeoutSeconds
                configuration.timeoutIntervalForResource = Self.timeoutSeconds

                let delegate = TLSFingerprintDelegate(
                    expectedFingerprints: [],
                    excludedDomains: config?.excludedDomains ?? [],
                    completion: { result in
                        continuation.resume(returning: result)
                    },
                    verboseLogging: config?.verboseLogging ?? false
                )

                let session = URLSession(
                    configuration: configuration,
                    delegate: delegate,
                    delegateQueue: nil
                )

                delegate.setSession(session)

                let timeoutWorkItem = DispatchWorkItem { [weak delegate] in
                    guard let delegate = delegate else { return }
                    let shouldTimeout = delegate.trySetCompleted()
                    if shouldTimeout {
                        session.invalidateAndCancel()
                        continuation.resume(returning: TLSFingerprintResult(
                            fingerprintMatched: false,
                            error: TLSFingerprintErrorMessages.timeout,
                            errorCode: "TIMEOUT"
                        ))
                    }
                }

                DispatchQueue.global().asyncAfter(deadline: .now() + Self.timeoutSeconds, execute: timeoutWorkItem)

                session.dataTask(with: url).resume()
            }
        }

        return try await withCheckedThrowingContinuation { continuation in

            let configuration = URLSessionConfiguration.ephemeral
            configuration.timeoutIntervalForRequest = 10
            configuration.timeoutIntervalForResource = 10

            let delegate = TLSFingerprintDelegate(
                expectedFingerprints: fingerprints,
                excludedDomains: config?.excludedDomains ?? [],
                completion: { result in
                    continuation.resume(returning: result)
                },
                verboseLogging: config?.verboseLogging ?? false
            )

            let session = URLSession(
                configuration: configuration,
                delegate: delegate,
                delegateQueue: nil
            )

            delegate.setSession(session)

            let timeoutWorkItem = DispatchWorkItem { [weak delegate] in
                guard let delegate = delegate else { return }
                let shouldTimeout = delegate.trySetCompleted()
                if shouldTimeout {
                    session.invalidateAndCancel()
                    continuation.resume(returning: TLSFingerprintResult(
                        fingerprintMatched: false,
                        error: TLSFingerprintErrorMessages.timeout,
                        errorCode: "TIMEOUT"
                    ))
                }
            }

            DispatchQueue.global().asyncAfter(deadline: .now() + Self.timeoutSeconds, execute: timeoutWorkItem)

            session.dataTask(with: url).resume()
        }
    }

    /**
     Determines whether the given host
     matches one of the configured excluded domains.

     Matching rules:
     - Exact match
     - Subdomain match

     Matching is case-insensitive.
     */
    private func isExcludedDomain(_ host: String) -> Bool {
        let hostLower = host.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        return config?.excludedDomains.contains { excluded in
            let excludedLower = excluded.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
            // Match exact domain or any subdomain (e.g., api.example.com matches example.com)
            return hostLower == excludedLower || hostLower.hasSuffix("." + excludedLower)
        } ?? false
    }
}
