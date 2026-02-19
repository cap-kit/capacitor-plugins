import Foundation
import Capacitor

/**
 * Capacitor bridge for the Rank plugin.
 *
 * This class handles the communication between the JavaScript layer and the native iOS implementation.
 * It is responsible for input validation, configuration merging, and thread safety.
 */
@objc(RankPlugin)
public final class RankPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = RankImpl()

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: RankConfig?

    /// The unique identifier for the plugin used by the Capacitor bridge.
    public let identifier = "RankPlugin"

    /// The name used to reference this plugin in JavaScript (e.g., Rank.requestReview()).
    public let jsName = "Rank"

    /**
     * A list of methods exposed by this plugin to the JavaScript layer.
     * All methods defined here must be implemented with the @objc attribute.
     - `isAvailable`: Checks if the native review prompt can be shown on the current device.
     - `requestReview`: Triggers the native iOS In-App Review prompt.
     - `presentProductPage`: Navigates the user to the App Store product page within the app.
     - `openStore`: Opens the app's page on the App Store for leaving a review or viewing details.
     - `getPluginVersion`: A method that returns the version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "isAvailable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkReviewEnvironment", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestReview", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "presentProductPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openStore", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openStoreListing", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "search", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openDevPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openCollection", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Lifecycle

    /**
     * Plugin lifecycle entry point.
     *
     * Called once when the plugin is loaded. This method initializes the configuration
     * and prepares the native implementation.
     */
    override public func load() {
        // Initialize RankConfig with the correct type
        let cfg = RankConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        RankLogger.debug("Plugin loaded.")
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native RankError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: RankError
    ) {
        // Use the centralized errorCode and message defined in RankError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let rankError = error as? RankError {
            call.reject(rankError.message, rankError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    // MARK: - Availability

    /**
     * Checks if the native In-App Review prompt can be displayed on the current device.
     *
     * This method verifies if the necessary APIs are available and if the OS version is compatible.
     * It returns a boolean indicating availability, which can be used by the JavaScript layer to
     * conditionally show review prompts or fallback UI.
     *
     * - Parameter call: CAPPluginCall used to return the availability result.
     */
    @objc func isAvailable(_ call: CAPPluginCall) {
        let available = implementation.isAvailable()
        call.resolve([
            "value": available
        ])
    }

    @objc func checkReviewEnvironment(_ call: CAPPluginCall) {
        call.resolve([
            "canRequestReview": false,
            "reason": "PLAY_STORE_NOT_AVAILABLE"
        ])
    }

    // MARK: - Product Page

    /**
     * Presents the App Store product page within the application.
     *
     * This method uses SKStoreProductViewController to display the product page without leaving the app.
     * It accepts an optional appId parameter, which can override the global configuration.
     *
     * - Parameter call: CAPPluginCall containing:
     * - appId (String, optional): The Apple App ID. Fallbacks to global config.
     */
    @objc func presentProductPage(_ call: CAPPluginCall) {
        let rawAppId = call.getString("appId") ?? config?.appleAppId
        guard let appId = RankValidators.validateAppId(rawAppId) else {
            reject(call, error: .invalidInput(RankErrorMessages.invalidAppleAppId))
            return
        }

        implementation.presentProductPage(appId: appId) { success, error in
            // Ensure bridge resolution occurs on the main thread.
            DispatchQueue.main.async {
                if success {
                    call.resolve()
                } else {
                    if let rankError = error as? RankError {
                        self.reject(call, error: rankError)
                    } else {
                        self.reject(call, error: .initFailed(error?.localizedDescription ?? RankErrorMessages.productPageLoadFailed))
                    }
                }
            }
        }
    }

    // MARK: - Public Plugin Methods

    /**
     * Triggers the native iOS In-App Review prompt using SKStoreReviewController.
     *
     * This method resolves immediately as iOS does not provide a callback to determine
     * if the prompt was shown or if the user interacted with it.
     *
     * - Parameter call: CAPPluginCall provided by the bridge.
     */
    @objc func requestReview(_ call: CAPPluginCall) {
        // Log the request if verbose logging is enabled
        RankLogger.debug("Requesting review prompt.")

        // Native iOS SKStoreReviewController logic
        implementation.requestReview()

        // Always resolve immediately for iOS consistency
        call.resolve()
    }

    // MARK: - Store Navigation

    /**
     * Navigates the user to the App Store page for a specific application.
     *
     * - Parameter call: CAPPluginCall containing:
     * - appId (String, optional): The Apple App ID. Fallbacks to global config.
     */
    @objc func openStore(_ call: CAPPluginCall) {
        let rawAppId = call.getString("appId") ?? config?.appleAppId
        // Attempt to retrieve the App ID from call parameters or static configuration
        guard let appId = RankValidators.validateAppId(rawAppId) else {
            reject(call, error: .invalidInput(RankErrorMessages.invalidAppleAppId))
            return
        }

        RankLogger.debug("Opening App Store for ID: \(appId)")
        implementation.openStore(appId: appId)
        call.resolve()
    }

    /**
     * Opens a collection page on the App Store.
     *
     * This method is not supported on iOS as there is no direct way to link to collections. It will reject with an appropriate message.
     *
     * - Parameter call: CAPPluginCall provided by the bridge.
     */
    @objc func openCollection(_ call: CAPPluginCall) {
        reject(call, error: .unavailable(RankErrorMessages.collectionsNotSupportedIos))
    }

    /**
     * Opens the App Store page for the specified application ID.
     *
     * This method constructs a deep link using the `itms-apps` scheme to direct the user
     * to the review section of the application's store page.
     *
     * - Parameter appId: The numeric Apple App ID (e.g., "123456789").
     */
    @objc func openStoreListing(_ call: CAPPluginCall) {
        let rawAppId = call.getString("appId") ?? config?.appleAppId
        guard let appId = RankValidators.validateAppId(rawAppId) else {
            reject(call, error: .invalidInput(RankErrorMessages.invalidAppleAppId))
            return
        }
        implementation.openStoreListing(appId: appId)
        call.resolve()
    }

    // MARK: - Search

    /**
     * Searches the App Store for the given terms.
     *
     * - Parameter call: CAPPluginCall containing:
     * - terms (String): The search query to find apps or developers.
     */
    @objc func search(_ call: CAPPluginCall) {
        guard let terms = call.getString("terms") else {
            reject(call, error: .invalidInput(RankErrorMessages.invalidSearchTerms))
            return
        }
        implementation.search(terms: terms)
        call.resolve()
    }

    // MARK: - Developer Page

    /**
     * Opens the developer's page on the App Store.
     *
     * Since iOS does not have a direct link for developer pages, this method performs a search using the developer's name or ID.
     *
     * - Parameter call: CAPPluginCall containing:
     * - devId (String): The developer identifier or name to search for.
     */
    @objc func openDevPage(_ call: CAPPluginCall) {
        guard let devId = call.getString("devId") else {
            reject(call, error: .invalidInput(RankErrorMessages.invalidDeveloperId))
            return
        }
        implementation.search(terms: devId) // Fallback on iOS
        call.resolve()
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
}
