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
     URL-safe base64 without padding. Used for key-name obfuscation:
     hides names from casual inspection, never encryption.
     */
    static func encodeB64(_ value: String) -> String {
        return Data(value.utf8)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    /**
     Best-effort inverse of `encodeB64`: returns nil unless the value
     round-trips cleanly, so plain names are never misdecoded.
     */
    static func decodeB64(_ value: String) -> String? {
        var normalized = value
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let remainder = normalized.count % 4
        if remainder > 0 {
            normalized += String(repeating: "=", count: 4 - remainder)
        }
        guard let data = Data(base64Encoded: normalized),
              let decoded = String(data: data, encoding: .utf8),
              encodeB64(decoded) == value else {
            return nil
        }
        return decoded
    }

    /**
     Order-preserving deduplication for key listings that may contain
     both encoded and plain forms of the same original.
     */
    static func uniqueOrdered(_ values: [String]) -> [String] {
        var seen = Set<String>()
        return values.filter { seen.insert($0).inserted }
    }

    /**
     Applies only the global prefix for secure storage keys.
     */
    static func formatSecureKey(_ key: String, globalPrefix: String = "") -> String {
        return "\(globalPrefix)\(key)"
    }
}
