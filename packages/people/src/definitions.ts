/// <reference types="@capacitor/cli" />

/**
 * Extend the PluginsConfig interface to include configuration options for the People plugin.
 */
import { PermissionState, PluginListenerHandle } from '@capacitor/core';

/**
 * Extension of the Capacitor CLI configuration to include specific settings for People.
 * This allows users to configure the plugin via capacitor.config.ts or capacitor.config.json.
 */
declare module '@capacitor/cli' {
  export interface PluginsConfig {
    /**
     * Configuration options for the People plugin.
     */
    People?: PeopleConfig;
  }
}

/**
 * Static configuration options for the People plugin.
 *
 * These values are defined in `capacitor.config.ts` and consumed
 * exclusively by native code during plugin initialization.
 *
 * Configuration values:
 * - do NOT change the JavaScript API shape
 * - do NOT enable/disable methods
 * - are applied once during plugin load
 */
export interface PeopleConfig {
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
}

/**
 * Options for picking a contact.
 */
export interface PickOptions {
  projection?: PeopleProjection[];
}

/**
 * Standardized error codes used by the People plugin.
 *
 * These codes are returned when a Promise is rejected and can be caught
 * via try/catch blocks.
 *
 * @since 8.0.0
 */
export enum PeopleErrorCode {
  /** The device does not have the requested hardware or the feature is not available on this platform. */
  UNAVAILABLE = 'UNAVAILABLE',
  /** The user cancelled an interactive flow. */
  CANCELLED = 'CANCELLED',
  /** The user denied the permission or the feature is disabled by the OS. */
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  /** The plugin failed to initialize or perform an operation. */
  INIT_FAILED = 'INIT_FAILED',
  /** The input provided to the plugin method is invalid, missing, or malformed. */
  INVALID_INPUT = 'INVALID_INPUT',
  /** The requested type is not valid or supported. */
  UNKNOWN_TYPE = 'UNKNOWN_TYPE',
  /** The requested resource does not exist. */
  NOT_FOUND = 'NOT_FOUND',
  /** The operation conflicts with the current state. */
  CONFLICT = 'CONFLICT',
  /** The operation did not complete within the expected time. */
  TIMEOUT = 'TIMEOUT',
}

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
// CORE DATA TYPES
// -----------------------------------------------------------------------------

/**
 * Supported fields for the Projection Engine.
 * Requesting only what you need reduces memory usage by O(N).
 * `image` projection support is iOS-only. On Android and Web, requesting `image` is rejected with `UNKNOWN_TYPE` and message `Unsupported projection field: image`.
 */
export type PeopleProjection =
  | 'name'
  | 'organization'
  | 'birthday'
  | 'phones'
  | 'emails'
  | 'addresses'
  | 'urls'
  // iOS only (base64 thumbnail). Android/Web reject this projection with UNKNOWN_TYPE.
  | 'image'
  | 'note';

/**
 * Capabilities of the People plugin on this device/implementation.
 */
export interface PeopleCapabilities {
  canRead: boolean;
  canWrite: boolean;
  canObserve: boolean;
  canManageGroups: boolean;
  canPickContact: boolean;
}

/**
 * Phone number representation.
 */
export interface PhoneNumber {
  /** Normalized label (e.g., 'mobile', 'home', 'work') */
  label?: string;
  /** The raw input string */
  number: string;
  /** E.164 formatted number (if parsing succeeded) */
  normalized?: string;
}

/**
 * Email address representation.
 */
export interface EmailAddress {
  label?: string;
  address: string;
}

/**
 * Postal address representation.
 */
export interface PostalAddress {
  label?: string;
  formatted?: string;
  street?: string;
  city?: string;
  region?: string;
  postcode?: string;
  country?: string;
}

/**
 * Represents a Unified Person in the directory.
 * Maps to CNContact (iOS) and Aggregated Contact (Android).
 */
