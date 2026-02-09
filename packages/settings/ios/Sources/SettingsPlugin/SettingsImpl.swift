import Foundation
import UIKit

/**
 Native iOS implementation for the Settings plugin.

 Responsibilities:
 - Perform platform-specific operations
 - Throw typed SettingsError values on failure

 Forbidden:
 - Accessing CAPPluginCall
 - Referencing Capacitor APIs
 - Constructing JS payloads
 */
@objc public final class SettingsImpl: NSObject {

    // Properties
    private var config: SettingsConfig?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method must be called exactly once
     during plugin initialization.
     */
    func applyConfig(_ config: SettingsConfig) {
        precondition(
            self.config == nil,
            "SettingsImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        SettingsLogger.verbose = config.verboseLogging

        SettingsLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Settings

    /**
     Opens the requested iOS settings page.

     - Parameter option: JS-facing settings key
     - Throws: SettingsError if the operation cannot be completed
     */
    func open(option: String?) throws {
        guard let url = SettingsUtils.resolveSettingsURL(for: option) else {
            throw SettingsError.unavailable(
                "Requested setting is not available on iOS"
            )
        }

        guard UIApplication.shared.canOpenURL(url) else {
            throw SettingsError.unavailable(
                "Cannot open settings URL"
            )
        }

        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: [:])
        }
    }
}
