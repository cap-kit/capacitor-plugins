import Foundation

/**
 * Typed token set persisted by the secure Keychain vault (iOS).
 *
 * Mirrors the TypeScript `OAuthTokenSet` shape (access, refresh, id, server auth code)
 * and the Android `TokenBundle`. All fields are optional because flows legitimately
 * produce partial sets; a set without an access token is *no credential*. Never
 * serialized to JS — vault material is native-only.
 */
struct TokenSet: Codable, Equatable {
    /// OAuth access token.
    var accessToken: String?

    /// OAuth refresh token, when issued.
    var refreshToken: String?

    /// OpenID Connect ID token, when issued.
    var idToken: String?

    /// Google server auth code, when issued.
    var serverAuthCode: String?

    /// Provider authorization code, when issued (Apple: all platforms).
    ///
    /// Exchanged server-side for access/refresh tokens; never refreshed client-side.
    var authorizationCode: String?

    /// Provider user profile, when shared (Apple: first sign-in only; facebook: `/me`).
    var user: SocialAuthResultUser?

    // Facebook shared access-token slots: populated only by
    // the facebook flow; google/apple leave them null (absent). Synthesized Codable
    // stays access-token-drop-safe — all fields are optional.
    var userId: String?

    var grantedPermissions: [String]?

    var declinedPermissions: [String]?

    /// Absolute epoch-seconds token expiry, when known (facebook).
    var expiresAt: Double?

    /// True when the set carries a usable (non-blank) access token.
    var hasCredential: Bool {
        accessToken?.isEmpty == false
    }
}

/**
 * Provider-agnostic user profile delivered with a sign-in result (iOS).
 *
 * Mirrors the TypeScript `SocialAuthResultUser` shape (`{ id?, email?, name?, realUserStatus? }`)
 * and the Android `SocialAuthResultUser`: every field is optional and omitted when absent.
 * Present only for providers that share profile data (Apple delivers it on first sign-in only).
 * No field here is ever a credential — treat `id`, `email` and `name` as profile data only.
 */
struct SocialAuthResultUser: Codable, Equatable {
    /// Provider-specific user id, when shared.
    var id: String?

    /// User email address, when shared by the provider.
    var email: String?

    /// Display name: first/last name parts (Apple's native split) or a display string.
    var name: SocialAuthUserName?

    /// Apple's real-user estimation, when shared. Literal spellings are preserved exactly as
    /// Apple issues them (including the historical `likleyRealUser` typo, intentional for API
    /// stability). Populated on iOS only (apple-provider spec).
    var realUserStatus: String?

    /// Provider profile image URL, when shared (facebook `/me` picture; apple/google leave
    /// it null/absent). Social-auth-facade homogeneous shape.
    var picture: String?
}

/**
 * Name shape of a `SocialAuthResultUser`: either first/last name parts (Apple's native split)
 * or a single display string.
 *
 * Mirrors the TypeScript `{ firstName?, lastName? } | string` union on
 * `OAuthTokenSet.user.name`; serialized into the vault with an explicit `nameType`
 * discriminator so both variants round-trip without lossy coercion (Android parity).
 */
enum SocialAuthUserName: Codable, Equatable {
    /// First/last name parts delivered by Apple on first sign-in.
    case nameParts(firstName: String?, lastName: String?)

    /// Single display string delivered by providers that do not split the name.
    case displayName(String)

    private enum CodingKeys: String, CodingKey {
        case nameType
        case firstName
        case lastName
        case value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let nameType = try container.decode(String.self, forKey: .nameType)
        switch nameType {
        case "nameParts":
            self = .nameParts(
                firstName: try container.decodeIfPresent(String.self, forKey: .firstName),
                lastName: try container.decodeIfPresent(String.self, forKey: .lastName)
            )
        case "displayName":
            self = .displayName(try container.decode(String.self, forKey: .value))
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .nameType,
                in: container,
                debugDescription: "Unknown SocialAuthUserName nameType: \(nameType)"
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        switch self {
        case let .nameParts(firstName, lastName):
            try container.encode("nameParts", forKey: .nameType)
            try container.encodeIfPresent(firstName, forKey: .firstName)
            try container.encodeIfPresent(lastName, forKey: .lastName)
        case let .displayName(value):
            try container.encode("displayName", forKey: .nameType)
            try container.encode(value, forKey: .value)
        }
    }
}
