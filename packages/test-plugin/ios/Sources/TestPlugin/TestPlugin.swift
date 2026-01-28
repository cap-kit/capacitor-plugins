import Foundation
import Capacitor

/**
 Capacitor bridge for the Test plugin.

 This file MUST:
 - read input from CAPPluginCall
 - delegate logic to TestImpl
 - resolve calls using state-based results
 */
@objc(TestPlugin)
public final class TestPlugin: CAPPlugin, CAPBridgedPlugin {

    // Configuration instance
    private var config: TestConfig?

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = TestImpl()

    /// The unique identifier for the plugin.
    public let identifier = "TestPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "Test"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     - `echo`: A method that accepts a string and returns the same string.
     - `getPluginVersion`: A method that returns the version of the plugin.
     - `openAppSettings`: A method that opens the app's settings page.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openAppSettings", returnType: CAPPluginReturnPromise)
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
        // Initialize TestConfig with the correct type
        let cfg = TestConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        TestLogger.debug("Test plugin loaded with config:", cfg)
    }

    // MARK: - Echo

    /**
     Echoes a string back to JavaScript.

     This method validates input, applies configuration-derived behavior,
     and delegates the core logic to the native implementation.
     */
    @objc func echo(_ call: CAPPluginCall) {
        let jsValue = call.getString("value", "")

        let valueToEcho: String
        if !jsValue.isEmpty {
            valueToEcho = jsValue
        } else {
            valueToEcho = config?.customMessage ?? ""
        }

        // Log input only if verbose logging is enabled
        TestLogger.debug("Echoing value:", valueToEcho)

        call.resolve([
            "value": implementation.echo(valueToEcho)
        ])
    }

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Settings

    /// Opens the iOS Settings app specifically for this application.
    @objc func openAppSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async(execute: DispatchWorkItem {
            guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
                TestLogger.error("Cannot create settings URL")
                call.resolve()
                return
            }

            if UIApplication.shared.canOpenURL(settingsUrl) {
                UIApplication.shared.open(settingsUrl, options: [:]) { success in
                    if !success {
                        TestLogger.error("Failed to open app settings")
                    }
                    call.resolve()
                }
            } else {
                TestLogger.error("Cannot open settings URL")
                call.resolve()
            }
        })
    }
}
