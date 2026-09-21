/// <reference types="@capacitor/cli" />

/**
 * Capacitor configuration extension for the Redsys plugin.
 *
 * Configuration values defined here can be provided under the `plugins.Redsys`
 * key inside `capacitor.config.ts`.
 *
 * These values are:
 * - read natively at build/runtime
 * - NOT accessible from JavaScript at runtime
 * - treated as read-only static configuration
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the Redsys plugin.
     */
    Redsys?: RedsysConfig;
  }
}

// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------

/**
 * Static configuration options for the Redsys plugin.
 * Defined in `capacitor.config.ts` and consumed exclusively by native code.
 */
export interface RedsysConfig {
  /**
   * Enables verbose native logging.
   *
   * When enabled, additional debug information is printed
   * to the native console (Logcat on Android, Xcode on iOS).
   *
   * @default false
   * @example true
   *
   * @since 8.0.0
   */
  verboseLogging?: boolean;

  /**
   * Optional global merchant signature for web payments.
   * If provided, it acts as a default when not passed in the method call.
   *
   * @since 8.0.0
   */
  signature?: string;

  /**
   * Application license.
   * Alphanumeric code provided by Redsys to validate the application.
   *
   * @example "your_license_here"
   *
   * @since 8.0.0
   */
  license: string;

  /**
   * Transaction environment.
   * Maps to ENVIRONMENT_TEST/REAL (Android) or EnviromentType (iOS).
   *
   * @example "RedsysEnvironment.Test"
   *
   * @since 8.0.0
   */
  environment: RedsysEnvironment;

  /**
   * Merchant identification code (FUC).
   *
   * @example "XXXXXXXXXXX"
   *
   * @since 8.0.0
   */
  fuc: string;

  /**
   * Terminal identifier associated with the merchant.
   *
   * @example "XX"
   *
   * @since 8.0.0
   */
  terminal: string;

  /**
   * ISO-4217 currency code. Default is "978" (EUR).
   *
   * @example "978"
   *
   * @since 8.0.0
   */
  currency: string;

  /**
   * Name of the payment titular.
   *
   */
  merchantName?: string;

  /**
   * URL of the commerce.
   *
   * @example "XXX"
   *
   * @since 8.0.0
   */
  merchantUrl?: string;

  /**
   * Additional merchant data (internal references, etc.).
   *
   * @example "XXX"
   *
   * @since 8.0.0
   */
  merchantData?: string;

  /**
   * Commerce descriptor or description.
   *
   * @example "XXX"
   *
   * @since 8.0.0
   */
  merchantDescription?: string;

  /**
   * FUC of the merchant group for inter-merchant references.
   *
   * @example "XXX"
   *
   * @since 8.0.0
   */
  merchantGroup?: string;

  /**
   * Global merchant language setting.
   * Maps to TPVVConfiguration.setLanguage (Android) or appMerchantConsumerLanguage (iOS).
   *
   * @since 8.0.0
   */
  merchantConsumerLanguage?: RedsysLanguage;

  /**
   * Default transaction type for the application.
   * Maps to TransactionType constants.
   *
   * @since 8.0.0
   */
  transactionType?: RedsysTransactionType;

  /**
   * Payment method(s) to be displayed in WebView flow.
   *
   * @example "XXX"
   *
   * @since 8.0.0
   */
  paymentMethods?: RedsysPaymentMethod[];

  /**
   * Enables automatic redirection to the bank's 3DS page when required.
   * If false, the plugin will return a specific error code for 3DS requirements,
   * allowing the application to handle redirection manually.
   *
   * @default true
   *
   * @since 8.0.0
   */
  enableRedirection?: boolean;

  /**
   * Enables display of result alerts after payment completion.
   * If false, the plugin will return the result data without showing native alerts,
   * allowing the application to handle result presentation.
   *
   * @default true
   *
   * @since 8.0.0
   */
  enableResultAlert?: boolean;

  /**
   * Native UI customization options.
   *
   * @since 8.0.0
   */
  ui?: RedsysUIOptions;
}

// -----------------------------------------------------------------------------
// Payment & UI Interfaces
// -----------------------------------------------------------------------------

/**
 * Abstract, platform-agnostic UI customization options for Redsys payment screens.
 * Some properties may only be supported on specific platforms due to SDK limitations.
 *
 */
export interface RedsysUIOptions {
  /**
   * Top logo image reference.
   * (Supported: Android & iOS)
   */
  logo?: string;

  /**
   * Payment screen background color (Hex string).
   * (Supported: Android & iOS)
   */
  backgroundColor?: string;

