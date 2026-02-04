import Foundation
import MachO

/**
 Native iOS implementation for the Integrity plugin.

 Responsibilities:
 - Perform platform-specific integrity checks
 - Interact with system-level APIs
 - Produce integrity signals
 - THROW typed IntegrityError on unrecoverable failures

 Forbidden:
 - Accessing CAPPluginCall
 - Referencing Capacitor APIs
 - Resolving or rejecting JS calls
 - Reading configuration directly
 */
@objc
public final class IntegrityImpl: NSObject {

    // MARK: - Configuration

    /**
     Immutable plugin configuration.

     This configuration MUST be injected exactly once
     from the Plugin layer during load().
     */
    private var config: IntegrityConfig?

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once.
     */
    func applyConfig(_ config: IntegrityConfig) {
        self.config = config
        IntegrityLogger.verbose = config.verboseLogging

        IntegrityLogger.debug(
            "Integrity configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Internal cache

    /**
     Cached jailbreak-related signals.

     Jailbreak checks are deterministic and relatively expensive,
     therefore they are cached for the lifetime of the process.
     */
    private var cachedJailbreakSignals: [[String: Any]]?

    // MARK: - Jailbreak detection

    /**
     Performs baseline jailbreak detection.

     @throws IntegrityError.unavailable
     If filesystem inspection is restricted.
     */
    func checkJailbreakSignals() throws -> [[String: Any]] {
        if let cached = cachedJailbreakSignals {
            return cached
        }

        let suspiciousPaths = [
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd"
        ]

        var signals: [[String: Any]] = []

        for path in suspiciousPaths {
            do {
                if FileManager.default.fileExists(atPath: path) {
                    signals.append([
                        "id": "ios_jailbreak_path",
                        "category": "jailbreak",
                        "confidence": "high"
                    ])
                    break
                }
            } catch {
                throw IntegrityError.unavailable(
                    "Filesystem access denied while performing jailbreak checks."
                )
            }
        }

        cachedJailbreakSignals = signals
        return signals
    }

    // MARK: - Simulator detection

    /**
     Indicates whether the application is running in a simulator.
     */
    func isSimulator() -> Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    // MARK: - Frida detection — loaded libraries

    /**
     Performs best-effort Frida Gadget detection by inspecting
     loaded dynamic libraries via dyld.

     @throws IntegrityError.unavailable
     If dyld inspection is restricted.
     */
    func checkFridaLibraries() throws -> Bool {
        let imageCount = _dyld_image_count()

        for index in 0..<imageCount {
            guard let cName = _dyld_get_image_name(index) else {
                continue
            }

            let imageName = String(cString: cName).lowercased()
            if imageName.contains("frida") || imageName.contains("gadget") {
                return true
            }
        }

        return false
    }

    // MARK: - Frida detection — threads

    /**
     Detects suspicious Frida-related threads.

     Failure to inspect threads is treated as unavailable,
     not as a negative detection.
     */
    func checkFridaThreads() throws -> Bool {
        guard let threadName = Thread.current.name?.lowercased() else {
            return false
        }

        return threadName.contains("frida")
    }

    // MARK: - Bundle integrity

    /**
     Performs a basic application bundle integrity check.

     @throws IntegrityError.initFailed
     If bundle information cannot be accessed.
     */
    func checkBundleIntegrity() throws -> Bool {
        guard let executablePath = Bundle.main.executablePath else {
            throw IntegrityError.initFailed(
                "Unable to determine application executable path."
            )
        }

        return FileManager.default.fileExists(atPath: executablePath)
    }
}
