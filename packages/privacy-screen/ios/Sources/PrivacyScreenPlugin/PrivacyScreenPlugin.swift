import Foundation
import Capacitor

/**
 Capacitor bridge for the PrivacyScreen plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(PrivacyScreenPlugin)
public final class PrivacyScreenPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "PrivacyScreenPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "PrivacyScreen"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "enable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isEnabled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addListener", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeAllListeners", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    let implementation = PrivacyScreenImpl()

    /// Configuration instance
    var config: PrivacyScreenConfig?

    // MARK: - Lifecycle

    override public func load() {
        let cfg = PrivacyScreenConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        if cfg.enabled {
            implementation.enable(blurEffect: nil)
        }

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleUserDidTakeScreenshot),
            name: UIApplication.userDidTakeScreenshotNotification,
            object: nil
        )

// Screen recording notifications (iOS 11+)
        // Observe screen recording start/stop events posted by iOS when user
        // starts/stops screen recording via Control Center.
        // Notifications are only posted on iOS 11+.
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleScreenRecordingStarted),
            name: NSNotification.Name(rawValue: "UIApplicationScreenRecordingDidStartNotification"),
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleScreenRecordingStopped),
            name: NSNotification.Name(rawValue: "UIApplicationScreenRecordingDidEndNotification"),
            object: nil
        )

        PrivacyScreenLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func handleUserDidTakeScreenshot() {
        notifyListeners("screenshotTaken", data: nil)
    }

    @objc private func handleScreenRecordingStarted() {
        notifyListeners("screenRecordingStarted", data: nil)
    }

    @objc private func handleScreenRecordingStopped() {
        notifyListeners("screenRecordingStopped", data: nil)
    }

    // MARK: - Version

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Privacy Screen

    /**
     Enables privacy-screen protection.

     The JS argument arrives flattened: `{ android, ios }` are top-level keys.
     Only `ios.blurEffect` is honored natively (`light` / `dark` / `none`
     mapping to the overlay blur style, defaulting to the system style);
     unknown values fall back to the system style.
     */
    @objc func enable(_ call: CAPPluginCall) {
        let blurEffect = call.getObject("ios")?["blurEffect"] as? String
        implementation.enable(blurEffect: blurEffect)
        call.resolve(["success": true])
    }

    @objc func disable(_ call: CAPPluginCall) {
        implementation.disable()
        call.resolve(["success": true])
    }

    @objc func isEnabled(_ call: CAPPluginCall) {
        call.resolve(["enabled": implementation.isEnabled()])
    }

    // MARK: - Error Mapping

    func reject(
        _ call: CAPPluginCall,
        error: PrivacyScreenError
    ) {
        call.reject(error.message, error.errorCode)
    }

    func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let privacyError = error as? PrivacyScreenError {
            call.reject(privacyError.message, privacyError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }
}
