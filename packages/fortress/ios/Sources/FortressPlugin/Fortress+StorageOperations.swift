import Foundation

extension Fortress {

    // MARK: - Secure Storage enumeration & sync

    /**
     Lists keys in secure or insecure storage, returned in original
     (de-obfuscated, de-prefixed) form.
     */
    func keys(secure: Bool) throws -> [String] {
        if secure {
            try ensureSecureVaultAccessible()
            let globalPrefix = config?.prefix ?? ""
            let mapped = try secureStorage.keys().map { originalSecureKey(from: $0, globalPrefix: globalPrefix) }
            return KeyUtils.uniqueOrdered(mapped)
        }

        // The runtime-config payload shares UserDefaults; it is not a value key.
        let listed = standardStorage
            .keys(matchingPrefixes: insecurePrefixes())
            .filter { $0 != "fortress_runtime_config_v1" }
        let insecure = deobfuscatedKeys(storedKeys: listed)
        return KeyUtils.uniqueOrdered(insecure)
    }

    /**
     Reads several keys in one call. Missing keys map to nil; a locked
     vault rejects the whole call instead of returning partial data.
     */
    func getMany(keys: [String], secure: Bool) throws -> [String: String?] {
        var values: [String: String?] = [:]
        for key in keys {
            values[key] = secure ? (try getValue(key: key)) : (try getInsecureValue(key: key))
        }
        return values
    }

    /// Session-scoped iCloud Keychain synchronization toggle (iOS only).
    func setSynchronize(_ enabled: Bool) {
        KeychainHelper.setSynchronizable(enabled)
    }

    func isSynchronized() -> Bool {
        return KeychainHelper.isSynchronizable()
    }

    /**
     Session-scoped default Keychain accessibility for subsequently
     stored secure items. Per-item `access` in `setValue()` wins.
     */
    func setDefaultKeychainAccess(_ access: String) {
        defaultKeychainAccess = access
    }

    // MARK: - Key mapping helpers

    /// Strips the global prefix applied at write time, if present, then
    /// base64-decodes when the remainder round-trips cleanly.
    func originalSecureKey(from stored: String, globalPrefix: String) -> String {
        let remainder: String
        if !globalPrefix.isEmpty, stored.hasPrefix(globalPrefix) {
            remainder = String(stored.dropFirst(globalPrefix.count))
        } else {
            remainder = stored
        }
        return KeyUtils.decodeB64(remainder) ?? remainder
    }

    /// Prefixes identifying insecure entries (current config first).
    func insecurePrefixes() -> [String] {
        let obfuscationPrefix = config?.obfuscationPrefix ?? "ftrss_"
        let globalPrefix = config?.prefix ?? ""
        noteObfuscationPrefix(obfuscationPrefix)
        var ordered = ["\(globalPrefix)\(obfuscationPrefix)"]
        for seen in seenObfuscationPrefixes where "\(globalPrefix)\(seen)" != ordered[0] {
            ordered.append("\(globalPrefix)\(seen)")
        }
        ordered.append(contentsOf: ["ftrss_", "fortress_"])
        return Array(Set(ordered.filter { !$0.isEmpty }))
    }

    /// Strips the first matching insecure prefix from each stored key,
    /// then base64-decodes when the remainder round-trips cleanly.
    func deobfuscatedKeys(storedKeys: [String]) -> [String] {
        let prefixes = insecurePrefixes()
        return storedKeys.map { stored in
            guard let prefix = prefixes.first(where: { stored.hasPrefix($0) }) else {
                return stored
            }
            let remainder = String(stored.dropFirst(prefix.count))
            return KeyUtils.decodeB64(remainder) ?? remainder
        }
    }

    /**
     Candidate stored names, primary first. Reads try each in order so
     toggling the flag never orphans existing entries.
     */
    func secureNameCandidates(for key: String) -> [String] {
        let globalPrefix = config?.prefix ?? ""
        let plain = KeyUtils.formatSecureKey(key, globalPrefix: globalPrefix)
        let encoded = KeyUtils.formatSecureKey(KeyUtils.encodeB64(key), globalPrefix: globalPrefix)
        return (config?.obfuscateKeys ?? false) ? [encoded, plain] : [plain, encoded]
    }

    /// Maps a `KeychainAccess` name (or the session default) to its class.
    func resolveAccessible(_ access: String?) -> CFString? {        switch access ?? defaultKeychainAccess {
        case "whenUnlocked":
            return kSecAttrAccessibleWhenUnlocked
        case "whenUnlockedThisDeviceOnly":
            return kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        case "afterFirstUnlock":
            return kSecAttrAccessibleAfterFirstUnlock
        case "afterFirstUnlockThisDeviceOnly":
            return kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        case "whenPasscodeSetThisDeviceOnly":
            return kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
        default:
            return nil
        }
    }
}
