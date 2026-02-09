import Foundation
import StoreKit

/**
 * Native iOS implementation for the Rank plugin.
 *
 * This class contains pure platform logic and is isolated from the Capacitor bridge.
 * Architectural constraints:
 * - MUST NOT access CAPPluginCall.
 * - MUST NOT depend on Capacitor bridge APIs directly.
 * - MUST perform UI operations on the Main Thread.
 */
@objc public final class RankImpl: NSObject {

    // MARK: - Properties

    /// Cached plugin configuration containing logging and behavioral flags.
    private var config: RankConfig?

    // MARK: - Initialization

    /**
     * Initializes the implementation instance.
     */
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     * Applies static plugin configuration.
     *
     * This method MUST be called exactly once from the Plugin bridge layer during `load()`.
     * It synchronizes the native logger state with the provided configuration.
     *
     * - Parameter config: The immutable configuration container.
     */
    func applyConfig(_ config: RankConfig) {
        precondition(
            self.config == nil,
            "RankImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        RankLogger.verbose = config.verboseLogging

        RankLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Availability

    /**
     * Checks if the current iOS version supports the SKStoreReviewController API.
     * Since the plugin target is iOS 15+, this generally returns true.
     */
    @objc public func isAvailable() -> Bool {
        if #available(iOS 10.3, *) {
            return true
        }
        return false
    }

    // MARK: - Product Page

    /**
     * Presents the App Store product page within the application.
     * * - Parameter appId: The numeric Apple App ID.
     * - Parameter completion: Callback to signal when the page has been loaded or failed.
     */
    @objc public func presentProductPage(appId: String, completion: @escaping (Bool, Error?) -> Void) {
        let storeViewController = SKStoreProductViewController()
        let parameters = [SKStoreProductParameterITunesItemIdentifier: appId]

        // UI operations must be on the main thread
        DispatchQueue.main.async {
            storeViewController.loadProduct(withParameters: parameters) { success, error in
                if success {
                    // Attempt to find the top-most view controller to present the sheet
                    if let rootVC = UIApplication.shared.windows.first?.rootViewController {
                        rootVC.present(storeViewController, animated: true) {
                            completion(true, nil)
                        }
                    } else {
                        completion(false, nil)
                    }
                } else {
                    completion(false, error)
                }
            }
        }
    }

    // MARK: - Review Methods

    /**
     * Triggers the native iOS In-App Review prompt.
     *
     * Uses the modern `requestReview(in:)` API for iOS 14+ by identifying the active foreground scene.
     * Fallback logic is provided for older iOS versions (though the plugin target is iOS 15+).
     *
     * NOTE: The OS manages the frequency of this prompt; calling this does not guarantee a UI will appear.
     */
    @objc public func requestReview() {
        if #available(iOS 14.0, *) {
            let activeScene = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first { $0.activationState == .foregroundActive }

            guard let windowScene = activeScene else {
                // Diagnostic logging only: review prompt cannot be requested without an active scene
                RankLogger.error("Cannot request review: no active UIWindowScene found")
                return
            }

            RankLogger.debug("Requesting in-app review for active UIWindowScene")
            SKStoreReviewController.requestReview(in: windowScene)
        } else {
            // Legacy fallback (not expected to be used due to iOS 15+ minimum target)
            RankLogger.debug("Requesting in-app review using legacy API")
            SKStoreReviewController.requestReview()
        }
    }

    // MARK: - Store Navigation

    /**
     * Opens the App Store page for the specified application ID.
     *
     * This method constructs a deep link using the `itms-apps` scheme to direct the user
     * to the review section of the application's store page.
     *
     * - Parameter appId: The numeric Apple App ID (e.g., "123456789").
     */
    @objc public func openStore(appId: String) {
        guard let url = RankUtils.appStoreURL(appId: appId) else {
            RankLogger.error("Invalid App Store URL for ID: \(appId)")
            return
        }

        // RULE: All UI-related operations MUST be dispatched to the main thread.
        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: [:]) { success in
                if success {
                    RankLogger.debug("App Store opened successfully.")
                } else {
                    RankLogger.error("Failed to open App Store URL.")
                }
            }
        }
    }

    /**
     * Opens the App Store product page for the specified application ID.
     *
     * This method constructs a deep link using the `itms-apps` scheme to direct the user
     * to the product page of the application in the App Store.
     *
     * - Parameter appId: The numeric Apple App ID (e.g., "123456789").
     */
    @objc public func openStoreListing(appId: String) {
        let urlString = "itms-apps://itunes.apple.com/app/id\(appId)"
        if let url = URL(string: urlString) {
            DispatchQueue.main.async {
                UIApplication.shared.open(url)
            }
        }
    }

    /**
     * Opens the App Store search page with the specified search terms.
     *
     * This method constructs a deep link using the `itms-apps` scheme to direct the user
     * to the search results page in the App Store for the given terms.
     *
     * - Parameter terms: The search query string to find relevant apps in the App Store.
     */
    @objc public func search(terms: String) {
        guard let url = RankUtils.searchURL(terms: terms) else { return }
        DispatchQueue.main.async {
            UIApplication.shared.open(url)
        }
    }
}
