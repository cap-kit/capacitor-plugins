import Foundation

/**
 * Native iOS implementation for the Device plugin.
 *
 * This class contains pure platform logic and is isolated from the Capacitor bridge.
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

}