  /**
   * Payment screen background image.
   * (Supported: iOS Only)
   */
  iosBackgroundImage?: string;

  /**
   * Color of the progress bar during network operations.
   * (Supported: Android Only)
   */
  androidProgressBarColor?: string;

  /**
   * Background color of the top action bar.
   * (Supported: Android Only)
   */
  androidTopBarColor?: string;

  /**
   * Text for the main action/payment button.
   * (Supported: Android & iOS)
   */
  confirmButtonText?: string;

  /**
   * Background color of the main action button.
   * (Supported: Android Only)
   */
  androidConfirmButtonColor?: string;

  /**
   * Text color of the main action button.
   * (Supported: Android Only)
   */
  androidConfirmButtonTextColor?: string;

  /**
   * Text for the cancel/back button.
   * (Supported: iOS Only)
   */
  iosCancelButtonText?: string;

  /**
   * Label text for the card number field.
   * (Supported: Android & iOS)
   */
  cardNumberLabel?: string;

  /**
   * Label text for the expiration date field.
   * (Supported: Android & iOS)
   */
  expirationLabel?: string;

  /**
   * Label text for the CVV/security code field.
   * (Supported: Android & iOS)
   */
  cvvLabel?: string;

  /**
   * Descriptive text or payment instructions displayed on screen.
   * (Supported: Android & iOS)
   */
  infoLabel?: string;

  /**
   * Text color for general labels (Card Number, Expiry, CVV).
   * (Supported: Android & iOS)
   */
  labelTextColor?: string;

  /**
   * Error message text displayed when the expiration date is invalid.
   * (Supported: Android Only)
   */
  androidExpirationErrorText?: string;

  /**
   * Error message text displayed when the CVV is invalid.
   * (Supported: Android Only)
   */
  androidCvvErrorText?: string;

  /**
   * Background color of the card header area.
   * (Supported: Android Only)
   */
  cardHeaderBackgroundColor?: string;

  /**
   * Text for the card header.
   * (Supported: Android Only)
   */
  androidCardHeaderText?: string;

  /**
   * Text color for the card header.
   * (Supported: Android Only)
   */
  androidCardHeaderTextColor?: string;

  /**
   * Custom text for the success alert message.
   * (Supported: Android Only)
   */
  androidResultAlertTextOk?: string;

  /**
   * Custom text for the error alert message.
   * (Supported: Android Only)
   */
  androidResultAlertTextKo?: string;

  /**
   * Custom text for the success alert button.
   * (Supported: Android Only)
   */
  androidResultAlertButtonTextOk?: string;

  /**
   * Custom text for the error alert button.
   * (Supported: Android Only)
   */
  androidResultAlertButtonTextKo?: string;
}

/**
 *
 */
export interface RedsysPaymentRequest {
  /** Unique merchant order identifier. */
  order: string;
  /** Amount in the smallest currency unit. */
  amount: number;
  /** Optional product description for display in the payment UI. */
  description?: string;
  /** ISO-4217 currency code (e.g., "978" for EUR). */
  transactionType: RedsysTransactionType;
  /** Token handling: undefined (normal), 'REQUEST_REFERENCE' (new token), or existing token string. */
  identifier?: string;
  /** Optional additional parameters to be sent to the backend and returned in the response. */
  extraParams?: Record<string, string>;
  /** Runtime UI overrides for this specific transaction. */
  uiOptions?: RedsysUIOptions;
}

/**
 *
 */
export interface RedsysWebPaymentInitResult {
  /** Base64 data from native SDK that must be signed by the backend. */
  base64Data: string;

  /**
   * The signature if pre-calculated or available from config.
   */
  signature?: string;
}

/**
 *
 */
export interface RedsysWebPaymentOptions {
  /**
   * Server-generated signature.
   * If omitted, the plugin will attempt to use the 'signature' defined in RedsysConfig.
   */
  signature?: string;
  /** Defaults to HMAC_SHA256_V1 if omitted. */
  signatureVersion?: 'HMAC_SHA256_V1' | 'HMAC_SHA512_V1';
}

/**
 *
 */
export interface HashOptions {
  /** Dati da firmare (solitamente la stringa Base64 ricevuta da initializeWebPayment) */
  data: string;
  /** Chiave segreta in formato Base64 */
  keyBase64: string;
  /** Algorithm: 'HMAC_SHA256_V1' | 'HMAC_SHA512_V1' */
  algorithm: 'HMAC_SHA256_V1' | 'HMAC_SHA512_V1';
}

/**
 *
 */
