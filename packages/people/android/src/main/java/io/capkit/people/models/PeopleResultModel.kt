package io.capkit.people.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical result models for the People plugin (Android).
 *
 * These DTOs mirror the public JavaScript payloads returned by the plugin
 * methods and are serialized to a JSObject bridge payload via
 * kotlinx.serialization. Nullable/defaulted fields are omitted when unset
 * (the encoding Json instance does NOT set encodeDefaults), matching the
 * optional TypeScript properties.
 *
 * These models are consumed ONLY by the bridge (PeoplePlugin) layer. The
 * business layer works with the internal models in ContactModels.kt and is
 * mapped to these DTOs at the bridge boundary.
 */
@Serializable
data class UnifiedContactResult(
  /** The platform-specific unique identifier (UUID or Long). */
  @SerialName("id")
  val id: String,
  /** The unified display name. */
  @SerialName("name")
  val name: NameResult? = null,
  /** List of phone numbers. */
  @SerialName("phones")
  val phones: List<PhoneResult>? = null,
  /** List of email addresses. */
  @SerialName("emails")
  val emails: List<EmailResult>? = null,
  /** Optional organization details. */
  @SerialName("organization")
  val organization: OrganizationResult? = null,
  /** List of postal addresses. */
  @SerialName("addresses")
  val addresses: List<AddressResult>? = null,
)

/**
 * Display name sub-object of a unified contact.
 *
 * Mirrors the TypeScript `UnifiedContact.name` interface.
 */
@Serializable
data class NameResult(
  @SerialName("display")
  val display: String,
)

/**
 * Phone number representation.
 *
 * Mirrors the TypeScript `PhoneNumber` interface.
 */
@Serializable
data class PhoneResult(
  @SerialName("number")
  val number: String? = null,
  @SerialName("label")
  val label: String? = null,
)

/**
 * Email address representation.
 *
 * Mirrors the TypeScript `EmailAddress` interface.
 */
@Serializable
data class EmailResult(
  @SerialName("address")
  val address: String? = null,
  @SerialName("label")
  val label: String? = null,
)

/**
 * Organization representation.
 *
 * Mirrors the nested `UnifiedContact.organization` interface.
 */
@Serializable
data class OrganizationResult(
  @SerialName("company")
  val company: String? = null,
  @SerialName("title")
  val title: String? = null,
  @SerialName("department")
  val department: String? = null,
)

/**
 * Postal address representation.
 *
 * Mirrors the TypeScript `PostalAddress` interface.
 */
@Serializable
data class AddressResult(
  @SerialName("label")
  val label: String? = null,
  @SerialName("street")
  val street: String? = null,
  @SerialName("city")
  val city: String? = null,
  @SerialName("region")
  val region: String? = null,
  @SerialName("postcode")
  val postcode: String? = null,
  @SerialName("country")
  val country: String? = null,
)

/**
 * Represents a group in the address book.
 *
 * Mirrors the TypeScript `Group` interface.
 */
@Serializable
data class GroupResult(
  @SerialName("id")
  val id: String,
  @SerialName("name")
  val name: String,
  @SerialName("source")
  val source: String? = null,
  @SerialName("readOnly")
  val readOnly: Boolean,
)

/**
 * Wrapper result carrying a single unified contact.
 *
 * Used by `pickContact`, `getContact`, `createContact`, `updateContact`
 * and `mergeContacts`. Mirrors the TypeScript `PickContactResult` /
 * `CreateContactResult` / `UpdateContactResult` / `MergeContactsResult`
 * interfaces (each exposes a single `contact` property).
 */
@Serializable
data class ContactResult(
  @SerialName("contact")
  val contact: UnifiedContactResult,
)

/**
 * Result of the `getContacts()` and `searchPeople()` methods.
 *
 * Mirrors the TypeScript `GetContactsResult` interface.
 */
@Serializable
data class GetContactsResult(
  @SerialName("contacts")
  val contacts: List<UnifiedContactResult>,
  @SerialName("totalCount")
  val totalCount: Int,
)

/**
 * Result of the `listGroups()` method.
 *
 * Mirrors the TypeScript `ListGroupsResult` interface.
 */
@Serializable
data class ListGroupsResult(
  @SerialName("groups")
  val groups: List<GroupResult>,
)

/**
 * Result of the `createGroup()` method.
 *
 * Mirrors the TypeScript `CreateGroupResult` interface.
 */
@Serializable
data class CreateGroupResult(
  @SerialName("group")
  val group: GroupResult,
)

/**
 * Result of the `getPluginVersion()` method.
 *
 * Mirrors the TypeScript `PluginVersionResult` interface:
 * - version: native plugin version synchronized from package.json.
 */
@Serializable
data class PluginVersionResult(
  @SerialName("version")
  val version: String,
)
