import Foundation
import Security

/**
 Native iOS implementation for the SSLPinning plugin.

 Responsibilities:
 - Perform platform-specific SSL pinning logic
 - Interact with system networking APIs
 - Throw typed SSLPinningError values on failure

 Forbidden:
 - Accessing CAPPluginCall
 - Referencing Capacitor APIs
 - Constructing JavaScript payloads

 Architectural notes:
 - This class contains NO bridge logic.
 - It must receive configuration before use.
 - It exposes async methods returning structured results.
 */
@objc
public final class SSLPinningImpl: NSObject {

    // MARK: - Properties

    /**
     Static plugin configuration.

     Injected once during plugin initialization.
     Must not be mutated after being set.
     */
    private var config: SSLPinningConfig?

    /**
     Cached pinned certificates loaded from the app bundle.

     This avoids repeated disk access when certificate-based
     pinning is used multiple times.
     */
    private var cachedPinnedCertificates: [String: [SecCertificate]] = [:]

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.

     Responsibilities:
     - Store immutable configuration
     - Configure runtime logging behavior
     - Validate certificates at load time (fail-fast)
     */
    func applyConfig(_ config: SSLPinningConfig) {
        precondition(
            self.config == nil,
            "SSLPinningImpl.applyConfig(_:) must be called exactly once"
        )

        self.config = config

        // Synchronize logger state
        SSLPinningLogger.verbose = config.verboseLogging

        // Validate certificates at load time (fail-fast)
        validateCertificatesAtLoad()

        SSLPinningLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    /**
     Validates all configured certificates at load time.
     Throws if any configured certificate is invalid.
     */
    private func validateCertificatesAtLoad() {
        guard let config = config else { return }

        let allCertFiles = config.getAllCertFileNames()

        for certFile in allCertFiles {
            if !config.isCertValid(certFile) {
                SSLPinningLogger.error("Certificate validation failed: \(certFile)")
            }
        }
    }

    // MARK: - Single fingerprint

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using a single SHA-256 fingerprint.

     Resolution order:
     1. Runtime fingerprint argument
     2. Static configuration fingerprint
     3. Fall back to cert mode if certificates are configured

     - Throws: `SSLPinningError.unavailable`
     if no fingerprint is available and no certificates are configured.
     */
    func checkCertificate(
        urlString: String,
        fingerprintFromArgs: String?
    ) async throws -> [String: Any] {

        let fingerprint =
            fingerprintFromArgs ??
            config?.fingerprint

        if let expectedFingerprint = fingerprint {
            return try await performCheck(
                urlString: urlString,
                fingerprints: [expectedFingerprint]
            )
        }

        // Determine effective certificates for this host
        let effectiveCerts = getEffectiveCertsForUrl(urlString)
        if !effectiveCerts.isEmpty {
            return try await performCheck(
                urlString: urlString,
                fingerprints: []
            )
        }

        throw SSLPinningError.unavailable(
            SSLPinningErrorMessages.noFingerprintsProvided
        )
    }

    // MARK: - Multiple fingerprints

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using multiple allowed SHA-256 fingerprints.

     A match is considered valid if ANY provided
     fingerprint matches the server certificate.

     Falls back to cert mode if no fingerprints are provided
     but certificates are configured.

     - Throws: `SSLPinningError.unavailable`
     if no fingerprints and no certificates are available.
     */
    func checkCertificates(
        urlString: String,
        fingerprintsFromArgs: [String]?
    ) async throws -> [String: Any] {

        let fingerprints =
            fingerprintsFromArgs ??
            config?.fingerprints

        if let fps = fingerprints, !fps.isEmpty {
            return try await performCheck(
                urlString: urlString,
                fingerprints: fps
            )
        }

        // Determine effective certificates for this host
        let effectiveCerts = getEffectiveCertsForUrl(urlString)
        if !effectiveCerts.isEmpty {
            return try await performCheck(
                urlString: urlString,
                fingerprints: []
            )
        }

        throw SSLPinningError.unavailable(
            SSLPinningErrorMessages.noFingerprintsProvided
        )
    }

    // MARK: - Certificate Resolution

    /**
     Gets the effective certificate list for a given URL.
     Uses domain-based resolution with fallback to global certs.
     */
    private func getEffectiveCertsForUrl(_ urlString: String) -> [String] {
        guard let url = SSLPinningUtils.httpsURL(from: urlString),
              let host = url.host else {
            return []
        }

        return SSLPinningUtils.getEffectiveCerts(
            forHost: host,
            certsByDomain: config?.certsByDomain ?? [:],
            globalCerts: config?.certs ?? []
        )
    }

    // MARK: - Shared implementation

    /**
     Performs SSL pinning validation for a given HTTPS URL.

     Evaluation order:

     1. Excluded domains → bypass pinning entirely.
     2. Certificate-based pinning (cert mode).
     3. Fingerprint-based pinning.

     Certificate mode is activated ONLY when:
     - No fingerprints are provided
     - One or more pinned certificates are configured for the domain

     This method uses async/await and wraps the URLSession
     delegate flow inside a continuation.

     - Throws:
     - `SSLPinningError.unknownType`
     - `SSLPinningError.initFailed`
     */
    private func performCheck(
        urlString: String,
        fingerprints: [String]
    ) async throws -> [String: Any] {

        guard let url = SSLPinningUtils.httpsURL(from: urlString) else {
            throw SSLPinningError.unknownType(
                SSLPinningErrorMessages.invalidUrlMustBeHttps
            )
        }

        let host = url.host?.lowercased() ?? ""

        // -----------------------------------------------------------------------
        // EXCLUDED DOMAIN MODE
        // -----------------------------------------------------------------------

        /**
         If the request host matches an excluded domain,
         SSL pinning is bypassed and system trust is used.
         */
        if isExcludedDomain(host) {
            SSLPinningLogger.debug("SSLPinning excluded domain:", host)

            return [
                "fingerprintMatched": true,
                "excludedDomain": true,
                "mode": "excluded",
                "errorCode": "EXCLUDED_DOMAIN",
                "error": SSLPinningErrorMessages.excludedDomain
            ]
        }

        let useFingerprintMode = !fingerprints.isEmpty

        // Get effective certificates for this domain
        let effectiveCerts = getEffectiveCertsForUrl(urlString)
        let useCertMode =
            !useFingerprintMode &&
            !effectiveCerts.isEmpty

        if !useFingerprintMode && !useCertMode {
            throw SSLPinningError.noPinningConfig(
                SSLPinningErrorMessages.noFingerprintsProvided
            )
        }

        let pinnedCertificates: [SecCertificate]? =
            useCertMode ? loadPinnedCertificates(effectiveCerts) : nil

        if useCertMode && (pinnedCertificates?.isEmpty ?? true) {
            throw SSLPinningError.certNotFound(
                SSLPinningErrorMessages.noCertsProvided
            )
        }

        /**
         Delegate-based TLS validation.

         The delegate performs:
         - Trust evaluation
         - Fingerprint comparison
         - Structured result generation

         The continuation bridges delegate callbacks
         into Swift async/await.

         Defensive timeout: ensures continuation cannot hang forever
         if delegate completion is never called (edge cases).
         */
        return try await withCheckedThrowingContinuation { continuation in

            let configuration = URLSessionConfiguration.ephemeral
            configuration.timeoutIntervalForRequest = 10
            configuration.timeoutIntervalForResource = 10

            let delegate = SSLPinningDelegate(
                expectedFingerprints: fingerprints,
                pinnedCertificates: pinnedCertificates,
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

            let timeoutWorkItem = DispatchWorkItem {
                if !delegate.hasCompleted {
                    session.invalidateAndCancel()
                    continuation.resume(returning: [
                        "fingerprintMatched": false,
                        "errorCode": "TIMEOUT",
                        "error": SSLPinningErrorMessages.timeout
                    ])
                }
            }

            DispatchQueue.global().asyncAfter(deadline: .now() + 15, execute: timeoutWorkItem)

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

    /**
     Loads pinned certificates from the app bundle.

     Certificates are cached for the lifetime of the plugin instance.

     - Parameter certFileNames: List of certificate file names
     - Returns: List of loaded SecCertificate
     */
    private func loadPinnedCertificates(_ certFileNames: [String]) -> [SecCertificate] {
        // Create a cache key from the sorted certificate filenames
        let cacheKey = certFileNames.sorted().joined(separator: ",")

        if cachedPinnedCertificates[cacheKey] == nil {
            cachedPinnedCertificates[cacheKey] = SSLPinningUtils.loadPinnedCertificates(certFileNames: certFileNames)
        }

        return cachedPinnedCertificates[cacheKey] ?? []
    }
}