export interface HashResult {
  /** Firma generata in Base64 */
  signature: string;
}

/**
 * Standardized success response object for Redsys operations.
 * This interface merges data from Android's ResultResponse and iOS's WebViewPaymentResponseOK.
 *
 */
export interface RedsysPaymentResponseOK {
  /** Internal SDK result code */
  code: number;

  /** Human-readable description */
  desc: string;

  /** Transaction amount */
  amount: string;

  /** Currency code (ISO-4217) */
  currency: string;

  /** Order identifier */
  order: string;

  /** Merchant FUC code */
  merchantCode: string;

  /** Terminal identifier */
  terminal: string;

  /** TPV response code */
  responseCode: string;

  /** Authorization code */
  authorisationCode: string;

  /** Transaction type */
  transactionType: string;

  /** Indicates secure payment (1/0) */
  securePayment: string;

  /** Digital signature */
  signature: string;

  /** Masked card number */
  cardNumber: string;

  /** Card brand (e.g. VISA) */
  cardBrand: string;

  /** Card issuing country */
  cardCountry: string;

  /** Card type (D/C) */
  cardType: string;

  /** Expiry date (MMYY) */
  expiryDate: string;

  /** Recurring / reference identifier */
  merchantIdentifier?: string;

  /** Consumer language code */
  consumerLanguage?: string;

  /** Operation date (WebView only) */
  date?: string;

  /** Operation hour (WebView only) */
  hour?: string;

  /** Merchant-specific data */
  merchantData?: string;

  /** Extra parameters returned by backend */
  extraParams?: Record<string, string>;
}

/**
 * Standardized error codes returned by the Redsys SDK on both Android and iOS.
 */
export enum RedsysSdkErrorCode {
  /** Invalid signature provided for the transaction. */
  InvalidSignature = 11,

  /** Cryptography error encountered during data processing. */
  CryptographyError = 29,

  /** The application configuration is invalid or unauthorized. */
  InvalidApplication = 31,

  /** Invalid JSON format or data structure. */
  InvalidJSON = 60,

  /** Error retrieving or validating the merchant signature key. */
  SignatureKeyError = 61,

  /** The provided signature type is not supported by the SDK. */
  UnsupportedSignatureType = 62,

  /**
   * Error returned directly by the Redsys Virtual TPV platform.
   * Refer to 'desc' field for specific TPV details.
   */
  RedsysTPVError = 78,

  /** Generic or Internal SDK error. */
  InternalError = 5548,

  /** Network connection error or timeout. */
  ConnectionError = 5550,

  /** The server returned an empty response (Android specific). */
  EmptyResponse = 5551,

  /** Error during Base64 encoding/decoding (Android specific). */
  Base64Error = 1000,
}

// -----------------------------------------------------------------------------
// Enums
// -----------------------------------------------------------------------------

/**
 * Payment methods available for WebView flows.
 * Note: Native Direct Payment does not support method selection and defaults to card payments.
 *
 * @since 8.0.0
 */
export enum RedsysPaymentMethod {
  /** Card payment */
  Card = 'C',
  /** Bank transfer */
  Transfer = 'R',
  /** Direct debit (domiciliation) */
  Domiciliation = 'D',
  /** PayPal */
  Paypal = 'P',
  /** Bizum (immediatePayment) */
  Bizum = 'Z',
  /** Oasys Wallet (Android specific) */
  Oasys = 'O',
}

/**
 * Transaction environment types.
 * These values correspond to the environments defined by Redsys for testing and production.
 *
 * @since 8.0.0
 */
export enum RedsysEnvironment {
  Integration = 'Integration',
  Real = 'Real',
  Test = 'Test',
}

/**
 * Supported transaction types for Redsys operations.
 *
 * @since 8.0.0
 */
export enum RedsysTransactionType {
  /** Normal payment (Standard) */
  Normal = 'normal',
  /** Pre-authorization for later capture */
  Preauthorization = 'preauthorization',
  /** Traditional transaction flow */
  Traditional = 'traditional',
  /** Authentication-only flow */
  Authentication = 'paymentTypeAuthentication',
}

/**
 * Supported language codes for the Redsys UI.
 * These codes follow the Redsys internal standard for localization.
 *
 * @since 8.0.0
 */
/**
 * Supported language codes for the Redsys UI.
 * Values are based on the official SDK documentation table.
 *
 */
