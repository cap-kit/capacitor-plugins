import Foundation
import Capacitor

/**
 * Plugin configuration container for the Authentication plugin.
 *
 * This struct is responsible for reading and exposing static configuration
 * values defined under the `Authentication` key in `capacitor.config.ts`.
 *
 * Architectural rules:
 * - Read once during plugin initialization (`load()`).
 * - Treated as immutable runtime input.
 * - Consumed only by native code (never by JavaScript).
 */
public struct AuthenticationConfig {

    // MARK: - Configuration Keys

    /**
     * Internal structure to maintain consistent configuration keys.
     */
    private struct Keys {
        static let verboseLogging = "verboseLogging"
        static let googleScopes = "googleScopes"
        static let googleAutoSelect = "googleAutoSelect"
        static let appleClientId = "appleClientId"
        static let appleRedirectURI = "appleRedirectURI"
        static let appleScopes = "appleScopes"
        static let appleNonce = "appleNonce"
        static let appleState = "appleState"
        static let facebookAppId = "facebookAppId"
        static let facebookClientToken = "facebookClientToken"
        static let facebookVersion = "facebookVersion"
        static let facebookScopes = "facebookScopes"
    }

    // MARK: - Public Properties

    /**
     * Enables verbose native logging via AuthenticationLogger.
     *
     * When enabled, additional debug information is printed to the Xcode console.
     * Default: false
     */
    public let verboseLogging: Bool

    /**
     * Additional OAuth scopes requested for the Google provider.
     *
     * Beyond the OpenID Connect defaults (`openid email profile`), matching the
     * Web and Android providers. Read once and immutable.
     */
    public let googleScopes: [String]

    /**
     * Whether to attempt zero-UX auto-select for returning users (Google One Tap).
     *
     * Mirror of the Android `autoSelect` flag; iOS attempts a silent
     * `restorePreviousSignIn` when enabled and falls back to the interactive flow
     * when no prior session exists.
     */
    public let googleAutoSelect: Bool

    /**
     * Apple provider configuration (native iOS flow).
     *
     * Mirrors the Android `AppleConfig`: `clientId` required, `redirectURI` required
     * (a Web/Android artifact — validated for cross-platform configuration parity),
     * `scopes` defaulting to `name email`, and an optional user-provided `nonce`
     * (iOS generates a fresh per-flow nonce when absent). Read once and immutable;
     * raw values never reach JavaScript.
     */
    public let apple: AppleConfig

    /**
     * Facebook provider configuration (native iOS flow).
     *
     * Mirrors the Android `FacebookConfig` (config parity gate): `facebookAppId`
     * required (also settable via Info.plist `FacebookAppID`), `facebookClientToken`
     * optional-but-blank-invalid, `scopes` defaulting to `public_profile email`, and a
     * Web/Android `facebookVersion` artifact that iOS never applies to the SDK (the
     * native `GraphRequest` uses the SDK default API version). Read once and immutable;
     * raw values never reach JavaScript.
     */
    public let facebook: FacebookConfig

    /**
     * Typed `apple` configuration sub-object.
     *
     * `clientId`/`redirectURI` are optional at the storage level; presence and
     * blank-check validation happen at init time (`AuthenticationImpl.requireAppleConfig`
     * via `AppleConfigValidator`), mirroring the Android `AppleConfigResolver` +
     * `AppleConfigValidator`.
     */
    public struct AppleConfig {
        /** Apple client identifier, from the Apple Developer portal. */
        public let clientId: String?

        /** Apple redirect URI (Web/Android artifact; validated blank-if-present on iOS). */
        public let redirectURI: String?

        /** OAuth scopes; defaults to `name email` (Web/Android parity). */
        public let scopes: [String]

        /** Caller-provided nonce for the authorize URL (Web/Android); iOS generates
         *  a fresh nonce when absent. */
        public let nonce: String?

        /** Caller-provided state for the authorize URL (Web/Android parity artifact);
         *  the native iOS flow never echoes state — CSRF protection comes from the
         *  nonce (`validateIDTokenNonce`). Android `AppleConfig.state` mirror. */
        public let state: String?
    }

