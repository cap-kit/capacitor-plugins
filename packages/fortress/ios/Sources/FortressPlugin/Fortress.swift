import Foundation

/**
 Native iOS implementation for the Fortress plugin.
 */
@objc
public final class Fortress: NSObject {

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    private var config: Config?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.

     Responsibilities:
     - Store immutable configuration
     - Configure runtime logging behavior
     */
    func applyConfig(_ config: Config) {
        precondition(
            self.config == nil,
            "Fortress.applyConfig(_:) must be called exactly once"
        )
        self.config = config

        // Synchronize logger state
        Logger.verbose = config.verboseLogging

        Logger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }
}
