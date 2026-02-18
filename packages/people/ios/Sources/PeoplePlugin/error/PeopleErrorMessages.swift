import Foundation

/// Canonical error messages shared across platforms.
/// Keep these strings identical on iOS and Android.
enum PeopleErrorMessages {
    static let idRequired = "id is required"
    static let queryRequired = "query is required"
    static let eventNameRequired = "eventName is required"

    static func unsupportedEventName(_ value: String) -> String {
        "Unsupported eventName: \(value)"
    }

    static let missingContactsUsageDescription = "Missing NSContactsUsageDescription in Info.plist"
    static let noContactUriReturned = "No contact URI returned"
    static let userCancelledSelection = "User cancelled selection"
    static let failedToParseContactFromUri = "Failed to parse contact from URI"

    static func unsupportedProjectionField(_ value: String) -> String {
        "Unsupported projection field: \(value)"
    }

    static let contactNotFound = "Contact not found"
    static let atLeastOneWritableFieldRequired = "At least one writable contact field is required"
}
