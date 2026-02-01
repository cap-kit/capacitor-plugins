import Foundation
import UIKit

/**
 Native iOS implementation for the Test plugin.

 This class contains ONLY platform logic and must not:
 - access CAPPluginCall
 - depend on Capacitor
 - perform mapping logic
 */
@objc public final class TestImpl: NSObject {

    // Properties
    private var config: TestConfig?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies the plugin configuration.

     This method should be called exactly once during plugin initialization.
     Its responsibility is to translate configuration values into runtime
     behavior (e.g. enabling verbose logging).
     */
    func applyConfig(_ config: TestConfig) {
        self.config = config
        TestLogger.verbose = config.verboseLogging
        TestLogger.debug("Configuration applied:", config.verboseLogging)
    }

    // MARK: - Echo Method

    /**
     Returns the provided value unchanged.

     This method represents a simple synchronous native operation
     and is intentionally side-effect free.
     */
    @objc public func echo(_ value: String) -> String {
        TestLogger.debug("Echoing value:", value)
        return value
    }

    // MARK: - openAppSettings

    /// Opens the system app settings screen.
    func openAppSettings() throws {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            throw TestError.unavailable("Settings URL not available")
        }

        guard UIApplication.shared.canOpenURL(url) else {
            throw TestError.unavailable("Cannot open settings")
        }

        UIApplication.shared.open(url)
    }
}
