package io.capkit.people.models

/**
 * Native representation of a Contact.
 *
 * Architectural rules:
 * - This class MUST remain agnostic to Capacitor JSObjects.
 * - Used exclusively by the Impl layer to orchestrate native data.
 * - Mapped to JS payloads only within the Utility or Plugin layer.
 */
data class ContactData(
  val id: String,
  val lookupKey: String? = null,
  val displayName: String? = null,
  val organization: ContactOrganization? = null, // Added for parity
  val phones: List<ContactField> = emptyList(),
  val emails: List<ContactField> = emptyList(),
  val addresses: List<PostalAddress> = emptyList(), // Extraction logic needed
)

/**
 * Native representation of a contact's organization.
 */
data class ContactOrganization(
  val company: String?,
  val title: String?,
  val department: String?,
)

// -----------------------------------------------------------------------------
// Support Models
// -----------------------------------------------------------------------------

/**
 * Represents a generic contact field (e.g., Phone or Email).
 * Includes a label (type) and the actual value.
 */
data class ContactField(
  val label: String?,
  val value: String?,
)

/**
 * Native representation of a physical postal address.
 * Maps to structured postal data from the Android Contacts provider.
 */
data class PostalAddress(
  val label: String?,
  val street: String?,
  val city: String?,
  val region: String?,
  val postcode: String?,
  val country: String?,
)

/**
 * Native representation of a contact group.
 */
data class GroupData(
  val id: String,
  val name: String,
  val source: String,
  val readOnly: Boolean,
)
