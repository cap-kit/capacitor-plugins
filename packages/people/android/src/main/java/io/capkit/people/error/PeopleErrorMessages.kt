package io.capkit.people.error

/**
 * Canonical error messages shared across platforms.
 * Keep these strings identical on iOS and Android.
 */
object PeopleErrorMessages {
  const val PERMISSION_DENIED = "Permission denied"
  const val ID_REQUIRED = "id is required"
  const val QUERY_REQUIRED = "query is required"
  const val EVENT_NAME_REQUIRED = "eventName is required"
  const val GROUP_NAME_REQUIRED = "Group name is required"
  const val GROUP_ID_REQUIRED = "Group ID is required"
  const val CONTACT_IDS_REQUIRED = "Contact IDs required"
  const val CONTACT_DATA_REQUIRED = "Contact data required"
  const val AT_LEAST_ONE_WRITABLE_FIELD_REQUIRED = "At least one writable contact field is required"
  const val CONTACT_ID_REQUIRED = "Contact ID is required"
  const val REQUIRED_FIELDS_MISSING = "Required fields missing"
  const val IDS_REQUIRED = "IDs required"

  fun unsupportedEventName(value: String) = "Unsupported eventName: $value"

  const val MISSING_CONTACTS_USAGE_DESCRIPTION = "Missing NSContactsUsageDescription in Info.plist"
  const val NO_CONTACT_URI_RETURNED = "No contact URI returned"
  const val USER_CANCELLED_SELECTION = "User cancelled selection"
  const val FAILED_TO_PARSE_CONTACT_FROM_URI = "Failed to parse contact from URI"
  const val ERROR_PROCESSING_CONTACT = "Error processing contact"

  fun unsupportedProjectionField(value: String) = "Unsupported projection field: $value"

  const val CONTACT_NOT_FOUND = "Contact not found"
  const val FAILED = "Failed"
  const val FAILED_TO_CREATE_GROUP = "Failed to create group"
  const val FAILED_TO_DELETE_GROUP = "Failed to delete group"
  const val FAILED_TO_RETRIEVE_CONTACT = "Failed to retrieve contact"
  const val FAILED_TO_CREATE_CONTACT = "Failed to create contact"
  const val UPDATE_FAILED = "Update failed"
  const val MERGE_FAILED = "Merge failed"
}
