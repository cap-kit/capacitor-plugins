import Foundation
import Contacts

/**
 * Native iOS implementation for the People plugin.
 *
 * Architectural rules:
 * - MUST NOT access CAPPluginCall.
 * - MUST NOT depend on Capacitor bridge APIs directly.
 * - MUST throw PeopleError for specific failures.
 */
@objc public final class PeopleImpl: NSObject {

    // MARK: - Properties

    /// Cached plugin configuration containing logging and behavioral flags.
    private var config: PeopleConfig?

    /// Shared contact store instance for performance optimization.
    private let store = CNContactStore()

    // MARK: - Initialization

    /**
     * Initializes the implementation instance.
     */
    override init() {
        super.init()
    }

    // MARK: - Configuration

    /**
     * Applies static plugin configuration.
     *
     * This method MUST be called exactly once from the Plugin bridge layer during `load()`.
     * It synchronizes the native logger state with the provided configuration.
     *
     * - Parameter config: The immutable configuration container.
     */
    public func applyConfig(_ config: PeopleConfig) {
        precondition(
            self.config == nil,
            "PeopleImpl.applyConfig(_:) must be called exactly once"
        )
        self.config = config
        PeopleLogger.verbose = config.verboseLogging

        PeopleLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Capabilities

    /**
     * Determines the capabilities based on the current authorization status.
     */
    public func getCapabilities(authStatus: CNAuthorizationStatus) -> [String: Bool] {
        let isAuthorized: Bool
        if #available(iOS 18.0, *) {
            isAuthorized = (authStatus == .authorized || authStatus == .limited)
        } else {
            isAuthorized = (authStatus == .authorized)
        }

        return [
            "canRead": isAuthorized,
            "canWrite": isAuthorized,
            "canObserve": isAuthorized,
            "canManageGroups": isAuthorized,
            "canPickContact": true
        ]
    }

    // MARK: - Contact Fetching

    /**
     * Fetches contacts with pagination and projection using nominal models.
     */
    public func fetchContacts(
        projection: [String],
        limit: Int,
        offset: Int,
        includeTotal: Bool = true
    ) throws -> GetContactsResultData {

        let safeLimit = max(0, limit)
        let safeOffset = max(0, offset)
        let keys = keysForProjection(projection)
        let request = CNContactFetchRequest(keysToFetch: keys)

        var results: [ContactData] = []
        var currentIndex = 0
        let upperBound = safeOffset + safeLimit

        try store.enumerateContacts(with: request) { contact, stop in
            if currentIndex >= safeOffset && currentIndex < upperBound {
                results.append(PeopleUtils.mapToContactData(contact, projection: projection))
            }

            // When includeTotal is true we must continue enumerating to compute totalCount.
            currentIndex += 1

            if !includeTotal && currentIndex >= upperBound {
                stop.pointee = true
            }
        }

        return GetContactsResultData(
            contacts: results,
            totalCount: includeTotal ? currentIndex : results.count
        )
    }

    /**
     * Fetches a single contact by identifier using nominal models.
     */
    public func fetchContactById(
        id: String,
        projection: [String]
    ) throws -> ContactData? {

        let keys = keysForProjection(projection)
        let contact = try store.unifiedContact(
            withIdentifier: id,
            keysToFetch: keys
        )

        return PeopleUtils.mapToContactData(contact, projection: projection)
    }

    /**
     * Searches for contacts matching a query string using nominal models.
     */
    public func searchContacts(
        query: String,
        projection: [String],
        limit: Int
    ) throws -> GetContactsResultData {

        let trimmedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedQuery.isEmpty else { return GetContactsResultData(contacts: [], totalCount: 0) }

        let safeLimit = max(0, limit)
        let keys = keysForProjection(projection)
        let predicate = CNContact.predicateForContacts(matchingName: trimmedQuery)
        let request = CNContactFetchRequest(keysToFetch: keys)
        request.predicate = predicate

        var results: [ContactData] = []
        var totalCount = 0

        try store.enumerateContacts(with: request) { contact, _ in
            totalCount += 1

            if results.count < safeLimit {
                results.append(PeopleUtils.mapToContactData(contact, projection: projection))
            }
        }

        return GetContactsResultData(contacts: results, totalCount: totalCount)
    }

