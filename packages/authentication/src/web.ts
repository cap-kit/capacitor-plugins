import { Capacitor, WebPlugin } from '@capacitor/core';

import {
  AuthProvider,
  AuthenticationPlugin,
  OAuthTokenSet,
  PluginVersionResult,
  RestoreCredentialOptions,
  SignInOptions,
  SocialAuthResult,
} from './definitions';
import { PLUGIN_VERSION } from './version';

const GOOGLE_AUTH_ENDPOINT = 'https://accounts.google.com/o/oauth2/v2/auth';
const GOOGLE_TOKEN_ENDPOINT = 'https://oauth2.googleapis.com/token';
const GOOGLE_CLIENT_SUFFIX = 'apps.googleusercontent.com';
const DEFAULT_SCOPES = ['openid', 'email', 'profile'];
const DEFAULT_APPLE_SCOPES = ['name', 'email'];
const APPLE_SCOPE_SEPARATOR = ' ';
const PKCE_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';

/**
 * Facebook JS SDK popup constants. The SDK is lazy-loaded from Meta's CDN at
 * first facebook sign-in and exposed as the global `FB`.
 */
const FACEBOOK_SDK_SRC = 'https://connect.facebook.net/en_US/sdk.js';
const FACEBOOK_SDK_ID = 'facebook-jssdk';
const FACEBOOK_DEFAULT_VERSION = 'v17.0';
const DEFAULT_FACEBOOK_SCOPES = ['public_profile', 'email'];
const FACEBOOK_PROFILE_FIELDS = 'id,name,email,picture.width(720).height(720)';

/**
 * Minimal typed view of Apple's `appleid.auth.js` SDK surface used by the
 * Web Apple provider. The SDK is loaded by the host page via Apple's CDN and
 * exposes a global `AppleID`; the plugin consumes it lazily at sign-in time.
 */
interface AppleIDAuthApi {
  initialize(config: AppleIDInitializeConfig): void;
  signIn(): Promise<AppleIDSignInResponse>;
}

interface AppleIDInitializeConfig {
  clientId: string;
  scope: string;
  redirectURI: string;
  state?: string;
  nonce?: string;
  usePopup: boolean;
}

interface AppleIDSignInResponse {
  authorization: {
    state?: string;
    code: string;
    id_token?: string;
    user?: AppleIDUser;
  };
}

interface AppleIDUser {
  email?: string;
  name?: { firstName?: string; lastName?: string };
  realUserStatus?: 'likleyRealUser' | 'unknown' | 'unsupported';
}

declare global {
  interface Window {
    AppleID?: { auth: AppleIDAuthApi };

    /**
     * Global `FB` object exposed by the Facebook JavaScript SDK
     * (`connect.facebook.net`), consumed lazily by the Web facebook provider.
     */
    FB?: FacebookSDK;
  }
}

/**
 * Minimal typed view of the Facebook JavaScript SDK surface used by the Web
 * facebook provider: `FB.init`, the `FB.login` popup, `FB.getLoginStatus`
 * identity binding, `FB.api` (Graph `/me`), and `FB.logout`. Only the subset
 * the provider relies on is declared; unknown SDK members are intentionally
 * typed away so every call is audit-traced.
 */
interface FacebookSDK {
  init(config: FacebookInitConfig): void;
  login(
    callback: (response: FacebookLoginResponse) => void,
    options?: { scope?: string; return_scopes?: boolean },
  ): void;
  getLoginStatus(callback: (response: FacebookLoginResponse) => void): void;
  api(path: string, callback: (response: FacebookApiResponse) => void): void;
  logout(callback?: () => void): void;
}

interface FacebookInitConfig {
  appId: string;
  version?: string;
  cookie?: boolean;
  xfbml?: boolean;
  status?: boolean;
}

interface FacebookAuthResponse {
  accessToken?: string;
  userID?: string;
  expiresIn?: number;
  grantedScopes?: string;
  declinedScopes?: string;
}

interface FacebookLoginResponse {
  status?: string;
  authResponse?: FacebookAuthResponse;
  error?: { message?: string };
}

