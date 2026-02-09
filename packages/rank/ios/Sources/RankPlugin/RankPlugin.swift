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
        CAPPluginMethod(name: "requestReview", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "presentProductPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openStore", returnType: CAPPluginReturnPromise),
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
        RankLogger.debug("Rank plugin loaded.")
    }

    // MARK: - Error Mapping

    /**
     * Maps native RankError categories to standardized JS-facing error codes.
     *
     * - Parameters:
     * - call: The CAPPluginCall to reject.
     * - error: The native RankError encountered during execution.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: RankError
    ) {
        let code: String

        switch error {
        case .unavailable:
            code = "UNAVAILABLE"
        case .permissionDenied:
            code = "PERMISSION_DENIED"
        case .initFailed:
            code = "INIT_FAILED"
        case .unknownType:
            code = "UNKNOWN_TYPE"
        }

        call.reject(error.message, code)
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
        guard let appId = call.getString("appId") ?? config?.appleAppId else {
            call.reject("Apple App ID is missing.", "INIT_FAILED")
            return
        }

        implementation.presentProductPage(appId: appId) { success, error in
            if success {
                call.resolve()
            } else {
                call.reject(error?.localizedDescription ?? "Failed to load product page.", "INIT_FAILED")
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
        // Attempt to retrieve the App ID from call parameters or static configuration
        guard let appId = call.getString("appId") ?? config?.appleAppId else {
            call.reject(
                "Apple App ID is missing. Provide it in config or as a parameter.",
                "INIT_FAILED"
            )
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
        call.unavailable("Collections are not supported on iOS.")
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
        guard let appId = call.getString("appId") ?? config?.appleAppId else {
            call.reject("Apple App ID is missing.", "INIT_FAILED")
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
            call.reject("Search terms are missing.")
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
            call.reject("devId is missing.")
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
