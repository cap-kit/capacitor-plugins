import Foundation

/**
 * @file FacebookTokenMapper.swift
 * Pure mapping from Facebook SDK token/profile primitives onto the shared
 * `TokenSet`: the four optional slots
 * (`userId`, `grantedPermissions`, `declinedPermissions`, `expiresAt` absolute
 * epoch seconds) fill from SDK values; `idToken`/`authorizationCode`/
 * `serverAuthCode`/`refreshToken` stay null for facebook (non-applicable);
 * `/me` maps to `SocialAuthResultUser` with a display-name string and the `picture`
 * URL. Permission lists are trimmed/deduped — facebook already delivers granted-only
 * permissions, so no refresh-time filtering is needed here. Pure and
 * side-effect free; the SDK binding layer wires `AccessToken`/`/me` onto these
 * primitives. Mirror of the Android `FacebookTokenMapper` (expiry floor parity:
 * Android `getExpires().time / 1000`, iOS floors the seconds `Date`).
 */
enum FacebookTokenMapper {

    /**
     * Converts an SDK expiry value in (fractional) epoch seconds to the canonical
     * absolute epoch-seconds slot value (floor, Android `/1000` division parity).
     */
    static func expiresAtEpochSeconds(_ expiresAt: Double?) -> Double? {
        expiresAt.map { floor($0) }
    }

    /**
     * Normalizes an SDK permission collection: trims, drops blanks, dedupes
     * preserving first occurrence. `nil` stays `nil` (slot omitted); an empty
     * collection stays empty.
     */
    static func sanitizePermissions(_ permissions: [String]?) -> [String]? {
        guard let permissions else { return nil }
        var seen = Set<String>()
        var result: [String] = []
        for permission in permissions {
            let trimmed = permission.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty, !seen.contains(trimmed) {
                seen.insert(trimmed)
                result.append(trimmed)
            }
        }
        return result
    }

    /**
     * Maps an SDK access token plus its optional slot primitives onto a `TokenSet`.
     * Non-applicable facebook fields (`idToken`, `refreshToken`, `authorizationCode`,
     * `serverAuthCode`) are never set — absent by design.
     */
    static func mapToken(
        accessToken: String,
        userId: String? = nil,
        grantedPermissions: [String]? = nil,
        declinedPermissions: [String]? = nil,
        expiresAt: Double? = nil
    ) -> TokenSet {
        TokenSet(
            accessToken: accessToken,
            userId: userId,
            grantedPermissions: sanitizePermissions(grantedPermissions),
            declinedPermissions: sanitizePermissions(declinedPermissions),
            expiresAt: expiresAtEpochSeconds(expiresAt)
        )
    }

    /**
     * Maps the granted-only `/me` fields onto the shared user profile. `realUserStatus`
     * is NEVER set for facebook; a blank display name stays `nil` (Android `takeIf`
     * parity); absent fields stay `nil`.
     */
    static func mapProfile(
        id: String? = nil,
        name: String? = nil,
        email: String? = nil,
        pictureUrl: String? = nil
    ) -> SocialAuthResultUser {
        let trimmedName = name?.trimmingCharacters(in: .whitespacesAndNewlines)
        return SocialAuthResultUser(
            id: id,
            email: email,
            name: trimmedName?.isEmpty == false ? .displayName(trimmedName ?? "") : nil,
            realUserStatus: nil,
            picture: pictureUrl
        )
    }

    /**
     * Combines token slot mapping and the `/me` profile into the set persisted by
     * the sign-in flow (vault persist step).
     */
    static func mapSignInResult(
        accessToken: String,
        userId: String? = nil,
        grantedPermissions: [String]? = nil,
        declinedPermissions: [String]? = nil,
        expiresAt: Double? = nil,
        profile: SocialAuthResultUser? = nil
    ) -> TokenSet {
        var tokenSet = mapToken(
            accessToken: accessToken,
            userId: userId,
            grantedPermissions: grantedPermissions,
            declinedPermissions: declinedPermissions,
            expiresAt: expiresAt
        )
        tokenSet.user = profile
        return tokenSet
    }
}
