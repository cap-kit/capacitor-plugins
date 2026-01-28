import Foundation
import UIKit

/**
 Native iOS implementation for the Settings plugin.

 This class contains ONLY platform logic and must not:
 - access CAPPluginCall
 - depend on Capacitor
 - contain JavaScript or bridge-related logic

 Mapping from JavaScript-facing options to platform-specific
 settings URLs is delegated to SettingsUtils.
 */
@objc public final class SettingsImpl: NSObject {

    // Properties
    private var config: SettingsConfig?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Result

    struct Result {
        let success: Bool
        let error: String?
        let code: String?
    }

    // MARK: - Configuration

    /**
     Applies the plugin configuration.

     This method should be called exactly once during plugin initialization.
     Its responsibility is to translate configuration values into runtime
     behavior (e.g. enabling verbose logging).
     */
    func applyConfig(_ config: SettingsConfig) {
        self.config = config
        SettingsLogger.verbose = config.verboseLogging
        SettingsLogger.debug("Configuration applied. Verbose logging:", "\(config.verboseLogging)")
    }

    // MARK: - Settings

    /**
     Opens the requested iOS settings page.

     - Parameter option: The settings option to open.

     - Returns:
     - success: Indicates whether the URL was valid and dispatched.
     - error: Optional human-readable error.
     - code: Optional machine-readable error code.
     */
    func open(option: String?) -> Result {
        guard let url = SettingsUtils.resolveSettingsURL(for: option) else {
            return Result(
                success: false,
                error: "Requested setting is not available on iOS",
                code: "UNAVAILABLE"
            )
        }

        guard UIApplication.shared.canOpenURL(url) else {
            SettingsLogger.debug("Cannot open URL:", url.absoluteString)
            return Result(
                success: false,
                error: "Cannot open settings URL",
                code: "UNAVAILABLE"
            )
        }

        DispatchQueue.main.async(execute: DispatchWorkItem {
            UIApplication.shared.open(url, options: [:])
        })

        return Result(success: true, error: nil, code: nil)
    }
}