export interface UnifiedContact {
  /** The platform-specific unique identifier (UUID or Long) */
  id: string;
  /** The unified display name */
  name?: {
    display: string;
    given?: string;
    middle?: string;
    family?: string;
    prefix?: string;
    suffix?: string;
  };
  organization?: {
    company?: string;
    title?: string;
    department?: string;
  };
  birthday?: {
    year?: number;
    month: number;
    day: number;
  };
  phones?: PhoneNumber[];
  emails?: EmailAddress[];
  addresses?: PostalAddress[];
  urls?: string[];
  note?: string;
  /** Base64 thumbnail string (iOS only, only if projected). */
  image?: string;
}

// -----------------------------------------------------------------------------
// EVENTS
// -----------------------------------------------------------------------------

/**
 * Payload delivered to listeners registered for "peopleChange".
 * - `ids`: always present, contains changed contact IDs (may be empty).
 * - `type`: one of 'insert' | 'update' | 'delete' (default 'update').
 *   Current native implementations emit `update`.
 */
export type PeopleChangeType = 'insert' | 'update' | 'delete';

/**
 * Event emitted when changes are detected in the device's address book.
 */
export interface PeopleChangeEvent {
  /** Array of affected contact IDs (always present, may be empty) */
  ids: string[];
  /** The type of change detected */
  type: PeopleChangeType;
}

/**
 * Result returned by pickContact().
 * Now strictly returns the contact object on success.
 */
export interface PickContactResult {
  contact: UnifiedContact;
}

// -----------------------------------------------------------------------------
// INPUT/OUTPUT INTERFACES
// -----------------------------------------------------------------------------

/**
 * Options for querying contacts.
 */
export interface GetContactsOptions {
  /**
   * Array of fields to fetch.
   * MISSING fields will not be read from DB (Performance).
   * @default ['name']
   */
  projection?: PeopleProjection[];
  /** Max number of records to return */
  limit?: number;
  /** Skip count (implement pagination via cursor usually better, but offset for now) */
  offset?: number;
  /**
   * Whether to compute totalCount across the full contacts set.
   * This may require scanning/counting the full address book and can be expensive on large datasets.
   * @default false
   */
  includeTotal?: boolean;
}

/**
 * Result returned by getContacts().
 */
export interface GetContactsResult {
  contacts: UnifiedContact[];
  /** Total count in the DB (permissions permitting) */
  totalCount: number;
}

/**
 * Permissions status interface.
 */
export interface PeoplePluginPermissions {
  contacts: PermissionState;
}

// -----------------------------------------------------------------------------
// GROUPS
// -----------------------------------------------------------------------------

/**
 * Represents a group in the address book.
 * A group can be system-generated (e.g., "All Contacts") or user-created.
 *
 * @since 8.0.0
 */
export interface Group {
  /**
   * A unique identifier for the group.
   * This ID is stable and can be used for subsequent operations.
   */
  id: string;

  /**
   * The display name of the group.
   * e.g., "Family", "Work", "Book Club"
   */
  name: string;

  /**
   * The source or account where the group originates.
   * On iOS, this could be "iCloud" or "Local". On Android, this corresponds to the account name (e.g., "user@gmail.com").
   * For logical groups, this will be 'local'.
   */
  source?: string;

  /**
   * Indicates if the group is read-only.
   * System groups are typically read-only and cannot be deleted or renamed.
   */
  readOnly: boolean;
}

/**
 * Result returned by `listGroups`.
 *
 * @since 8.0.0
 */
export interface ListGroupsResult {
  /**
   * An array of groups found in the address book.
   */
  groups: Group[];
}

/**
 * Options for creating a new group.
 *
 * @since 8.0.0
 */
export interface CreateGroupOptions {
  /**
   * The name for the new group.
   */
  name: string;
}

/**
 * Result returned by `createGroup`.
 *
 * @since 8.0.0
 */
export interface CreateGroupResult {
  /**
   * The newly created group.
   */
  group: Group;
}

/**
 * Options for deleting a group.
 *
 * @since 8.0.0
 */
