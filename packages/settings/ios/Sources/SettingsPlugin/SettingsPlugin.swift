import Foundation
import Capacitor

/**
 Capacitor bridge for the Settings plugin.

 Responsibilities:
 - Parse JS input
 - Delegate to SettingsImpl
 - Resolve or reject CAPPluginCall
 */
@objc(SettingsPlugin)
public final class SettingsPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = SettingsImpl()

    // Configuration instance
    private var config: SettingsConfig?

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
        // Initialize SettingsConfig with the correct type
        let cfg = SettingsConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        SettingsLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native SettingsError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: SettingsError
    ) {
        // Use the centralized errorCode and message defined in SettingsError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let settingsError = error as? SettingsError {
            call.reject(settingsError.message, settingsError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    // MARK: - Settings

    /// Opens the requested iOS settings page.
    @objc func open(_ call: CAPPluginCall) {
        let option = call.getString("optionIOS")
        guard let option = option, !option.isEmpty else {
            reject(call, error: .invalidInput("`optionIOS` must be provided and not empty."))
            return
        }
        handleOpen(call, option: option)
    }

    /// Opens the requested iOS settings page.
    @objc func openIOS(_ call: CAPPluginCall) {
        let option = call.getString("option")
        guard let option = option, !option.isEmpty else {
            reject(call, error: .invalidInput("`option` must be provided and not empty."))
            return
        }
        handleOpen(call, option: option)
    }

    ///
    private func handleOpen(_ call: CAPPluginCall, option: String) {
        do {
            try implementation.open(option: option)
            call.resolve()
        } catch let error as SettingsError {
            reject(call, error: error)
        } catch {
            reject(call, error: .initFailed(error.localizedDescription))
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
