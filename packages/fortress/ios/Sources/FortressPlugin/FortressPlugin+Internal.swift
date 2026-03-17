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

}
