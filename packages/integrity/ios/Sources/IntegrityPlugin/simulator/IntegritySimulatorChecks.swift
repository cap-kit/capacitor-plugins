import Foundation

/**
 * Detects if the application is running in an iOS simulator environment.
 */
struct IntegritySimulatorChecks {

    /**
     * Determines whether the current process is running under the iOS Simulator.
     *
     * NOTE: This uses compile-time environment checks and is not spoofable at runtime.
     */
    static func isSimulator() -> Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }
}
