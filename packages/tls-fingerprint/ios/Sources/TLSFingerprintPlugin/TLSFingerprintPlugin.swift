import Foundation
import Capacitor

/**
 Capacitor bridge for the TLSFingerprint plugin.

 Responsibilities:
 - Parse JS input
 - Delegate to TLSFingerprintImpl
 - Resolve or reject CAPPluginCall
 */
@objc(TLSFingerprintPlugin)
public final class TLSFingerprintPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "TLSFingerprintPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "TLSFingerprint"

    /**
     * A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     * - `checkCertificate`: Validates an SSL certificate for a given URL.
     * - `checkCertificates`: Validates multiple SSL certificates for given URLs.
     * - `getPluginVersion`: Retrieves the current version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkCertificate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkCertificates", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    private let implementation = TLSFingerprintImpl()

    /// Configuration instance
    private var config: TLSFingerprintConfig?

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read configuration values
     - initialize native resources
     - configure the implementation instance
     */
    override public func load() {
        // Initialize TLSFingerprintConfig with the correct type
        let cfg = TLSFingerprintConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        TLSFingerprintLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native TLSFingerprintError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: TLSFingerprintError
    ) {
        // Use the centralized errorCode and message defined in TLSFingerprintError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let settingsError = error as? TLSFingerprintError {
            call.reject(settingsError.message, settingsError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    // MARK: - SSL Pinning (single fingerprint)

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using a single fingerprint.
     */
    @objc func checkCertificate(_ call: CAPPluginCall) {
        let url = call.getString("url", "")
        let fingerprintValue = call.getString("fingerprint")
        let fingerprint =
            fingerprintValue?.isEmpty == false
            ? fingerprintValue
            : nil

        guard !url.isEmpty else {
            call.reject(
                TLSFingerprintErrorMessages.urlRequired,
                "INVALID_INPUT"
            )
            return
        }

        guard let urlObj = URL(string: url), let host = urlObj.host, !host.isEmpty else {
            call.reject(
                TLSFingerprintErrorMessages.noHostFoundInUrl,
                "INVALID_INPUT"
            )
            return
        }

        if let fp = fingerprint, !TLSFingerprintUtils.isValidFingerprintFormat(fp) {
            call.reject(
                TLSFingerprintErrorMessages.invalidFingerprintFormat,
                "INVALID_INPUT"
            )
            return
        }

        Task {
            do {
                let result =
                    try await implementation.checkCertificate(
                        urlString: url,
                        fingerprintFromArgs: fingerprint
                    )

                // Converting Swift TLSFingerprintResult to JSObject
                call.resolve(result.toDictionary())
            } catch let error as TLSFingerprintError {
                self.reject(call, error: error)
            } catch {
                call.reject(
                    error.localizedDescription,
                    "INIT_FAILED"
                )
            }
        }
    }

    // MARK: - SSL Pinning (multiple fingerprints)

    /**
     Validates the SSL certificate of a HTTPS endpoint
     using multiple allowed fingerprints.
     */
    @objc func checkCertificates(_ call: CAPPluginCall) {
        let url = call.getString("url", "")

        // Extraction and filtering of optional fingerprints
        let fingerprints =
            call.getArray("fingerprints")?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }

        guard !url.isEmpty else {
            call.reject(
                TLSFingerprintErrorMessages.urlRequired,
                "INVALID_INPUT"
            )
            return
        }

        guard let urlObj = URL(string: url), let host = urlObj.host, !host.isEmpty else {
            call.reject(
                TLSFingerprintErrorMessages.noHostFoundInUrl,
                "INVALID_INPUT"
            )
            return
        }

        if let fps = fingerprints {
            for fp in fps {
                if !TLSFingerprintUtils.isValidFingerprintFormat(fp) {
                    call.reject(
                        TLSFingerprintErrorMessages.invalidFingerprintFormat,
                        "INVALID_INPUT"
                    )
                    return
                }
            }
        }

        Task {
            do {
                let result =
                    try await implementation.checkCertificates(
                        urlString: url,
                        fingerprintsFromArgs:
                            fingerprints?.isEmpty == false
                            ? fingerprints
                            : nil
                    )

                call.resolve(result.toDictionary())
            } catch let error as TLSFingerprintError {
                self.reject(call, error: error)
            } catch {
                call.reject(
                    error.localizedDescription,
                    "INIT_FAILED"
                )
            }
        }
    }

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }
}
