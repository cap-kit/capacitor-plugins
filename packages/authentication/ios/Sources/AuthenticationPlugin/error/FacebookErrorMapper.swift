import Foundation

/**
 * Typed Facebook flow-level failure categories consumed by `FacebookErrorMapper`.
 *
 * These are outcome classes — the mapper translates them onto the shared ten-code
 * `AuthenticationError` set; no new codes exist. Mirror of the
 * Android `FacebookSignInError` enum (including `KEY_HASH_MISMATCH`, whose
 * actionable classification is preserved cross-platform even though the `1349094`
 * key-hash failure is Android-specific — parity gate).
 */
enum FacebookSignInError {
    /// The user dismissed or closed the Facebook login dialog.
    case userCancelled

    /// The facebook configuration (app id) is missing or malformed.
    case missingConfiguration

    /// The Android key hash does not match the Meta developer settings (`1349094`).
    case keyHashMismatch

    /// The Graph `/me` request failed for a network or server-side reason.
    case graphNetwork

    /// The flow returned blank or malformed input.
    case malformedInput

    /// `refreshToken('facebook')` — Facebook issues no refresh token.
    case refreshNotSupported

    /// Any other, unclassified failure.
    case unknown
}

/**
 * @file FacebookErrorMapper.swift
 * Maps Facebook sign-in failures onto the plugin's native `AuthenticationError`
 * model: cancellation → `USER_CANCELLED`; missing config /
 * keyhash `1349094` → actionable `INIT_FAILED`; Graph/network → `UNAVAILABLE`;
 * blank/malformed and no-refresh → `INVALID_INPUT` (the latter with the exact
 * "Facebook issues no refresh token" message). Zero new error codes. Pure and side-effect free; messages
 * match the Android `FacebookErrorMapper` verbatim.
 */
enum FacebookErrorMapper {

    /// The Android key-hash value in the SDK's registration-failure message.
    static let androidKeyHashMismatchCode = "1349094"

    /// Exact no-refresh rejection message, shared across platforms.
    static let refreshNotSupportedMessage = "Facebook issues no refresh token"

    private static let keyHashActionableMessage =
        "Facebook sign-in failed: verify key hash matches Facebook developer settings."

    /**
     * Maps a typed `FacebookSignInError` category onto a native `AuthenticationError`.
     */
    static func map(_ error: FacebookSignInError) -> AuthenticationError {
        switch error {
        case .userCancelled:
            return AuthenticationError.userCancelled("The user cancelled the Facebook sign-in flow.")
        case .missingConfiguration:
            return AuthenticationError.initFailed("Facebook configuration is missing or malformed.")
        case .keyHashMismatch:
            return AuthenticationError.initFailed(keyHashActionableMessage)
        case .graphNetwork:
            return AuthenticationError.unavailable("The Facebook Graph request failed over the network.")
        case .malformedInput:
            return AuthenticationError.invalidInput("The Facebook sign-in returned malformed input.")
        case .refreshNotSupported:
            return AuthenticationError.invalidInput(refreshNotSupportedMessage)
        case .unknown:
            return AuthenticationError.initFailed("The Facebook sign-in flow failed for an unknown reason.")
        }
    }

    /**
     * True when an SDK exception message is the known Android key-hash registration
     * failure (message contains `1349094`).
     */
    static func isKeyHashMismatch(_ rawMessage: String?) -> Bool {
        rawMessage?.contains(androidKeyHashMismatchCode) == true
    }

    /**
     * Classifies a raw SDK exception message onto the ten-code set:
     * keyhash `1349094` → actionable `INIT_FAILED`; blank/null message → `INVALID_INPUT`;
     * anything else (Graph/network) → `UNAVAILABLE`. Mirrors the Web reducer's
     * classification so all platforms report the same code.
     */
    static func mapSdkMessage(_ rawMessage: String?) -> AuthenticationError {
        if isKeyHashMismatch(rawMessage) {
            return AuthenticationError.initFailed(keyHashActionableMessage)
        }
        guard let rawMessage,
              !rawMessage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return AuthenticationError.invalidInput("The Facebook SDK returned an empty error message.")
        }
        return AuthenticationError.unavailable(rawMessage)
    }
}
