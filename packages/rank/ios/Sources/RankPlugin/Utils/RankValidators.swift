import Foundation

/**
 Collection of input validation helpers for the Rank plugin.

 These functions are pure and side-effect free.
 They MUST NOT depend on Capacitor APIs.
 */
enum RankValidators {

    /**
     Validates that the provided Apple App ID
     is a non-empty numeric string.
     */
    static func validateAppId(_ value: String?) -> String? {
        guard let id = value?
                .trimmingCharacters(in: .whitespacesAndNewlines),
              !id.isEmpty,
              CharacterSet.decimalDigits
                .isSuperset(of: CharacterSet(charactersIn: id))
        else {
            return nil
        }
        return id
    }
}
