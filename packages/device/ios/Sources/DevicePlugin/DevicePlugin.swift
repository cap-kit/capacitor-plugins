import Foundation
import Capacitor

/**
 * Capacitor bridge for the Device plugin.
 *
 * This class handles the communication between the JavaScript layer and the native iOS implementation.
 * It is responsible for input validation, configuration merging, and thread safety.
 */
@objc(DevicePlugin)
public final class DevicePlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = DeviceImpl()

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: DeviceConfig?

    /// The unique identifier for the plugin used by the Capacitor bridge.
    public let identifier = "DevicePlugin"

    /// The name used to reference this plugin in JavaScript (e.g., Device.getPluginVersion()).
    public let jsName = "Device"

    /**
     * A list of methods exposed by this plugin to the JavaScript layer.
     * All methods defined here must be implemented with the @objc attribute.
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

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
        // Initialize DeviceConfig with the correct type
        let cfg = DeviceConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        DeviceLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native DeviceError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: DeviceError
    ) {
        // Use the centralized errorCode and message defined in DeviceError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let deviceError = error as? DeviceError {
            call.reject(deviceError.message, deviceError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    // MARK: - Version

    /**
     * Retrieves the current native plugin version.
     *
     * This version is synchronized from the project's package.json during the build process.
     *
     * - Parameter call: CAPPluginCall used to return the version string.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }
}
