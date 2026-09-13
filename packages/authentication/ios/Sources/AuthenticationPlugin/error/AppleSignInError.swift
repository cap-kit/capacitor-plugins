/**
 * @file AppleSignInError.swift
 * Typed Apple sign-in failure categories (iOS).
 *
 * Mirror of the Android `AppleSignInError`; `AuthenticationUtils.mapAppleError`
 * translates them onto the shared ten-code `AuthenticationErrorCode` set.
 * No new codes exist.
 */
enum AppleSignInError {
    /// The user dismissed or closed the native sign-in UI.
    case userCancelled

    /// The nonce returned by the flow does not match the one issued.
    case invalidNonce

    /// The state returned by the flow does not match the one issued.
    case invalidState

    /// The apple configuration (clientId / redirectURI) is missing or malformed.
    case missingConfiguration

    /// The flow failed for a network or server-side reason.
    case network

    /// Any other, unclassified failure.
    case unknown
}
