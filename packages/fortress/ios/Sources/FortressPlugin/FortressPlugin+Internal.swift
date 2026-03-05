import Foundation
import Capacitor

extension FortressPlugin {

    func parsePromptOptions(_ call: CAPPluginCall) -> Fortress.PromptOptions? {
        guard let promptOptions = call.getObject("promptOptions") else {
            return nil
        }

        return Fortress.PromptOptions(
            title: promptOptions["title"] as? String,
            subtitle: promptOptions["subtitle"] as? String,
            description: promptOptions["description"] as? String,
            negativeButtonText: promptOptions["negativeButtonText"] as? String,
            confirmationRequired: promptOptions["confirmationRequired"] as? Bool
        )
    }

    /**
     Maps native `NativeError` values to JS-facing error codes.

     CONTRACT:
     - Error codes MUST be stable and documented
     - Error codes MUST match across platforms
     - Platform-specific error codes are FORBIDDEN
     */
    func reject(
        _ call: CAPPluginCall,
        error: NativeError
    ) {
        call.reject(error.message, error.errorCode)
    }

    func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let nativeError = error as? NativeError {
            reject(call, error: nativeError)
        } else {
            let message = error.localizedDescription.isEmpty
                ? ErrorMessages.unexpectedNativeError
                : error.localizedDescription
            reject(call, error: .initFailed(message))
        }
    }

    func runtimeConfigSnapshot(_ config: Config) -> [String: Any] {
        return [
            "verboseLogging": config.verboseLogging,
            "logLevel": config.logLevel,
            "lockAfterMs": config.lockAfterMs,
            "enablePrivacyScreen": config.enablePrivacyScreen,
            "privacyOverlayText": config.privacyOverlayText,
            "privacyOverlayImageName": config.privacyOverlayImageName,
            "privacyOverlayShowText": config.privacyOverlayShowText,
            "privacyOverlayShowImage": config.privacyOverlayShowImage,
            "privacyOverlayTextColor": config.privacyOverlayTextColor,
            "privacyOverlayBackgroundOpacity": config.privacyOverlayBackgroundOpacity,
            "privacyOverlayTheme": config.privacyOverlayTheme,
            "fallbackStrategy": config.fallbackStrategy,
            "allowCachedAuthentication": config.allowCachedAuthentication,
            "cachedAuthenticationTimeoutMs": config.cachedAuthenticationTimeoutMs,
            "maxBiometricAttempts": config.maxBiometricAttempts,
            "lockoutDurationMs": config.lockoutDurationMs,
            "requireFreshAuthenticationMs": config.requireFreshAuthenticationMs,
            "encryptionAlgorithm": config.encryptionAlgorithm,
            "persistSessionState": config.persistSessionState
        ]
    }

    func applyRuntimeStringOverrides(call: CAPPluginCall, to config: inout Config) {
        if let value = call.getString("logLevel") { config.logLevel = value }
        if let value = call.getString("privacyOverlayText") { config.privacyOverlayText = value }
        if let value = call.getString("privacyOverlayImageName") { config.privacyOverlayImageName = value }
        if let value = call.getString("privacyOverlayTextColor") { config.privacyOverlayTextColor = value }
        if let value = call.getString("privacyOverlayTheme") { config.privacyOverlayTheme = value }
        if let value = call.getString("fallbackStrategy") { config.fallbackStrategy = value }
        if let value = call.getString("encryptionAlgorithm") { config.encryptionAlgorithm = value }
    }

    func applyRuntimeBoolOverrides(call: CAPPluginCall, to config: inout Config) {
        if let value = call.getBool("verboseLogging") { config.verboseLogging = value }
        if let value = call.getBool("enablePrivacyScreen") { config.enablePrivacyScreen = value }
        if let value = call.getBool("privacyOverlayShowText") { config.privacyOverlayShowText = value }
        if let value = call.getBool("privacyOverlayShowImage") { config.privacyOverlayShowImage = value }
        if let value = call.getBool("allowCachedAuthentication") { config.allowCachedAuthentication = value }
        if let value = call.getBool("persistSessionState") { config.persistSessionState = value }
    }

    func applyRuntimeIntOverrides(call: CAPPluginCall, to config: inout Config) {
        if let value = call.getInt("lockAfterMs") { config.lockAfterMs = value }
        if let value = call.getInt("cachedAuthenticationTimeoutMs") { config.cachedAuthenticationTimeoutMs = value }
        if let value = call.getInt("maxBiometricAttempts") { config.maxBiometricAttempts = value }
        if let value = call.getInt("lockoutDurationMs") { config.lockoutDurationMs = value }
        if let value = call.getInt("requireFreshAuthenticationMs") { config.requireFreshAuthenticationMs = value }
    }

    func applyRuntimeConfigOverrides(call: CAPPluginCall, to config: inout Config) {
        applyRuntimeStringOverrides(call: call, to: &config)
        applyRuntimeBoolOverrides(call: call, to: &config)
        applyRuntimeIntOverrides(call: call, to: &config)

        if let value = call.getDouble("privacyOverlayBackgroundOpacity") {
            config.privacyOverlayBackgroundOpacity = value
        }
    }
}