export enum RedsysLanguage {
  /** Default (typically Spanish) */
  Default = '0',
  /** Spanish */
  Spanish = '1',
  /** English */
  English = '2',
  /** Catalan */
  Catalan = '3',
  /** French */
  French = '4',
  /** German */
  German = '5',
  /** Dutch */
  Dutch = '6',
  /** Italian */
  Italian = '7',
  /** Swedish */
  Swedish = '8',
  /** Portuguese */
  Portuguese = '9',
  /** Valencian */
  Valencian = '10',
  /** Polish */
  Polish = '11',
  /** Galician */
  Galician = '12',
  /** Basque */
  Basque = '13',
  /** Bulgarian */
  Bulgarian = '100',
  /** Chinese */
  Chinese = '156',
  /** Croatian */
  Croatian = '191',
  /** Czech */
  Czech = '203',
  /** Danish */
  Danish = '208',
  /** Estonian */
  Estonian = '233',
  /** Finnish */
  Finnish = '246',
  /** Greek */
  Greek = '300',
  /** Hungarian */
  Hungarian = '348',
  /** Japanese */
  Japanese = '392',
  /** Latvian */
  Latvian = '428',
  /** Lithuanian */
  Lithuanian = '440',
  /** Maltese */
  Maltese = '470',
  /** Romanian */
  Romanian = '642',
  /** Russian */
  Russian = '643',
  /** Slovak */
  Slovak = '703',
  /** Slovenian */
  Slovenian = '705',
  /** Turkish */
  Turkish = '792',
}

/**
 * Standardized error codes used by the Redsys plugin.
 *
 * These codes are returned as part of structured error objects
 * and allow consumers to implement programmatic error handling.
 *
 * @since 8.0.0
 */
export enum RedsysPluginErrorCode {
  /** The device does not have the requested hardware or the API is unavailable. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user denied a required permission or the feature is disabled. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The Redsys plugin failed to initialize (e.g., native SDK error). */
  INIT_FAILED = 'INIT_FAILED',
  /** The requested operation or type is not valid or supported. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  /** */
  CRYPTO_ERROR = 'CRYPTO_ERROR',
  /** */
  SDK_ERROR = 'SDK_ERROR',
}

// -----------------------------------------------------------------------------
// Results
// -----------------------------------------------------------------------------

/**
 * Result object returned by the `getPluginVersion()` method.
 */
export interface PluginVersionResult {
  /**
   * The native plugin version string.
   */
  version: string;
}

// -----------------------------------------------------------------------------
// Public API
// -----------------------------------------------------------------------------

/**
 * Public JavaScript API for the Redsys Capacitor plugin.
 *
 * This interface defines a stable, platform-agnostic API.
 * All methods behave consistently across Android, iOS, and Web.
 */
export interface RedsysPlugin {
  /**
   * Executes a direct payment (Native SDK UI). No 3DS support.
   * @example
   * ```ts
   * const result = await Redsys.doDirectPayment({
   *   order: '12345',
   *   amount: 1000,
   *   transactionType: RedsysTransactionType.Normal,
   *   description: 'Test Product',
   *   identifier: 'REQUEST_REFERENCE',
   *   extraParams: { customKey: 'customValue' },
   *   uiOptions: { confirmButtonText: 'Pay Now' }
   * });
   * ```
   *
   * @since 8.0.0
   */
  doDirectPayment(options: RedsysPaymentRequest): Promise<RedsysPaymentResponseOK>;

  /**
   * Web Payment: Initializes and returns Base64 data for signing.
   *
   * @example
   * ```ts
   * const initResult = await Redsys.initializeWebPayment({
   *   order: '12345',
   *   amount: 1000,
   *   transactionType: RedsysTransactionType.Normal,
   *   description: 'Test Product',
   *   identifier: 'REQUEST_REFERENCE',
   *   extraParams: { customKey: 'customValue' },
   *   uiOptions: { confirmButtonText: 'Pay Now' }
   * });
   * ```
   *
   * @since 8.0.0
   */
  initializeWebPayment(options: RedsysPaymentRequest): Promise<RedsysWebPaymentInitResult>;

  /**
   * Web Payment: Executes WebView flow with the server-generated signature.
   *
   * @example
   * ```ts
   * const result = await Redsys.processWebPayment({
   *   signature: 'base64-encoded-signature',
   * });
   * ```
   *
   * @since 8.0.0
   */
  processWebPayment(options: RedsysWebPaymentOptions): Promise<RedsysPaymentResponseOK>;

  /**
   *
   * @example
   * ```ts
   *
   * ```
   *
   * @since 8.0.0
   */
  computeHash(options: HashOptions): Promise<HashResult>;

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
   * const { version } = await Redsys.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;
}
