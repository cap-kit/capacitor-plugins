import Foundation

extension Config {
    private static let allowedLogLevels: Set<String> = ["error", "warn", "info", "debug", "verbose"]
    private static let allowedOverlayThemes: Set<String> = ["system", "light", "dark"]
    private static let allowedFallbackStrategies: Set<String> = ["deviceCredential", "none", "systemDefault"]
    private static let allowedEncryptionAlgorithms: Set<String> = ["AES-GCM", "AES-CBC"]

    mutating func applyRuntimeOverrides(_ overrides: [String: Any]) {
        applyBoolOverrides(overrides)
        applyIntOverrides(overrides)
        applyStringOverrides(overrides)

        if let value = overrides["privacyOverlayBackgroundOpacity"] as? Double,
           value == -1 || (0...1).contains(value) {
            privacyOverlayBackgroundOpacity = value
        }
    }

    private mutating func applyBoolOverrides(_ overrides: [String: Any]) {
        if let value = overrides["verboseLogging"] as? Bool { verboseLogging = value }
        if let value = overrides["enablePrivacyScreen"] as? Bool { enablePrivacyScreen = value }
        if let value = overrides["privacyOverlayShowText"] as? Bool { privacyOverlayShowText = value }
        if let value = overrides["privacyOverlayShowImage"] as? Bool { privacyOverlayShowImage = value }
        if let value = overrides["allowCachedAuthentication"] as? Bool { allowCachedAuthentication = value }
        if let value = overrides["persistSessionState"] as? Bool { persistSessionState = value }
    }

    private mutating func applyIntOverrides(_ overrides: [String: Any]) {
        if let value = overrides["lockAfterMs"] as? Int, value >= 0 { lockAfterMs = value }
        if let value = overrides["cachedAuthenticationTimeoutMs"] as? Int, value >= 0 {
            cachedAuthenticationTimeoutMs = value
        }
        if let value = overrides["maxBiometricAttempts"] as? Int, value >= 1 { maxBiometricAttempts = value }
        if let value = overrides["lockoutDurationMs"] as? Int, value >= 0 { lockoutDurationMs = value }
        if let value = overrides["requireFreshAuthenticationMs"] as? Int, value >= 0 {
            requireFreshAuthenticationMs = value
        }
    }

    private mutating func applyStringOverrides(_ overrides: [String: Any]) {
        if let value = overrides["logLevel"] as? String, Self.allowedLogLevels.contains(value) {
            logLevel = value
        }
        if let value = overrides["privacyOverlayText"] as? String { privacyOverlayText = value }
        if let value = overrides["privacyOverlayImageName"] as? String { privacyOverlayImageName = value }
        if let value = overrides["privacyOverlayTextColor"] as? String { privacyOverlayTextColor = value }
        if let value = overrides["privacyOverlayTheme"] as? String,
           Self.allowedOverlayThemes.contains(value) {
            privacyOverlayTheme = value
        }
        if let value = overrides["fallbackStrategy"] as? String,
           Self.allowedFallbackStrategies.contains(value) {
            fallbackStrategy = value
        }
        if let value = overrides["encryptionAlgorithm"] as? String,
           Self.allowedEncryptionAlgorithms.contains(value) {
            encryptionAlgorithm = value
        }
    }
}