interface FacebookApiResponse {
  id?: string;
  name?: string;
  email?: string;
  picture?: { data?: { url?: string } };
  error?: { code?: number; message?: string };
}

/**
 * Shape of the Google token endpoint response.
 */
interface TokenResponse {
  access_token?: string;
  refresh_token?: string;
  id_token?: string;
  expires_in?: number;
  error?: string;
  error_description?: string;
}

/**
 * Friction between starting a full-page OAuth redirect and the callback page
 * that returns to process the resulting authorization code.
 */
interface PendingAuthFlow {
  state: string;
  verifier: string;
}

/**
 * Web implementation of the Authentication plugin.
 *
 * Implements the Google authorization-code + PKCE redirect flow, token refresh,
 * and logout for browser environments. The Restore (Zero Tap) credential surface
 * reports `available=false` on Web rather than `unimplemented`.
 */
export class AuthenticationWeb extends WebPlugin implements AuthenticationPlugin {
  private pendingFlow: PendingAuthFlow | null = null;
  private tokens: OAuthTokenSet | null = null;

  constructor() {
    super();
  }

  // ---------------------------------------------------------------------------
  // Plugin Info
  // ---------------------------------------------------------------------------

  /**
   * Returns the JavaScript package version.
   */
  async getPluginVersion(): Promise<PluginVersionResult> {
    return { version: PLUGIN_VERSION };
  }

  // ---------------------------------------------------------------------------
  // Facade: initialize / signIn
  // ---------------------------------------------------------------------------

  /**
   * Initializes the provider. Web client id validation happens lazily at
   * sign-in time, so there is nothing to eagerly preload here.
   *
   * Facebook is the exception: `initialize({ provider: 'facebook' })`
   * validates the app id up front — absent → `INIT_FAILED`, blank →
   * `INVALID_INPUT`.
   *
   * @param options Options carrying the provider key.
   */
  async initialize(options: { provider: AuthProvider }): Promise<void> {
    const { provider } = options;
    if (provider !== 'facebook') {
      // Google/Apple: validated when signIn/refresh builds the request.
      return;
    }
    const appId = resolveFacebookAppId();
    if (appId === undefined) {
      throw facebookConfigError();
    }
    if (appId.trim().length === 0) {
      throw invalidFacebookConfig();
    }
  }

  /**
   * Signs the user in with the given provider.
   *
   * Apple: runs the sanctioned `appleid.auth.js` popup flow. Google: runs the
   * authorization-code + PKCE redirect flow as described below.
   *
   * - If the current URL already carries an authorization `code` (we are the
   *   OAuth callback), validates `state`, exchanges the code for tokens, and
   *   resolves.
   * - Otherwise builds the Google authorization URL (state + PKCE challenge),
   *   stores the pending flow, and redirects the browser.
   *
   * Rejects with a typed error before any redirect if the web client id does
   * not carry the required `apps.googleusercontent.com` suffix.
   *
   * @param options Options carrying the provider key plus any per-call
   * settings defined in [SignInOptions].
   */
  async signIn(options: SignInOptions & { provider: AuthProvider }): Promise<SocialAuthResult> {
    const { provider } = options;
    if (provider === 'apple') {
      return this.appleSignIn(options);
    }
    if (provider === 'facebook') {
      return this.facebookSignIn(options);
    }
    if (provider !== 'google') {
      throw invalidProvider(provider);
    }

    const clientId = this.webClientId();
    const redirectUri = this.webRedirectUri();

    const authCode = this.readAuthCodeFromUrl();
    if (authCode) {
      await this.processAuthCode(authCode, clientId, redirectUri);
      return { provider, tokens: this.tokens as OAuthTokenSet };
    }

    const { url, pending } = await this.buildAuthRequest(clientId, redirectUri, options);
    this.pendingFlow = pending;
    window.location.assign(url);
    // After a real redirect this context is unloaded; this rejects defensively
    // for harnesses that short-circuit navigation.
    throw redirectInitiated();
  }

  // ---------------------------------------------------------------------------
  // Facade: signOut / logout / getCurrentAccessToken / refreshToken
  // ---------------------------------------------------------------------------