    /**
     * Determines the CNKeyDescriptors needed for the given projection.
     */
    private func keysForProjection(_ projection: [String]) -> [CNKeyDescriptor] {
        let normalizedProjection = Set(projection)
        var keyNames: Set<String> = [
            CNContactIdentifierKey,
            CNContactGivenNameKey,
            CNContactFamilyNameKey
        ]

        let projectionMap: [String: [String]] = [
            "name": [CNContactGivenNameKey, CNContactFamilyNameKey],
            "phones": [CNContactPhoneNumbersKey],
            "emails": [CNContactEmailAddressesKey],
            "organization": [CNContactOrganizationNameKey, CNContactJobTitleKey, CNContactDepartmentNameKey],
            "birthday": [CNContactBirthdayKey],
            "image": [CNContactThumbnailImageDataKey],
            "addresses": [CNContactPostalAddressesKey],
            "urls": [CNContactUrlAddressesKey],
            "note": [CNContactNoteKey]
        ]

        for field in normalizedProjection {
            if let mappedKeys = projectionMap[field] {
                keyNames.formUnion(mappedKeys)
            }
        }

        return keyNames.map { $0 as CNKeyDescriptor }
    }

    // MARK: - Group Management

    /**
     * Fetches all contact groups from the contact store using nominal models.
     */
    public func listGroups() throws -> ListGroupsResultData {
        let groups = try store.groups(matching: nil)
        let result = groups.map { PeopleUtils.mapToGroupData($0) }
        return ListGroupsResultData(groups: result)
    }

    /**
     * Creates a new group with the given name.
     */
    public func createGroup(name: String) throws -> GroupData {
        let saveRequest = CNSaveRequest()
        let newGroup = CNMutableGroup()
        newGroup.name = name

        saveRequest.add(newGroup, toContainerWithIdentifier: nil)
        try store.execute(saveRequest)

        let groups = try store.groups(matching: CNGroup.predicateForGroups(withIdentifiers: [newGroup.identifier]))
        if let createdGroup = groups.first {
            return PeopleUtils.mapToGroupData(createdGroup)
        } else {
            throw PeopleError.initFailed("Failed to retrieve created group")
        }
    }

    /**
     * Deletes a group by its identifier.
     */
    public func deleteGroup(groupId: String) throws {
        let saveRequest = CNSaveRequest()
        let groups = try store.groups(matching: CNGroup.predicateForGroups(withIdentifiers: [groupId]))

        guard let groupToDelete = groups.first else {
            throw PeopleError.notFound("Group not found")
        }

        saveRequest.delete(groupToDelete.mutableCopy() as! CNMutableGroup)
        try store.execute(saveRequest)
    }

    /**
     * Adds contacts to a group.
     */
    public func addPeopleToGroup(groupId: String, contactIds: [String]) throws {
        let saveRequest = CNSaveRequest()
        let groups = try store.groups(matching: CNGroup.predicateForGroups(withIdentifiers: [groupId]))

        guard let group = groups.first else {
            throw PeopleError.unavailable("Group not found")
        }

        let contacts = try store.unifiedContacts(matching: CNContact.predicateForContacts(withIdentifiers: contactIds), keysToFetch: [])
        for contact in contacts {
            saveRequest.addMember(contact, to: group)
        }

        try store.execute(saveRequest)
    }

