import Foundation

/**
 Utility helpers for the Rank plugin.

 This type is intentionally empty and serves as a placeholder
 for future shared helper functions.

 Keeping a dedicated utilities file helps maintain a clean
 separation between core logic and helper code.
 */
struct RankUtils {
    /**
     Builds the App Store URL for the given Apple App ID.

     - Parameter appId: The numeric Apple App ID.
     - Returns: A valid App Store URL for opening the product page or review flow.
     */
    static func appStoreURL(appId: String) -> URL? {
        let urlString = "itms-apps://itunes.apple.com/app/id\(appId)?action=write-review"
        return URL(string: urlString)
    }

    /**
     Builds the App Store search URL for the given search terms.

     - Parameter terms: The search query string.
     - Returns: A valid App Store URL for performing a search.
     */
    static func searchURL(terms: String) -> URL? {
        let encoded = terms.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return URL(string: "itms-apps://itunes.apple.com/search?term=\(encoded)")
    }

    /**
     Builds the App Store URL for a developer page based on the given developer identifier.

     - Parameter devId: The developer identifier or name.
     - Returns: A valid App Store URL for the developer's page or search results.
     */
    static func devPageURL(devId: String) -> URL? {
        // iOS not have a direct link for DevID, so we use search as fallback
        return searchURL(terms: devId)
    }
}
