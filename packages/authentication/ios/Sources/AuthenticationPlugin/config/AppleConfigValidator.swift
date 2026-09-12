import Foundation

/**
 * @file AppleConfigValidator.swift
 * Pure validation gate for the resolved Apple configuration (iOS).
 *
 * Mirror of the Android `AppleConfigValidator`
 * (`android/.../apple/AppleConfigValidator.kt`): a blank `clientId` or a blank
 * (but present) `redirectURI` is caller input failure (`INVALID_INPUT` with
 * Android-verbatim messages); a MISSING `redirectURI` is valid — the native
 * Sign in with Apple flow never uses it (Web/Android artifact), matching
 * Android's blank-if-present check. Absence of the whole `apple` block is
 * handled by `AuthenticationImpl.requireAppleConfig` (the Android
 * `AppleConfigResolver` analog). Side-effect free; messages match Android
 * verbatim (config parity gate).
 */
enum AppleConfigValidator {

    /**
     * Validates a resolved Apple configuration.
     *
     * - Parameter config: the resolved Apple configuration.
     * - Returns: the mapped `AuthenticationError` when invalid, or `nil` when valid.
     */
    static func validate(_ config: AuthenticationConfig.AppleConfig) -> AuthenticationError? {
        // Kotlin `isBlank()` semantics: whitespace-only strings are blank.
        let trimsBlank: (String) -> Bool = { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if config.clientId.map(trimsBlank) != false {
            return AuthenticationError.invalidInput("apple.clientId must not be blank.")
        }
        if config.redirectURI.map(trimsBlank) == true {
            return AuthenticationError.invalidInput("apple.redirectURI must not be blank.")
        }
        return nil
    }
}
