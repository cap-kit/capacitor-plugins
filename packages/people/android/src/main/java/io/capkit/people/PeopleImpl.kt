package io.capkit.people

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import io.capkit.people.config.PeopleConfig
import io.capkit.people.error.PeopleError
import io.capkit.people.error.PeopleErrorMessages
import io.capkit.people.logger.PeopleLogger
import io.capkit.people.models.ContactData
import io.capkit.people.models.ContactField
import io.capkit.people.models.ContactOrganization
import io.capkit.people.models.GroupData
import io.capkit.people.models.PostalAddress
import io.capkit.people.utils.PeopleUtils
import io.capkit.people.utils.PeopleUtils.getInt
import io.capkit.people.utils.PeopleUtils.getString

/**
 * Native implementation for the People plugin.
 *
 * Architectural rules:
 * - MUST NOT reference Capacitor APIs (JSObject, PluginCall, etc.).
 * - Returns only native models (ContactData) or standard Kotlin types.
 * - Logic is executed in the background thread provided by the bridge.
 */
class PeopleImpl(
  private val context: Context,
) {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Cached plugin configuration container.
   * Provided once during initialization via [updateConfig].
   */
  private lateinit var config: PeopleConfig

  // -----------------------------------------------------------------------------
  // Companion Object
  // -----------------------------------------------------------------------------

  private companion object {
    /**
     * Account type identifier for internal plugin identification.
     */
    const val ACCOUNT_TYPE = "io.capkit.people"

    /**
     * Human-readable account name for the plugin.
     */
    const val ACCOUNT_NAME = "People"

    /**
     * Chunk size for bulk Data queries to avoid very large IN clauses and memory spikes.
     */
    const val BULK_CONTACT_BATCH_SIZE = 200
  }

  // -----------------------------------------------------------------------------
  // Configuration & Capabilities
  // -----------------------------------------------------------------------------

  /**
   * Applies the plugin configuration to the implementation layer.
   */
  fun updateConfig(newConfig: PeopleConfig) {
    this.config = newConfig
    PeopleLogger.verbose = newConfig.verboseLogging
    PeopleLogger.debug(
      "Configuration applied. Verbose logging:",
      newConfig.verboseLogging.toString(),
    )
  }

  /**
   * Returns capabilities as a native Map.
   * Linked to runtime permission state for consistency.
   */
  fun getCapabilities(hasPermission: Boolean): Map<String, Boolean> =
    mapOf(
      "canRead" to hasPermission,
      "canWrite" to hasPermission,
      "canObserve" to hasPermission, // Updated: cannot observe without permissions
      "canManageGroups" to hasPermission,
      "canPickContact" to true,
    )

  // -----------------------------------------------------------------------------
  // Core Engine (Projection & Mapping)
  // -----------------------------------------------------------------------------

  /**
   * Internal Core Engine: Queries ContactsContract.Data and populates a ContactData object.
   * This is the heart of the "Projection Engine" to minimize memory usage.
   */
  private fun fillContactData(
    contactId: String,
    baseContact: ContactData,
    projection: List<String>,
  ): ContactData {
    val contentResolver = context.contentResolver
    val phones = mutableListOf<ContactField>()
    val emails = mutableListOf<ContactField>()
    var displayName: String? = baseContact.displayName
    var organization: ContactOrganization? = null
    val addresses = mutableListOf<PostalAddress>()

    val selection = "${ContactsContract.Data.CONTACT_ID} = ?"
    val selectionArgs = arrayOf(contactId)
    val sortOrder = ContactsContract.Data.MIMETYPE

    val cursor =
      contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        null,
        selection,
        selectionArgs,
        sortOrder,
      )

    cursor?.use { c ->
      while (c.moveToNext()) {
        val mimeType = c.getString(ContactsContract.Data.MIMETYPE)

        // Only process data rows that match the requested projection fields
        when (mimeType) {
          PeopleUtils.MIME_NAME -> {
            if (projection.contains("name")) {
              displayName = c.getString(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
            }
          }
          PeopleUtils.MIME_PHONE -> {
            if (projection.contains("phones")) {
              val number = c.getString(ContactsContract.CommonDataKinds.Phone.NUMBER)
              val type = c.getInt(ContactsContract.CommonDataKinds.Phone.TYPE) ?: 0
              val rawLabel =
                ContactsContract.CommonDataKinds.Phone
                  .getTypeLabel(context.resources, type, "")
                  .toString()
              val label = PeopleUtils.normalizeLabel(rawLabel)
              if (number != null) phones.add(ContactField(label, number))
            }
          }
          PeopleUtils.MIME_EMAIL -> {
            if (projection.contains("emails")) {
              val address = c.getString(ContactsContract.CommonDataKinds.Email.ADDRESS)
              val type = c.getInt(ContactsContract.CommonDataKinds.Email.TYPE) ?: 0
              val label =
                PeopleUtils.normalizeLabel(
                  ContactsContract.CommonDataKinds.Email
                    .getTypeLabel(context.resources, type, "")
                    .toString(),
                )
              if (address != null) emails.add(ContactField(label, address))
            }
          }
          PeopleUtils.MIME_ORG -> {
            if (projection.contains("organization")) {
              organization =
                ContactOrganization(
                  company = c.getString(ContactsContract.CommonDataKinds.Organization.COMPANY),
                  title = c.getString(ContactsContract.CommonDataKinds.Organization.TITLE),
                  department = c.getString(ContactsContract.CommonDataKinds.Organization.DEPARTMENT),
                )
            }
          }
          PeopleUtils.MIME_ADDRESS -> {
            if (projection.contains("addresses")) {
              val type = c.getInt(ContactsContract.CommonDataKinds.StructuredPostal.TYPE) ?: 0
              val label =
                PeopleUtils.normalizeLabel(
                  ContactsContract.CommonDataKinds.StructuredPostal
                    .getTypeLabel(context.resources, type, "")
                    .toString(),
                )

              addresses.add(
                PostalAddress(
                  label = label,
                  street = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.STREET),
                  city = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.CITY),
                  region = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.REGION),
                  postcode = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE),
                  country = c.getString(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY),
                ),
              )
            }
          }
          // Optimization: Load image data (thumbnail) only if explicitly requested
          ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> {
            if (projection.contains("image")) {
              // Lazy loading for images would be implemented here
              PeopleLogger.debug("Image data detected for contact $contactId")
            }
          }
        }
      }
    }

    // Return a new immutable copy of the data
    return baseContact.copy(
      displayName = displayName,
      organization = organization,
      phones = phones,
      emails = emails,
      addresses = addresses,
    )
  }

  private data class ContactAccum(
    var displayName: String? = null,
    var organization: ContactOrganization? = null,
    val phones: MutableList<ContactField> = mutableListOf(),
    val emails: MutableList<ContactField> = mutableListOf(),
    val addresses: MutableList<PostalAddress> = mutableListOf(),
  )

  /**
   * Bulk version of the projection engine.
   * It performs a single ContactsContract.Data query for a page of contacts.
   *
   * IMPORTANT: This function performs no side effects and does not touch PluginCall.
   */
  private fun fillContactsDataBulk(
    baseContacts: List<ContactData>,
    projection: List<String>,
  ): List<ContactData> {
    if (baseContacts.isEmpty()) return emptyList()

    val resolver = context.contentResolver
    val includeName = projection.contains("name")
    val includePhones = projection.contains("phones")
    val includeEmails = projection.contains("emails")
    val includeOrganization = projection.contains("organization")
    val includeAddresses = projection.contains("addresses")
    val includeImage = projection.contains("image")

    val ids = baseContacts.map { it.id }
    val sortOrder = "${ContactsContract.Data.CONTACT_ID} ASC, ${ContactsContract.Data.MIMETYPE} ASC"

    // Pre-size maps/lists to reduce allocations on large datasets.
    val acc = HashMap<String, ContactAccum>(ids.size * 2)
    for (id in ids) {
      acc[id] =
        ContactAccum(
          phones = ArrayList(2),
          emails = ArrayList(2),
          addresses = ArrayList(1),
        )
    }

    // Build a minimal column projection to avoid fetching unnecessary data.
    val cols = LinkedHashSet<String>(16)
    cols.add(ContactsContract.Data.CONTACT_ID)
    cols.add(ContactsContract.Data.MIMETYPE)

    if (includeName) {
      cols.add(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
    }

    if (includePhones) {
      cols.add(ContactsContract.CommonDataKinds.Phone.NUMBER)
      cols.add(ContactsContract.CommonDataKinds.Phone.TYPE)
    }

    if (includeEmails) {
      cols.add(ContactsContract.CommonDataKinds.Email.ADDRESS)
      cols.add(ContactsContract.CommonDataKinds.Email.TYPE)
    }

    if (includeOrganization) {
      cols.add(ContactsContract.CommonDataKinds.Organization.COMPANY)
      cols.add(ContactsContract.CommonDataKinds.Organization.TITLE)
      cols.add(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)
    }

    if (includeAddresses) {
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
      cols.add(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
    }

    // Note: "image" is currently only detected (no blob read), so no extra columns needed.
    val dataColumns = cols.toTypedArray()
    val requestedMimeTypes = mutableListOf<String>()
    if (includeName) requestedMimeTypes.add(PeopleUtils.MIME_NAME)
    if (includePhones) requestedMimeTypes.add(PeopleUtils.MIME_PHONE)
    if (includeEmails) requestedMimeTypes.add(PeopleUtils.MIME_EMAIL)
    if (includeOrganization) requestedMimeTypes.add(PeopleUtils.MIME_ORG)
    if (includeAddresses) requestedMimeTypes.add(PeopleUtils.MIME_ADDRESS)
    if (includeImage) requestedMimeTypes.add(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)

    // Query in fixed-size chunks to cap cursor and in-memory accumulation pressure on large pages.
    for (idChunk in ids.chunked(BULK_CONTACT_BATCH_SIZE)) {
      val idPlaceholders = idChunk.joinToString(",") { "?" }
      val selectionBuilder = StringBuilder("${ContactsContract.Data.CONTACT_ID} IN ($idPlaceholders)")
      val selectionArgs = mutableListOf<String>().apply { addAll(idChunk) }

      if (requestedMimeTypes.isNotEmpty()) {
        val mimePlaceholders = requestedMimeTypes.joinToString(",") { "?" }
        selectionBuilder.append(" AND ${ContactsContract.Data.MIMETYPE} IN ($mimePlaceholders)")
        selectionArgs.addAll(requestedMimeTypes)
      }

      resolver
        .query(
          ContactsContract.Data.CONTENT_URI,
          dataColumns,
          selectionBuilder.toString(),
          selectionArgs.toTypedArray(),
          sortOrder,
        )?.use { c ->
          val idxContactId = c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
          val idxMimeType = c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)

          // Cache column indexes to avoid repeated lookups inside the loop.
          val idxDisplayName =
            if (includeName) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
            } else {
              -1
            }

          val idxPhoneNumber =
            if (includePhones) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            } else {
              -1
            }
          val idxPhoneType =
            if (includePhones) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            } else {
              -1
            }

          val idxEmailAddress =
            if (includeEmails) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            } else {
              -1
            }
          val idxEmailType =
            if (includeEmails) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            } else {
              -1
            }

          val idxOrgCompany =
            if (includeOrganization) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
            } else {
              -1
            }
          val idxOrgTitle =
            if (includeOrganization) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
            } else {
              -1
            }
          val idxOrgDepartment =
            if (includeOrganization) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)
            } else {
              -1
            }

          val idxAddrType =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
            } else {
              -1
            }
          val idxAddrStreet =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
            } else {
              -1
            }
          val idxAddrCity =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
            } else {
              -1
            }
          val idxAddrRegion =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
            } else {
              -1
            }
          val idxAddrPostcode =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
            } else {
              -1
            }
          val idxAddrCountry =
            if (includeAddresses) {
              c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
            } else {
              -1
            }

          while (c.moveToNext()) {
            val contactId = c.getString(idxContactId) ?: continue
            val bucket = acc[contactId] ?: continue

            val mimeType = c.getString(idxMimeType)

            when (mimeType) {
              PeopleUtils.MIME_NAME -> {
                if (idxDisplayName >= 0) {
                  bucket.displayName = c.getString(idxDisplayName)
                }
              }

              PeopleUtils.MIME_PHONE -> {
                if (idxPhoneNumber >= 0 && idxPhoneType >= 0) {
                  val number = c.getString(idxPhoneNumber)
                  val type = c.getInt(idxPhoneType)
                  val rawLabel =
                    ContactsContract.CommonDataKinds.Phone
                      .getTypeLabel(context.resources, type, "")
                      .toString()
                  val label = PeopleUtils.normalizeLabel(rawLabel)
                  if (number != null) bucket.phones.add(ContactField(label, number))
                }
              }

              PeopleUtils.MIME_EMAIL -> {
                if (idxEmailAddress >= 0 && idxEmailType >= 0) {
                  val address = c.getString(idxEmailAddress)
                  val type = c.getInt(idxEmailType)
                  val label =
                    PeopleUtils.normalizeLabel(
                      ContactsContract.CommonDataKinds.Email
                        .getTypeLabel(context.resources, type, "")
                        .toString(),
                    )
                  if (address != null) bucket.emails.add(ContactField(label, address))
                }
              }

              PeopleUtils.MIME_ORG -> {
                if (idxOrgCompany >= 0 || idxOrgTitle >= 0 || idxOrgDepartment >= 0) {
                  bucket.organization =
                    ContactOrganization(
                      company = if (idxOrgCompany >= 0) c.getString(idxOrgCompany) else null,
                      title = if (idxOrgTitle >= 0) c.getString(idxOrgTitle) else null,
                      department = if (idxOrgDepartment >= 0) c.getString(idxOrgDepartment) else null,
                    )
                }
              }

              PeopleUtils.MIME_ADDRESS -> {
                if (idxAddrType >= 0) {
                  val type = c.getInt(idxAddrType)
                  val label =
                    PeopleUtils.normalizeLabel(
                      ContactsContract.CommonDataKinds.StructuredPostal
                        .getTypeLabel(context.resources, type, "")
                        .toString(),
                    )

                  bucket.addresses.add(
                    PostalAddress(
                      label = label,
                      street = if (idxAddrStreet >= 0) c.getString(idxAddrStreet) else null,
                      city = if (idxAddrCity >= 0) c.getString(idxAddrCity) else null,
                      region = if (idxAddrRegion >= 0) c.getString(idxAddrRegion) else null,
                      postcode = if (idxAddrPostcode >= 0) c.getString(idxAddrPostcode) else null,
                      country = if (idxAddrCountry >= 0) c.getString(idxAddrCountry) else null,
                    ),
                  )
                }
              }

              ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE -> {
                if (includeImage) {
                  PeopleLogger.debug("Image data detected for contact $contactId")
                }
              }
            }
          }
        }
    }

    // Preserve original order of baseContacts
    return baseContacts.map { base ->
      val bucket = acc[base.id]
      base.copy(
        displayName = bucket?.displayName ?: base.displayName,
        organization = bucket?.organization,
        phones = bucket?.phones ?: mutableListOf(),
        emails = bucket?.emails ?: mutableListOf(),
        addresses = bucket?.addresses ?: mutableListOf(),
      )
    }
  }

  // -----------------------------------------------------------------------------
  // Read Operations (Systemic & Picker)
  // -----------------------------------------------------------------------------

  /**
   * Extracts contact details from a specific URI returned by the Picker.
   * Returns the native ContactData model.
   */
  fun getContactFromUri(
    contactUri: Uri,
    projection: List<String>,
  ): ContactData? {
    var contactId: String? = null
    var lookupKey: String? = null

    context.contentResolver
      .query(
        contactUri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY),
        null,
        null,
        null,
      )?.use {
        if (it.moveToFirst()) {
          contactId = it.getString(ContactsContract.Contacts._ID)
          lookupKey = it.getString(ContactsContract.Contacts.LOOKUP_KEY)
        }
      }

    val id = contactId ?: return null
    return fillContactData(id, PeopleUtils.createEmptyContact(id, lookupKey), projection)
  }

  /**
   * Retrieves a paginated list of contacts using native models.
   */
  fun getContacts(
    projection: List<String>,
    limit: Int,
    offset: Int,
  ): Pair<List<ContactData>, Int> {
    val resolver = context.contentResolver

    // Never pass SQL functions (e.g. COUNT(_ID)) in projection: some providers reject non-column tokens.
    // Use a second safe query with minimal projection and read cursor.count instead.
    val totalCount =
      resolver
        .query(
          ContactsContract.Contacts.CONTENT_URI,
          arrayOf(ContactsContract.Contacts._ID),
          null,
          null,
          null,
        )?.use { it.count } ?: 0

    val baseContacts = mutableListOf<ContactData>()

    // Fetch paginated contact IDs
    resolver
      .query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY),
        null,
        null,
        "${ContactsContract.Contacts._ID} ASC LIMIT $limit OFFSET $offset",
      )?.use { c ->
        while (c.moveToNext()) {
          val id = c.getString(ContactsContract.Contacts._ID) ?: continue
          val lookupKey = c.getString(ContactsContract.Contacts.LOOKUP_KEY)
          baseContacts.add(PeopleUtils.createEmptyContact(id, lookupKey))
        }
      }

    val contacts = fillContactsDataBulk(baseContacts, projection)
    return Pair(contacts, totalCount)
  }

  /**
   * Retrieves a single contact by its unique ID using native models.
   */
  fun getContactById(
    id: String,
    projection: List<String>,
  ): ContactData? {
    context.contentResolver
      .query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY),
        "${ContactsContract.Contacts._ID} = ?",
        arrayOf(id),
        null,
      )?.use {
        if (it.moveToFirst()) {
          val lookupKey = it.getString(ContactsContract.Contacts.LOOKUP_KEY)
          return fillContactData(id, PeopleUtils.createEmptyContact(id, lookupKey), projection)
        }
      }
    return null
  }

  /**
   * Searches for contacts matching a query string using native models.
   */
  fun searchContacts(
    query: String,
    projection: List<String>,
    limit: Int,
  ): Pair<List<ContactData>, Int> {
    val matchedIds = mutableSetOf<String>()
    val selection =
      """
      ${ContactsContract.Data.MIMETYPE} IN (?, ?, ?) AND 
      (${ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME} LIKE ? OR 
       ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ? OR 
       ${ContactsContract.CommonDataKinds.Email.ADDRESS} LIKE ?)
      """.trimIndent()

    val args =
      arrayOf(PeopleUtils.MIME_NAME, PeopleUtils.MIME_PHONE, PeopleUtils.MIME_EMAIL, "%$query%", "%$query%", "%$query%")

    context.contentResolver
      .query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.Data.CONTACT_ID),
        selection,
        args,
        null,
      )?.use { c ->
        while (c.moveToNext()) {
          c.getString(ContactsContract.Data.CONTACT_ID)?.let { matchedIds.add(it) }
        }
      }

    val results = matchedIds.take(limit).mapNotNull { getContactById(it, projection) }
    return Pair(results, matchedIds.size)
  }

  // -----------------------------------------------------------------------------
  // Group Management
  // -----------------------------------------------------------------------------

  /**
   * Lists all available contact groups as native Maps.
   */
  fun listGroups(): List<GroupData> {
    val groups = mutableListOf<GroupData>()
    context.contentResolver
      .query(
        ContactsContract.Groups.CONTENT_URI,
        arrayOf(
          ContactsContract.Groups._ID,
          ContactsContract.Groups.TITLE,
          ContactsContract.Groups.ACCOUNT_NAME,
          ContactsContract.Groups.GROUP_IS_READ_ONLY,
        ),
        null,
        null,
        "${ContactsContract.Groups.TITLE} ASC",
      )?.use { c ->
        while (c.moveToNext()) {
          groups.add(
            GroupData(
              id = c.getString(ContactsContract.Groups._ID) ?: "",
              name = c.getString(ContactsContract.Groups.TITLE) ?: "",
              source = c.getString(ContactsContract.Groups.ACCOUNT_NAME) ?: "local",
              readOnly = c.getInt(ContactsContract.Groups.GROUP_IS_READ_ONLY) == 1,
            ),
          )
        }
      }
    return groups
  }

  /**
   * Creates a new group.
   *
   * @param name The name of the new group.
   * @return The created group model.
   */
  fun createGroup(name: String): GroupData {
    val values = ContentValues().apply { put(ContactsContract.Groups.TITLE, name) }
    val uri =
      context.contentResolver.insert(ContactsContract.Groups.CONTENT_URI, values)
        ?: throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_CREATE_GROUP)

    context.contentResolver
      .query(
        uri,
        arrayOf(
          ContactsContract.Groups._ID,
          ContactsContract.Groups.TITLE,
          ContactsContract.Groups.ACCOUNT_NAME,
          ContactsContract.Groups.GROUP_IS_READ_ONLY,
        ),
        null,
        null,
        null,
      )?.use { c ->
        if (c.moveToFirst()) {
          return GroupData(
            id = c.getString(ContactsContract.Groups._ID) ?: "",
            name = c.getString(ContactsContract.Groups.TITLE) ?: "",
            source = c.getString(ContactsContract.Groups.ACCOUNT_NAME) ?: "local",
            readOnly = c.getInt(ContactsContract.Groups.GROUP_IS_READ_ONLY) == 1,
          )
        }
      }
    throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_CREATE_GROUP)
  }

  /**
   * Deletes a group.
   *
   * @param groupId The ID of the group to delete.
   */
  fun deleteGroup(groupId: String) {
    val uri =
      Uri
        .withAppendedPath(ContactsContract.Groups.CONTENT_URI, groupId)
        // To prevent accidental deletion of all groups, some systems require this
        .buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build()

    if (context.contentResolver.delete(uri, null, null) <= 0) {
      throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_DELETE_GROUP)
    }
  }

  /**
   * Adds multiple contacts to a group.
   *
   * @param groupId The ID of the group.
   * @param contactIds A list of contact IDs to add.
   */
  fun addPeopleToGroup(
    groupId: String,
    contactIds: List<String>,
  ) {
    for (id in contactIds) {
      val values =
        ContentValues().apply {
          put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID, id)
          put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
          put(
            ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
          )
        }
      if (context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) == null) {
        throw PeopleError.Unavailable(PeopleErrorMessages.FAILED)
      }
    }
  }

  /**
   * Removes multiple contacts from a group.
   *
   * @param groupId The ID of the group.
   * @param contactIds A list of contact IDs to remove.
   */
  fun removePeopleFromGroup(
    groupId: String,
    contactIds: List<String>,
  ) {
    // Define the selection criteria with formatted string to respect line length limits
    val where =
      """
      ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ? AND 
      ${ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID} = ? AND 
      ${ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE} = ?
      """.trimIndent()

    for (id in contactIds) {
      val args =
        arrayOf(
          groupId,
          id,
          ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
        )

      val deletedRows =
        context.contentResolver.delete(
          ContactsContract.Data.CONTENT_URI,
          where,
          args,
        )

      if (deletedRows == 0) {
        PeopleLogger.error("Failed to remove contact $id from group $groupId")
        throw PeopleError.Unavailable(PeopleErrorMessages.FAILED)
      }
    }
  }

  // -----------------------------------------------------------------------------
  // CRUD Operations
  // -----------------------------------------------------------------------------

  /**
   * Creates a new contact using native parameters.
   */
  fun createContact(
    givenName: String?,
    familyName: String?,
    phones: List<String>,
    emails: List<String>,
  ): ContactData {
    val ops = ArrayList<android.content.ContentProviderOperation>()

    ops.add(
      android.content.ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
        .build(),
    )

    ops.add(
      android.content.ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, PeopleUtils.MIME_NAME)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
        .build(),
    )

    phones.forEach { num ->
      ops.add(
        android.content.ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE, PeopleUtils.MIME_PHONE)
          .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
          .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
          .build(),
      )
    }

    val newContactId =
      try {
        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        results[0].uri?.lastPathSegment
          ?: throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_CREATE_CONTACT)
      } catch (e: Exception) {
        PeopleLogger.error("Failed to create contact", e)
        throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_CREATE_CONTACT)
      }

    return getContactById(newContactId, listOf("name", "phones", "emails"))
      ?: throw PeopleError.Unavailable(PeopleErrorMessages.FAILED_TO_RETRIEVE_CONTACT)
  }

  /**
   * Updates contact names using native parameters.
   * Returns a native ContactData model upon success.
   */
  fun updateContact(
    contactId: String,
    givenName: String?,
    familyName: String?,
  ): ContactData {
    if (!isAppOwned(contactId)) {
      PeopleLogger.error("Cannot update a contact that is not owned by the app.")
      throw PeopleError.PermissionDenied("Cannot update a contact that is not owned by the app.")
    }

    val ops = ArrayList<android.content.ContentProviderOperation>()

    ops.add(
      android.content.ContentProviderOperation
        .newUpdate(ContactsContract.Data.CONTENT_URI)
        .withSelection(
          "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
          arrayOf(contactId, PeopleUtils.MIME_NAME),
        ).withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, givenName)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, familyName)
        .build(),
    )

    return try {
      context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
      // Re-fetch using native model
      getContactById(contactId, listOf("name", "phones", "emails"))
        ?: throw PeopleError.Unavailable(PeopleErrorMessages.UPDATE_FAILED)
    } catch (e: Exception) {
      PeopleLogger.error("Failed to update contact", e)
      throw PeopleError.Unavailable(PeopleErrorMessages.UPDATE_FAILED)
    }
  }

  /**
   * Deletes a contact if it is owned by the app.
   */
  fun deleteContact(contactId: String) {
    if (!isAppOwned(contactId)) {
      throw PeopleError.PermissionDenied("Cannot modify a contact that is not owned by the app.")
    }
    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
    try {
      if (context.contentResolver.delete(uri, null, null) <= 0) {
        throw PeopleError.InitFailed(PeopleErrorMessages.FAILED)
      }
    } catch (e: Exception) {
      throw PeopleError.InitFailed(PeopleErrorMessages.FAILED)
    }
  }

  /**
   * Merges two contacts natively.
   * Returns the final state of the destination contact as ContactData.
   */
  fun mergeContacts(
    sourceId: String,
    destId: String,
  ): ContactData {
    if (!isAppOwned(sourceId) || !isAppOwned(destId)) {
      throw PeopleError.PermissionDenied("Cannot merge contacts that are not owned by the app.")
    }
    val ops = ArrayList<android.content.ContentProviderOperation>()
    val rawDestId = getRawContactId(destId) ?: throw PeopleError.Unavailable(PeopleErrorMessages.MERGE_FAILED)

    context.contentResolver
      .query(
        ContactsContract.Data.CONTENT_URI,
        null,
        "${ContactsContract.Data.CONTACT_ID} = ?",
        arrayOf(sourceId),
        null,
      )?.use {
        while (it.moveToNext()) {
          if (it.getString(ContactsContract.Data.MIMETYPE) != PeopleUtils.MIME_NAME) {
            ops.add(
              android.content.ContentProviderOperation
                .newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                  "${ContactsContract.Data._ID} = ?",
                  arrayOf(it.getString(it.getColumnIndexOrThrow(ContactsContract.Data._ID))),
                ).withValue(ContactsContract.Data.RAW_CONTACT_ID, rawDestId)
                .build(),
            )
          }
        }
      }

    ops.add(
      android.content.ContentProviderOperation
        .newDelete(
          Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, sourceId),
        ).build(),
    )

    return try {
      context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
      getContactById(destId, listOf("name", "phones", "emails"))
        ?: throw PeopleError.Unavailable(PeopleErrorMessages.MERGE_FAILED)
    } catch (e: Exception) {
      throw PeopleError.Unavailable(PeopleErrorMessages.MERGE_FAILED)
    }
  }

  // -----------------------------------------------------------------------------
  // Ownership & IDs Helpers
  // -----------------------------------------------------------------------------

  private fun isAppOwned(contactId: String): Boolean {
    val rawId = getRawContactId(contactId) ?: return false
    context.contentResolver
      .query(
        ContactsContract.RawContacts.CONTENT_URI,
        arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
        "${ContactsContract.RawContacts._ID} = ?",
        arrayOf(rawId),
        null,
      )?.use {
        if (it.moveToFirst()) return it.getString(0) == ACCOUNT_TYPE
      }
    return false
  }

  private fun getRawContactId(contactId: String): String? {
    context.contentResolver
      .query(
        ContactsContract.RawContacts.CONTENT_URI,
        arrayOf(ContactsContract.RawContacts._ID),
        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
        arrayOf(contactId),
        null,
      )?.use {
        if (it.moveToFirst()) return it.getString(0)
      }
    return null
  }
}
