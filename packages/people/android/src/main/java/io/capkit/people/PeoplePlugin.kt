package io.capkit.people

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import io.capkit.people.config.PeopleConfig
import io.capkit.people.error.PeopleError
import io.capkit.people.error.PeopleErrorMessages
import io.capkit.people.logger.PeopleLogger
import io.capkit.people.utils.PeopleUtils

/**
 * Capacitor bridge for the People plugin.
 *
 * This class acts as the boundary between JavaScript and native Android code.
 * It handles input parsing, configuration management, and delegates execution
 * to the platform-specific implementation.
 */
@CapacitorPlugin(
  name = "People",
  permissions = [
    Permission(
      strings = [Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS],
      alias = "contacts",
    ),
  ],
)
class PeoplePlugin : Plugin() {
  // -----------------------------------------------------------------------------
  // Properties
  // -----------------------------------------------------------------------------

  /**
   * Immutable plugin configuration read from capacitor.config.ts.
   * * CONTRACT:
   * - Initialized exactly once in `load()`.
   * - Treated as read-only afterwards.
   */
  private lateinit var config: PeopleConfig

  /**
   * Native implementation layer containing core Android logic.
   *
   * CONTRACT:
   * - Owned by the Plugin layer.
   * - MUST NOT access PluginCall or Capacitor bridge APIs directly.
   */
  private lateinit var implementation: PeopleImpl

  /**
   * Observer state for monitoring system-wide contact changes.
   */
  private var observer: PeopleObserver? = null

  // -----------------------------------------------------------------------------
  // Lifecycle
  // -----------------------------------------------------------------------------

  /**
   * Called once when the plugin is loaded by the Capacitor bridge.
   *
   * This is the correct place to:
   * - read static configuration
   * - initialize native resources
   * - inject configuration into the implementation
   */
  override fun load() {
    super.load()

    config = PeopleConfig(this)
    implementation = PeopleImpl(context)
    implementation.updateConfig(config)

    PeopleLogger.debug("Plugin loaded. Version: ", BuildConfig.PLUGIN_VERSION)
  }

  /**
   * Called when the plugin is being destroyed.
   * Ensures all native resources are released.
   */
  override fun handleOnDestroy() {
    cleanupObserver()
    super.handleOnDestroy()
  }

  // -----------------------------------------------------------------------------
  // Internal Helpers & Denial-Path Handling
  // -----------------------------------------------------------------------------

  /**
   * Helper to validate 'contacts' permission alias.
   * Returns true if granted, otherwise rejects the call and returns false.
   */
  private fun checkContactsPermission(call: PluginCall): Boolean {
    if (getPermissionState("contacts") != PermissionState.GRANTED) {
      reject(call, PeopleError.PermissionDenied(PeopleErrorMessages.PERMISSION_DENIED))
      return false
    }
    return true
  }

  /**
   * Rejects the call with a message and a standardized error code.
   * Ensure consistency with the JS PeopleErrorCode enum.
   */
  private fun reject(
    call: PluginCall,
    error: PeopleError,
  ) {
    val code =
      when (error) {
        is PeopleError.Unavailable -> "UNAVAILABLE"
        is PeopleError.Cancelled -> "CANCELLED"
        is PeopleError.PermissionDenied -> "PERMISSION_DENIED"
        is PeopleError.InitFailed -> "INIT_FAILED"
        is PeopleError.InvalidInput -> "INVALID_INPUT"
        is PeopleError.UnknownType -> "UNKNOWN_TYPE"
        is PeopleError.NotFound -> "NOT_FOUND"
        is PeopleError.Conflict -> "CONFLICT"
        is PeopleError.Timeout -> "TIMEOUT"
      }

    // Always use the message from the PeopleError instance
    val message = error.message ?: "Unknown native error"
    call.reject(message, code)
  }

  private fun cleanupObserver() {
    observer?.let {
      try {
        it.dispose()
        context.contentResolver.unregisterContentObserver(it)
      } catch (e: Exception) {
        PeopleLogger.error("Error unregistering observer", e)
      }
      observer = null
    }
  }

