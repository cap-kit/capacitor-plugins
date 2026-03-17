import Foundation
import UIKit

/**
 Native iOS implementation for the Fortress plugin.
 */
@objc
public final class Fortress: NSObject {

    static let defaultBiometricKeyAlias = "biometric_keypair"

    struct SessionState {
        let isLocked: Bool
        let lastActiveAt: Int64
    }

    struct PromptOptions {
        let title: String?
        let subtitle: String?
        let description: String?
        let negativeButtonText: String?
        let confirmationRequired: Bool?
    }

    struct ChallengePayload: Codable {
        let nonce: String
        let timestamp: Int64
        let deviceIdentifierHash: String
    }

    // MARK: - Configuration

    /// Immutable plugin configuration injected by the Plugin layer.
    var config: Config?
    let secureStorage = SecureStorage()
    let standardStorage = StandardStorage()
    let biometricAuth = BiometricAuth()
    let sessionManager = SessionManager()
    let privacyScreen = PrivacyScreen()
    var runtimeEnablePrivacyScreenOverride: Bool?
    var lastSuccessfulAuthAtMs: Int64 = 0
    var failedBiometricAttempts: Int = 0
    var lockoutUntilMs: Int64 = 0
    var overrideBiometryType: String?
    var overrideIsBiometricsAvailable: Bool?
    var overrideIsBiometricsEnabled: Bool?
    var overrideIsDeviceSecure: Bool?

    // Initializer
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     Sets the callback to be called when the user taps on the privacy screen.
     */
    func setPrivacyScreenTapCallback(_ callback: @MainActor @escaping () -> Void) {
        privacyScreen.setOnTapUnlock(callback)
    }

    /**
     Applies static plugin configuration.

     This method MUST be called exactly once
     from the Plugin layer during `load()`.

     Responsibilities:
     - Store immutable configuration
     - Configure runtime logging behavior
     - Configure privacy screen overlay
     */
    func applyConfig(_ config: Config) {
        self.config = config

        // Synchronize logger state
        Logger.verbose = config.verboseLogging
        Logger.setLevel(config.logLevel)
        KeychainHelper.setSynchronizable(config.enableICloudKeychainSync)

        // Configure privacy screen overlay
        var overlayConfig = OverlayConfig()
        overlayConfig.text = config.privacyOverlayText
        overlayConfig.showText = config.privacyOverlayShowText
        overlayConfig.textColor = UIColor(hex: config.privacyOverlayTextColor) ?? .white
        overlayConfig.backgroundOpacity = CGFloat(config.privacyOverlayBackgroundOpacity)
        overlayConfig.theme = config.privacyOverlayTheme
        overlayConfig.imageName = config.privacyOverlayImageName
        overlayConfig.showImage = config.privacyOverlayShowImage
        privacyScreen.updateOverlayConfig(overlayConfig)

        Logger.debug(
            "Configuration applied. Log level:",
            config.logLevel
        )
    }

    func configure(_ config: Config) {
        if config.fallbackStrategy != "deviceCredential" &&
            config.fallbackStrategy != "none" &&
            config.fallbackStrategy != "systemDefault" {
            Logger.warn("Invalid fallbackStrategy value. Falling back to systemDefault semantics.")
        }

        applyConfig(config)
    }

    func setRuntimeEnablePrivacyScreen(_ enabled: Bool?) {
        runtimeEnablePrivacyScreenOverride = enabled
    }

    func isPrivacyScreenActive() -> Bool {
        return isPrivacyScreenEnabled()
    }

    func shouldUseCachedAuthentication() -> Bool {
        guard let config else {
            return false
        }

        guard config.allowCachedAuthentication else {
            return false
        }

        guard config.cachedAuthenticationTimeoutMs > 0 else {
            return false
        }

        if config.requireFreshAuthenticationMs > 0 {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            if now - lastSuccessfulAuthAtMs > Int64(config.requireFreshAuthenticationMs) {
                return false
            }
        }

        guard lastSuccessfulAuthAtMs > 0 else {
            return false
        }

        let now = Int64(Date().timeIntervalSince1970 * 1000)
        return (now - lastSuccessfulAuthAtMs) <= Int64(config.cachedAuthenticationTimeoutMs)
    }

    func markAuthenticationSuccess() {
        lastSuccessfulAuthAtMs = Int64(Date().timeIntervalSince1970 * 1000)
        failedBiometricAttempts = 0
        lockoutUntilMs = 0
    }

    func clearAuthenticationCache() {
        lastSuccessfulAuthAtMs = 0
        failedBiometricAttempts = 0
        lockoutUntilMs = 0
    }

    func assertNotLockedOut() throws {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if now < lockoutUntilMs {
            throw NativeError.securityViolation(ErrorMessages.securityViolation)
        }
    }

    func recordBiometricFailure(_ error: Error) {
        guard let config else {
            return
        }

        let maxAttempts = config.maxBiometricAttempts
        let lockoutDuration = config.lockoutDurationMs
        guard maxAttempts > 0, lockoutDuration > 0 else {
            return
        }

        if let nativeError = error as? NativeError,
           case .cancelled = nativeError {
            return
        }

        failedBiometricAttempts += 1
        if failedBiometricAttempts >= maxAttempts {
            lockoutUntilMs = Int64(Date().timeIntervalSince1970 * 1000) + Int64(lockoutDuration)
            failedBiometricAttempts = 0
        }
    }

    func resolvePromptReason(
        promptMessage: String?,
        promptOptions: PromptOptions?,
        fallback: String
    ) -> String {
        if let promptMessage, !promptMessage.isEmpty {
            return promptMessage
        }

        var components: [String] = []
        if let title = promptOptions?.title, !title.isEmpty {
            components.append(title)
        }
        if let subtitle = promptOptions?.subtitle, !subtitle.isEmpty {
            components.append(subtitle)
        }
        if let description = promptOptions?.description, !description.isEmpty {
            components.append(description)
        }

        if components.isEmpty {
            return fallback
        }

        return components.joined(separator: "\n")
    }

    // MARK: - Session Lifecycle Delegates

    func setSessionLockCallback(_ callback: @escaping (Bool) -> Void) {
        sessionManager.onLockStatusChanged = callback
    }

    func setSessionBackgroundTimestamp() {
        sessionManager.setBackgroundTimestamp()
    }

    func evaluateSessionBackgroundGracePeriod(lockAfterMs: Int64) {
        sessionManager.evaluateBackgroundGracePeriod(lockAfterMs: lockAfterMs)
    }

    func setPrivacyScreenVisible(_ visible: Bool) {
        if !isPrivacyScreenEnabled() {
            privacyScreen.unlock()
            return
        }

        if visible {
            privacyScreen.lock()
        } else {
            privacyScreen.unlock()
        }
    }

}
