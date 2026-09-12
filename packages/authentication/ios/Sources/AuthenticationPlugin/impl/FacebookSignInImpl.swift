import Foundation
import UIKit
import FBSDKCoreKit
import FBSDKLoginKit

/**
 * SDK surface seam for the iOS Facebook flow. The production bindings
 * (`FacebookLoginGatewaySDK`, `FacebookGraphRequestSDK`) drive the real
 * facebook-ios-sdk 18.x artifacts; the orchestration core in `FacebookSignInImpl`
 * depends only on these protocols, keeping every behavioral decision XCTestable
 * without the SDK's presentation machinery.
 */
protocol FacebookSignInGateway: AnyObject {
    /**
     * Launches the native Facebook login dialog with the requested read permissions.
     *
     * - Parameter scopes: the read permissions to request (defaults applied by the impl).
     * - Parameter viewController: the presenting view controller resolved by the impl.
     * - Parameter completion: invoked exactly once with the typed outcome.
     */
    func launchLogin(
        scopes: [String],
        presenting viewController: UIViewController,
        completion: @escaping (FacebookLoginOutcome) -> Void
    )

    /** Logs the current Facebook session out of the SDK. */
    func logOut()
}

/**
 * `/me` profile fetch seam. The production binding drives `GraphRequest`; the
 * impl fetches the profile AFTER a successful login because the Graph result is
 * not part of `LoginManager`'s login result (graph round-trip).
 */
protocol FacebookGraphRequesting: AnyObject {
    /**
     * Fetches the granted-only `/me` profile for an access token.
     *
     * - Parameter token: the granted access token.
     * - Parameter completion: invoked exactly once with the mapped profile.
     */
    func fetchProfile(
        token: String,
        completion: @escaping (Result<SocialAuthResultUser, AuthenticationError>) -> Void
    )
}

/**
 * Typed outcome of a launched Facebook login (mirror of the Android
 * `FacebookSdkGateway` completion surface).
 */
enum FacebookLoginOutcome {
    /// The SDK delivered a granted access token plus the optional slot primitives.
    case success(
            accessToken: String,
            userId: String?,
            granted: [String]?,
            declined: [String]?,
            expiresAt: Double?
         )

    /// The user dismissed or closed the Facebook login dialog.
    case cancelled

    /// The flow failed with a mapped, ten-code `AuthenticationError`.
    case failure(AuthenticationError)
}

/**
 * @file FacebookSignInImpl.swift
 * Native iOS Facebook sign-in orchestration core: the `Settings.appID` configuration guard, the
 * launch surface (injected presenting view controller — the headless test
 * process resolves none, mirroring the plugin's `topViewController()` contract),
 * the typed outcome mapping (`UserCancelled` / ten-code failures), the `/me`
 * profile round-trip and the exactly-once completion. The SDK dialog surface is
 * injected (`FacebookSignInGateway` + `FacebookGraphRequesting`); the production
 * bindings live in this file. Completion is never invoked when the flow cannot
 * start (the caller resolves the returned error instead).
 */
final class FacebookSignInImpl {

    /// Default read permissions, matching the Web/Android provider defaults.
    private static let defaultScopes = ["public_profile", "email"]

    /// The injected dialog surface.
    private let gateway: FacebookSignInGateway

    /// The injected `/me` profile surface.
    private let graph: FacebookGraphRequesting

    /// Effective read permissions (defaults applied when the caller passes none).
    private let scopes: [String]

    /// Completion invoked exactly once with the typed outcome.
    private let onCompletion: (Result<TokenSet, AuthenticationError>) -> Void

    /// Exactly-once gate (the gateway may fire at most once per launch).
    private var isFinished = false

    /**
     * Creates the coordinator for one interactive flow.
     *
     * - Parameter scopes: OAuth scope keys; empty → `["public_profile", "email"]`.
     * - Parameter gateway: the injected SDK dialog surface.
     * - Parameter graph: the injected `/me` profile surface.
     * - Parameter completion: invoked exactly once with the mapped outcome.
     */
    init(
        scopes: [String],
        gateway: FacebookSignInGateway,
        graph: FacebookGraphRequesting,
        completion: @escaping (Result<TokenSet, AuthenticationError>) -> Void
    ) {
        self.gateway = gateway
        self.graph = graph
        self.scopes = scopes.isEmpty ? Self.defaultScopes : scopes
        self.onCompletion = completion
    }

    /**
     * Presents the Facebook login dialog. MUST be called from the main thread
     * (LoginManager presents a UIKit modal).
     *
     * - Parameter viewController: the presenting view controller resolved by the
     *   caller (the plugin's `topViewController()` analog — the impl never
     *   resolves it so the flow stays testable in a headless test process).
     * - Returns: the mapped `AuthenticationError` when the flow CANNOT start
     *   (completion is NOT invoked), or `nil` when the dialog was presented.
     */
    func start(presenting viewController: UIViewController) -> AuthenticationError? {
        guard Settings.shared.appID?.isEmpty == false else {
            return AuthenticationError.initFailed(
                "Facebook is not configured. Set 'facebookAppId' (or Info.plist 'FacebookAppID')."
            )
        }
        gateway.launchLogin(scopes: scopes, presenting: viewController) { [weak self] outcome in
            guard let self = self else { return }
            self.handle(outcome)
        }
        return nil
    }

