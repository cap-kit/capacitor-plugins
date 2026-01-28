import Foundation
import Capacitor

/**
 Capacitor bridge for the Settings plugin.

 This file MUST:
 - read input from CAPPluginCall
 - delegate logic to SettingsImpl
 - resolve calls using state-based results
 */
@objc(SettingsPlugin)
public final class SettingsPlugin: CAPPlugin, CAPBridgedPlugin {

    // Configuration instance
    private var config: SettingsConfig?

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = SettingsImpl()

    /// The unique identifier for the plugin.
    public let identifier = "SettingsPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Settings"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     - `open`: A method that opens a specified iOS settings page.
     - `openIOS`: An alias for the `open` method, specifically for iOS
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "open", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openIOS", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read configuration values
     - initialize native resources
     - configure the implementation instance
     */
    override public func load() {
        // Initialize SettingsConfig with the correct type
        let cfg = SettingsConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        SettingsLogger.debug("Settings plugin loaded with config:", cfg)
    }

    // MARK: - Settings

    /// Opens the requested iOS settings page.
    @objc func open(_ call: CAPPluginCall) {
        resolveOpenResult(call, option: call.getString("optionIOS", ""))
    }

    /// Opens the requested iOS settings page.
    @objc func openIOS(_ call: CAPPluginCall) {
        resolveOpenResult(call, option: call.getString("option", ""))
    }

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Helpers

    /// Resolves the result of an open settings call.
    private func resolveOpenResult(
        _ call: CAPPluginCall,
        option: String
    ) {
        let result = implementation.open(option: option)

        var payload: [String: Any] = [
            "success": result.success
        ]

        if let error = result.error {
            payload["error"] = error
        }

        if let code = result.code {
            payload["code"] = code
        }

        call.resolve(payload)
    }
}