export interface DeleteGroupOptions {
  /**
   * The unique identifier of the group to delete.
   */
  groupId: string;
}

/**
 * Options for adding people to a group.
 *
 * @since 8.0.0
 */
export interface AddPeopleToGroupOptions {
  /**
   * The unique identifier of the group.
   */
  groupId: string;

  /**
   * An array of contact identifiers to add to the group.
   */
  contactIds: string[];
}

/**
 * Options for removing people from a group.
 *
 * @since 8.0.0
 */
export interface RemovePeopleFromGroupOptions {
  /**
   * The unique identifier of the group.
   */
  groupId: string;

  /**
   * An array of contact identifiers to remove from the group.
   */
  contactIds: string[];
}

// -----------------------------------------------------------------------------
// CRUD
// -----------------------------------------------------------------------------

/**
 * Options for creating a new contact.
 * The contact data is provided as a partial UnifiedContact.
 *
 * @since 8.0.0
 */
export interface CreateContactOptions {
  /**
   * The contact data to be saved.
   * At least one writable field (e.g., name, email) must be provided.
   */
  contact: Partial<Omit<UnifiedContact, 'id'>>;
}

/**
 * Result returned by `createContact`.
 *
 * @since 8.0.0
 */
export interface CreateContactResult {
  /**
   * The full contact object as it was saved, including its new unique ID.
   */
  contact: UnifiedContact;
}

/**
 * Options for updating an existing contact.
 * This operation performs a patch, not a full replacement.
 *
 * @since 8.0.0
 */
export interface UpdateContactOptions {
  /**
   * The unique identifier of the contact to update.
   */
  contactId: string;

  /**
   * An object containing the fields to be updated.
   * Only the provided fields will be modified.
   */
  contact: Partial<Omit<UnifiedContact, 'id'>>;
}

/**
 * Result returned by `updateContact`.
 *
 * @since 8.0.0
 */
export interface UpdateContactResult {
  /**
   * The full contact object after the update has been applied.
   */
  contact: UnifiedContact;
}

/**
 * Options for deleting a contact.
 *
 * @since 8.0.0
 */
export interface DeleteContactOptions {
  /**
   * The unique identifier of the contact to delete.
   */
  contactId: string;
}

/**
 * Options for merging two contacts.
 *
 * @since 8.0.0
 */
export interface MergeContactsOptions {
  /**
   * The identifier of the contact that will be subsumed and deleted.
   */
  sourceContactId: string;

  /**
   * The identifier of the contact that will be kept and updated.
   */
  destinationContactId: string;
}

/**
 * Result returned by `mergeContacts`.
 *
 * @since 8.0.0
 */
export interface MergeContactsResult {
  /**
   * The state of the destination contact after the merge.
   */
  contact: UnifiedContact;
}

/**
 * Capacitor People plugin interface.
 * * This interface defines the contract between the JavaScript layer and the
 * native implementations (Android and iOS).
 */
export interface PeoplePlugin {
  /**
   * [CRUD]
   * Creates a new contact in the device's address book.
   * * @throws {PeopleError} PERMISSION_DENIED if contacts permission is missing.
   * @throws {PeopleError} UNAVAILABLE if the operation fails on the native side.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * try {
   * const { contact } = await People.createContact({
   * contact: {
   * name: { given: 'John', family: 'Appleseed' },
   * emails: [{ address: 'john.appleseed@example.com', label: 'work' }],
   * },
   * });
   * console.log('Created contact ID:', contact.id);
   * } catch (error) {
   * console.error('Failed to create contact:', error.code);
   * }
   * ```
   *
   * @since 8.0.0
   */
  createContact(options: CreateContactOptions): Promise<CreateContactResult>;

