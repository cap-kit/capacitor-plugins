import Foundation
import CryptoKit

/**
 Utility helpers for the Fortress plugin.

 Responsibilities:
 - Provide deterministic SHA-256 hashing utilities
 - Derive a stable, non-PII device identifier hash for payloads
 */
enum Utils {

    /**
     Produces a SHA-256 hex digest.
     */
    static func sha256Hex(_ value: String) -> String {
        let digest = SHA256.hash(data: Data(value.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    /**
     Returns a non-PII stable device identifier hash.
     */
    static func deviceIdentifierHash() -> String {
        let bundleIdentifier = Bundle.main.bundleIdentifier ?? "io.capkit.fortress"
        let machine = machineIdentifier()
        let rawIdentifier = "\(bundleIdentifier)|\(machine)"
        return sha256Hex(rawIdentifier)
    }

    private static func machineIdentifier() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let mirror = Mirror(reflecting: systemInfo.machine)
        return mirror.children.reduce(into: "") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return }
            identifier.append(Character(UnicodeScalar(UInt8(value))))
        }
    }
}
