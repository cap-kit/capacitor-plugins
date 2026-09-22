import Foundation

/**
 Key-formatting helpers for Fortress storage namespaces.

 Responsibilities:
 - Apply deterministic obfuscation prefixing for standard-storage keys
 - Apply global prefixing for secure-storage keys
 */
enum KeyUtils {
    /**
     Applies both the global prefix and the obfuscation prefix for standard storage.
     */
    static func obfuscate(_ key: String, prefix: String, globalPrefix: String = "") -> String {
        return "\(globalPrefix)\(prefix)\(key)"
    }

    /**
     Applies only the global prefix for secure storage keys.
     */
    static func formatSecureKey(_ key: String, globalPrefix: String = "") -> String {
        return "\(globalPrefix)\(key)"
    }
}