  /**
   * Ends the session and clears persisted tokens.
   *
   * Facebook: additionally calls `FB.logout()` (best-effort — if the SDK is
   * not loaded or the Graph logout fails, this never rejects; the local
   * session is still cleared).
   *
   * @param options Options carrying the provider key.
   */
  async signOut(options: { provider: AuthProvider }): Promise<void> {
    const { provider } = options;
    if (provider === 'facebook') {
      const FB = window.FB;
      if (FB && typeof FB.logout === 'function') {
        FB.logout();
      }
    }
    this.tokens = null;
    this.pendingFlow = null;
  }

  /**
   * Alias for [signOut]: clears the session and persisted tokens for the
   * given provider (facebook also runs `FB.logout()`).
   *
   * @param options Options carrying the provider key.
   */
  async logout(options: { provider: AuthProvider }): Promise<void> {
    await this.signOut(options);
  }

  /**
   * Returns the stored access token, if any.
   *
   * @param options Options carrying the provider key.
   */
  async getCurrentAccessToken(_options: { provider: AuthProvider }): Promise<{ accessToken?: string }> {
    return { accessToken: this.tokens?.accessToken };
  }

  /**
   * Refreshes the access token using the stored refresh token. Available on Web
   * (never `unimplemented`). Rejects with a typed error if no refresh token is
   * stored.
   *
   * Apple is rejected outright across platforms: Apple issues no refresh token
   * (`authorizationCode` is exchanged server-side instead, never refreshed
   * client-side).
   *
   * @param options Options carrying the provider key.
   */
  async refreshToken(options: { provider: AuthProvider }): Promise<OAuthTokenSet> {
    const { provider } = options;
    if (provider === 'apple') {
      throw appleNoRefreshToken();
    }
    if (provider === 'facebook') {
      throw facebookNoRefreshToken();
    }
    if (provider !== 'google') {
      throw invalidProvider(provider);
    }
    const refreshToken = this.tokens?.refreshToken;
    if (!refreshToken) {
      throw noRefreshToken();
    }

    const body = new URLSearchParams();
    body.set('grant_type', 'refresh_token');
    body.set('client_id', this.webClientId());
    body.set('refresh_token', refreshToken);

    const json = await postTokenRequest(body);

    if (json.error) {
      throw tokenEndpointError(json.error, json.error_description);
    }
    if (!json.access_token) {
      throw tokenEndpointError('invalid_grant', 'No access token in response');
    }

    this.tokens = {
      accessToken: json.access_token,
      refreshToken: this.tokens?.refreshToken,
      idToken: json.id_token ?? this.tokens?.idToken,
      accessTokenExpiresIn: json.expires_in,
    };
    return this.tokens;
  }

  // ---------------------------------------------------------------------------
  // Restore (Zero Tap) credential — graceful unavailability on Web
  // ---------------------------------------------------------------------------

  async createRestoreCredential(_options: RestoreCredentialOptions): Promise<{ available: boolean }> {
    return { available: false };
  }

  /**
   * Retrieves a previously created restore credential. Web reports
   * `available=false` (graceful unavailability, never `unimplemented`).
   *
   * @param options Options carrying the provider key.
   */
  async getRestoreCredential(_options: {
    provider: AuthProvider;
  }): Promise<{ available: boolean; requestJson?: string }> {
    return { available: false };
  }

  /**
   * Clears a previously created restore credential. Web reports
   * `available=false` (graceful unavailability, never `unimplemented`).
   *
   * @param options Options carrying the provider key.
   */
  async clearRestoreCredential(_options: { provider: AuthProvider }): Promise<{ available: boolean }> {
    return { available: false };
  }

  // ---------------------------------------------------------------------------
  // Private: config and URL helpers
  // ---------------------------------------------------------------------------

  private webClientId(): string {
    const configId = readWebConfig().webClientId;
    const clientId = typeof configId === 'string' ? configId : '';
    if (!clientId.endsWith(GOOGLE_CLIENT_SUFFIX)) {
      throw invalidWebClientId(clientId);
    }
    return clientId;
  }

