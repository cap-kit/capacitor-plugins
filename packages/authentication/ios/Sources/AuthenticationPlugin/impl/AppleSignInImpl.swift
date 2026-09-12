import AuthenticationServices
import Foundation
import UIKit

/**
 * @file AppleSignInImpl.swift
 * Native Sign in with Apple orchestration.
 *
 * `ASAuthorizationController.delegate` and `presentationContextProvider` are WEAK
 * references, so this coordinator MUST be retained by `AuthenticationImpl`
 * (`activeAppleSignIn`) for the whole interactive flow. The completion callback
 * is invoked exactly once, on the main queue, typed as
 * `Result<AppleSignInResult, AuthenticationError>`.
 *
 * The raw nonce is hashed (`sha256Base64URL`) onto the request; the `id_token`
 * `nonce` claim MUST equal that hash ("OpenID nonce mismatch." → INVALID_INPUT,
 * Android `StateNonceValidator` parity). Apple issues no native access/refresh
 * token — the server exchanges the `authorizationCode`.
 */
final class AppleSignInImpl: NSObject {

    /// Typed outcome of a completed Apple sign-in flow.
    struct AppleSignInResult {
        /// Vault `TokenSet` carrying `authorizationCode`/`idToken` (no fabricated tokens).
        let tokenSet: TokenSet

        /// User profile, when Apple shared it (first sign-in only; nil on later sign-ins).
        let user: SocialAuthResultUser?
    }

    /// Completion invoked exactly once on the main queue.
    private let onCompletion: (Result<AppleSignInResult, AuthenticationError>) -> Void

    /// SHA-256 base64url of the raw nonce, placed on the request and validated
    /// against the `id_token` `nonce` claim.
    private let nonceHash: String

    /// OAuth scopes, mapped to `ASAuthorization.Scope` (`name`/`email` defaults).
    private let scopes: [String]

    /// Single-flight gate within the flow (delegate callbacks fire at most once).
    private var isFinished = false

    /**
     * Creates the coordinator for one interactive flow.
     *
     * - Parameter scopes: OAuth scope keys (`name`, `email`); empty → Apple defaults.
     * - Parameter nonce: raw nonce; its SHA-256 hash is placed on the request and the
     *   `id_token` `nonce` claim is validated against it.
     * - Parameter completion: invoked exactly once on the main queue.
     */
    init(
        scopes: [String],
        nonce: String,
        completion: @escaping (Result<AppleSignInResult, AuthenticationError>) -> Void
    ) {
        self.scopes = scopes
        self.nonceHash = AuthenticationUtils.sha256Base64URL(nonce)
        self.onCompletion = completion
        super.init()
    }

    /**
     * Presents the Sign in with Apple sheet. MUST be called from the main thread.
     *
     * Places the nonce hash on the request and requests the mapped scopes
     * (default `name email`, Web/Android parity).
     *
     * - Returns: `false` when the request could not be configured (never presents).
     */
    func start() -> Bool {
        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = Self.mapScopes(scopes)
        request.nonce = nonceHash

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
        return true
    }

    // MARK: - Private

    private static func mapScopes(_ scopes: [String]) -> [ASAuthorization.Scope] {
        var result: [ASAuthorization.Scope] = []
        let effective = scopes.isEmpty ? ["name", "email"] : scopes
        if effective.contains("name") {
            result.append(.fullName)
        }
        if effective.contains("email") {
            result.append(.email)
        }
        return result
    }

    private func finish(with outcome: Result<AppleSignInResult, AuthenticationError>) {
        guard !isFinished else { return }
        isFinished = true
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.onCompletion(outcome)
        }
    }

    /**
     * Maps an `ASAuthorizationAppleIDCredential` onto the typed outcome.
     *
     * Profile fields (`fullName`/`email`) are shared by Apple ONLY on first sign-in;
     * a credential without token material (no `authorizationCode` and no `id_token`)
     * fails with `INIT_FAILED`. A missing `id_token` is tolerated when the
     * `authorizationCode` is present (server exchange remains possible).
     */
    private func mapCredential(_ authorization: ASAuthorization) -> Result<AppleSignInResult, AuthenticationError> {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            return .failure(AuthenticationError.initFailed("The Apple sign-in returned no credential."))
        }

        let authorizationCode = credential.authorizationCode.map { code in
            String(data: code, encoding: .utf8) ?? code.base64EncodedString()
        }
        let idToken = credential.identityToken.map { token in
            String(data: token, encoding: .utf8) ?? token.base64EncodedString()
        }

        if let idToken,
           let nonceError = AuthenticationUtils.validateIDTokenNonce(idToken: idToken, expectedHash: nonceHash) {
            return .failure(nonceError)
        }

        guard let tokenSet = AuthenticationUtils.mapAppleTokenSet(
            authorizationCode: authorizationCode,
            idToken: idToken
        ) else {
            return .failure(AuthenticationError.initFailed("The Apple sign-in returned no credential."))
        }

        let user = AuthenticationUtils.mapUser(
            id: credential.user,
            email: credential.email,
            givenName: credential.fullName?.givenName,
            familyName: credential.fullName?.familyName,
            detectionStatus: credential.realUserStatus
        )
        return .success(AppleSignInResult(tokenSet: tokenSet, user: user))
    }

    private static func mapAuthorizationError(_ error: Error) -> AuthenticationError {
        guard let authorizationError = error as? ASAuthorizationError else {
            return AuthenticationUtils.mapAppleError(.unknown)
        }
        switch authorizationError.code {
        case .canceled:
            return AuthenticationUtils.mapAppleError(.userCancelled)
        case .failed, .invalidResponse, .notHandled, .notInteractive, .unknown:
            return AuthenticationUtils.mapAppleError(.unknown)
        case .matchedExcludedCredential, .credentialImport, .credentialExport,
             .preferSignInWithApple, .deviceNotConfiguredForPasskeyCreation:
            return AuthenticationUtils.mapAppleError(.unknown)
        @unknown default:
            return AuthenticationUtils.mapAppleError(.unknown)
        }
    }
}

// MARK: - ASAuthorizationControllerDelegate

extension AppleSignInImpl: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        finish(with: mapCredential(authorization))
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        finish(with: .failure(Self.mapAuthorizationError(error)))
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding

extension AppleSignInImpl: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let windows = scenes.flatMap { $0.windows }
        return windows.first { $0.isKeyWindow } ?? windows.first ?? ASPresentationAnchor()
    }
}
