package io.capkit.people.utils

import android.database.Cursor
import android.provider.ContactsContract
import io.capkit.people.error.PeopleError
import io.capkit.people.models.ContactData

/**
 * Utility helpers for the People plugin.
 *
 * This object provides centralized logic for data transformation,
 * cursor management, and JSON marshalling between native and bridge layers.
 */
object PeopleUtils {
  // -----------------------------------------------------------------------------
  // Projection Validation
  // -----------------------------------------------------------------------------

  private val supportedProjections =
    setOf(
      "name",
      "organization",
      "phones",
      "emails",
      "addresses",
    )
  // "image" is intentionally not supported on Android and must be rejected by validation.

  /**
   * Validates that all requested projection fields are supported.
   * Throws PeopleError.UnknownType if a field is invalid.
   */
  fun validateProjection(projection: List<String>) {
    for (field in projection) {
      if (!supportedProjections.contains(field)) {
        throw PeopleError.UnknownType("Unsupported projection field: $field")
      }
    }
  }

  /**
   * Normalizes native labels into a shared canonical set:
   * mobile, home, work, main, fax, other.
   */
  fun normalizeLabel(label: String?): String {
    val normalized = label?.trim()?.lowercase() ?: return "other"
    if (normalized.contains("mobile") || normalized.contains("cell")) return "mobile"
    if (normalized.contains("home")) return "home"
    if (normalized.contains("work")) return "work"
    if (normalized.contains("main")) return "main"
    if (normalized.contains("fax")) return "fax"
    return "other"
  }

  // -----------------------------------------------------------------------------
  // MimeType Constants
  // -----------------------------------------------------------------------------

  const val MIME_NAME = ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
  const val MIME_PHONE = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
  const val MIME_EMAIL = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
  const val MIME_ORG = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
  const val MIME_ADDRESS = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
  const val MIME_NOTE = ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
  const val MIME_WEBSITE = ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE

  // -----------------------------------------------------------------------------
  // Cursor Helpers
  // -----------------------------------------------------------------------------

  /**
   * Safely retrieves a String from a Cursor column.
   * Returns null if the column is missing or data is unavailable.
   */
  fun Cursor.getString(columnName: String): String? {
    val index = this.getColumnIndex(columnName)
    if (index == -1) return null
    return this.getString(index)
  }

  /**
   * Safely retrieves an Int from a Cursor column.
   * Returns null if the column is missing or data is unavailable.
   */
  fun Cursor.getInt(columnName: String): Int? {
    val index = this.getColumnIndex(columnName)
    if (index == -1) return null
    return this.getInt(index)
  }

  // -----------------------------------------------------------------------------
  // Native Model Factories
  // -----------------------------------------------------------------------------

  /**
   * Creates a native ContactData model instance.
   * Ensures the Implementation layer remains decoupled from Capacitor types.
   */
  fun createEmptyContact(
    id: String,
    lookupKey: String?,
  ): ContactData =
    ContactData(
      id = id,
      lookupKey = lookupKey,
    )

  // -----------------------------------------------------------------------------
  // Bridge Marshalling (JS -> Native)
  // -----------------------------------------------------------------------------

  /**
   * Safely extracts a list of strings from a JSONArray based on a specific key.
   * Uses optJSONObject and optString for maximum stability during data parsing.
   */
  fun extractStringList(
    array: org.json.JSONArray?,
    key: String,
  ): List<String> {
    val list = mutableListOf<String>()
    if (array == null) return list

    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i)
      obj?.optString(key)?.let { value ->
        if (value.isNotEmpty()) {
          list.add(value)
        }
      }
    }
    return list
  }
}