  private webRedirectUri(): string {
    const configUri = readWebConfig().webRedirectUri;
    if (typeof configUri === 'string' && configUri.length > 0) {
      return configUri;
    }
    return window.location.origin + window.location.pathname;
  }

  private async buildAuthRequest(
    clientId: string,
    redirectUri: string,
    options?: SignInOptions,
  ): Promise<{ url: string; pending: PendingAuthFlow }> {
    const verifier = generatePkceVerifier();
    const challenge = await generatePkceChallenge(verifier);
    const state = generateState();
    const scopes = options?.scopes?.length ? options.scopes : DEFAULT_SCOPES;

    const params = new URLSearchParams();
    params.set('response_type', 'code');
    params.set('client_id', clientId);
    params.set('redirect_uri', redirectUri);
    params.set('scope', scopes.join(' '));
    params.set('state', state);
    params.set('code_challenge', challenge);
    params.set('code_challenge_method', 'S256');
    params.set('access_type', 'offline');
    if (options?.prompt) {
      params.set('prompt', options.prompt);
    }

    return {
      url: `${GOOGLE_AUTH_ENDPOINT}?${params.toString()}`,
      pending: { state, verifier },
    };
  }

  /**
   * Runs the sanctioned Apple popup flow via the `appleid.auth.js` SDK.
   *
   * The SDK must be loaded by the host page (Apple CDN); when absent, the call
   * rejects with a typed error instead of reporting `unimplemented`.
   *
   * Apple returns `authorization.{code,id_token,user?}` directly from the
   * popup (no client-side token exchange), so `authorizationCode` is surfaced
   * for the mandatory server-side `client_secret` exchange and is never
   * treated as a refresh token.
   */
  private async appleSignIn(options?: SignInOptions): Promise<SocialAuthResult> {
    const auth = window.AppleID?.auth;
    if (!auth) {
      throw appleSdkUnavailable();
    }

    const clientId = resolveAppleClientId(options);
    const redirectUri = resolveAppleRedirectURI(options);
    if (!clientId) {
      throw appleConfigError('clientId');
    }
    if (!redirectUri) {
      throw appleConfigError('redirectURI');
    }

    const scopes = options?.scopes?.length ? options.scopes : DEFAULT_APPLE_SCOPES;
    const state = options?.state ?? generateState();
    const nonce = options?.nonce ?? generateState();

    auth.initialize({
      clientId,
      scope: scopes.join(APPLE_SCOPE_SEPARATOR),
      redirectURI: redirectUri,
      state,
      nonce,
      usePopup: true,
    });

    let authorization: AppleIDSignInResponse['authorization'];
    try {
      const response = await auth.signIn();
      authorization = response.authorization;
    } catch (error) {
      throw mapAppleSdkError(error);
    }

    if (authorization.state !== undefined && authorization.state !== state) {
      throw invalidState();
    }

    this.tokens = {
      authorizationCode: authorization.code,
      idToken: authorization.id_token,
      user: mapAppleUser(authorization.user),
    };
    return { provider: 'apple', tokens: this.tokens };
  }

