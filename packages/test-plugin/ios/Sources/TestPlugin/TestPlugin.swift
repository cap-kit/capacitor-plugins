import Foundation
import Capacitor
import UIKit

/**
 Capacitor bridge for the Test plugin.

 This file MUST:
 - read input from CAPPluginCall
 - delegate logic to TestImpl
 - mapping native errors to JS error codes
 - resolving or rejecting calls exactly once
 */
@objc(TestPlugin)
public final class TestPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Properties

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

    // MARK: - Lifecycle

    /**
     Called once when the plugin is loaded by the Capacitor bridge.

     Responsibilities:
     - read static configuration
     - initialize native implementation
     - apply configuration-derived behavior
     */
    override public func load() {
        // Initialize TestConfig with the correct type
        let cfg = TestConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        TestLogger.debug("Test plugin loaded with config:", cfg)
    }

    // MARK: - Error Mapping

    /**
     Maps native TestError instances to JS-facing TestErrorCode values.

     IMPORTANT:
     - This is the ONLY place where native errors are translated
     - TestImpl must NEVER reference JS or Capacitor
     */
    private func reject(_ call: CAPPluginCall, error: TestError) {
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

    // MARK: - Echo

    /**
     Echoes a string back to JavaScript.

     - Validates input
     - Applies configuration-derived behavior
     - Delegates logic to the native implementation
     */
    @objc func echo(_ call: CAPPluginCall) {
        var value = call.getString("value") ?? ""

        // Log input only if verbose logging is enabled
        TestLogger.debug("Echoing value:", value)

        // Append the custom message from the configuration
        if let configMessage = config?.customMessage {
            value += configMessage
        }

        call.resolve([
            "value": implementation.echo(value)
        ])
    }

    // MARK: - Version

    /**
     Returns the native plugin version.

     The version is synchronized from package.json
     and shared across JS, Android, and iOS.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Settings

    /**
     Opens the iOS Settings page for the current application.
     */
    @objc func openAppSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            do {
                try self.implementation.openAppSettings()
                call.resolve()
            } catch let error as TestError {
                self.reject(call, error: error)
            } catch {
                call.reject("Unknown error", "UNAVAILABLE")
            }
        }
    }
}
