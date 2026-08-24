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
        if let uuid = UIDevice.current.identifierForVendor {
            call.resolve([
                "identifier": uuid.uuidString
            ])
        } else {
            reject(call, error: .unavailable("Id not available"))
        }
    }

    /**
     * Returns information about the underlying device, OS, and platform.
     */
    @objc func getInfo(_ call: CAPPluginCall) {
        var isSimulator = false
        var modelName = ""
        #if targetEnvironment(simulator)
        isSimulator = true
        modelName = ProcessInfo().environment["SIMULATOR_MODEL_IDENTIFIER"] ?? "Simulator"
        #else
        modelName = implementation.getModelName()
        #endif

        let memUsed = implementation.getMemoryUsage()
        let diskFree = implementation.getFreeDiskSize() ?? 0
        let realDiskFree = implementation.getRealFreeDiskSize() ?? 0
        let diskTotal = implementation.getTotalDiskSize() ?? 0
        let realDiskTotal = implementation.getRealTotalDiskSize() ?? 0
        let systemVersionNum = implementation.getSystemVersionInt() ?? 0

        call.resolve([
            "memUsed": memUsed,
            "diskFree": diskFree,
            "diskTotal": diskTotal,
            "realDiskFree": realDiskFree,
            "realDiskTotal": realDiskTotal,
            "name": UIDevice.current.name,
            "model": modelName,
            "operatingSystem": "ios",
            "osVersion": UIDevice.current.systemVersion,
            "iOSVersion": systemVersionNum,
            "platform": "ios",
            "manufacturer": "Apple",
            "isVirtual": isSimulator,
            "webViewVersion": UIDevice.current.systemVersion
        ])
    }

    /**
     * Returns information about the device battery.
     */
    @objc func getBatteryInfo(_ call: CAPPluginCall) {
        // Battery data is only valid while monitoring is enabled; re-arm it defensively
        // but never disable it afterwards so listeners keep receiving events.
        UIDevice.current.isBatteryMonitoringEnabled = true

        call.resolve([
            "batteryLevel": UIDevice.current.batteryLevel,
            "isCharging": UIDevice.current.batteryState == .charging || UIDevice.current.batteryState == .full
        ])
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
