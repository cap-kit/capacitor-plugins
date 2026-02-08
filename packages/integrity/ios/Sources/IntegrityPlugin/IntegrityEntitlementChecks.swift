import Foundation

/**
 Utility to verify the integrity of the application's entitlements
 and provisioning profile.
 */
internal struct IntegrityEntitlementChecks {

    /**
     Reads the embedded.mobileprovision file to extract entitlements.
     NOTE: This is a complex check as the file is a CMS/PKCS7 signed message.
     We perform a simplified string-based heuristic for performance.
     */
    static func checkEntitlements() -> [String: Any]? {
        guard let path = Bundle.main.path(forResource: "embedded", ofType: "mobileprovision") else {
            // If the file is missing in a production build, it's a signal
            return ["error": "Provisioning profile missing"]
        }

        do {
            let data = try Data(contentsOf: URL(fileURLWithPath: path))
            // Convert to string to look for specific entitlement keys
            // In a real RASP implementation, we would parse the full ASN.1/XML structure
            if let content = String(data: data, encoding: .ascii) {
                let hasGetTaskAllow = content.contains("<key>get-task-allow</key>\n\t\t<true/>")

                // Extraction of Keychain Access Groups (Heuristic)
                // Re-signed apps will have different or missing access groups.
                var keychainGroups: [String] = []
                if content.contains("<key>keychain-access-groups</key>") {
                    // Simple scan for common team-prefixed group patterns
                    let pattern = "<string>$(AppIdentifierPrefix)[^<]+"
                    if let regex = try? NSRegularExpression(pattern: pattern, options: []) {
                        let nsString = content as NSString
                        let results = regex.matches(
                            in: content,
                            options: [],
                            range: NSRange(location: 0, length: nsString.length)
                        )
                        keychainGroups = results.map { nsString.substring(with: $0.range)
                            .replacingOccurrences(of: "<string>$(AppIdentifierPrefix)", with: "") }
                    }
                }

                return [
                    "debuggable": hasGetTaskAllow,
                    "provisioning_present": true,
                    "keychain_groups_found": keychainGroups.count,
                    "has_keychain_access": !keychainGroups.isEmpty
                ]
            }
        } catch {
            return ["error": "Failed to read profile"]
        }

        return nil
    }
}
