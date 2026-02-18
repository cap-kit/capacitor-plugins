import Foundation
import Contacts

/// Nominal model for organization data to ensure cross-platform parity.
public struct ContactOrganization {
    public let company: String?
    public let title: String?
    public let department: String?
}

/// Nominal model for a phone field.
public struct ContactPhone {
    public let label: String?
    public let number: String
}

/// Nominal model for an email field.
public struct ContactEmail {
    public let label: String?
    public let address: String
}

/// Nominal model for a postal address field.
public struct ContactAddress {
    public let label: String?
    public let formatted: String?
    public let street: String?
    public let city: String?
    public let region: String?
    public let postcode: String?
    public let country: String?
}

/// Nominal model representing contact data, independent of Capacitor.
/// This ensures type safety within the Implementation layer.
public struct ContactData {
    public let id: String
    public let displayName: String?
    public let firstName: String?
    public let lastName: String?
    public let organization: ContactOrganization? // Updated from String? to struct
    public let birthday: DateComponents?
    public let phones: [ContactPhone]?
    public let emails: [ContactEmail]?
    public let addresses: [ContactAddress]?
    public let urls: [String]?
    public let note: String?
    public let image: String? // Base64 string
}

/// Nominal model for Group data to replace raw dictionaries.
public struct GroupData {
    public let id: String
    public let name: String
    public let source: String
    public let readOnly: Bool
}

/// Result model for paginated contact fetches.
public struct GetContactsResultData {
    public let contacts: [ContactData]
    public let totalCount: Int
}

/// Result model for group listings.
public struct ListGroupsResultData {
    public let groups: [GroupData]
}