  /**
   * Runs the Facebook popup flow via the Facebook JS SDK (`connect.facebook.net`).
   *
   * The SDK is lazy-loaded on first facebook sign-in (never `unimplemented`).
   * Flow: `FB.init` → `FB.login({scope, return_scopes:true})` popup → state /
   * identity bind check (`FB.getLoginStatus` userID must match the login
   * response; mismatch → `INVALID_INPUT`) → `FB.api('/me?fields=...')`
   * granted-only profile → typed result.
   *
   * Mapping: `accessToken` + `userId` + `grantedPermissions`
   * (comma-split `grantedScopes`) + `expiresAt` (now + `expiresIn`, epoch
   * seconds). `declinedPermissions` is absent — the JS SDK does not surface it.
   * `idToken`/`authorizationCode`/`refreshToken`/`serverAuthCode`/`realUserStatus`
   * are never set for facebook (non-applicable fields null; web result
   * MUST NOT carry `realUserStatus`).
   */
  private async facebookSignIn(options?: SignInOptions): Promise<SocialAuthResult> {
    const FB = await loadFacebookSdk();

    const appId = resolveFacebookAppId();
    if (appId === undefined) {
      throw facebookConfigError();
    }
    if (appId.trim().length === 0) {
      throw invalidFacebookConfig();
    }

    FB.init({
      appId,
      version: resolveFacebookVersion(),
      cookie: true,
      xfbml: false,
      status: false,
    });

    const scopes = options?.scopes?.length ? options.scopes : DEFAULT_FACEBOOK_SCOPES;

    const loginResponse = await new Promise<FacebookLoginResponse>((resolve) => {
      FB.login(resolve, { scope: scopes.join(','), return_scopes: true });
    });

    if (loginResponse.error) {
      throw mapFacebookError(loginResponse.error);
    }
    const authResponse = loginResponse.authResponse;
    if (!authResponse?.accessToken || !authResponse.userID) {
      // Dialog dismissed/declined → no token issued.
      throw facebookCancelled();
    }

    // State/identity bind check: the SDK's current session user must be
    // the same user the login response was issued for.
    const statusResponse = await new Promise<FacebookLoginResponse>((resolve) => {
      FB.getLoginStatus(resolve);
    });
    const statusUserID = statusResponse.authResponse?.userID;
    if (statusResponse.error || (statusUserID && statusUserID !== authResponse.userID)) {
      throw invalidFacebookState();
    }

    const profile = await new Promise<FacebookApiResponse>((resolve) => {
      FB.api(`/me?fields=${FACEBOOK_PROFILE_FIELDS}`, resolve);
    });
    if (profile.error || typeof profile.id !== 'string' || !profile.id) {
      throw facebookUnavailable();
    }

    const expiresAt =
      typeof authResponse.expiresIn === 'number' && authResponse.expiresIn > 0
        ? Math.floor(Date.now() / 1000) + authResponse.expiresIn
        : undefined;

    this.tokens = {
      accessToken: authResponse.accessToken,
      userId: authResponse.userID,
      grantedPermissions: splitFacebookScopes(authResponse.grantedScopes),
      expiresAt,
      user: mapFacebookProfile(profile),
    };
    return { provider: 'facebook', tokens: this.tokens };
  }

  private readAuthCodeFromUrl(): AuthCode | null {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    if (!code) {
      return null;
    }
    return { code, state: params.get('state'), error: params.get('error') };
  }

  private async processAuthCode(authCode: AuthCode, clientId: string, redirectUri: string): Promise<void> {
    if (authCode.error) {
      throw tokenEndpointError(authCode.error, 'Authorization failed');
    }
    const flow = this.pendingFlow;
    if (flow?.state !== authCode.state) {
      throw invalidState();
    }

    const body = new URLSearchParams();
    body.set('grant_type', 'authorization_code');
    body.set('client_id', clientId);
    body.set('code', authCode.code);
    body.set('redirect_uri', redirectUri);
    body.set('code_verifier', flow.verifier);

    const json = await postTokenRequest(body);

    if (json.error) {
      throw tokenEndpointError(json.error, json.error_description);
    }
    if (!json.access_token) {
      throw tokenEndpointError('invalid_grant', 'No access token in response');
    }

    this.tokens = {
      accessToken: json.access_token,
      refreshToken: json.refresh_token,
      idToken: json.id_token,
      accessTokenExpiresIn: json.expires_in,
    };
    this.pendingFlow = null;
    this.cleanCallbackQuery();
  }

  private cleanCallbackQuery(): void {
    const url = window.location;
    const cleaned = url.pathname + url.search.replace(/([?&])code=[^&]*&?/, '$1').replace(/([?&])state=[^&]*&?/, '$1');
    window.history.replaceState(null, '', cleaned);
  }
}

interface AuthCode {
  code: string;
  state: string | null;
  error: string | null;
}

/**
 * Minimal typed view of the Authentication plugin's Web configuration block.
 */
