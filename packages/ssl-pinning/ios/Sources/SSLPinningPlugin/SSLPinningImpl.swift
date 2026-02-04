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
 */
@objc
public final class SSLPinningImpl: NSObject {

    // MARK: - Properties

    /**
     Immutable plugin configuration.
     Injected once during plugin initialization.
     */
    private var config: SSLPinningConfig?

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.
     */
    func applyConfig(_ config: SSLPinningConfig) {
        self.config = config
        SSLPinningLogger.verbose = config.verboseLogging
        SSLPinningLogger.debug("Configuration applied. Verbose logging:", config.verboseLogging)
    }

    // MARK: - Single fingerprint

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using a single SHA-256 fingerprint.
     */
    func checkCertificate(
        urlString: String,
        fingerprintFromArgs: String?
    ) async throws -> [String: Any] {

        let fingerprint =
            fingerprintFromArgs ??
            config?.fingerprint

        guard let expectedFingerprint = fingerprint else {
            throw SSLPinningError.unavailable(
                "No fingerprint provided (args or config)"
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
     */
    func checkCertificates(
        urlString: String,
        fingerprintsFromArgs: [String]?
    ) async throws -> [String: Any] {

        let fingerprints =
            fingerprintsFromArgs ??
            config?.fingerprints

        guard let fingerprints,
              !fingerprints.isEmpty else {
            throw SSLPinningError.unavailable(
                "No fingerprints provided (args or config)"
            )
        }

        return try await performCheck(
            urlString: urlString,
            fingerprints: fingerprints
        )
    }

    // MARK: - Shared implementation

    /**
     Performs the actual SSL pinning validation.

     This method:
     - Validates the HTTPS URL
     - Creates an ephemeral URLSession
     - Intercepts the TLS handshake via URLSessionDelegate
     - Compares the server leaf certificate fingerprint
     against the expected ones

     IMPORTANT:
     - The system trust chain is NOT evaluated
     - Only fingerprint matching determines acceptance
     */
    private func performCheck(
        urlString: String,
        fingerprints: [String]
    ) async throws -> [String: Any] {

        guard let url = SSLPinningUtils.httpsURL(from: urlString) else {
            throw SSLPinningError.unknownType(
                "Invalid HTTPS URL"
            )
        }

        return try await withCheckedThrowingContinuation { continuation in
            let delegate = SSLPinningDelegate(
                expectedFingerprints: fingerprints,
                verboseLogging: config?.verboseLogging ?? false
            ) { result in
                continuation.resume(returning: result)
            }

            let session = URLSession(
                configuration: .ephemeral,
                delegate: delegate,
                delegateQueue: nil
            )

            session.dataTask(with: url).resume()
        }
    }
}