    /**
     * Typed `facebook` configuration sub-object.
     *
     * `facebookAppId`/`facebookClientToken` are optional at the storage level;
     * presence and blank-check validation happen at init time
     * (`AuthenticationImpl.requireFacebookConfig` via `FacebookConfigValidator`),
     * mirroring the Android `FacebookConfigResolver` + `FacebookConfigValidator`
     * (config parity gate). `facebookVersion` is a Web/Android
     * artifact — the native iOS `GraphRequest` uses the SDK default API version,
     * so it is read for cross-platform parity but never applied to the SDK.
     */
    public struct FacebookConfig {
        /** Facebook App ID, from the Meta developer portal (also settable via Info.plist `FacebookAppID`). */
        public let facebookAppId: String?

        /** Facebook client token (App Settings → Advanced); optional, blank-when-present invalid. */
        public let facebookClientToken: String?

        /** Graph API version (Web/Android artifact; iOS never applies it — SDK default). */
        public let facebookVersion: String?

        /** OAuth read permissions; defaults to `public_profile email` (Web/Android parity). */
        public let scopes: [String]
    }

    // MARK: - Private Defaults

    private static let defaultVerboseLogging: Bool = false

    /** Default OpenID Connect scopes, matching the Web/Android provider defaults. */
    private static let defaultGoogleScopes: [String] = ["openid", "email", "profile"]

    private static let defaultGoogleAutoSelect: Bool = false

    /** Default Apple scopes, matching the Web/Android provider defaults. */
    private static let defaultAppleScopes: [String] = ["name", "email"]

    /** Default Facebook read permissions, matching the Web/Android provider defaults. */
    private static let defaultFacebookScopes: [String] = ["public_profile", "email"]

    /** Default Graph API version (Web/Android parity; iOS never applies it to the SDK). */
    private static let defaultFacebookVersion = "v17.0"

    // MARK: - Initialization

    /**
     * Initializes the configuration by reading values from the Capacitor bridge.
     *
     * - Parameter plugin: The CAPPlugin instance used to access typed configuration via `getConfig()`.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        self.verboseLogging = config.getBoolean(
            Keys.verboseLogging,
            Self.defaultVerboseLogging
        )

        let rawScopes = config.getString(Keys.googleScopes)
        self.googleScopes = rawScopes?.isEmpty == false
            ? (rawScopes?.components(separatedBy: " ").filter { !$0.isEmpty }) ?? []
            : Self.defaultGoogleScopes

        self.googleAutoSelect = config.getBoolean(
            Keys.googleAutoSelect,
            Self.defaultGoogleAutoSelect
        )

        let rawAppleScopes = config.getString(Keys.appleScopes)
        let appleScopes: [String]
        if let rawAppleScopes, !rawAppleScopes.isEmpty {
            appleScopes = rawAppleScopes.components(separatedBy: " ").filter { !$0.isEmpty }
        } else {
            appleScopes = Self.defaultAppleScopes
        }
        self.apple = AppleConfig(
            clientId: config.getString(Keys.appleClientId),
            redirectURI: config.getString(Keys.appleRedirectURI),
            scopes: appleScopes,
            nonce: config.getString(Keys.appleNonce),
            state: config.getString(Keys.appleState)
        )

        let rawFacebookScopes = config.getString(Keys.facebookScopes)
        let facebookScopes: [String]
        if let rawFacebookScopes, !rawFacebookScopes.isEmpty {
            facebookScopes = rawFacebookScopes.components(separatedBy: " ").filter { !$0.isEmpty }
        } else {
            facebookScopes = Self.defaultFacebookScopes
        }
        self.facebook = FacebookConfig(
            facebookAppId: config.getString(Keys.facebookAppId),
            facebookClientToken: config.getString(Keys.facebookClientToken),
            facebookVersion: config.getString(Keys.facebookVersion) ?? Self.defaultFacebookVersion,
            scopes: facebookScopes
        )
    }
}
