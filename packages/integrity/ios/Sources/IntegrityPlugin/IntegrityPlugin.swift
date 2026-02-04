import Foundation
import Capacitor
import UIKit

/**
 Capacitor bridge for the Integrity plugin (iOS).

 Responsibilities:
 - Parse JavaScript input
 - Invoke the native implementation
 - Resolve or reject CAPPluginCall exactly once
 - Map native IntegrityError to JS-facing error codes

 Forbidden:
 - Platform-specific business logic
 - System API usage
 - Throwing uncaught exceptions
 */
@objc(IntegrityPlugin)
public final class IntegrityPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "IntegrityPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Integrity"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     - `check`:
     - `presentBlockPage`:
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "check", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "presentBlockPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = IntegrityImpl()

    // Configuration instance
    private var config: IntegrityConfig?

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.

     This is the correct place to:
     - read static configuration
     - initialize native resources
     - inject configuration into the implementation
     */
    override public func load() {
        // Initialize IntegrityConfig with the correct type
        let cfg = IntegrityConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        IntegrityLogger.debug("Integrity plugin loaded")
    }

    // MARK: - Error mapping

    /**
     Maps native IntegrityError values to JS-facing error codes.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: IntegrityError
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

    // MARK: - Integrity check

    /**
     Executes a baseline integrity check.
     */
    @objc func check(_ call: CAPPluginCall) {
        do {
            var signals = try implementation.checkJailbreakSignals()

            let isSimulator = implementation.isSimulator()
            if isSimulator {
                signals.append([
                    "id": "ios_simulator",
                    "category": "emulator",
                    "confidence": "high"
                ])
            }

            if try implementation.checkFridaLibraries() {
                signals.append([
                    "id": "ios_frida_library",
                    "category": "hook",
                    "confidence": "high"
                ])
            }

            if try implementation.checkFridaThreads() {
                signals.append([
                    "id": "ios_frida_thread",
                    "category": "hook",
                    "confidence": "medium"
                ])
            }

            if try !implementation.checkBundleIntegrity() {
                signals.append([
                    "id": "ios_bundle_integrity",
                    "category": "tamper",
                    "confidence": "high"
                ])
            }

            let confidenceWeights: [String: Int] = [
                "high": 30,
                "medium": 15,
                "low": 5
            ]

            let score = signals.reduce(0) { acc, signal in
                guard let confidence = signal["confidence"] as? String else {
                    return acc
                }
                return acc + (confidenceWeights[confidence] ?? 0)
            }

            call.resolve([
                "signals": signals,
                "score": score,
                "compromised": score >= 30,
                "environment": [
                    "platform": "ios",
                    "isEmulator": isSimulator,
                    "isDebugBuild": false
                ],
                "timestamp": Int(Date().timeIntervalSince1970 * 1000)
            ])
        } catch let error as IntegrityError {
            reject(call, error: error)
        } catch {
            call.reject(
                "Unexpected native error during integrity check.",
                "INIT_FAILED"
            )
        }
    }

    // MARK: - Present block page

    /**
     Presents the configured integrity block page, if enabled.

     The plugin never decides *when* this method should be called.
     The decision is fully delegated to the host application.
     */
    @objc func presentBlockPage(_ call: CAPPluginCall) {
        guard
            let blockPage = config?.blockPage,
            blockPage.enabled,
            let baseURL = blockPage.url
        else {
            call.resolve(["presented": false])
            return
        }

        let reason = call.getString("reason")
        let dismissible = call.getBool("dismissible") ?? false

        let finalURL =
            reason != nil
            ? "\(baseURL)?reason=\(reason!)"
            : baseURL

        DispatchQueue.main.async {
            guard let rootVC = self.bridge?.viewController else {
                return
            }

            let blockVC = IntegrityBlockViewController(
                url: finalURL,
                dismissible: dismissible
            )
            let nav = UINavigationController(rootViewController: blockVC)
            rootVC.present(nav, animated: true)
        }

        call.resolve(["presented": true])
    }

    // MARK: - Version

    /**
     Returns the native plugin version.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }
}
