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
    ///
    /// CONTRACT:
    /// - MUST match the plugin registration name used by Capacitor
    /// - MUST remain stable across releases (breaking change otherwise)
    public let identifier = "IntegrityPlugin"

    /// The name used to reference this plugin in JavaScript.
    ///
    /// CONTRACT:
    /// - MUST match `registerPlugin({ name })` on the JS side
    /// - Changing this value breaks JS ↔ native binding
    public let jsName = "Integrity"

    /**
     A list of methods exposed by this plugin.
     - `check`:
     - `presentBlockPage`:
     - `getPluginVersion`: A method that returns the version of the plugin.

     CONTRACT:
     - Every method listed here MUST:
     - be implemented exactly once
     - resolve or reject its CAPPluginCall exactly once
     - return a Promise on the JS side

     NOTE:
     - No method in this list may perform platform logic directly.
     - All business logic MUST be delegated to the Impl layer.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "check", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "presentBlockPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation instance.
    ///
    /// NOTE:
    /// - Owned by the Plugin layer
    /// - Lifetime == plugin lifetime
    /// - MUST NOT be recreated per call
    ///
    /// WARNING:
    /// - The implementation MUST NOT access:
    ///   - CAPPluginCall
    ///   - bridge
    ///   - view controllers
    private let implementation = IntegrityImpl()

    // Configuration instance
    //
    // CONTRACT:
    // - Initialized once during load()
    // - Treated as immutable afterwards
    // - NEVER read directly by the Impl layer
    private var config: IntegrityConfig?

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     CONTRACT:
     - Called exactly once by Capacitor
     - This is the ONLY valid place to:
     - read plugin configuration
     - create IntegrityConfig
     - inject configuration into the Impl layer

     WARNING:
     - Calling applyConfig(_:) outside this method is FORBIDDEN
     */
    override public func load() {
        // NOTE:
        // Consider guarding against double-initialization in debug builds
        // to detect invalid Capacitor lifecycle usage early.

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

     CONTRACT:
     - Error codes MUST be stable and documented
     - Error codes MUST match across platforms
     - No platform-specific codes are allowed here
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

     CONTRACT:
     - Resolves exactly once on success
     - Rejects exactly once on failure
     - Never throws outside this scope

     NOTE:
     - All option normalization happens here
     - The Impl layer MUST receive fully normalized options
     */
    @objc func check(_ call: CAPPluginCall) {
        // NOTE:
        // Defaulting logic lives in the Plugin layer by design.
        // The Impl layer MUST NOT know about JS defaults or option absence.

        // WARNING:
        // Any error escaping `performCheck` MUST be mapped here.
        // Uncaught native errors are considered a plugin defect.

        do {
            let options =
                try call.decode(IntegrityCheckOptions.self)

            let normalizedOptions = IntegrityCheckOptions(
                level: options.level ?? "basic",
                includeDebugInfo: options.includeDebugInfo ?? false
            )

            let result =
                try implementation.performCheck(
                    options: normalizedOptions
                )

            call.resolve(result)

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

     CONTRACT:
     - UI decisions are delegated to the host app
     - This method only performs presentation when explicitly requested

     WARNING:
     - UI work MUST be dispatched to the main thread
     - Failure to access the root view controller MUST reject the call
     */
    @objc func presentBlockPage(_ call: CAPPluginCall) {
        // NOTE:
        // Returning `{ presented: false }` is NOT an error.
        // This allows the JS side to branch deterministically
        // without relying on rejection for control flow.

        // WARNING:
        // The Impl layer MUST NEVER perform UI presentation.
        // This method is the ONLY allowed UI entry point for this plugin.

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
            // CONTRACT:
            // All UIKit interactions MUST occur on the main thread.
            guard let rootVC = self.bridge?.viewController else {
                call.reject(
                    "View controller not available",
                    "UNAVAILABLE"
                )
                return
            }

            let blockVC = IntegrityBlockViewController(
                url: finalURL,
                dismissible: dismissible
            )
            let nav = UINavigationController(rootViewController: blockVC)
            rootVC.present(nav, animated: true) {
                call.resolve(["presented": true])
            }
        }
    }

    // MARK: - Version

    /**
     Returns the native plugin version.
     - Used for diagnostics and compatibility checks only
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }
}
