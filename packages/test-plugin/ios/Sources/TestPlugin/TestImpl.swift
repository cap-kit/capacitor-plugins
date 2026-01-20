import Foundation
import Capacitor

/**
 Native implementation for the Test Capacitor plugin.

 This class contains platform-specific logic only and MUST NOT:
 - access CAPPluginCall
 - interact directly with the Capacitor bridge
 - depend on JavaScript concepts

 The Capacitor plugin class is responsible for delegating work to this class.
 */
@objc public class TestImpl: NSObject {

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
        TestLogger.debug("Configuration applied. Verbose logging enabled.")
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
}
