import Foundation

struct RuntimeConfigStore {

    private static let payloadVersion = 1
    private static let storageKey = "fortress_runtime_config_v1"

    private static let allowedOverrideKeys: Set<String> = [
        "verboseLogging",
        "logLevel",
        "lockAfterMs",
        "enablePrivacyScreen",
        "privacyOverlayText",
        "privacyOverlayImageName",
        "privacyOverlayShowText",
        "privacyOverlayShowImage",
        "privacyOverlayTextColor",
        "privacyOverlayBackgroundOpacity",
        "privacyOverlayTheme",
        "fallbackStrategy",
        "allowCachedAuthentication",
        "cachedAuthenticationTimeoutMs",
        "maxBiometricAttempts",
        "lockoutDurationMs",
        "requireFreshAuthenticationMs",
        "encryptionAlgorithm",
        "persistSessionState"
    ]

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func loadOverrides() -> [String: Any]? {
        guard let payload = defaults.dictionary(forKey: Self.storageKey) else {
            return nil
        }

        guard let version = payload["version"] as? Int, version == Self.payloadVersion else {
            return nil
        }

        guard let rawOverrides = payload["overrides"] as? [String: Any] else {
            return nil
        }

        return sanitize(rawOverrides)
    }

    func saveOverrides(_ overrides: [String: Any]) {
        let sanitized = sanitize(overrides)
        let payload: [String: Any] = [
            "version": Self.payloadVersion,
            "updatedAt": Int64(Date().timeIntervalSince1970 * 1000),
            "overrides": sanitized
        ]
        defaults.set(payload, forKey: Self.storageKey)
    }

    func clearOverrides() {
        defaults.removeObject(forKey: Self.storageKey)
    }

    private func sanitize(_ source: [String: Any]) -> [String: Any] {
        var result: [String: Any] = [:]

        for (key, value) in source {
            guard Self.allowedOverrideKeys.contains(key) else { continue }

            if value is Bool || value is Int || value is Double || value is String {
                result[key] = value
            }
        }

        return result
    }
}
