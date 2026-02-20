package io.capkit.rank.error

/**
 * Canonical error messages shared across platforms.
 * Keep these strings identical on iOS and Android.
 */
object RankErrorMessages {
  const val PLAY_STORE_NOT_AVAILABLE = "Play Store not available."
  const val NOT_INSTALLED_FROM_PLAY_STORE = "App is not installed from Play Store."
  const val ACTIVITY_NOT_AVAILABLE = "Activity not available"
  const val INVALID_APPLE_APP_ID = "Invalid or missing Apple App ID."
  const val INVALID_ANDROID_PACKAGE_NAME = "Invalid or missing Android package name."
  const val INVALID_COLLECTION_NAME = "Invalid or missing collection name."
  const val INVALID_SEARCH_TERMS = "Invalid or missing search terms."
  const val INVALID_DEVELOPER_ID = "Invalid or missing developer ID."
  const val REVIEW_ALREADY_IN_PROGRESS = "Review flow already in progress."
  const val NATIVE_OPERATION_FAILED = "Native operation failed."
  const val PLAY_CORE_REVIEW_API_UNAVAILABLE = "Play Core review API unavailable."
  const val PRODUCT_PAGE_LOAD_FAILED = "Failed to load product page."
  const val COLLECTIONS_NOT_SUPPORTED_IOS = "Collections are not supported on iOS."
}
