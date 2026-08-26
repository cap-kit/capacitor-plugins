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

    // MARK: - Constants

    /// Name of the event emitted when the battery charging state changes.
    public static let batteryChargingStateChangeEvent = "batteryChargingStateChange"

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = DeviceImpl()

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: DeviceConfig?

    /// Last charging state observed by the battery listener, used to filter duplicate notifications.
    private var lastBatteryChargingState: Bool?

    /// The unique identifier for the plugin used by the Capacitor bridge.
    public let identifier = "DevicePlugin"

    /// The name used to reference this plugin in JavaScript (e.g., Device.getPluginVersion()).
    public let jsName = "Device"

    /**
     * A list of methods exposed by this plugin to the JavaScript layer.
     * All methods defined here must be implemented with the @objc attribute.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getId", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getBatteryInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getLanguageCode", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getLanguageTag", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addListener", returnType: CAPPluginReturnNone),
        CAPPluginMethod(name: "getBatteryExtras", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDisplayInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getConfiguration", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPowerState", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getMemoryInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSystemUptime", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAppVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeListener", returnType: CAPPluginReturnNone),
        CAPPluginMethod(name: "removeAllListeners", returnType: CAPPluginReturnNone),
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
     - start battery state tracking
     */
    override public func load() {
        // Initialize DeviceConfig with the correct type
        let cfg = DeviceConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Start battery tracking immediately so `batteryChargingStateChange`
        // listeners registered later receive events without extra setup.
        UIDevice.current.isBatteryMonitoringEnabled = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(batteryStateDidChange),
            name: UIDevice.batteryStateDidChangeNotification,
            object: nil
        )

        // Log if verbose logging is enabled
        DeviceLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
        UIDevice.current.isBatteryMonitoringEnabled = false
    }

    // MARK: - Event Handling

    /**
     * Observes battery state changes and emits `batteryChargingStateChange`
     * whenever the charging state transitions (charging, discharging, or full).
     */
    @objc private func batteryStateDidChange() {
        let state = UIDevice.current.batteryState
        if state == .unknown {
            return
        }
        let charging = state == .charging || state == .full
        if lastBatteryChargingState == nil {
            // Baseline observation: never emit for the very first known state.
            lastBatteryChargingState = charging
            return
        }
        if lastBatteryChargingState != charging {
            lastBatteryChargingState = charging
            notifyListeners(DevicePlugin.batteryChargingStateChangeEvent, data: [
                "batteryLevel": UIDevice.current.batteryLevel,
                "isCharging": charging
            ])
        }
    }

    // MARK: - Plugin Methods

    /**
     * Returns the vendor identifier assigned to the app on this device.
     */
    @objc func getId(_ call: CAPPluginCall) {
        do {
            let result = implementation.getId()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    /**
     * Returns information about the underlying device, OS, and platform.
     */
    @objc func getInfo(_ call: CAPPluginCall) {
        do {
            let result = implementation.getInfo()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    /**
     * Returns information about the device battery.
     */
    @objc func getBatteryInfo(_ call: CAPPluginCall) {
        do {
            let result = implementation.getBatteryInfo()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    /**
     * Returns the two-character language code of the preferred locale.
     */
    @objc func getLanguageCode(_ call: CAPPluginCall) {
        let code = implementation.getLanguageCode()
        call.resolve([
            "value": code
        ])
    }

    /**
     * Returns the BCP 47 language tag of the preferred locale.
     */
    @objc func getLanguageTag(_ call: CAPPluginCall) {
        let tag = implementation.getLanguageTag()
        call.resolve([
            "value": tag
        ])
    }

    // MARK: - Battery Extras

    @objc func getBatteryExtras(_ call: CAPPluginCall) {
        do {
            let result = implementation.getBatteryExtras()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Display Info

    @objc func getDisplayInfo(_ call: CAPPluginCall) {
        do {
            let result = implementation.getDisplayInfo()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Configuration

    @objc func getConfiguration(_ call: CAPPluginCall) {
        do {
            let result = implementation.getConfiguration()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Power State

    @objc func getPowerState(_ call: CAPPluginCall) {
        do {
            let result = implementation.getPowerState()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Memory Info

    @objc func getMemoryInfo(_ call: CAPPluginCall) {
        do {
            let result = implementation.getMemoryInfo()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - System Uptime

    @objc func getSystemUptime(_ call: CAPPluginCall) {
        do {
            let result = implementation.getSystemUptime()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - App Version

    @objc func getAppVersion(_ call: CAPPluginCall) {
        do {
            let result = implementation.getAppVersion()
            call.resolve(result)
        } catch let error as NativeError {
            reject(call, error: error)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native NativeError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: NativeError
    ) {
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let nativeError = error as? NativeError {
            call.reject(nativeError.message, nativeError.errorCode)
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