    /**
     * Removes contacts from a group.
     */
    public func removePeopleFromGroup(groupId: String, contactIds: [String]) throws {
        let saveRequest = CNSaveRequest()
        let groups = try store.groups(matching: CNGroup.predicateForGroups(withIdentifiers: [groupId]))

        guard let group = groups.first else {
            throw PeopleError.unavailable("Group not found")
        }

        let contacts = try store.unifiedContacts(matching: CNContact.predicateForContacts(withIdentifiers: contactIds), keysToFetch: [])
        for contact in contacts {
            saveRequest.removeMember(contact, from: group)
        }

        try store.execute(saveRequest)
    }

    // MARK: - CRUD

    /**
     * Creates a new contact and returns a nominal model.
     */
    public func createContact(contactData: [String: Any]) throws -> ContactData {
        let saveRequest = CNSaveRequest()
        let newContact = CNMutableContact()

        // Name mapping
        if let nameDict = contactData["name"] as? [String: String] {
            newContact.givenName = nameDict["given"] ?? ""
            newContact.familyName = nameDict["family"] ?? ""
        }

        // Phones mapping
        if let phonesArray = contactData["phones"] as? [[String: String]] {
            newContact.phoneNumbers = phonesArray.map { phoneDict in
                let number = CNPhoneNumber(stringValue: phoneDict["number"] ?? "")
                let label = phoneDict["label"] ?? CNLabelPhoneNumberMobile
                return CNLabeledValue(label: label, value: number)
            }
        }

        // Emails mapping
        if let emailsArray = contactData["emails"] as? [[String: String]] {
            newContact.emailAddresses = emailsArray.map { emailDict in
                let address = (emailDict["address"] ?? "") as NSString
                let label = emailDict["label"] ?? CNLabelWork
                return CNLabeledValue(label: label, value: address)
            }
        }

        newContact.note = "[cap-owned]"
        saveRequest.add(newContact, toContainerWithIdentifier: nil)
        try store.execute(saveRequest)

        return PeopleUtils.mapToContactData(newContact, projection: ["name", "phones", "emails"])
    }

    /**
     * Deletes a contact by its identifier.
     */
    public func deleteContact(contactId: String) throws {
        let saveRequest = CNSaveRequest()
        let keysToFetch = [CNContactIdentifierKey as CNKeyDescriptor, CNContactNoteKey as CNKeyDescriptor]
        let contact = try store.unifiedContact(withIdentifier: contactId, keysToFetch: keysToFetch)

        guard isAppOwned(contact: contact) else {
            throw PeopleError.permissionDenied("Cannot modify a contact that is not owned by the app.")
        }

        saveRequest.delete(contact.mutableCopy() as! CNMutableContact)
        try store.execute(saveRequest)
    }

