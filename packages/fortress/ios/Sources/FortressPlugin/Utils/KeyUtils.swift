import Foundation

enum KeyUtils {
    static func obfuscate(_ key: String, prefix: String) -> String {
        return "\(prefix)\(key)"
    }
}
