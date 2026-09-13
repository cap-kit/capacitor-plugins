import Foundation
import UIKit
import Capacitor
import FBSDKCoreKit

/**
 * Capacitor bridge for the Authentication plugin.
 *
 * This class handles the communication between the JavaScript layer and the native iOS implementation.
 * It is responsible for input validation, configuration merging, and thread safety.
 */
@objc(AuthenticationPlugin)
public final class AuthenticationPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = AuthenticationImpl()

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: AuthenticationConfig?

    /// The unique identifier for the plugin used by the Capacitor bridge.
    public let identifier = "AuthenticationPlugin"

    /// The name used to reference this plugin in JavaScript (e.g., Authentication.getPluginVersion()).
    public let jsName = "Authentication"

    /**
     * A list of methods exposed by this plugin to the JavaScript layer.
     * All methods defined here must be implemented with the @objc attribute.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "signIn", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "signOut", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCurrentAccessToken", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "refreshToken", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "logout", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createRestoreCredential", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRestoreCredential", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clearRestoreCredential", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     Called once when the plugin is loaded by the Capacitor bridge.
     This is the correct place to:
     - read configuration values
     - initialize native resources
     - configure the implementation instance
     */
    override public func load() {
        // Initialize AuthenticationConfig with the correct type
        let cfg = AuthenticationConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        AuthenticationLogger.debug("Plugin loaded. Version: ", PluginVersion.number)

        // The facebook flow's Safari/App-switch redirect returns through Capacitor's
        // open-URL notification (forward to the SDK ApplicationDelegate).
        observeCapacitorOpenURL()
    }

    // MARK: - Facebook open-URL forwarding

    /// Retained observer token for the Capacitor open-URL notification (removed in deinit).
    private var openURLToken: NSObjectProtocol?

    /**
     * Forwards the incoming Capacitor open-URL notification to the Facebook SDK.
     *
     * Capacitor v8 has no `CAPPlugin.handleOpenURL` override; the bridge posts
     * `CapacitorOpenURLNotification` with `userInfo["url"]` when the app is opened
     * through a registered scheme or universal link. The observer is registered on
     * the main queue — `ApplicationDelegate` must hand the URL to the active
     * `LoginManager` flow on the main thread.
     */
    private func observeCapacitorOpenURL() {
        guard openURLToken == nil else { return }
        openURLToken = NotificationCenter.default.addObserver(
            forName: Notification.Name("CapacitorOpenURLNotification"),
            object: nil,
            queue: .main
        ) { notification in
            guard let url = notification.userInfo?["url"] as? URL else { return }
            _ = ApplicationDelegate.shared.application(
                UIApplication.shared,
                open: url,
                options: [:]
            )
        }
    }

    deinit {
        if let openURLToken {
            NotificationCenter.default.removeObserver(openURLToken)
        }
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native AuthenticationError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: AuthenticationError
    ) {
        // Use the centralized errorCode and message defined in AuthenticationError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let authenticationError = error as? AuthenticationError {
            call.reject(authenticationError.message, authenticationError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    // MARK: - Version

    /**
     * Retrieves the current native plugin version.
     *
     * This version is synchronized from the project's package.json during the build process.
     *
     * - Parameter call: CAPPluginCall used to return the version string.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Facade: initialize

    /**
     * Initializes the provider, loading any provider-specific configuration.
     *
     * - Parameter call: the Capacitor bridge call carrying the `provider` argument.
     */
    @objc func initialize(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            try implementation.initialize(provider: provider)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Facade: signIn

    /**
     * Signs the user in with the given provider and returns typed tokens.
     *
     * - Parameter call: the Capacitor bridge call carrying `provider` and `options.autoSelect`.
     */
    @objc func signIn(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        let autoSelect = call.getBool("options.autoSelect", call.getBool("autoSelect") ?? false)

        implementation.signIn(
            provider: provider,
            autoSelect: autoSelect,
            callback: SignInBridgeCallback(call: call, provider: provider)
        )
    }

    // MARK: - Facade: signOut / logout

    /**
     * Ends the session for the provider, removing persisted tokens.
     *
     * - Parameter call: the Capacitor bridge call carrying the `provider` argument.
     */
    @objc func signOut(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            try implementation.signOut(provider: provider)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    /**
     * Alias for [signOut]: clears the session and persisted tokens.
     *
     * - Parameter call: the Capacitor bridge call carrying the `provider` argument.
     */
    @objc func logout(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            try implementation.logout(provider: provider)
            call.resolve()
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Facade: getCurrentAccessToken

    /**
     * Returns the current access token for the provider, if any.
     *
     * - Parameter call: the Capacitor bridge call carrying the `provider` argument.
     */
    @objc func getCurrentAccessToken(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            let accessToken = try implementation.getCurrentAccessToken(provider: provider)
            call.resolve([
                "accessToken": accessToken ?? NSNull()
            ])
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Facade: refreshToken

    /**
     * Refreshes the access token using a stored refresh token.
     *
     * - Parameter call: the Capacitor bridge call carrying the `provider` argument.
     */
    @objc func refreshToken(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            let tokenSet = try implementation.refreshToken(provider: provider)
            call.resolve(tokenSetToDict(tokenSet))
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Facade: Restore credentials (iOS available=false)

    @objc func createRestoreCredential(_ call: CAPPluginCall) {
        do {
            let options = call.options?.reduce(into: [String: Any]()) { result, entry in
                result[String(describing: entry.key)] = entry.value
            }
            let result = try implementation.createRestoreCredential(options: options)
            call.resolve(result)
        } catch {
            handleError(call, error)
        }
    }

    @objc func getRestoreCredential(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            let result = try implementation.getRestoreCredential(provider: provider)
            call.resolve(result)
        } catch {
            handleError(call, error)
        }
    }

    @objc func clearRestoreCredential(_ call: CAPPluginCall) {
        let provider = call.getString("provider")
        do {
            let result = try implementation.clearRestoreCredential(provider: provider)
            call.resolve(result)
        } catch {
            handleError(call, error)
        }
    }

    // MARK: - Private: token set mapping

    /**
     * Maps a `TokenSet` to a JS-facing dictionary, omitting absent optional fields.
     */
    private func tokenSetToDict(_ tokenSet: TokenSet) -> [String: Any] {
        var dict: [String: Any] = [:]
        if let value = tokenSet.accessToken { dict["accessToken"] = value }
        if let value = tokenSet.refreshToken { dict["refreshToken"] = value }
        if let value = tokenSet.idToken { dict["idToken"] = value }
        if let value = tokenSet.serverAuthCode { dict["serverAuthCode"] = value }
        return dict
    }
}

/**
 * Bridges the Impl-layer `SignInCallback` to a Capacitor `CAPPluginCall`.
 *
 * Thick-bridge contract: resolves/rejects the call on the main thread so
 * GIDSignIn's completion (delivered on the main queue) is safe.
 */
private final class SignInBridgeCallback: AuthenticationImpl.SignInCallback {
    private let call: CAPPluginCall
    private let provider: String?

    init(call: CAPPluginCall, provider: String?) {
        self.call = call
        self.provider = provider
    }

    func onResult(_ tokens: TokenSet) {
        guard provider == "apple" else {
            if provider == "facebook" {
                call.resolve([
                    "provider": "facebook",
                    "tokens": AuthenticationUtils.mapFacebookTokens(tokens)
                ])
                return
            }
            // Google path — unchanged shape (code + full token set).
            var dict: [String: Any] = [:]
            if let value = tokens.accessToken { dict["accessToken"] = value }
            if let value = tokens.refreshToken { dict["refreshToken"] = value }
            if let value = tokens.idToken { dict["idToken"] = value }
            if let value = tokens.serverAuthCode { dict["serverAuthCode"] = value }
            call.resolve([
                "provider": "google",
                "tokens": dict
            ])
            return
        }
        // Apple path — authorization code + id token + optional first-sign-in profile.
        call.resolve([
            "provider": "apple",
            "tokens": AuthenticationUtils.mapAppleTokens(tokens)
        ])
    }

    func onError(_ error: AuthenticationError) {
        call.reject(error.message, error.errorCode)
    }
}