  private fun hasWritableCreateContactField(contactData: JSObject): Boolean {
    val name = contactData.optJSONObject("name")
    if (name != null && name.length() > 0) return true

    val organization = contactData.optJSONObject("organization")
    if (organization != null && organization.length() > 0) return true

    val birthday = contactData.optJSONObject("birthday")
    if (birthday != null && birthday.length() > 0) return true

    val phones = contactData.optJSONArray("phones")
    if (phones != null && phones.length() > 0) return true

    val emails = contactData.optJSONArray("emails")
    if (emails != null && emails.length() > 0) return true

    val addresses = contactData.optJSONArray("addresses")
    if (addresses != null && addresses.length() > 0) return true

    val urls = contactData.optJSONArray("urls")
    if (urls != null && urls.length() > 0) return true

    if (contactData.optString("note", "").isNotBlank()) return true
    if (contactData.optString("image", "").isNotBlank()) return true

    return false
  }

  // -----------------------------------------------------------------------------
  // Zero-Permission Picker
  // -----------------------------------------------------------------------------

  /**
   * Launches the native contact picker (Zero-Permission).
   * This allows the user to select a single contact without granting full address book access.
   */
  @PluginMethod
  fun pickContact(call: PluginCall) {
    // 1. Prepare the intent
    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

    // Persist the call before launching an external Activity (Capacitor v8 rule).
    bridge.saveCall(call)

    // 2. Start Activity using Capacitor Bridge
    // Note: We use a specific callback name defined below
    startActivityForResult(call, intent, "handlePickContactResult")
  }

  /**
   * Processes the result from the system Contact Picker activity.
   */
  @ActivityCallback
  private fun handlePickContactResult(
    call: PluginCall,
    result: ActivityResult,
  ) {
    // Retrieve the persisted call (Capacitor v8 rule).
    val savedCall = bridge.getSavedCall(call.callbackId) ?: call
    try {
      if (result.resultCode == Activity.RESULT_OK && result.data != null) {
        val contactUri: Uri? = result.data?.data
        if (contactUri != null) {
          try {
            val projectionArray = savedCall.getArray("projection")
            val projection =
              if (projectionArray != null && projectionArray.length() > 0) {
                List(projectionArray.length()) { i -> projectionArray.getString(i) }
              } else {
                // Default projection: basic contact info
                listOf("name", "phones", "emails")
              }

            val contact = implementation.getContactFromUri(contactUri, projection)
            if (contact != null) {
              val ret = JSObject()
              ret.put("contact", contactToJS(contact))
              savedCall.resolve(ret)
            } else {
              reject(savedCall, PeopleError.NotFound(PeopleErrorMessages.CONTACT_NOT_FOUND))
            }
          } catch (e: Exception) {
            PeopleLogger.error("Error processing picked contact", e)
            reject(savedCall, PeopleError.InitFailed(PeopleErrorMessages.ERROR_PROCESSING_CONTACT))
          }
        } else {
          reject(savedCall, PeopleError.InitFailed(PeopleErrorMessages.NO_CONTACT_URI_RETURNED))
        }
      } else {
        // User cancellation should be mapped explicitly.
        reject(savedCall, PeopleError.Cancelled(PeopleErrorMessages.USER_CANCELLED_SELECTION))
      }
    } finally {
      bridge.releaseCall(savedCall)
    }
  }

  // -----------------------------------------------------------------------------
  // Event Listeners
  // -----------------------------------------------------------------------------

