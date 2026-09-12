/// <reference types="@capacitor/cli" />

/**
 * Extension of the Capacitor CLI configuration to include specific settings for Authentication.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Authentication plugin.
     */
    Authentication?: AuthenticationConfig;
  }
}

/**
 * Static configuration options for the Authentication plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface AuthenticationConfig {
  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information is printed
   * to the native console (Logcat on Android, Xcode on iOS).
   *
   * This option affects native logging behavior only and
   * has no impact on the JavaScript API.
   *
   * @default false
   * @example true
   * @since 8.0.0
   */
  verboseLogging?: boolean;

  /**
   * Web OAuth client id (must end in `apps.googleusercontent.com`).
   *
   * Used only by the Web implementation to build the Google authorization URL.
   *
   * @example '1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com'
   * @since 8.0.0
   */
  webClientId?: string;

  /**
   * Web OAuth redirect URI registered with the provider.
   *
   * @example 'https://example.com/auth/google'
   * @since 8.0.0
   */
  webRedirectUri?: string;

  /**
   * Google provider configuration (Android sub-object).
   *
   * Consumed by the Android implementation via the `google` sub-object.
   * The iOS implementation reads the FLAT `googleScopes` and `googleAutoSelect`
   * keys instead (see [GoogleProviderConfig] parity note); both platforms
   * derive the same defaults when the keys are absent.
   *
   * `serverClientId` is REQUIRED for native token exchange: absent →
   * `INIT_FAILED`, blank → `INVALID_INPUT` (Android `GoogleConfigResolver`).
   *
   * @since 8.0.0
   */
  google?: GoogleProviderConfig;

  /**
   * Apple provider configuration (Android sub-object).
   *
   * Consumed by the Android implementation via the `apple` sub-object.
   * The iOS implementation reads the FLAT `appleClientId`, `appleRedirectURI`,
   * `appleScopes`, `appleNonce` and `appleState` keys instead (see
   * [AppleProviderConfig] parity note); both platforms derive the same
   * defaults when the keys are absent.
   *
   * `clientId` is REQUIRED for the native flow: absent → `INIT_FAILED`,
   * blank → `INVALID_INPUT` (Android `AppleConfigResolver`).
   *
   * @since 8.0.0
   */
  apple?: AppleProviderConfig;

  /**
   * Facebook provider configuration (Android sub-object).
   *
   * Consumed by the Android implementation via the `facebook` sub-object.
   * The iOS implementation reads the FLAT `facebookAppId`, `facebookClientToken`,
   * `facebookVersion` and `facebookScopes` keys instead (see
   * [FacebookProviderConfig] parity note).
   *
   * `facebookAppId` is REQUIRED: absent → `INIT_FAILED`, blank →
   * `INVALID_INPUT`.
   *
   * @since 8.0.0
   */
  facebook?: FacebookProviderConfig;

  /**
   * Google OAuth scopes (FLAT iOS key).
   *
   * Equivalent to `google.scopes`; the iOS implementation reads this flat key
   * and splits it on spaces. Both platforms derive `['openid', 'email', 'profile']`
   * when the key is absent.
   *
   * @default ['openid', 'email', 'profile']
   * @since 8.0.0
   */
  googleScopes?: string[];

  /**
   * Whether to attempt zero-UX auto-select for returning users (FLAT iOS key).
   *
   * Equivalent to `google.autoSelect`; the iOS implementation reads this flat key.
   *
   * @default false
   * @since 8.0.0
   */
  googleAutoSelect?: boolean;

  /**
   * Apple client identifier / Services ID (FLAT iOS key).
   *
   * Equivalent to `apple.clientId`; the iOS implementation reads this flat key.
   * Required for the iOS native flow: absent → `INIT_FAILED`, blank →
   * `INVALID_INPUT`.
   *
   * @example 'com.example.services'
   * @since 8.0.0
   */
  appleClientId?: string;

  /**
   * Apple redirect URI registered with the Services ID (FLAT iOS key).
   *
   * Equivalent to `apple.redirectURI`; the iOS implementation reads this flat key.
   *
   * @example 'https://example.com/auth/apple'
   * @since 8.0.0
   */
  appleRedirectURI?: string;

  /**
   * Apple OAuth scopes (FLAT iOS key).
   *
   * Equivalent to `apple.scopes`; the iOS implementation reads this flat key.
   *
   * @default ['name', 'email']
   * @since 8.0.0
   */
  appleScopes?: string[];

  /**
   * Apple OpenID Connect `nonce` binding the issued id_token to this sign-in (FLAT iOS key).
   *
   * Equivalent to `apple.nonce`; the iOS implementation reads this flat key.
   *
   * @since 8.0.0
   */
  appleNonce?: string;

  /**
   * Apple OAuth `state` value for CSRF protection (FLAT iOS key).
   *
   * Equivalent to `apple.state`; the iOS implementation reads this flat key.
   *
   * @since 8.0.0
   */
  appleState?: string;

  /**
   * Facebook App ID, from the Meta developer portal (FLAT iOS key).
   *
   * Equivalent to `facebook.facebookAppId`; the iOS implementation reads this
   * flat key. Required for the iOS native flow: absent → `INIT_FAILED`,
   * blank → `INVALID_INPUT`.
   *
   * @example '123456789012345'
   * @since 8.0.0
   */
  facebookAppId?: string;

  /**
   * Facebook client token, App Settings → Advanced (FLAT iOS key).
   *
   * Equivalent to `facebook.facebookClientToken`; the iOS implementation reads
   * this flat key. Native-only secret: accepted for config-schema parity on Web
   * but NEVER sent to the browser.
   *
   * @example 'YOUR_FACEBOOK_CLIENT_TOKEN'
   * @since 8.0.0
   */
  facebookClientToken?: string;

  /**
   * Facebook Graph API version (FLAT key).
   *
   * Equivalent to `facebook.facebookVersion`; used by the Web and Android
   * implementations. The native SDKs pin their own version and never apply it.
   *
   * @default 'v17.0'
   * @since 8.0.0
   */
  facebookVersion?: string;

  /**
   * Facebook OAuth scopes (FLAT iOS key).
   *
   * Equivalent to `facebook.scopes`; the iOS implementation reads this flat key.
   *
   * @default ['public_profile', 'email']
   * @since 8.0.0
   */
  facebookScopes?: string[];
}