  /**
   * [CRUD]
   * Updates an existing contact using a patch-based approach.
   * * @throws {PeopleError} PERMISSION_DENIED if permission is missing.
   * @throws {PeopleError} UNAVAILABLE if the contact is not owned by the app or not found.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * try {
   * const { contact } = await People.updateContact({
   * contactId: 'some-contact-id',
   * contact: {
   * organization: { company: 'New Company Inc.' },
   * },
   * });
   * console.log('Updated contact company:', contact.organization?.company);
   * } catch (error) {
   * console.error('Update failed:', error.message);
   * }
   * ```
   *
   * @since 8.0.0
   */
  updateContact(options: UpdateContactOptions): Promise<UpdateContactResult>;

  /**
   * [CRUD]
   * Deletes a contact from the device's address book.
   * Only contacts owned by the app can be deleted.
   * * @throws {PeopleError} UNAVAILABLE if deletion fails or contact is not app-owned.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * await People.deleteContact({ contactId: 'contact-id-to-delete' });
   * ```
   *
   * @since 8.0.0
   */
  deleteContact(options: DeleteContactOptions): Promise<void>;

  /**
   * [CRUD]
   * Merges a source contact into a destination contact.
   * The source contact is deleted after the merge.
   * * @throws {PeopleError} PERMISSION_DENIED if permission is missing.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * const { contact } = await People.mergeContacts({
   * sourceContactId: 'duplicate-contact-id',
   * destinationContactId: 'main-contact-id',
   * });
   * console.log('Final contact state:', contact);
   * ```
   *
   * @since 8.0.0
   */
  mergeContacts(options: MergeContactsOptions): Promise<MergeContactsResult>;

  /**
   * [GROUPS]
   * Lists all available contact groups.
   *
   * @example
   * ```typescript
   * const { groups } = await People.listGroups();
   * ```
   *
   * @since 8.0.0
   */
  listGroups(): Promise<ListGroupsResult>;

  /**
   * [GROUPS]
   * Creates a new contact group.
   * * @example
   * ```typescript
   * const { group } = await People.createGroup({ name: 'Family' });
   * ```
   *
   * @since 8.0.0
   */
  createGroup(options: CreateGroupOptions): Promise<CreateGroupResult>;

  /**
   * [GROUPS]
   * Deletes a contact group.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * await People.deleteGroup({ groupId: 'group-id-to-delete' });
   * ```
   *
   * @since 8.0.0
   */
  deleteGroup(options: DeleteGroupOptions): Promise<void>;

  /**
   * [GROUPS]
   * Adds contacts to a group.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * await People.addPeopleToGroup({
   *   groupId: 'group-id',
   *   contactIds: ['contact-id-1', 'contact-id-2'],
   * });
   * ```
   *
   * @since 8.0.0
   */
  addPeopleToGroup(options: AddPeopleToGroupOptions): Promise<void>;

  /**
   * [GROUPS]
   * Removes contacts from a group.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * await People.removePeopleFromGroup({
   *   groupId: 'group-id',
   *   contactIds: ['contact-id-1'],
   * });
   * ```
   *
   * @since 8.0.0
   */
  removePeopleFromGroup(options: RemovePeopleFromGroupOptions): Promise<void>;

  /**
   * Check the status of permissions.
   * @returns A promise resolving to the current permission states.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   * const permissions = await People.checkPermissions();
   * console.log(permissions.contacts); // Output: 'granted' | 'denied' | 'prompt'
   * ```
   *
   * @since 8.0.0
   */
  checkPermissions(): Promise<PeoplePluginPermissions>;

  /**
   * Request permissions.
   * @param permissions - An optional object specifying which permissions to request.
   * If not provided, all permissions defined in the plugin will be requested.
   * @returns A promise resolving to the updated permission states.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   * const permissions = await People.requestPermissions();
   * // OR
   * // const permissions = await People.requestPermissions({ permissions: ['contacts'] });
   * console.log(permissions.contacts); // Output: 'granted' | 'denied'
   * ```
   *
   * @since 8.0.0
   */
  requestPermissions(permissions?: { permissions: 'contacts'[] }): Promise<PeoplePluginPermissions>;