    /**
     * Updates an existing contact and returns the updated nominal model.
     */
    public func updateContact(contactId: String, contactData: [String: Any]) throws -> ContactData {
        let saveRequest = CNSaveRequest()
        let keysToFetch = [
            CNContactIdentifierKey as CNKeyDescriptor,
            CNContactNoteKey as CNKeyDescriptor,
            CNContactGivenNameKey as CNKeyDescriptor,
            CNContactFamilyNameKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor,
            CNContactEmailAddressesKey as CNKeyDescriptor,
            CNContactPostalAddressesKey as CNKeyDescriptor,
            CNContactUrlAddressesKey as CNKeyDescriptor
        ]
        let contact = try store.unifiedContact(withIdentifier: contactId, keysToFetch: keysToFetch)

        guard isAppOwned(contact: contact) else {
            throw PeopleError.permissionDenied("Cannot update a contact that is not owned by the app.")
        }

        let mutableContact = contact.mutableCopy() as! CNMutableContact

        // Patch semantics for name:
        // - absent field: keep current value
        // - present subfield: update that subfield only
        if let nameDict = contactData["name"] as? [String: Any] {
            if let given = nameDict["given"] as? String {
                mutableContact.givenName = given
            }
            if let family = nameDict["family"] as? String {
                mutableContact.familyName = family
            }
        }

        // Patch semantics for arrays:
        // - absent key: keep current array
        // - present key with []: clear array
        // - present key with values: replace with provided values
        if contactData.keys.contains("phones"),
           let phonesArray = contactData["phones"] as? [[String: String]] {
            mutableContact.phoneNumbers = phonesArray.map { phoneDict in
                let number = CNPhoneNumber(stringValue: phoneDict["number"] ?? "")
                let label = phoneDict["label"] ?? CNLabelPhoneNumberMobile
                return CNLabeledValue(label: label, value: number)
            }
        }

        if contactData.keys.contains("emails"),
           let emailsArray = contactData["emails"] as? [[String: String]] {
            mutableContact.emailAddresses = emailsArray.map { emailDict in
                let address = (emailDict["address"] ?? "") as NSString
                let label = emailDict["label"] ?? CNLabelWork
                return CNLabeledValue(label: label, value: address)
            }
        }

        if contactData.keys.contains("addresses"),
           let addressesArray = contactData["addresses"] as? [[String: String]] {
            mutableContact.postalAddresses = addressesArray.map { addressDict in
                let postal = CNMutablePostalAddress()
                postal.street = addressDict["street"] ?? ""
                postal.city = addressDict["city"] ?? ""
                postal.state = addressDict["region"] ?? ""
                postal.postalCode = addressDict["postcode"] ?? ""
                postal.country = addressDict["country"] ?? ""
                let label = addressDict["label"] ?? CNLabelHome
                return CNLabeledValue(label: label, value: postal)
            }
        }

        if contactData.keys.contains("urls"),
           let urlsArray = contactData["urls"] as? [String] {
            mutableContact.urlAddresses = urlsArray.map { url in
                CNLabeledValue(label: CNLabelURLAddressHomePage, value: url as NSString)
            }
        }

        saveRequest.update(mutableContact)
        try store.execute(saveRequest)

        return PeopleUtils.mapToContactData(mutableContact, projection: ["name", "phones", "emails"])
    }

    /**
     * Merges two contacts and returns the final nominal model with deduplicated data.
     */
    public func mergeContacts(sourceContactId: String, destinationContactId: String) throws -> ContactData {
        let saveRequest = CNSaveRequest()
        let keysToFetch = [
            CNContactIdentifierKey as CNKeyDescriptor,
            CNContactNoteKey as CNKeyDescriptor,
            CNContactPhoneNumbersKey as CNKeyDescriptor,
            CNContactEmailAddressesKey as CNKeyDescriptor
        ]

        let sourceContact = try store.unifiedContact(withIdentifier: sourceContactId, keysToFetch: keysToFetch)
        let destinationContact = try store.unifiedContact(withIdentifier: destinationContactId, keysToFetch: keysToFetch)

        guard isAppOwned(contact: sourceContact), isAppOwned(contact: destinationContact) else {
            throw PeopleError.permissionDenied("Cannot merge contacts that are not owned by the app.")
        }

        let mutableDestinationContact = destinationContact.mutableCopy() as! CNMutableContact

        // Deduplicate Phone Numbers by string value
        let existingPhones = destinationContact.phoneNumbers.map { $0.value.stringValue }
        let newPhones = sourceContact.phoneNumbers.filter { !existingPhones.contains($0.value.stringValue) }
        mutableDestinationContact.phoneNumbers = destinationContact.phoneNumbers + newPhones

        // Deduplicate Email Addresses
        let existingEmails = destinationContact.emailAddresses.map { $0.value as String }
        let newEmails = sourceContact.emailAddresses.filter { !existingEmails.contains($0.value as String) }
        mutableDestinationContact.emailAddresses = destinationContact.emailAddresses + newEmails

        saveRequest.update(mutableDestinationContact)
        saveRequest.delete(sourceContact.mutableCopy() as! CNMutableContact)

        try store.execute(saveRequest)

        return PeopleUtils.mapToContactData(mutableDestinationContact, projection: ["name", "phones", "emails"])
    }

    private func isAppOwned(contact: CNContact) -> Bool {
        return contact.note.contains("[cap-owned]")
    }
}
