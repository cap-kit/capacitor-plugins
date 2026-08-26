import Foundation
import UIKit
import MachO

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

    /// Previous CPU load ticks for delta-based CPU usage computation.
    private var previousCpuLoad: host_cpu_load_info?

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

    // MARK: - Display Info

    func getDisplayInfo() -> [String: Any] {
        let screen = UIScreen.main
        return [
            "widthPx": Int(screen.nativeBounds.width),
            "heightPx": Int(screen.nativeBounds.height),
            "densityDpi": Int(screen.scale * 160),
            "scale": screen.scale,
            "refreshRateHz": screen.maximumFramesPerSecond
        ]
    }

    // MARK: - Configuration

    func getConfiguration() -> [String: Any] {
        let screen = UIScreen.main
        let device = UIDevice.current

        // Map UIUserInterfaceIdiom to our string
        let idiom: String
        switch device.userInterfaceIdiom {
        case .phone: idiom = "phone"
        case .pad: idiom = "tablet"
        case .mac: idiom = "desktop"
        default: idiom = "unknown"
        }

        // Map traitCollection.userInterfaceStyle
        let isDarkMode = screen.traitCollection.userInterfaceStyle == .dark

        // Map preferredContentSizeCategory to numeric scale
        // UIContentSizeCategory doesn't expose fontScale directly;
        // map the category to a representative multiplier.
        let contentSizeCategory = UIApplication.shared.preferredContentSizeCategory
        let fontScale: Double
        switch contentSizeCategory {
        case .extraSmall: fontScale = 0.8
        case .small: fontScale = 0.9
        case .medium: fontScale = 1.0
        case .large: fontScale = 1.15
        case .extraLarge: fontScale = 1.3
        case .extraExtraLarge: fontScale = 1.6
        case .extraExtraExtraLarge: fontScale = 1.9
        case .accessibilityMedium: fontScale = 2.3
        case .accessibilityLarge: fontScale = 2.8
        case .accessibilityExtraLarge: fontScale = 3.3
        case .accessibilityExtraExtraLarge: fontScale = 3.8
        case .accessibilityExtraExtraExtraLarge: fontScale = 4.4
        default: fontScale = 1.0
        }

        // Map orientation
        let orientation: String
        switch device.orientation {
        case .portrait, .portraitUpsideDown: orientation = "portrait"
        case .landscapeLeft, .landscapeRight: orientation = "landscape"
        default: orientation = "unknown"
        }

        // Map screen size bucket
        let screenSize: String
        let screenHeight = screen.nativeBounds.height / screen.scale
        if screenHeight < 568 {
            screenSize = "small"
        } else if screenHeight < 812 {
            screenSize = "normal"
        } else if screenHeight < 1024 {
            screenSize = "large"
        } else {
            screenSize = "xlarge"
        }

        return [
            "orientation": orientation,
            "isDarkMode": isDarkMode,
            "fontScale": fontScale,
            "idiom": idiom,
            "screenSize": screenSize
        ]
    }

    // MARK: - Power State

    func getPowerState() -> [String: Any] {
        let thermalState: String
        switch ProcessInfo.processInfo.thermalState {
        case .nominal: thermalState = "nominal"
        case .fair: thermalState = "fair"
        case .serious: thermalState = "serious"
        case .critical: thermalState = "critical"
        @unknown default: thermalState = "unknown"
        }

        return [
            "isLowPowerMode": ProcessInfo.processInfo.isLowPowerModeEnabled,
            "thermalState": thermalState
        ]
    }

    // MARK: - Memory Info

    func getMemoryInfo() -> [String: Any] {
        let physicalRam = ProcessInfo.processInfo.physicalMemory

        // Get CPU core count via sysctl
        var numCores: Int = 0
        var sizeOfCores = MemoryLayout<Int>.size
        var mib: [Int32] = [CTL_HW, HW_NCPU]
        sysctl(&mib, 2, &numCores, &sizeOfCores, nil, 0)

        // Delta-based CPU usage
        let cpuUsage = getCpuUsage()

        // Memory pressure: derive from free memory ratio
        let memoryPressure = getMemoryPressure(physicalRam: physicalRam)

        return [
            "physicalRam": physicalRam,
            "cpuCores": numCores,
            "memoryClassMb": Int(physicalRam / (1024 * 1024)),
            "isLowRamDevice": false,
            "cpuUsagePercent": cpuUsage as Any,
            "memoryPressure": memoryPressure
        ]
    }

    // MARK: - CPU Usage (Delta-based)

    /**
     * Computes CPU usage as a percentage (0–100) since the last call.
     *
     * Uses Mach `host_statistics` with `HOST_CPU_LOAD_INFO` to read user, system,
     * idle, and nice ticks. Returns `nil` on the first call (no delta available)
     * or if the Mach call fails.
     */
    private func getCpuUsage() -> Double? {
        var numCPUInfo = mach_msg_type_number_t(
            MemoryLayout<host_cpu_load_info>.size / MemoryLayout<integer_t>.size
        )
        var cpuInfo = host_cpu_load_info()

        let result = withUnsafeMutablePointer(to: &cpuInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(numCPUInfo)) {
                host_statistics(
                    mach_host_self(),
                    host_flavor_t(HOST_CPU_LOAD_INFO),
                    $0,
                    &numCPUInfo
                )
            }
        }

        guard result == KERN_SUCCESS else { return nil }

        let user = Double(cpuInfo.cpu_ticks.0)
        let system = Double(cpuInfo.cpu_ticks.1)
        let idle = Double(cpuInfo.cpu_ticks.2)
        let nice = Double(cpuInfo.cpu_ticks.3)
        let total = user + system + idle + nice

        guard let previous = previousCpuLoad else {
            previousCpuLoad = cpuInfo
            return nil
        }

        let prevUser = Double(previous.cpu_ticks.0)
        let prevSystem = Double(previous.cpu_ticks.1)
        let prevIdle = Double(previous.cpu_ticks.2)
        let prevNice = Double(previous.cpu_ticks.3)
        let prevTotal = prevUser + prevSystem + prevIdle + prevNice

        let totalDelta = total - prevTotal
        let idleDelta = idle - prevIdle

        previousCpuLoad = cpuInfo

        guard totalDelta > 0 else { return 0.0 }
        return ((totalDelta - idleDelta) / totalDelta) * 100.0
    }

    // MARK: - Memory Pressure

    /**
     * Derives memory pressure from the ratio of free to total physical RAM.
     *
     * Uses `host_statistics64` with `HOST_VM_INFO64` to read free and inactive
     * page counts, then classifies into normal / warning / critical thresholds.
     */
    private func getMemoryPressure(physicalRam: UInt64) -> String {
        var vmInfo = vm_statistics64()
        var count = mach_msg_type_number_t(
            MemoryLayout<vm_statistics64>.size / MemoryLayout<integer_t>.size
        )

        let result = withUnsafeMutablePointer(to: &vmInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                host_statistics64(
                    mach_host_self(),
                    host_flavor_t(HOST_VM_INFO64),
                    $0,
                    &count
                )
            }
        }

        guard result == KERN_SUCCESS else { return "unknown" }

        let pageSize = UInt64(vm_page_size)
        let freePages = UInt64(vmInfo.free_count)
        let inactivePages = UInt64(vmInfo.inactive_count)
        let freeBytes = (freePages + inactivePages) * pageSize

        guard physicalRam > 0 else { return "unknown" }
        let freeRatio = Double(freeBytes) / Double(physicalRam)

        if freeRatio < 0.05 {
            return "critical"
        } else if freeRatio < 0.15 {
            return "warning"
        } else {
            return "normal"
        }
    }

    // MARK: - Storage Info

    /**
     * Returns disk storage information for the primary data volume.
     */
    func getStorageInfo() -> [String: Any] {
        let url = URL(fileURLWithPath: NSHomeDirectory())
        do {
            let values = try url.resourceValues(forKeys: [
                URLResourceKey.volumeTotalCapacityKey,
                URLResourceKey.volumeAvailableCapacityForImportantUsageKey
            ])
            let total = values.volumeTotalCapacity.map { Int64($0) } ?? 0
            let free = values.volumeAvailableCapacityForImportantUsage ?? 0
            let used = total - free
            let usedPercent = total > 0 ? (Double(used) / Double(total)) * 100.0 : 0.0

            return [
                "totalBytes": total,
                "freeBytes": free,
                "usedBytes": used,
                "usedPercent": usedPercent
            ]
        } catch {
            return [
                "totalBytes": 0,
                "freeBytes": 0,
                "usedBytes": 0,
                "usedPercent": 0.0
            ]
        }
    }

    // MARK: - System Uptime

    func getSystemUptime() -> [String: Any] {
        return [
            "uptimeSeconds": ProcessInfo.processInfo.systemUptime
        ]
    }

    // MARK: - App Version

    func getAppVersion() -> [String: Any] {
        let bundle = Bundle.main
        let version = bundle.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown"
        let buildNumber = bundle.infoDictionary?["CFBundleVersion"] as? String ?? "0"

        return [
            "version": version,
            "buildNumber": Int(buildNumber) ?? 0
        ]
    }

    // MARK: - Battery Extras

    func getBatteryExtras() -> [String: Any] {
        let device = UIDevice.current

        // Determine charge source
        // iOS UIDevice doesn't directly expose charge source, but batteryState gives us partial info
        // We use ProcessInfo for power source info
        let chargeSource: String
        switch device.batteryState {
        case .charging, .full:
            chargeSource = "ac"  // Default assumption when plugged in
        default:
            chargeSource = "unknown"
        }

        // Map detailed state
        let detailedState: String
        switch device.batteryState {
        case .unknown: detailedState = "unknown"
        case .unplugged: detailedState = "unplugged"
        case .charging: detailedState = "charging"
        case .full: detailedState = "full"
        @unknown default: detailedState = "unknown"
        }

        return [
            "chargeSource": chargeSource,
            "detailedState": detailedState
        ]
    }
}
