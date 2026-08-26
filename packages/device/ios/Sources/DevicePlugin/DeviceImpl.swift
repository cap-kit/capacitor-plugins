import Foundation
import UIKit

/**
 * Native iOS implementation for the Device plugin.
 *
 * This class contains pure platform logic and is isolated from the Capacitor bridge.
 * It ports the data providers of the official Capacitor Device plugin: memory usage,
 * legacy and real disk metrics, hardware model identifier, crushed-down OS version,
 * and locale helpers.
 *
 * Architectural constraints:
 * - MUST NOT access CAPPluginCall.
 * - MUST NOT depend on Capacitor bridge APIs directly.
 * - MUST perform UI operations on the Main Thread.
 */
@objc public final class DeviceImpl: NSObject {

    // MARK: - Properties

    /// Cached plugin configuration containing logging and behavioral flags.
    private var config: DeviceConfig?

    // MARK: - Initialization

    /**
     * Initializes the implementation instance.
     */
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     * Applies static plugin configuration.
     *
     * This method MUST be called exactly once from the Plugin bridge layer during `load()`.
     * It synchronizes the native logger state with the provided configuration.
     *
     * - Parameter config: The immutable configuration container.
     */
    func applyConfig(_ config: DeviceConfig) {
        precondition(
            self.config == nil,
            "DeviceImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        DeviceLogger.verbose = config.verboseLogging

        DeviceLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Device Identity

    /**
     * Returns the vendor identifier and its type.
     */
    func getId() -> [String: Any] {
        let uid = UIDevice.current.identifierForVendor?.uuidString ?? ""
        return ["uid": uid, "type": "vendor"]
    }

    // MARK: - Device Info

    /**
     * Returns a dictionary containing device hardware and OS information.
     */
    func getInfo() -> [String: Any] {
        let device = UIDevice.current
        let matrix = DeviceImpl.getDeviceMatrix()
        return [
            "name": device.name,
            "model": getModelName(),
            "platform": matrix["platform"] ?? "",
            "operatingSystem": matrix["os"] ?? "",
            "osVersion": device.systemVersion,
            "systemVersionInt": getSystemVersionInt() ?? 0,
            "memUsed": getMemoryUsage(),
            "batteryLevel": Int(round(device.batteryLevel * 100)),
            "isVirtual": isVirtual()
        ] as [String: Any]
    }

    // MARK: - Battery

    /**
     * Returns battery level and charging state.
     */
    func getBatteryInfo() -> [String: Any] {
        let device = UIDevice.current
        return [
            "batteryLevel": Int(round(device.batteryLevel * 100)),
            "isCharging": device.batteryState == .charging || device.batteryState == .full,
            "isPlugged": device.batteryState == .charging || device.batteryState == .full
        ]
    }

    // MARK: - Device Matrix

    /**
     * Returns platform and OS identifiers derived from the runtime environment.
     */
    static func getDeviceMatrix() -> [String: String] {
        #if os(iOS)
        return ["platform": "ios", "os": "ios"]
        #elseif os(macOS)
        return ["platform": "mac", "os": "macos"]
        #elseif os(tvOS)
        return ["platform": "tv", "os": "tvos"]
        #elseif os(watchOS)
        return ["platform": "watch", "os": "watchos"]
        #else
        return ["platform": "unknown", "os": "unknown"]
        #endif
    }

    // MARK: - Simulator Detection

    /**
     * Returns whether the app is running on a simulator.
     */
    func isVirtual() -> Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    // MARK: - Memory

    /**
     * Gets the current memory usage of the hosting process, in bytes.
     */
    public func getMemoryUsage() -> UInt64 {
        var taskInfo = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &taskInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }

        if kerr == KERN_SUCCESS {
            return taskInfo.resident_size
        } else {
            return 0
        }
    }

    // MARK: - Disk Space

    /**
     * Gets free disk space on the normal data storage path.
     *
     * This value is not accurate on modern iOS releases and is kept for
     * backwards compatibility with the deprecated `diskFree` field.
     */
    public func getFreeDiskSize() -> Int64? {
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        if let dictionary = try? FileManager.default.attributesOfFileSystem(forPath: paths.last!) {
            if let freeSize = dictionary[FileAttributeKey.systemFreeSize] as? NSNumber {
                return freeSize.int64Value
            }
        }
        return nil
    }

    /**
     * Gets the real free disk space of the home volume.
     */
    public func getRealFreeDiskSize() -> Int64? {
        do {
            let url = URL(fileURLWithPath: NSHomeDirectory() as String)
            let values = try url.resourceValues(
                forKeys: [URLResourceKey.volumeAvailableCapacityForImportantUsageKey]
            )
            if let available = values.volumeAvailableCapacityForImportantUsage {
                return available
            } else {
                return nil
            }
        } catch {
            return nil
        }
    }

    /**
     * Gets total size of the normal data storage path.
     *
     * This value is not accurate on modern iOS releases and is kept for
     * backwards compatibility with the deprecated `diskTotal` field.
     */
    public func getTotalDiskSize() -> Int64? {
        let paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
        if let dictionary = try? FileManager.default.attributesOfFileSystem(forPath: paths.last!) {
            if let totalSize = dictionary[FileAttributeKey.systemSize] as? NSNumber {
                return totalSize.int64Value
            }
        }
        return nil
    }

    /**
     * Gets the real total size of the home volume.
     */
    public func getRealTotalDiskSize() -> Int64? {
        do {
            let url = URL(fileURLWithPath: NSHomeDirectory() as String)
            let values = try url.resourceValues(forKeys: [URLResourceKey.volumeTotalCapacityKey])
            if let total = values.volumeTotalCapacity {
                return Int64(total)
            } else {
                return nil
            }
        } catch {
            return nil
        }
    }

    // MARK: - Locale

    /**
     * Gets the two-character language code of the preferred locale.
     */
    public func getLanguageCode() -> String {
        return String(Locale.preferredLanguages[0].prefix(2))
    }

    /**
     * Gets the well-formed BCP 47 language tag of the preferred locale.
     */
    public func getLanguageTag() -> String {
        return String(Locale.preferredLanguages[0])
    }

    // MARK: - Hardware Model

    /**
     * Gets the machine model identifier (e.g. "iPhone13,4").
     */
    public func getModelName() -> String {
        #if targetEnvironment(simulator)
        return ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] ?? "Simulator"
        #else
        var size = 0
        sysctlbyname("hw.machine", nil, &size, nil, 0)
        var machine = [CChar](repeating: 0, count: size)
        sysctlbyname("hw.machine", &machine, &size, nil, 0)
        return String(cString: machine)
        #endif
    }

    // MARK: - System Version

    /**
     * Gets the system version as an integer padded to two digits per component.
     *
     * Example: "16.3.1" becomes 160301.
     */
    public func getSystemVersionInt() -> Int? {
        let exploded = UIDevice.current.systemVersion.split(separator: ".")

        var major = 0
        var minor = 0
        var patch = 0

        for (index, numStr) in exploded.enumerated() {
            switch index {
            case 0:
                major = Int(numStr) ?? 0
            case 1:
                minor = Int(numStr) ?? 0
            case 2:
                patch = Int(numStr) ?? 0
            default:
                break
            }
        }

        var combined: [String] = []
        combined.append(String(format: "%02d", major))
        combined.append(String(format: "%02d", minor))
        combined.append(String(format: "%02d", patch))

        return Int(combined.joined())
    }
}
