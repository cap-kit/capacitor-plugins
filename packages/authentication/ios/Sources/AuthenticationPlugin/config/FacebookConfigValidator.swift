import Foundation

/**
 * @file FacebookConfigValidator.swift
 * Pure validation gate for the resolved Facebook configuration (iOS).
 *
 * Mirror of the Android `FacebookConfigValidator`
 * (`android/.../facebook/FacebookConfigValidator.kt`, config parity gate):
 * an ABSENT `facebook` block is an initialization failure
 * (`INIT_FAILED`, "Missing 'facebook' configuration; provide appId."); a blank
 * `facebookAppId` or a blank (but present) `facebookClientToken` is caller input
 * failure (`INVALID_INPUT`, Kotlin `isBlank()` parity — whitespace-only strings
 * are blank); an explicitly empty scope set is caller error. The client token is
 * OPTIONAL but blank-when-present is invalid. Side-effect free; messages match
 * Android verbatim.
 */
enum FacebookConfigValidator {

    /**
     * Validates a resolved Facebook configuration.
     *
     * - Parameter config: the resolved Facebook configuration, or `nil` when the
     *   `facebook` block is absent.
     * - Returns: the mapped `AuthenticationError` when invalid, or `nil` when valid.
     */
    static func validate(_ config: AuthenticationConfig.FacebookConfig?) -> AuthenticationError? {
        guard let config = config else {
            return AuthenticationError.initFailed("Missing 'facebook' configuration; provide appId.")
        }
        // Kotlin `isBlank()` semantics: whitespace-only strings are blank.
        let trimsBlank: (String) -> Bool = { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if config.facebookAppId.map(trimsBlank) != false {
            return AuthenticationError.invalidInput("facebook.appId must not be blank.")
        }
        if config.facebookClientToken.map(trimsBlank) == true {
            return AuthenticationError.invalidInput("facebook.clientToken must not be blank.")
        }
        // The config reader applies the default scope set, so only an EXPLICIT
        // empty list arrives here (Android parity).
        if config.scopes.isEmpty {
            return AuthenticationError.invalidInput("facebook.scopes must not be empty.")
        }
        return nil
    }
}