/**
 * Google provider configuration.
 *
 * Grouped `google` sub-object under `plugins.Authentication`, consumed by the
 * Android implementation at init time. The iOS implementation does NOT read
 * this sub-object: it reads the FLAT `googleScopes` / `googleAutoSelect` keys
 * instead and derives the same defaults when they are absent (parity note:
 * the flat iOS keys are the documentation point of reference for iOS, this
 * sub-object for Android).
 *
 * `serverClientId` is REQUIRED for native token exchange: absent →
 * `INIT_FAILED`, blank → `INVALID_INPUT` (Android `GoogleConfigResolver`).
 *
 * @since 8.0.0
 */
export interface GoogleProviderConfig {
  /**
   * Google OAuth web client (server) id used for native token exchange.
   *
   * Required. Absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android
   * `GoogleConfigResolver`).
   *
   * @since 8.0.0
   */
  serverClientId: string;

  /**
   * Additional OAuth scopes requested for the Google provider.
   *
   * @default ['openid', 'email', 'profile']
   * @since 8.0.0
   */
  scopes?: string[];

  /**
   * Whether to attempt zero-UX auto-select for returning users (Google One Tap).
   *
   * @default false
   * @since 8.0.0
   */
  autoSelect?: boolean;

  /**
   * OpenID Connect `nonce` binding the issued id_token to this sign-in.
   *
   * @since 8.0.0
   */
  nonce?: string;
}

/**
 * Apple provider configuration.
 *
 * Grouped `apple` sub-object under `plugins.Authentication`, consumed by the
 * Android implementation at init time. The iOS implementation does NOT read
 * this sub-object: it reads the FLAT `appleClientId`, `appleRedirectURI`,
 * `appleScopes`, `appleNonce` and `appleState` keys instead and derives the
 * same defaults when they are absent (parity note: the flat iOS keys are the
 * documentation point of reference for iOS, this sub-object for Android).
 *
 * `clientId` is REQUIRED for the native flow: absent → `INIT_FAILED`,
 * blank → `INVALID_INPUT` (Android `AppleConfigResolver`).
 *
 * @since 8.0.0
 */