interface WebAuthConfig {
  webClientId?: string;
  webRedirectUri?: string;
  /**
   * Facebook app id. Required for the facebook provider: absent → `INIT_FAILED`,
   * blank → `INVALID_INPUT`.
   */
  facebookAppId?: string;
  /**
   * Facebook Graph API version passed to `FB.init`. Defaults to `'v17.0'`.
   */
  facebookVersion?: string;
  /**
   * Accepted for config-schema parity but NEVER sent to the browser: the
   * facebook client token is a native-only secret (docs-only on Web).
   */
  facebookClientToken?: string;
}

interface CapacitorConfigWithPlugins {
  plugins?: { Authentication?: WebAuthConfig };
}

/**
 * Reads the Authentication plugin's Web configuration from the Capacitor
 * runtime config (`plugins.Authentication`), tolerating runtimes where the
 * config is not yet loaded or `getConfig` is absent.
 */
function readWebConfig(): WebAuthConfig {
  const getConfig = (Capacitor as { getConfig?: () => unknown }).getConfig;
  if (typeof getConfig !== 'function') {
    return {};
  }
  const config = getConfig() as CapacitorConfigWithPlugins | undefined;
  return config?.plugins?.Authentication ?? {};
}

// -----------------------------------------------------------------------------
// Pure helpers (mirrored on Android as JUnit-proven Kotlin pure utils)
// -----------------------------------------------------------------------------

async function postTokenRequest(body: URLSearchParams): Promise<TokenResponse> {
  const response = await fetch(GOOGLE_TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });
  return (await response.json()) as TokenResponse;
}

function generatePkceVerifier(): string {
  const bytes = new Uint8Array(64);
  window.crypto.getRandomValues(bytes);
  let verifier = '';
  for (const byte of bytes) {
    verifier += PKCE_ALPHABET[byte % PKCE_ALPHABET.length];
  }
  return verifier;
}

