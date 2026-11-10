import Foundation
import UIKit
import Capacitor

/**
 * Capacitor bridge for the LocalNetwork plugin.
 *
 * iOS has no equivalent of Android's local-network runtime permission:
 * the system shows the local-network privacy prompt on first local-network
 * access and exposes no public API to query or request it. Both permission
 * methods therefore resolve as `granted`, mirroring the web implementation.
 */
@objc(LocalNetworkPlugin)
public class LocalNetworkPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "LocalNetworkPlugin"
    public let jsName = "LocalNetwork"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    /// The only state iOS can ever report: the platform exposes no API to
    /// query or request local-network access.
    private static let grantedState = "granted"

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: LocalNetworkConfig?

    /// Last reported permission state to avoid emitting duplicate events.
    private var lastPermissionState: String?

    /// Lifecycle observers removed when the plugin is deallocated.
    private var observers: [NSObjectProtocol] = []

    // MARK: - Lifecycle

    /**
     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read configuration values
     - configure logging verbosity
     */
    override public func load() {
        let cfg = LocalNetworkConfig(plugin: self)
        self.config = cfg
        LocalNetworkLogger.verbose = cfg.verboseLogging

        LocalNetworkLogger.debug("Plugin loaded. Verbose logging:", cfg.verboseLogging)
        self.lastPermissionState = Self.grantedState

        // CAPPlugin exposes no resume hook on iOS (handleOnResume is an
        // Android-only lifecycle callback), so the app lifecycle is observed
        // directly through UIKit notifications.
        self.observers.append(
            NotificationCenter.default.addObserver(
                forName: UIApplication.didBecomeActiveNotification,
                object: nil,
                queue: OperationQueue.main
            ) { [weak self] _ in
                self?.checkForPermissionChange()
            }
        )
    }

    deinit {
        for observer in self.observers {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    /**
     Check for permission changes when the app becomes active again.

     iOS cannot observe the local-network privacy prompt, so the state is
     always `granted` and this branch never fires. It is kept so that the
     event contract stays identical across platforms.
     */
    private func checkForPermissionChange() {
        let currentState = Self.grantedState
        if let lastState = self.lastPermissionState, lastState != currentState {
            if self.hasListeners("permissionChange") {
                self.notifyListeners("permissionChange", data: ["localNetwork": currentState])
            }
        }
        self.lastPermissionState = currentState
    }

    // MARK: - Permissions

    /**
     Reports the local-network permission state.

     iOS does not expose a runtime permission for local-network access,
     so the state is always `granted`.
     */
    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        resolveGranted(call)
    }

    /**
     Requests the local-network permission.

     iOS has no permission to request: the privacy prompt is presented by
     the system on first local-network access, so this resolves immediately.
     */
    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        resolveGranted(call)
    }

    private func resolveGranted(_ call: CAPPluginCall) {
        LocalNetworkLogger.debug("iOS has no local-network runtime permission; reporting granted.")
        call.resolve(["localNetwork": Self.grantedState])
    }

    // MARK: - Plugin Info

    /**
     Returns the native plugin version.

     The value is generated from `package.json` into `Version.swift` by
     `scripts/sync-version.mjs` during the build.
     */
    @objc public func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": PluginVersion.number])
    }

    // MARK: - Error Mapping

    /**
     Rejects the call with a message and a standardized error code.
     Ensure consistency with the JS LocalNetworkErrorCode object.
     */
    private func reject(_ call: CAPPluginCall, error: NativeError) {
        call.reject(error.message, error.errorCode)
    }

    /**
     Handles unexpected errors from the implementation layer.
     If the error is already a NativeError, delegates to `reject`.
     Otherwise wraps it in an `initFailed` rejection.
     */
    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let nativeError = error as? NativeError {
            reject(call, error: nativeError)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }
}