export interface AppleProviderConfig {
  /**
   * Apple client identifier (Services ID) from the Apple Developer portal.
   *
   * Required. Absent → `INIT_FAILED`, blank → `INVALID_INPUT` (Android
   * `AppleConfigResolver`).
   *
   * @since 8.0.0
   */
  clientId: string;

  /**
   * Apple redirect URI registered with the Services ID.
   *
   * @since 8.0.0
   */
  redirectURI?: string;

  /**
   * OAuth scopes requested for the Apple flow.
   *
   * @default ['name', 'email']
   * @since 8.0.0
   */
  scopes?: string[];

  /**
   * OpenID Connect `nonce` binding the issued id_token to this sign-in.
   *
   * When omitted, the platform generates a fresh per-flow nonce.
   *
   * @since 8.0.0
   */
  nonce?: string;

  /**
   * OAuth `state` value for CSRF protection.
   *
   * @since 8.0.0
   */
  state?: string;
}

/**
 * Facebook provider configuration.
 *
 * Grouped `facebook` sub-object under `plugins.Authentication`, consumed by the
 * Android implementation at init time. The iOS implementation does NOT read
 * this sub-object: it reads the FLAT `facebookAppId`, `facebookClientToken`,
 * `facebookVersion` and `facebookScopes` keys instead and derives the same
 * defaults when they are absent (parity note: the flat iOS keys are the
 * documentation point of reference for iOS, this sub-object for Android).
 *
 * `facebookAppId` is REQUIRED: absent → `INIT_FAILED`, blank → `INVALID_INPUT`.
 * `facebookClientToken` is optional but blank-when-present invalid.
 * `facebookVersion` is a Web/Android Graph API version artifact; the native
 * SDKs pin their own and never apply it.
 *
 * @since 8.0.0
 */
export interface FacebookProviderConfig {
  /**
   * Facebook App ID, from the Meta developer portal.
   *
   * Required. Absent → `INIT_FAILED`, blank → `INVALID_INPUT`.
   *
   * @since 8.0.0
   */
  facebookAppId: string;

  /**
   * Facebook client token (App Settings → Advanced), for native token exchange.
   *
   * Native-only secret: accepted for config-schema parity on Web but NEVER
   * sent to the browser.
   *
   * @since 8.0.0
   */
  facebookClientToken?: string;

  /**
   * Facebook Graph API version passed to the Web provider.
   *
   * @default 'v17.0'
   * @since 8.0.0
   */
  facebookVersion?: string;

  /**
   * OAuth read permissions granted to the app.
   *
   * @default ['public_profile', 'email']
   * @since 8.0.0
   */
  scopes?: string[];
}

/**
 * Standardized error codes used by the Authentication plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * @since 8.0.0
 */
export const AuthenticationErrorCode = {
  /** The device does not have the requested hardware or the feature is not available on this platform. */
  UNAVAILABLE: 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED: 'CANCELLED',
  /** The user dismissed or cancelled an interactive consent flow. Reported identically across Web, iOS and Android. */
  USER_CANCELLED: 'USER_CANCELLED',
  /** The user denied the permission or the feature is disabled by the OS. */
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  /** The plugin failed to initialize or perform an operation. */
  INIT_FAILED: 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT: 'INVALID_INPUT',
  /** The requested type is not valid or supported. */
  UNKNOWN_TYPE: 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND: 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT: 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT: 'TIMEOUT',
} as const;

/**
 * Union type of the standardized Authentication error codes.
 */
export type AuthenticationErrorCode = (typeof AuthenticationErrorCode)[keyof typeof AuthenticationErrorCode];

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /**
   * The native plugin version string.
   */
  version: string;
}

/**
 * Supported authentication provider key.
 *
 * The facade is provider-keyed; Google is the first concrete provider, Apple
 * the second, and Facebook the third.
 */