async function generatePkceChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function generateState(): string {
  const bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

// -----------------------------------------------------------------------------
// Typed error helpers (CustomError parity)
// -----------------------------------------------------------------------------

type ErrorCode =
  | 'UNAVAILABLE'
  | 'CANCELLED'
  | 'USER_CANCELLED'
  | 'PERMISSION_DENIED'
  | 'INIT_FAILED'
  | 'INVALID_INPUT'
  | 'UNKNOWN_TYPE'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'TIMEOUT';

interface CustomError extends Error {
  code: ErrorCode;
}

function customError(code: ErrorCode, message: string): CustomError {
  const error = new Error(message) as CustomError;
  error.code = code;
  return error;
}

function invalidProvider(provider: string): CustomError {
  return customError('INVALID_INPUT', `Unsupported provider: ${provider}`);
}

function invalidWebClientId(clientId: string): CustomError {
  return customError('INVALID_INPUT', `Web client id must end in ${GOOGLE_CLIENT_SUFFIX}, got: ${clientId}`);
}

function invalidState(): CustomError {
  return customError('INVALID_INPUT', 'OAuth state mismatch');
}

function noRefreshToken(): CustomError {
  return customError('INVALID_INPUT', 'No refresh token available');
}

function redirectInitiated(): CustomError {
  return customError('INIT_FAILED', 'OAuth redirect in progress');
}

function tokenEndpointError(code: string, description?: string): CustomError {
  const message = description ?? `Token endpoint error: ${code}`;
  if (code === 'access_denied' || code === 'user_cancelled') {
    return customError('USER_CANCELLED', message);
  }
  return customError('INIT_FAILED', message);
}

// -----------------------------------------------------------------------------
// Apple helpers (typed popup error mapping + user profile normalization)
// -----------------------------------------------------------------------------

/**
 * Resolves the Apple client id: per-call `SignInOptions.clientId` wins,
 * otherwise the static `webClientId` Capacitor config value is used.
 */
function resolveAppleClientId(options?: SignInOptions): string {
  const fromOptions = options?.clientId;
  if (typeof fromOptions === 'string' && fromOptions.length > 0) {
    return fromOptions;
  }
  const fromConfig = readWebConfig().webClientId;
  return typeof fromConfig === 'string' ? fromConfig : '';
}

/**
 * Resolves the Apple redirect URI: per-call `SignInOptions.redirectURI` wins,
 * otherwise the static `webRedirectUri` Capacitor config value is used.
 */
function resolveAppleRedirectURI(options?: SignInOptions): string {
  const fromOptions = options?.redirectURI;
  if (typeof fromOptions === 'string' && fromOptions.length > 0) {
    return fromOptions;
  }
  const fromConfig = readWebConfig().webRedirectUri;
  return typeof fromConfig === 'string' ? fromConfig : '';
}

/**
 * Maps `appleid.auth.js` SDK rejections onto the shared 10-code error set:
 * popup closed / user canceled / denied → `USER_CANCELLED`; invalid nonce →
 * `INVALID_INPUT`; anything else (network, server rejection) → `INIT_FAILED`.
 */
function mapAppleSdkError(error: unknown): CustomError {
  const sdkError = typeof error === 'object' && error !== null ? (error as { error?: unknown; message?: unknown }) : {};
  const code = typeof sdkError.error === 'string' ? sdkError.error : '';
  const message = typeof sdkError.message === 'string' ? sdkError.message : 'Apple sign-in failed';

  if (code === 'popup_closed_by_user' || code === 'user_cancelled_authorize' || code === 'access_denied') {
    return customError('USER_CANCELLED', message);
  }
  if (code === 'invalid_nonce') {
    return customError('INVALID_INPUT', message);
  }
  return customError('INIT_FAILED', message);
}

/**
 * Normalizes Apple's optional first-sign-in `user` payload onto the
 * provider-agnostic profile shape.
 *
 * `realUserStatus` is deliberately NOT surfaced here: the spec scopes it
 * "iOS only" (apple-provider Out of Scope), so the Web popup path omits it
 * even when the SDK reports it. The optional field stays on the shared
 * `SocialAuthResultUser` type; only Web does not populate it.
 */
function mapAppleUser(user?: AppleIDUser): SocialAuthResult['tokens']['user'] {
  if (!user) {
    return undefined;
  }
  const profile: NonNullable<SocialAuthResult['tokens']['user']> = {};
  if (typeof user.email === 'string') {
    profile.email = user.email;
  }
  if (user.name && (user.name.firstName || user.name.lastName)) {
    profile.name = {
      ...(user.name.firstName ? { firstName: user.name.firstName } : {}),
      ...(user.name.lastName ? { lastName: user.name.lastName } : {}),
    };
  }
  return profile;
}

function appleSdkUnavailable(): CustomError {
  return customError('INIT_FAILED', 'Apple JS SDK (appleid.auth.js) is not loaded on this page');
}

function appleConfigError(field: 'clientId' | 'redirectURI'): CustomError {
  return customError('INIT_FAILED', `Apple ${field} is required`);
}

function appleNoRefreshToken(): CustomError {
  return customError('INVALID_INPUT', 'Apple issues no refresh token');
}

// -----------------------------------------------------------------------------
// Facebook helpers (lazy SDK loader, config, pure mapping, error parity)
// -----------------------------------------------------------------------------

/**
 * Lazily loads the Facebook JavaScript SDK from `connect.facebook.net`.
 *
 * The load is idempotent: an already-present `FB` global short-circuits. A
 * script-tag injection failure (network/CSP/SSR) rejects `INIT_FAILED` so the
 * flow never reports `unimplemented`.
 */
function loadFacebookSdk(): Promise<FacebookSDK> {
  const existing = window.FB;
  if (existing) {
    return Promise.resolve(existing);
  }
  return new Promise<FacebookSDK>((resolve, reject) => {
    const script = document.createElement('script');
    script.id = FACEBOOK_SDK_ID;
    script.src = FACEBOOK_SDK_SRC;
    script.async = true;
    script.onload = () => {
      const FB = window.FB;
      if (!FB) {
        reject(facebookSdkUnavailable());
        return;
      }
      resolve(FB);
    };
    script.onerror = () => {
      document.getElementById(FACEBOOK_SDK_ID)?.remove();
      reject(facebookSdkUnavailable());
    };
    document.head.appendChild(script);
  });
}

/**
 * Resolves the configured facebook app id from the Capacitor runtime config.
 * `undefined` means the key is absent (→ `INIT_FAILED`); a present key is
 * checked for blank (→ `INVALID_INPUT`) by the caller.
 */
function resolveFacebookAppId(): string | undefined {
  const value = readWebConfig().facebookAppId;
  return typeof value === 'string' ? value : undefined;
}

/**
 * Resolves the Graph API version for `FB.init` (default `'v17.0'`).
 */
function resolveFacebookVersion(): string {
  const value = readWebConfig().facebookVersion;
  return typeof value === 'string' && value.trim().length > 0 ? value : FACEBOOK_DEFAULT_VERSION;
}

/**
 * Splits the SDK's comma-separated `grantedScopes` string into the
 * `grantedPermissions` slot. The JS SDK never surfaces declined scopes, so
 * `declinedPermissions` is deliberately left absent.
 */
function splitFacebookScopes(grantedScopes?: string): string[] | undefined {
  if (typeof grantedScopes !== 'string' || grantedScopes.length === 0) {
    return undefined;
  }
  const scopes = grantedScopes
    .split(',')
    .map((scope) => scope.trim())
    .filter((scope) => scope.length > 0);
  return scopes.length > 0 ? scopes : undefined;
}

/**
 * Maps the granted-only `/me` Graph response onto the homogeneous
 * `SocialAuthResultUser` shape. Only fields present in the response are set —
 * facebook omits non-granted fields so no explicit grant check is needed here.
 * `picture` is returned when granted, never throwing when absent.
 * `realUserStatus` is NEVER set for facebook.
 */
function mapFacebookProfile(profile: FacebookApiResponse): SocialAuthResult['tokens']['user'] {
  const user: NonNullable<SocialAuthResult['tokens']['user']> = {};
  if (typeof profile.id === 'string' && profile.id.length > 0) {
    user.id = profile.id;
  }
  if (typeof profile.name === 'string' && profile.name.length > 0) {
    user.name = profile.name;
  }
  if (typeof profile.email === 'string' && profile.email.length > 0) {
    user.email = profile.email;
  }
  const pictureUrl = profile.picture?.data?.url;
  if (typeof pictureUrl === 'string' && pictureUrl.length > 0) {
    user.picture = pictureUrl;
  }
  return Object.keys(user).length > 0 ? user : undefined;
}

/**
 * Maps facebook flows onto the EXACT shared 10-code set (zero new
 * codes): cancellation → `USER_CANCELLED`; SDK load/missing appId/blank →
 * `INIT_FAILED`/`INVALID_INPUT` via dedicated factories; Graph/network/no user
 * → `UNAVAILABLE`.
 */
function facebookSdkUnavailable(): CustomError {
  return customError('INIT_FAILED', 'Facebook JS SDK (connect.facebook.net) failed to load');
}

function facebookConfigError(): CustomError {
  return customError('INIT_FAILED', "Missing 'facebook' configuration; provide appId.");
}

function invalidFacebookConfig(): CustomError {
  return customError('INVALID_INPUT', 'Facebook appId must not be blank');
}

function invalidFacebookState(): CustomError {
  return customError('INVALID_INPUT', 'Facebook identity/state mismatch');
}

function facebookCancelled(): CustomError {
  return customError('USER_CANCELLED', 'Facebook login was cancelled');
}

function facebookUnavailable(): CustomError {
  return customError('UNAVAILABLE', 'Facebook Graph API/profile unavailable: no user data returned');
}

function facebookNoRefreshToken(): CustomError {
  return customError('INVALID_INPUT', 'Facebook issues no refresh token');
}

/**
 * Maps a facebook SDK/Graph `error` payload onto the 10-code set: no error
 * code / unknown → `UNAVAILABLE` (Graph or network); explicit auth/denied
 * codes → `USER_CANCELLED`; anything else stays `UNAVAILABLE`. Never adds new
 * codes.
 */
function mapFacebookError(error: { message?: string }): CustomError {
  const message =
    typeof error?.message === 'string' && error.message.length > 0 ? error.message : 'Facebook sign-in error';
  return customError('UNAVAILABLE', message);
}
