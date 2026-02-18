import Foundation
import Capacitor
import Contacts
import ContactsUI

/**
 Capacitor Bridge for People.
 Implements CNContactPickerDelegate for Zero-Permission access.
 */
@objc(PeoplePlugin)
public class PeoplePlugin: CAPPlugin, CAPBridgedPlugin, CNContactPickerDelegate {

    // MARK: - Properties

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = PeopleImpl()

    /// Internal storage for the plugin configuration read from capacitor.config.ts.
    private var config: PeopleConfig?

    /// The unique identifier for the plugin used by the Capacitor bridge.
    public let identifier = "PeoplePlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "People"

    // State for the active picker call
    private var savedPickCall: CAPPluginCall?

    // State for observing changes (not yet implemented)
    private var peopleObserverActive = false

    // Stores the NotificationCenter observer token so it can be removed reliably
    private var peopleObserverToken: NSObjectProtocol?

    private func beginPickCall(_ call: CAPPluginCall) -> Bool {
        objc_sync_enter(self)
        defer { objc_sync_exit(self) }
        if savedPickCall != nil { return false }
        savedPickCall = call
        return true
    }

    private func clearPickCall() {
        objc_sync_enter(self)
        savedPickCall = nil
        objc_sync_exit(self)
    }

    /**
     * A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getCapabilities", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pickContact", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getContact", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getContacts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "searchPeople", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "listGroups", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createGroup", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteGroup", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addPeopleToGroup", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removePeopleFromGroup", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createContact", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateContact", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteContact", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "mergeContacts", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "addListener", returnType: CAPPluginReturnNone),
        CAPPluginMethod(name: "removeListener", returnType: CAPPluginReturnNone),
        CAPPluginMethod(name: "removeAllListeners", returnType: CAPPluginReturnNone),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Lifecycle

    /**
     * Plugin lifecycle entry point.
     *
     * Called once when the plugin is loaded. This method initializes the configuration
     * and prepares the native implementation.
     */
    override public func load() {
        // Initialize PeopleConfig with the correct type
        let cfg = PeopleConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        // Log if verbose logging is enabled
        PeopleLogger.debug("Plugin loaded.")
    }

    deinit {
        // Ensure NotificationCenter observers are always removed
        stopPeopleObserver()
    }

    // MARK: - Error Mapping

    /**
     * Rejects the call using standardized error codes from the native PeopleError enum.
     */
    private func reject(
        _ call: CAPPluginCall,
        error: PeopleError
    ) {
        // Use the centralized errorCode and message defined in PeopleError.swift
        call.reject(error.message, error.errorCode)
    }

    private func handleError(_ call: CAPPluginCall, _ error: Error) {
        if let peopleError = error as? PeopleError {
            call.reject(peopleError.message, peopleError.errorCode)
        } else {
            reject(call, error: .initFailed(error.localizedDescription))
        }
    }

