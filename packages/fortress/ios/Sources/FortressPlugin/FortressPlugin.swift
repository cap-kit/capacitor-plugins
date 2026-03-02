import Foundation
import Capacitor

/**
 Capacitor bridge for the Fortress plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(FortressPlugin)
public final class FortressPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "FortressPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Fortress"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    private let implementation = Fortress()

    /// Configuration instance
    private var config: Config?

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
        // Initialize Config with the correct type
        let cfg = Config(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        Logger.info("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Error mapping

    /**
     Maps native `NativeError` values to JS-facing error codes.

     CONTRACT:
     - Error codes MUST be stable and documented
     - Error codes MUST match across platforms
     - Platform-specific error codes are FORBIDDEN
     */
    private func reject(
        _ call: CAPPluginCall,
        error: NativeError
    ) {
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let nativeError = error as? NativeError {
            reject(call, error: nativeError)
        } else {
            let message = error.localizedDescription.isEmpty
                ? ErrorMessages.unexpectedNativeError
                : error.localizedDescription
            reject(call, error: .initFailed(message))
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