export type AuthProvider = 'google' | 'apple' | 'facebook';

/**
 * Provider-agnostic user profile delivered with a sign-in result.
 *
 * Present for providers that share profile data (Apple delivers it on first
 * sign-in only). Optional fields are omitted when absent. No field here is
 * ever a credential; treat `id`, `email` and `name` as profile data only.
 */
export interface SocialAuthResultUser {
  /**
   * Provider-specific user id, when shared.
   */
  id?: string;
  /**
   * User email address, when shared by the provider.
   */
  email?: string;
  /**
   * Display name. Apple delivers first/last name parts on first sign-in;
   * other providers may deliver a single display string.
   */
  name?: { firstName?: string; lastName?: string } | string;
  /**
   * Apple's real-user estimation, when shared. Literal spellings are
   * preserved exactly as Apple issues them (including the historical
   * `likleyRealUser` typo, which is intentional for API stability).
   */
  realUserStatus?: 'likleyRealUser' | 'unknown' | 'unsupported';
  /**
   * Provider profile image URL, when shared (facebook `/me` picture; google/apple
   * return it null/absent). Social-auth-facade homogeneous shape.
   */
  picture?: string;
}

/**
 * Normalized OAuth2/OIDC token set returned from a completed sign-in or refresh.
 *
 * Mirrors the shape used by all three platforms so the result is homogeneous
 * across Web, iOS and Android. Optional fields are omitted when absent.
 */
export interface OAuthTokenSet {
  /**
   * OAuth access token.
   *
   * Omitted on the zero-UX auto-select path with default scopes
   * (no consent UI — only the ID token is issued). Present on
   * interactive sign-in and refresh paths.
   */
  accessToken?: string;
  /**
   * OAuth refresh token, when issued.
   */
  refreshToken?: string;
  /**
   * OpenID Connect ID token, when issued.
   */
  idToken?: string;
  /**
   * Google server auth code, when issued.
   */
  serverAuthCode?: string;
  /**
   * Provider authorization code, when issued (Apple: all platforms).
   *
   * For Apple this is the OAuth `code` returned by the native/WebView/popup
   * flow. It MUST be exchanged server-side (POST to Apple with a
   * `client_secret`) for access/refresh tokens — it is never a refresh token
   * itself and the plugin never refreshes it client-side.
   */
  authorizationCode?: string;
  /**
   * Provider user profile, when shared (Apple: first sign-in only; facebook: `/me`).
   */
  user?: SocialAuthResultUser;
  /**
   * Facebook shared access-token slot: provider-scoped user id (string|null).
   *
   * Populated only by the facebook flow; google/apple return it null/absent
   * (shared-data-model slots ADDED by facebook).
   */
  userId?: string;
  /**
   * Facebook shared access-token slot: granted read permissions (string[]|null).
   */
  grantedPermissions?: string[];
  /**
   * Facebook shared access-token slot: declined read permissions (string[]|null).
   */
  declinedPermissions?: string[];
  /**
   * Facebook shared access-token slot: absolute epoch-seconds token expiry (number|null).
   *
   * Canonical slot name is `expiresAt`, not `expires`.
   */
  expiresAt?: number;
  /**
   * Access token lifetime in seconds, when known.
   */
  accessTokenExpiresIn?: number;
}

/**
 * Normalized result of a successful sign-in for a provider.
 */
export interface SocialAuthResult {
  /**
   * The provider that produced this result.
   */
  provider: AuthProvider;
  /**
   * The typed token data for that provider.
   */
  tokens: OAuthTokenSet;
}

/**
 * Options for a provider sign-in.
 */
