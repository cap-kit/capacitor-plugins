import Foundation

/// Canonical error messages shared across platforms.
/// Keep these strings identical on iOS and Android.
enum RankErrorMessages {
    static let activityNotAvailable = "Activity not available"
    static let invalidAppleAppId = "Invalid or missing Apple App ID."
    static let invalidAndroidPackageName = "Invalid or missing Android package name."
    static let invalidCollectionName = "Invalid or missing collection name."
    static let invalidSearchTerms = "Invalid or missing search terms."
    static let invalidDeveloperId = "Invalid or missing developer ID."
    static let reviewAlreadyInProgress = "Review flow already in progress."
    static let nativeOperationFailed = "Native operation failed."
    static let productPageLoadFailed = "Failed to load product page."
    static let noRootViewControllerAvailable = "No rootViewController available."
    static let collectionsNotSupportedIos = "Collections are not supported on iOS."
}