  /**
   * [ZERO-PERMISSION]
   * Launches the native OS contact picker UI.
   * This method does NOT require any entries in AndroidManifest.xml or Info.plist
   * as the user explicitly selects the data via the system UI.
   *
   * @throws {PeopleError} CANCELLED if the user cancels or the picker fails.
   *
   * @example
   * ```typescript
   * try {
   * const { contact } = await People.pickContact({
   * projection: ['name', 'phones', 'emails']
   * });
   * console.log('User selected:', contact);
   * } catch (error) {
   * if (error.code === 'CANCELLED') {
   * console.log('User cancelled the picker.');
   * }
   * }
   * ```
   *
   * @since 8.0.0
   */
  pickContact(options?: { projection?: PeopleProjection[] }): Promise<PickContactResult>;

  /**
   * [SYSTEMIC-ACCESS]
   * Queries the entire contact database with specific projection and pagination.
   * REQUIRES 'contacts' permission.
   * Use `includeTotal` only when needed: computing `totalCount` may require scanning/counting across the full contacts set and can be expensive on large address books. Default is `false`.
   *
   * @example
   * ```typescript
   * const result = await People.getContacts({
   * projection: ['name', 'phones'],
   * limit: 20,
   * offset: 0
   * });
   * ```
   *
   * @since 8.0.0
   */
  getContacts(options?: GetContactsOptions): Promise<GetContactsResult>;

  /**
   * Retrieves a single contact by ID.
   * * @throws {PeopleError} UNAVAILABLE if contact is not found.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * const { contact } = await People.getContact({ id: 'contact-id', projection: ['name', 'emails'] });
   * console.log('Contact details:', contact);
   * ```
   *
   * @since 8.0.0
   */
  getContact(options: { id: string; projection?: PeopleProjection[] }): Promise<{ contact: UnifiedContact }>;

  /**
   * Returns what this device/implementation is capable of.
   * Useful for UI adaptation (e.g. hiding "Edit" buttons).
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * const capabilities = await People.getCapabilities();
   * console.log('Can Read Contacts:', capabilities.canRead);
   * console.log('Can Write Contacts:', capabilities.canWrite);
   * ```
   *
   * @since 8.0.0
   */
  getCapabilities(): Promise<PeopleCapabilities>;

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
   * const { version } = await People.getPluginVersion();
   * ```
   *
   * @since 8.0.0
   */
  getPluginVersion(): Promise<PluginVersionResult>;

  /**
   * [SYSTEMIC-ACCESS]
   * Searches the database with projection.
   * REQUIRES 'contacts' permission.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * const result = await People.searchPeople({ query: 'John', projection: ['name', 'phones'], limit: 10 });
   * console.log('Fetched contacts:', result.contacts);
   * ```
   *
   * @since 8.0.0
   */
  searchPeople(options: { query: string; projection?: PeopleProjection[]; limit?: number }): Promise<GetContactsResult>;

  /**
   * Listen for changes in the system address book.
   * REQUIRES 'contacts' permission.
   * * @returns A promise that resolves to a handle to remove the listener.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * const handle = await People.addListener('peopleChange', (event) => {
   *   console.log('People change detected:', event.type);
   * });
   *
   * // To remove the listener later:
   * // await handle.remove();
   * ```
   *
   * @since 8.0.0
   */
  addListener(
    eventName: 'peopleChange',
    listenerFunc: (payload: PeopleChangeEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Registers a listener for plugin events using a generic event name.
   *
   * Prefer the typed `peopleChange` overload for full payload type safety.
   *
   * @since 8.0.0
   */
  addListener(eventName: string, listenerFunc: (...args: unknown[]) => void): Promise<PluginListenerHandle>;

  /**
   * Removes all registered listeners for this plugin.
   * @returns Promise that resolves when all listeners have been removed.
   *
   * @example
   * ```typescript
   * import { People } from '@cap-kit/people';
   *
   * await People.removeAllListeners();
   * ```
   *
   * @since 8.0.0
   */
  removeAllListeners(): Promise<void>;
}