    /**
     * Signs the current Facebook session out of the SDK.
     */
    static func logOut() {
        LoginManager().logOut()
    }

    // MARK: - Private

    private func handle(_ outcome: FacebookLoginOutcome) {
        switch outcome {
        case let .success(accessToken, userId, granted, declined, expiresAt):
            fetchProfile(
                accessToken: accessToken,
                userId: userId,
                granted: granted,
                declined: declined,
                expiresAt: expiresAt
            )
        case .cancelled:
            finish(with: .failure(FacebookErrorMapper.map(.userCancelled)))
        case let .failure(error):
            finish(with: .failure(error))
        }
    }

    private func fetchProfile(
        accessToken: String,
        userId: String?,
        granted: [String]?,
        declined: [String]?,
        expiresAt: Double?
    ) {
        graph.fetchProfile(token: accessToken) { [weak self] result in
            guard let self = self else { return }
            switch result {
            case let .success(profile):
                let tokenSet = FacebookTokenMapper.mapSignInResult(
                    accessToken: accessToken,
                    userId: userId,
                    grantedPermissions: granted,
                    declinedPermissions: declined,
                    expiresAt: expiresAt,
                    profile: profile
                )
                self.finish(with: .success(tokenSet))
            case let .failure(error):
                self.finish(with: .failure(error))
            }
        }
    }

    private func finish(with outcome: Result<TokenSet, AuthenticationError>) {
        guard !isFinished else { return }
        isFinished = true
        onCompletion(outcome)
    }
}

// MARK: - Production SDK bindings

/**
 * Production `FacebookSignInGateway`: drives `LoginManager` + `LoginConfiguration`
 * against the real facebook-ios-sdk 18.x (`logIn(viewController:
 * configuration:completion:)` `@nonobjc` entry point — the `LoginConfiguration?`
 * is passed through, the SDK applies its default configuration for `nil`).
 * Permission names are sorted for deterministic bridge output.
 */
final class FacebookLoginGatewaySDK: FacebookSignInGateway {

    private let loginManager = LoginManager()

    func launchLogin(
        scopes: [String],
        presenting viewController: UIViewController,
        completion: @escaping (FacebookLoginOutcome) -> Void
    ) {
        let configuration = LoginConfiguration(permissions: scopes, tracking: .enabled)
        loginManager.logIn(viewController: viewController, configuration: configuration) { result in
            switch result {
            case let .success(_, _, accessToken):
                guard let token = accessToken else {
                    completion(.failure(FacebookErrorMapper.map(.malformedInput)))
                    return
                }
                // SDK 18.x: `expirationDate` is a non-optional `Date` defaulting to
                // `.distantFuture` when no expiry was issued — emit the slot only
                // when an actual expiry is known (Android parity).
                let expiresAt: Double? = token.expirationDate == .distantFuture
                    ? nil
                    : token.expirationDate.timeIntervalSince1970
                completion(
                    .success(
                        accessToken: token.tokenString,
                        userId: token.userID,
                        granted: token.permissions.map(\.name).sorted(),
                        declined: token.declinedPermissions.map(\.name).sorted(),
                        expiresAt: expiresAt
                    )
                )
            case .cancelled:
                completion(.cancelled)
            case let .failed(error):
                completion(.failure(FacebookErrorMapper.mapSdkMessage(error.localizedDescription)))
            }
        }
    }

    func logOut() {
        loginManager.logOut()
    }
}

/**
 * Production `FacebookGraphRequesting`: drives `GraphRequest` `/me` with the
 * granted-only fields (`id,name,email,picture.width(720).height(720)`) using the
 * granted access token (graph round-trip). The picture URL is lifted
 * from the `picture.data.url` nesting; a blank name stays nil (Android parity).
 */
final class FacebookGraphRequestSDK: FacebookGraphRequesting {

    func fetchProfile(
        token: String,
        completion: @escaping (Result<SocialAuthResultUser, AuthenticationError>) -> Void
    ) {
        let request = GraphRequest(
            graphPath: "me",
            parameters: ["fields": "id,name,email,picture.width(720).height(720)"],
            tokenString: token,
            version: nil,
            httpMethod: .get
        )
        request.start { _, result, error in
            if let error = error {
                completion(.failure(FacebookErrorMapper.mapSdkMessage(error.localizedDescription)))
                return
            }
            guard let dict = result as? [String: Any] else {
                // A successful (non-error) /me response MUST carry a JSON object;
                // anything else is malformed input.
                completion(.failure(FacebookErrorMapper.map(.malformedInput)))
                return
            }
            let profile = FacebookTokenMapper.mapProfile(
                id: dict["id"] as? String,
                name: dict["name"] as? String,
                email: dict["email"] as? String,
                pictureUrl: Self.extractPictureURL(from: dict)
            )
            completion(.success(profile))
        }
    }

    private static func extractPictureURL(from dict: [String: Any]) -> String? {
        guard let picture = dict["picture"] as? [String: Any],
              let data = picture["data"] as? [String: Any] else {
            return nil
        }
        return data["url"] as? String
    }
}
