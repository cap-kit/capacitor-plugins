import Foundation
import Contacts

/**
 * A utility struct providing helper methods for data mapping.
 * Architectural rules:
 * - Pure helper functions only.
 * - No Capacitor dependencies.
 */
struct PeopleUtils {

    /// Supported projection fields constant.
    static let supportedProjections: Set<String> = [
        "name", "organization", "birthday", "phones", "emails", "addresses", "urls", "image", "note"
    ]

    /// Normalizes native iOS contact labels into standardized strings.
    static func normalizeLabel(_ label: String?) -> String {
        guard let label = label else { return "other" }

        // Use the localized string from the framework to clean internal identifiers like _$!<Mobile>!$_
        let localizedLabel = CNLabeledValue<NSString>.localizedString(forLabel: label).lowercased()

        if localizedLabel.contains("mobile") || localizedLabel.contains("cell") { return "mobile" }
        if localizedLabel.contains("home") { return "home" }
        if localizedLabel.contains("work") { return "work" }
        if localizedLabel.contains("main") { return "main" }
        if localizedLabel.contains("fax") { return "fax" }
        return "other"
    }

    /// Validates that all requested projection fields are supported.
    /// - Parameter projection: The array of strings to validate.
    /// - Throws: PeopleError.unknownType if a field is invalid.
    static func validateProjection(_ projection: [String]) throws {
        for field in projection {
            if !supportedProjections.contains(field) {
                throw PeopleError.unknownType("Unsupported projection field: \(field)")
            }
        }
    }

    /// Maps a native CNContact to the nominal ContactData model.
    static func mapToContactData(_ contact: CNContact, projection: [String]) -> ContactData {
        let nameAvailable =
            contact.isKeyAvailable(CNContactGivenNameKey) &&
            contact.isKeyAvailable(CNContactFamilyNameKey)
        let organizationAvailable =
            contact.isKeyAvailable(CNContactOrganizationNameKey) &&
            contact.isKeyAvailable(CNContactJobTitleKey) &&
            contact.isKeyAvailable(CNContactDepartmentNameKey)
        let birthdayAvailable = contact.isKeyAvailable(CNContactBirthdayKey)
        let phonesAvailable = contact.isKeyAvailable(CNContactPhoneNumbersKey)
        let emailsAvailable = contact.isKeyAvailable(CNContactEmailAddressesKey)
        let addressesAvailable = contact.isKeyAvailable(CNContactPostalAddressesKey)
        let urlsAvailable = contact.isKeyAvailable(CNContactUrlAddressesKey)
        let noteAvailable = contact.isKeyAvailable(CNContactNoteKey)
        let imageAvailable = contact.isKeyAvailable(CNContactThumbnailImageDataKey)

        let displayName: String? = {
            guard nameAvailable else { return nil }
            let parts = [contact.givenName, contact.familyName].filter { !$0.isEmpty }
            return parts.isEmpty ? nil : parts.joined(separator: " ")
        }()

        let organization: ContactOrganization? = {
            guard projection.contains("organization"), organizationAvailable else { return nil }
            return ContactOrganization(
                company: contact.organizationName.isEmpty ? nil : contact.organizationName,
                title: contact.jobTitle.isEmpty ? nil : contact.jobTitle,
                department: contact.departmentName.isEmpty ? nil : contact.departmentName
            )
        }()

        return ContactData(
            id: contact.identifier,
            displayName: displayName,
            firstName: projection.contains("name") && nameAvailable ? contact.givenName : nil,
            lastName: projection.contains("name") && nameAvailable ? contact.familyName : nil,
            organization: organization,
            birthday: projection.contains("birthday") && birthdayAvailable ? contact.birthday : nil,
            phones: projection.contains("phones") && phonesAvailable ? contact.phoneNumbers.map {
                ContactPhone(
                    label: normalizeLabel($0.label),
                    number: $0.value.stringValue
                )
            } : nil,
            emails: projection.contains("emails") && emailsAvailable ? contact.emailAddresses.map {
                ContactEmail(
                    label: normalizeLabel($0.label),
                    address: $0.value as String
                )
            } : nil,
            addresses: projection.contains("addresses") && addressesAvailable ? contact.postalAddresses.map {
                ContactAddress(
                    label: normalizeLabel($0.label),
                    formatted: CNPostalAddressFormatter.string(from: $0.value, style: .mailingAddress),
                    street: $0.value.street.isEmpty ? nil : $0.value.street,
                    city: $0.value.city.isEmpty ? nil : $0.value.city,
                    region: $0.value.state.isEmpty ? nil : $0.value.state,
                    postcode: $0.value.postalCode.isEmpty ? nil : $0.value.postalCode,
                    country: $0.value.country.isEmpty ? nil : $0.value.country
                )
            } : nil,
            urls: projection.contains("urls") && urlsAvailable ? contact.urlAddresses.map { $0.value as String } : nil,
            note: projection.contains("note") && noteAvailable ? contact.note : nil,
            image: (projection.contains("image") && imageAvailable && contact.thumbnailImageData != nil) ? contact.thumbnailImageData?.base64EncodedString() : nil
        )
    }

    /// Maps a native CNGroup to the nominal GroupData model.
    static func mapToGroupData(_ group: CNGroup) -> GroupData {
        return GroupData(
            id: group.identifier,
            name: group.name,
            source: "local",
            readOnly: false
        )
    }
}