export interface SignInOptions {
  /**
   * User-visible prompt behavior for the provider consent screen.
   * Google: 'none', 'consent', or 'select_account'.
   */
  prompt?: 'none' | 'consent' | 'select_account';
  /**
   * Additional OAuth scopes requested for this sign-in.
   * Apple: 'name' and/or 'email' (defaults to both when omitted).
   * Facebook: granted read permissions (defaults to `['public_profile','email']` when omitted).
   */
  scopes?: string[];
  /**
   * Whether to attempt zero-UX auto-select for returning users (Google One Tap).
   */
  autoSelect?: boolean;
  /**
   * Apple client id (Services ID) for the Web popup flow.
   *
   * Overrides the static `webClientId` Capacitor config value for this call.
   */
  clientId?: string;
  /**
   * Apple redirect URI registered with the Services ID, for the Web popup flow.
   *
   * Overrides the static `webRedirectUri` Capacitor config value for this call.
   */
  redirectURI?: string;
  /**
   * OAuth `state` value for the Apple Web popup flow; CSRF protection.
   *
   * When omitted, the Web implementation generates one.
   *
   * Facebook (Web): the popup `state` is also validated (CSRF); a mismatched
   * `state` aborts the flow with `INVALID_INPUT`.
   */
  state?: string;
  /**
   * OpenID Connect `nonce` for the Apple Web popup flow; binds the issued
   * id_token to this sign-in.
   *
   * When omitted, the Web implementation generates one.
   */
  nonce?: string;
}

/**
 * Options for creating a Google Restore (Zero Tap) credential.
 */
export interface RestoreCredentialOptions {
  /**
   * The provider for which the restore credential is created.
   */
  provider: AuthProvider;
  /**
   * The requestJson payload describing the WebAuthn-style credential operation.
   */
  requestJson: string;
  /**
   * Whether cloud backup is enabled for the credential.
   */
  isCloudBackupEnabled?: boolean;
}

/**
 * Structured error object returned by Authentication plugin operations.
 *
 * This object allows consumers to handle errors without relying
 * on exception-based control flow.
 */
export interface AuthenticationError {
  /**
   * Human-readable error description.
   */
  message: string;

  /**
   * Machine-readable error code.
   */
  code: AuthenticationErrorCode;
}

/**
 * Public JavaScript API for the Authentication Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface AuthenticationPlugin {
  /**
   * Returns the native plugin version.
   *
   * The returned version corresponds to the native implementation
   * bundled with the application.
   *
   * @returns A promise resolving to the plugin version.
   *
   * @example
   * ```ts
   * const { version } = await Authentication.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;

  /**
   * Initializes the provider, loading any provider-specific configuration.
   *
   * @param options Options carrying the provider key to initialize.
   *
   * @since 8.0.0
   */
  initialize(options: { provider: AuthProvider }): Promise<void>;

  /**
   * Signs the user in with the given provider and returns typed tokens.
   *
   * Sign-in is single-flight per provider: concurrent calls for the same
   * provider do not start a second native flow.
   *
   * @param options Sign-in options carrying the provider key plus any
   * per-call settings defined in [SignInOptions].
   * @returns A promise resolving to normalized token data.
   *
   * @since 8.0.0
   */
  signIn(options: SignInOptions & { provider: AuthProvider }): Promise<SocialAuthResult>;

  /**
   * Ends the session for the provider, removing persisted tokens.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  signOut(options: { provider: AuthProvider }): Promise<void>;

  /**
   * Returns the current access token for the provider, if any.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  getCurrentAccessToken(options: { provider: AuthProvider }): Promise<{ accessToken?: string }>;

  /**
   * Refreshes the access token using a stored refresh token. Available on Web.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  refreshToken(options: { provider: AuthProvider }): Promise<OAuthTokenSet>;

  /**
   * Alias for [signOut]: clears the session and persisted tokens.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  logout(options: { provider: AuthProvider }): Promise<void>;

  /**
   * Creates a Google Restore (Zero Tap) credential.
   *
   * @param options Restore credential options carrying requestJson.
   *
   * @since 8.0.0
   */
  createRestoreCredential(options: RestoreCredentialOptions): Promise<{ available: boolean }>;

  /**
   * Retrieves a previously created restore credential.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  getRestoreCredential(options: { provider: AuthProvider }): Promise<{ available: boolean; requestJson?: string }>;

  /**
   * Clears a previously created restore credential.
   *
   * @param options Options carrying the provider key.
   *
   * @since 8.0.0
   */
  clearRestoreCredential(options: { provider: AuthProvider }): Promise<{ available: boolean }>;
}
