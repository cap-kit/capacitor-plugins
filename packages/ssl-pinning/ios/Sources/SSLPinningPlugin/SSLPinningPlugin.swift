import Foundation
import Capacitor

/**
 Capacitor bridge for the SSLPinning plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(SSLPinningPlugin)
public final class SSLPinningPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "SSLPinningPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "SSLPinning"

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
    private let implementation = SSLPinningImpl()

    /// Configuration instance
    private var config: SSLPinningConfig?

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read static configuration
     - initialize native resources
     - configure the implementation
     */
    override public func load() {
        // Initialize SSLPinningConfig with the correct type
        let cfg = SSLPinningConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        SSLPinningLogger.debug("SSLPinning plugin loaded")
    }

    // MARK: - Error Mapping

    /**
     Maps native SSLPinningError values
     to JavaScript-facing error codes.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: SSLPinningError
    ) {
        let code: String

        switch error {
        case .unavailable:
            code = "UNAVAILABLE"
        case .permissionDenied:
            code = "PERMISSION_DENIED"
        case .initFailed:
            code = "INIT_FAILED"
        case .unknownType:
            code = "UNKNOWN_TYPE"
        }

        call.reject(error.message, code)
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
                "Missing url",
                "UNKNOWN_TYPE"
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

                call.resolve(result)
            } catch let error as SSLPinningError {
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

        let fingerprints =
            call.getArray("fingerprints")?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }

        guard !url.isEmpty else {
            call.reject(
                "Missing url",
                "UNKNOWN_TYPE"
            )
            return
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

                call.resolve(result)
            } catch let error as SSLPinningError {
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