    private func ensureContactsPermission(_ call: CAPPluginCall) -> Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        if #available(iOS 18.0, *) {
            if status == .authorized || status == .limited {
                return true
            }
        } else if status == .authorized {
            return true
        }
        reject(call, error: .permissionDenied("Permission denied"))
        return false
    }

    private func hasWritableCreateContactField(_ contactData: [String: Any]) -> Bool {
        let writableKeys: Set<String> = [
            "name", "organization", "birthday",
            "phones", "emails", "addresses", "urls",
            "note", "image"
        ]

        for key in writableKeys {
            guard let value = contactData[key] else { continue }

            if let dict = value as? [String: Any], !dict.isEmpty { return true }
            if let array = value as? [Any], !array.isEmpty { return true }
            if let stringValue = value as? String, !stringValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        }
        return false
    }

    // MARK: - Capabilities

    /// Retrieves the plugin capabilities based on authorization status.
    @objc func getCapabilities(_ call: CAPPluginCall) {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        let caps = implementation.getCapabilities(authStatus: status)
        call.resolve(caps)
    }

    // MARK: - Zero-Permission Picker

    /// Opens the contact picker for user to select a contact.
    @objc func pickContact(_ call: CAPPluginCall) {
        if !beginPickCall(call) {
            return reject(call, error: .conflict("Another pickContact call is already in progress"))
        }

        // 1. Extract and validate projection before opening the UI
        let projection = call.getArray("projection", String.self) ?? ["name", "phones", "emails"]

        do {
            try PeopleUtils.validateProjection(projection)
        } catch {
            clearPickCall()
            handleError(call, error)
            return
        }

        // Dispatch UI to Main Thread
        DispatchQueue.main.async { [weak self] in
            guard let self = self, let viewController = self.bridge?.viewController else {
                self?.clearPickCall()
                self?.reject(call, error: .initFailed("Unable to access ViewController"))
                return
            }

            let picker = CNContactPickerViewController()
            picker.delegate = self

            viewController.present(picker, animated: true, completion: nil)
        }
    }

    // MARK: - CNContactPickerDelegate

    /// User selected a contact
    public func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
        guard let call = self.savedPickCall else { return }
        defer { clearPickCall() }
        let projectionJS = call.getArray("projection", String.self) ?? ["name", "phones", "emails"]

        // Use static mapper from Utils instead of implementation member
        let contactData = PeopleUtils.mapToContactData(contact, projection: projectionJS)
        call.resolve(["contact": contactToJS(contactData)])
    }

    /// User cancelled
    public func contactPickerDidCancel(_ picker: CNContactPickerViewController) {
        guard let call = self.savedPickCall else { return }
        defer { clearPickCall() }
        self.reject(call, error: .cancelled("User cancelled selection"))
    }

    // MARK: - Event Listeners

    /// Adds a listener for contact changes.
    @objc override public func addListener(_ call: CAPPluginCall) {
        guard let eventName = call.getString("eventName"), !eventName.isEmpty else {
            return reject(call, error: .invalidInput(PeopleErrorMessages.eventNameRequired))
        }

        guard eventName == "peopleChange" else {
            return reject(call, error: .invalidInput(PeopleErrorMessages.unsupportedEventName(eventName)))
        }

        if !ensureContactsPermission(call) { return }

        call.keepAlive = true
        super.addListener(call)
        if !peopleObserverActive { startPeopleObserver() }
    }

    /// Remove a listener for contact changes.
    @objc override public func removeListener(_ call: CAPPluginCall) {
        super.removeListener(call)

        // Release the listener call to prevent retained calls and potential leaks.
        bridge?.releaseCall(withID: call.callbackId)

        if !hasListeners("peopleChange") {
            stopPeopleObserver()
        }
    }

    /// Removes all listeners for contact changes.
    @objc override public func removeAllListeners(_ call: CAPPluginCall) {
        super.removeAllListeners(call)
        stopPeopleObserver()
    }

    private func startPeopleObserver() {
        guard !peopleObserverActive else { return }
        peopleObserverToken = NotificationCenter.default.addObserver(
            forName: .CNContactStoreDidChange,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard let self = self else { return }

            // Ensure a consistent payload for "peopleChange" listeners.
            // Always include "ids" as an array (may be empty) and a "type" string with default "update".
            if self.hasListeners("peopleChange") {
                // iOS does not provide specific IDs in CNContactStoreDidChange.
                let changedIds: [String] = []
                let eventType: String = "update"

                self.notifyListeners("peopleChange", data: [
                    "ids": changedIds,
                    "type": eventType
                ])
            } else {
                self.stopPeopleObserver()
            }
        }
        peopleObserverActive = true
    }

    private func stopPeopleObserver() {
        if let token = peopleObserverToken {
            NotificationCenter.default.removeObserver(token)
            peopleObserverToken = nil
        }
        peopleObserverActive = false
    }

    // MARK: - Permissions

    /**
     * Checks permission status.
     */
    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        var state: String
        if #available(iOS 18.0, *) {
            switch status {
            case .authorized, .limited: state = "granted"
            case .denied, .restricted: state = "denied"
            case .notDetermined: state = "prompt"
            @unknown default: state = "prompt"
            }
        } else {
            switch status {
            case .authorized: state = "granted"
            case .denied, .restricted: state = "denied"
            case .notDetermined: state = "prompt"
            @unknown default: state = "prompt"
            }
        }
        call.resolve(["contacts": state])
    }

    /**
     * Requests permission.
     */
    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        // Fail fast if the host app is missing the required Info.plist usage description key.
        let usageDescription =
            Bundle.main.object(forInfoDictionaryKey: "NSContactsUsageDescription") as? String
        if usageDescription == nil || usageDescription?.isEmpty == true {
            return reject(
                call,
                error: .initFailed(PeopleErrorMessages.missingContactsUsageDescription)
            )
        }

        let store = CNContactStore()
        store.requestAccess(for: .contacts) { [weak self] granted, error in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if let error = error {
                    self.reject(call, error: .initFailed(error.localizedDescription))
                    return
                }
                call.resolve(["contacts": granted ? "granted" : "denied"])
            }
        }
    }

    // MARK: - Contact Fetching

    /// Retrieves a list of contacts based on provided options.
    @objc func getContacts(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        let projection = call.getArray("projection", String.self) ?? ["name", "phones", "emails"]
        let limit = call.getInt("limit") ?? 10
        let offset = call.getInt("offset") ?? 0
        let includeTotal = call.getBool("includeTotal") ?? false

        do {
            // Validation Block
            try PeopleUtils.validateProjection(projection)

            let result = try implementation.fetchContacts(projection: projection, limit: limit, offset: offset, includeTotal: includeTotal)
            call.resolve([
                "contacts": result.contacts.map { contactToJS($0) },
                "totalCount": result.totalCount
            ])
        } catch { handleError(call, error) }
    }

    /// Fetch a specific contact by ID
    @objc func getContact(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        let id = call.getString("id", "")
        if id.isEmpty { return reject(call, error: .invalidInput("id is required")) }
        let projection = call.getArray("projection", String.self) ?? ["name", "phones", "emails"]

        do {
            // Validation Block
            try PeopleUtils.validateProjection(projection)

            if let contact = try implementation.fetchContactById(id: id, projection: projection) {
                call.resolve(["contact": contactToJS(contact)])
            } else {
                reject(call, error: .notFound(PeopleErrorMessages.contactNotFound))
            }
        } catch { handleError(call, error) }
    }

    /// Searches for contacts matching a query string.
    @objc func searchPeople(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let query = call.getString("query"), !query.isEmpty else {
            return reject(call, error: .invalidInput("Missing query"))
        }
        let projection = call.getArray("projection", String.self) ?? ["name", "phones", "emails"]
        let limit = call.getInt("limit") ?? 50

        do {
            // Validation Block
            try PeopleUtils.validateProjection(projection)

            let result = try implementation.searchContacts(query: query, projection: projection, limit: limit)
            call.resolve([
                "contacts": result.contacts.map { contactToJS($0) },
                "totalCount": result.totalCount
            ])
        } catch { handleError(call, error) }
    }

    // MARK: - Group Management

    /// Deletes a contact group.
    @objc func listGroups(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        do {
            let result = try implementation.listGroups()
            call.resolve(["groups": result.groups.map { groupToJS($0) }])
        } catch { handleError(call, error) }
    }

    /// Creates a new contact group.
    @objc func createGroup(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let name = call.getString("name"), !name.isEmpty else {
            return reject(call, error: .invalidInput("Group name is required"))
        }
        do {
            let groupData = try implementation.createGroup(name: name)
            call.resolve(["group": groupToJS(groupData)])
        } catch { handleError(call, error) }
    }

    /// Deletes a group by ID.
    @objc func deleteGroup(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let groupId = call.getString("groupId") else {
            return reject(call, error: .invalidInput("Group ID is required"))
        }
        do {
            try implementation.deleteGroup(groupId: groupId)
            call.resolve()
        } catch { handleError(call, error) }
    }

    /// Adds people to a group.
    @objc func addPeopleToGroup(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let groupId = call.getString("groupId"),
              let contactIds = call.getArray("contactIds", String.self) else {
            return reject(call, error: .invalidInput("Group ID and Contact IDs are required"))
        }
        do {
            try implementation.addPeopleToGroup(groupId: groupId, contactIds: contactIds)
            call.resolve()
        } catch { handleError(call, error) }
    }

    /// Removes contacts from a group.
    @objc func removePeopleFromGroup(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let groupId = call.getString("groupId"),
              let contactIds = call.getArray("contactIds", String.self) else {
            return reject(call, error: .invalidInput("Group ID and Contact IDs are required"))
        }
        do {
            try implementation.removePeopleFromGroup(groupId: groupId, contactIds: contactIds)
            call.resolve()
        } catch { handleError(call, error) }
    }

    // MARK: - CRUD

    /// Creates a new contact.
    @objc func createContact(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let contactData = call.getObject("contact") else {
            return reject(call, error: .invalidInput("Contact data is required"))
        }

        guard hasWritableCreateContactField(contactData) else {
            return reject(call, error: .invalidInput(PeopleErrorMessages.atLeastOneWritableFieldRequired))
        }
        do {
            let newContact = try implementation.createContact(contactData: contactData)
            call.resolve(["contact": contactToJS(newContact)])
        } catch { handleError(call, error) }
    }

    /// Updates an existing contact.
    @objc func updateContact(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let contactId = call.getString("contactId"),
              let contactData = call.getObject("contact") else {
            return reject(call, error: .invalidInput("Contact ID and data are required"))
        }
        do {
            let updatedContact = try implementation.updateContact(contactId: contactId, contactData: contactData)
            call.resolve(["contact": contactToJS(updatedContact)])
        } catch { handleError(call, error) }
    }

    /// Merges two contacts.
    @objc func mergeContacts(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let sourceId = call.getString("sourceContactId"),
              let destId = call.getString("destinationContactId") else {
            return reject(call, error: .invalidInput("Both source and destination IDs are required"))
        }
        if sourceId == destId {
            return reject(call, error: .invalidInput("sourceContactId and destinationContactId must be different"))
        }
        do {
            let mergedContact = try implementation.mergeContacts(sourceContactId: sourceId, destinationContactId: destId)
            call.resolve(["contact": contactToJS(mergedContact)])
        } catch { handleError(call, error) }
    }

    /// Deletes a contact.
    @objc func deleteContact(_ call: CAPPluginCall) {
        if !ensureContactsPermission(call) { return }

        guard let contactId = call.getString("contactId") else {
            return reject(call, error: .invalidInput("Contact ID is required"))
        }
        do {
            try implementation.deleteContact(contactId: contactId)
            call.resolve()
        } catch { handleError(call, error) }
    }

    // MARK: - Version

    /**
     * Retrieves the current native plugin version.
     *
     * This version is synchronized from the project's package.json during the build process.
     *
     * - Parameter call: CAPPluginCall used to return the version string.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": PluginVersion.number])
    }

    // MARK: - Private Mapping Helpers

    /// JS marshalling helper for UnifiedContact payloads.
    /// Responsibility: bridge layer only (ContactData → JSObject).
    /// Mapping from CNContact → ContactData is handled in PeopleUtils.
    private func contactToJS(_ contact: ContactData) -> JSObject {
        var res = JSObject()
        res["id"] = contact.id

        var nameObj = JSObject()
        nameObj["display"] = contact.displayName ?? ""
        if let first = contact.firstName { nameObj["given"] = first }
        if let last = contact.lastName { nameObj["family"] = last }
        res["name"] = nameObj

        if let org = contact.organization {
            var orgObj = JSObject()
            orgObj["company"] = org.company
            orgObj["title"] = org.title
            orgObj["department"] = org.department
            res["organization"] = orgObj
        }

        if let birthday = contact.birthday {
            var bday = JSObject()
            bday["day"] = birthday.day
            bday["month"] = birthday.month
            if let year = birthday.year, year != NSDateComponentUndefined { bday["year"] = year }
            res["birthday"] = bday
        }

        if let phones = contact.phones {
            let mappedPhones: [JSObject] = phones.map { phoneData in
                var phone = JSObject()
                phone["label"] = phoneData.label
                phone["number"] = phoneData.number
                return phone
            }
            res["phones"] = mappedPhones
        }

        if let emails = contact.emails {
            let mappedEmails: [JSObject] = emails.map { emailData in
                var email = JSObject()
                email["label"] = emailData.label
                email["address"] = emailData.address
                return email
            }
            res["emails"] = mappedEmails
        }

        if let addresses = contact.addresses {
            let mappedAddresses: [JSObject] = addresses.map { addressData in
                var address = JSObject()
                address["label"] = addressData.label
                address["formatted"] = addressData.formatted
                address["street"] = addressData.street
                address["city"] = addressData.city
                address["region"] = addressData.region
                address["postcode"] = addressData.postcode
                address["country"] = addressData.country
                return address
            }
            res["addresses"] = mappedAddresses
        }
        if let urls = contact.urls { res["urls"] = urls }
        if let note = contact.note { res["note"] = note }
        if let image = contact.image { res["image"] = image }

        return res
    }

    /// JS marshalling helper for contact groups.
    /// Responsibility: bridge layer only (GroupData → JSObject).
    private func groupToJS(_ group: GroupData) -> JSObject {
        var res = JSObject()
        res["id"] = group.id
        res["name"] = group.name
        res["source"] = group.source
        res["readOnly"] = group.readOnly
        return res
    }
}
