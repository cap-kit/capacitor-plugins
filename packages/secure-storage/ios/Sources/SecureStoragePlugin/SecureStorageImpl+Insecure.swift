import Foundation

extension SecureStorageImpl {

    // MARK: - Runtime configuration

    func configure(group: String?, obfuscateKeys: Bool? = nil, obfuscationPrefix: String? = nil) {
        if let group, !group.isEmpty {
            insecureGroup = group
        }
        if let obfuscateKeys {
            runtimeObfuscateKeys = obfuscateKeys
        }
        if let obfuscationPrefix {
            runtimeObfuscationPrefix = obfuscationPrefix
            notePrefix(obfuscationPrefix)
        }
    }

    /// Prefixes observed this session, current first. Reads span all of
    /// them so prefix changes never orphan existing entries. The empty
    /// prefix is always included as a legacy fallback: entries written
    /// before any prefix existed carry no prefix.
    func prefixVariants() -> [String] {
        var ordered = [effectivePrefix()]
        for seen in seenPrefixes where seen != effectivePrefix() {
            ordered.append(seen)
        }
        if !ordered.contains("") {
            ordered.append("")
        }
        return ordered
    }

    func notePrefix(_ prefix: String) {
        // The empty (default) prefix must be recorded too: entries written
        // before a runtime prefix change carry no prefix, and reads must
        // keep spanning it or those entries would be orphaned.
        seenPrefixes.insert(prefix)
    }

    // MARK: - Name encoding (prefix + optional base64)

    func effectiveObfuscate() -> Bool {
        return runtimeObfuscateKeys ?? config?.obfuscateKeys ?? false
    }

    func effectivePrefix() -> String {
        return runtimeObfuscationPrefix ?? config?.obfuscationPrefix ?? ""
    }

    /**
     Encodes a key name for storage. Obfuscation hides names from casual
     inspection; it is not encryption.
     */
    func encodeName(_ key: String) -> String {
        let body = effectiveObfuscate() ? SecureStorageUtils.encodeB64(key) : key
        return "\(effectivePrefix())\(body)"
    }

    /**
     Candidate stored names, primary first. Reads try each in order so
     toggling the flag never orphans existing entries.
     */
    func nameCandidates(_ key: String) -> [String] {
        let bodies = effectiveObfuscate()
            ? [SecureStorageUtils.encodeB64(key), key]
            : [key, SecureStorageUtils.encodeB64(key)]
        var names: [String] = []
        for prefix in prefixVariants() {
            for body in bodies {
                names.append("\(prefix)\(body)")
            }
        }
        return names
    }

    /**
     Best-effort inverse of the name encoding: strips the prefix, then
     base64-decodes only when the value round-trips cleanly.
     */
    func decodeName(_ stored: String) -> String {
        var remainders = [stored]
        for prefix in prefixVariants() where !prefix.isEmpty && stored.hasPrefix(prefix) {
            remainders.append(String(stored.dropFirst(prefix.count)))
        }
        for remainder in remainders {
            if let decoded = SecureStorageUtils.decodeB64(remainder) {
                return decoded
            }
        }
        let current = effectivePrefix()
        return !current.isEmpty && stored.hasPrefix(current)
            ? String(stored.dropFirst(current.count))
            : stored
    }

    // MARK: - Insecure tier (plaintext UserDefaults, group-namespaced)

    private func insecureKey(_ key: String) -> String {
        return "\(insecureGroup).\(encodeName(key))"
    }

    private func insecureCandidates(_ key: String) -> [String] {
        let bodies = effectiveObfuscate()
            ? [SecureStorageUtils.encodeB64(key), key]
            : [key, SecureStorageUtils.encodeB64(key)]
        var names: [String] = []
        for prefix in prefixVariants() {
            for body in bodies {
                names.append("\(insecureGroup).\(prefix)\(body)")
            }
        }
        return names
    }

    func setInsecure(key: String, value: String) {
        UserDefaults.standard.set(value, forKey: insecureKey(key))
    }

    func getInsecure(key: String) -> String? {
        for name in insecureCandidates(key) {
            if let value = UserDefaults.standard.string(forKey: name) {
                return value
            }
        }
        return nil
    }

    func removeInsecure(key: String) -> Bool {
        var existed = false
        for name in insecureCandidates(key) {
            if UserDefaults.standard.object(forKey: name) != nil {
                existed = true
            }
            UserDefaults.standard.removeObject(forKey: name)
        }
        return existed
    }

    func hasInsecure(key: String) -> Bool {
        return insecureCandidates(key).contains { UserDefaults.standard.object(forKey: $0) != nil }
    }

    func keysInsecure() -> [String] {
        let prefix = "\(insecureGroup)."
        return UserDefaults.standard.dictionaryRepresentation().keys.compactMap { stored in
            guard stored.hasPrefix(prefix) else {
                return nil
            }
            return decodeName(String(stored.dropFirst(prefix.count)))
        }
    }

    func clearInsecure() {
        let prefix = "\(insecureGroup)."
        let defaults = UserDefaults.standard
        for key in defaults.dictionaryRepresentation().keys where key.hasPrefix(prefix) {
            defaults.removeObject(forKey: key)
        }
    }
}
