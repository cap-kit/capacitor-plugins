import Foundation

/**
 Utility helpers for the SecureStorage plugin.

 Pure, side-effect-free key-name codecs shared by the secure and insecure
 tiers. This type is intentionally stateless: all functions are static.
 */
struct SecureStorageUtils {

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
}