  @PluginMethod
  override fun addListener(call: PluginCall) {
    val eventName = call.getString("eventName")

    if (eventName.isNullOrBlank()) {
      return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.EVENT_NAME_REQUIRED))
    }

    if (eventName != "peopleChange") {
      return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.unsupportedEventName(eventName)))
    }

    if (!checkContactsPermission(call)) return

    call.setKeepAlive(true)
    super.addListener(call)

    if (observer == null) {
      observer =
        PeopleObserver { _, ids ->
          // Ensure a consistent payload for "peopleChange" listeners.
          // Always include "ids" as an array (may be empty) and a "type" string with default "update".
          val payload = JSObject()
          val changedIds: List<String> = ids ?: emptyList()
          val idsArray = JSArray()
          for (id in changedIds) {
            idsArray.put(id)
          }
          payload.put("ids", idsArray)

          payload.put("type", "update")

          if (hasListeners("peopleChange")) {
            notifyListeners("peopleChange", payload)
          }
        }

      context.contentResolver.registerContentObserver(PeopleObserver.CONTACTS_URI, true, observer!!)
    }
  }

  @PluginMethod
  override fun removeListener(call: PluginCall) {
    super.removeListener(call)
    if (!hasListeners("peopleChange")) {
      cleanupObserver()
    }
  }

  @PluginMethod
  override fun removeAllListeners(call: PluginCall) {
    cleanupObserver()
    super.removeAllListeners(call)
  }

  // -----------------------------------------------------------------------------
  // Systemic Access
  // -----------------------------------------------------------------------------

  /**
   * Retrieves a list of contacts from the device address book.
   * Requires the 'contacts' permission to be granted.
   *
   * @param call PluginCall containing 'limit', 'offset', and 'projection'.
   */
  @PluginMethod
  fun getContacts(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    // Ensure a safe default projection if none is provided to avoid empty results
    val projectionArray = call.getArray("projection")
    val projection =
      if (projectionArray != null && projectionArray.length() > 0) {
        List(projectionArray.length()) { i -> projectionArray.getString(i) }
      } else {
        // Default projection: basic contact info
        listOf("name", "phones", "emails")
      }

    try {
      PeopleUtils.validateProjection(projection)
    } catch (e: PeopleError) {
      return reject(call, e)
    }

    val limit = call.getInt("limit") ?: 50
    val offset = call.getInt("offset") ?: 0

    // Execute implementation logic (runs on plugin background thread)
    val (contacts, total) =
      implementation.getContacts(
        projection,
        limit,
        offset,
      )

    val ret = JSObject()
    ret.put("contacts", contactsToJSArray(contacts))
    ret.put("totalCount", total)

    call.resolve(ret)
  }

  /**
   * Retrieves a single contact by its ID.
   * Requires the 'contacts' permission to be granted.
   *
   * @param call PluginCall containing the 'id' of the contact.
   */
  @PluginMethod
  fun getContact(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val id = call.getString("id") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.ID_REQUIRED))

    // Apply safe default projection for single contact retrieval
    val projectionArray = call.getArray("projection")
    val projection =
      if (projectionArray != null && projectionArray.length() > 0) {
        List(projectionArray.length()) { i -> projectionArray.getString(i) }
      } else {
        // Default projection: basic contact info
        listOf("name", "phones", "emails")
      }

    try {
      PeopleUtils.validateProjection(projection)
    } catch (e: PeopleError) {
      return reject(call, e)
    }

    val contact = implementation.getContactById(id, projection)
    if (contact == null) return reject(call, PeopleError.NotFound(PeopleErrorMessages.CONTACT_NOT_FOUND))

    val ret = JSObject()
    ret.put("contact", contactToJS(contact))
    call.resolve(ret)
  }

  /**
   * Searches for contacts based on a query string.
   * Requires the 'contacts' permission to be granted.
   *
   * @param call PluginCall containing 'query', 'projection', and 'limit'.
   */
  @PluginMethod
  fun searchPeople(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val query =
      call.getString("query") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.QUERY_REQUIRED))
    // Apply safe default projection for search results
    val projectionArray = call.getArray("projection")
    val projection =
      if (projectionArray != null && projectionArray.length() > 0) {
        List(projectionArray.length()) { i -> projectionArray.getString(i) }
      } else {
        // Default projection: basic contact info
        listOf("name", "phones", "emails")
      }

    try {
      PeopleUtils.validateProjection(projection)
    } catch (e: PeopleError) {
      return reject(call, e)
    }

    val limit = call.getInt("limit") ?: 50

    // Execute implementation logic
    val (contacts, total) =
      implementation.searchContacts(
        query,
        projection,
        limit,
      )

    val ret = JSObject()
    ret.put("contacts", contactsToJSArray(contacts))
    ret.put("totalCount", total)

    call.resolve(ret)
  }

  // -----------------------------------------------------------------------------
  // CRUD & Group Management (Denied-path protected)
  // -----------------------------------------------------------------------------

  @PluginMethod
  fun listGroups(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    try {
      val groups = implementation.listGroups()
      val groupsArray = JSArray()
      for (group in groups) {
        groupsArray.put(groupToJS(group))
      }
      val ret = JSObject()
      ret.put("groups", groupsArray)
      call.resolve(ret)
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun createGroup(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val name =
      call.getString("name") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.GROUP_NAME_REQUIRED))

    try {
      val group = implementation.createGroup(name)
      val ret = JSObject()
      ret.put("group", groupToJS(group))
      call.resolve(ret)
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun deleteGroup(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val groupId =
      call.getString("groupId") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.GROUP_ID_REQUIRED))

    try {
      implementation.deleteGroup(groupId)
      call.resolve()
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun addPeopleToGroup(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val groupId =
      call.getString("groupId") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.GROUP_ID_REQUIRED))
    val contactIdsArray =
      call.getArray("contactIds")
        ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.CONTACT_IDS_REQUIRED))

    val contactIds = List(contactIdsArray.length()) { i -> contactIdsArray.getString(i) }

    try {
      implementation.addPeopleToGroup(groupId, contactIds)
      call.resolve()
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun removePeopleFromGroup(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val groupId =
      call.getString("groupId") ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.GROUP_ID_REQUIRED))
    val contactIdsArray =
      call.getArray("contactIds")
        ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.CONTACT_IDS_REQUIRED))

    val contactIds = List(contactIdsArray.length()) { i -> contactIdsArray.getString(i) }

    try {
      implementation.removePeopleFromGroup(groupId, contactIds)
      call.resolve()
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun createContact(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val contactJS =
      call.getObject("contact")
        ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.CONTACT_DATA_REQUIRED))
    if (!hasWritableCreateContactField(contactJS)) {
      return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.AT_LEAST_ONE_WRITABLE_FIELD_REQUIRED))
    }

    // Marshalling: Safe data extraction using Capacitor v8 typed getters
    val nameObj = contactJS.getJSObject("name")
    val givenName = nameObj?.getString("given")
    val familyName = nameObj?.getString("family")

    // Marshalling: Extract JSONArrays using the standard getJSONArray method
    val phones = PeopleUtils.extractStringList(contactJS.getJSONArray("phones"), "number")

    // Delegate extraction to updated Utils helper
    val emails = PeopleUtils.extractStringList(contactJS.getJSONArray("emails"), "address")

    try {
      val contact = implementation.createContact(givenName, familyName, phones, emails)
      val ret = JSObject()
      // Use Mapper to return JSObject to the Bridge
      ret.put("contact", contactToJS(contact))
      call.resolve(ret)
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun updateContact(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val contactId = call.getString("contactId")
    val contactData = call.getObject("contact")

    if (contactId.isNullOrEmpty() ||
      contactData == null
    ) {
      return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.REQUIRED_FIELDS_MISSING))
    }

    // Marshalling: Extract only what the native Impl needs
    val nameObj = contactData.getJSObject("name")

    // Delegate to Impl using primitive types
    try {
      val updatedContact =
        implementation.updateContact(
          contactId,
          nameObj?.getString("given"),
          nameObj?.getString("family"),
        )
      val ret = JSObject()
      // Map native model back to JSObject via Utils
      ret.put("contact", contactToJS(updatedContact))
      call.resolve(ret)
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun deleteContact(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val contactId =
      call.getString("contactId")
        ?: return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.CONTACT_ID_REQUIRED))

    try {
      implementation.deleteContact(contactId)
      call.resolve()
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  fun mergeContacts(call: PluginCall) {
    if (!checkContactsPermission(call)) return

    val sourceId = call.getString("sourceContactId")
    val destId = call.getString("destinationContactId")

    if (sourceId.isNullOrEmpty() ||
      destId.isNullOrEmpty()
    ) {
      return reject(call, PeopleError.InvalidInput(PeopleErrorMessages.IDS_REQUIRED))
    }
    if (sourceId == destId) {
      return reject(
        call,
        PeopleError.InvalidInput("sourceContactId and destinationContactId must be different"),
      )
    }
    try {
      val mergedContact = implementation.mergeContacts(sourceId, destId)
      val ret = JSObject()
      ret.put("contact", contactToJS(mergedContact))
      call.resolve(ret)
    } catch (e: PeopleError) {
      reject(call, e)
    }
  }

  @PluginMethod
  override fun requestPermissions(call: PluginCall) {
    if (getPermissionState("contacts") == PermissionState.GRANTED) {
      checkPermissions(call)
      return
    }

    // The callback must be a method annotated with @PermissionCallback
    requestPermissionForAlias("contacts", call, "contactsPermissionsCallback")
  }

  @PermissionCallback
  private fun contactsPermissionsCallback(call: PluginCall) {
    // After the permission prompt, return the updated permission states
    checkPermissions(call)
  }

  @PluginMethod
  fun getCapabilities(call: PluginCall) {
    val caps = implementation.getCapabilities(getPermissionState("contacts") == PermissionState.GRANTED)
    call.resolve(JSObject.fromJSONObject(org.json.JSONObject(caps)))
  }

  // -----------------------------------------------------------------------------
  // Version Information
  // -----------------------------------------------------------------------------

  /**
   * Returns the native plugin version synchronized from package.json.
   *
   * This information is used for diagnostics and ensuring parity between
   * the JavaScript and native layers.
   *
   * @param call The bridge call to resolve with version data.
   */
  @PluginMethod
  fun getPluginVersion(call: PluginCall) {
    val ret = JSObject()
    ret.put("version", BuildConfig.PLUGIN_VERSION)
    call.resolve(ret)
  }

  // -----------------------------------------------------------------------------
  // Bridge Marshalling (Native -> JS)
  // -----------------------------------------------------------------------------

  // Add these private helpers at the end of PeoplePlugin class
  private fun contactToJS(contact: io.capkit.people.models.ContactData): JSObject {
    val js = JSObject()
    js.put("id", contact.id)

    contact.displayName?.let {
      val nameObj = JSObject()
      nameObj.put("display", it)
      js.put("name", nameObj)
    }

    if (contact.phones.isNotEmpty()) {
      val phonesArr = JSArray()
      for (phone in contact.phones) {
        val p = JSObject()
        p.put("number", phone.value)
        p.put("label", phone.label)
        phonesArr.put(p)
      }
      js.put("phones", phonesArr)
    }

    if (contact.emails.isNotEmpty()) {
      val emailsArr = JSArray()
      for (email in contact.emails) {
        val e = JSObject()
        e.put("address", email.value)
        e.put("label", email.label)
        emailsArr.put(e)
      }
      js.put("emails", emailsArr)
    }

    contact.organization?.let { org ->
      val orgObj = JSObject()
      orgObj.put("company", org.company)
      orgObj.put("title", org.title)
      orgObj.put("department", org.department)
      js.put("organization", orgObj)
    }

    if (contact.addresses.isNotEmpty()) {
      val addrArr = JSArray()
      for (addr in contact.addresses) {
        val a = JSObject()
        a.put("label", addr.label)
        a.put("street", addr.street)
        a.put("city", addr.city)
        a.put("region", addr.region)
        a.put("postcode", addr.postcode)
        a.put("country", addr.country)
        addrArr.put(a)
      }
      js.put("addresses", addrArr)
    }
    return js
  }

  private fun groupToJS(group: io.capkit.people.models.GroupData): JSObject {
    val js = JSObject()
    js.put("id", group.id)
    js.put("name", group.name)
    js.put("source", group.source)
    js.put("readOnly", group.readOnly)
    return js
  }

  /**
   * JS marshalling helper for UnifiedContact payloads.
   * Responsibility: bridge layer only (ContactData → JSObject / JSArray).
   * Native contact mapping from platform types → ContactData is handled in the Impl/Utils layer.
   */
  private fun contactsToJSArray(contacts: List<io.capkit.people.models.ContactData>): JSArray {
    val arr = JSArray()
    for (contact in contacts) {
      arr.put(contactToJS(contact))
    }
    return arr
  }
}
