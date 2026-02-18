<p align="center">
  <img
    src="https://raw.githubusercontent.com/cap-kit/capacitor-plugins/main/assets/logo.png"
    alt="CapKit Logo"
    width="128"
  />
</p>

<h3 align="center">People</h3>
<p align="center">
  <strong>
    <code>@cap-kit/people</code>
  </strong>
</p>

<p align="center">
  Enterprise-grade People Directory for Capacitor. Provides a unified, capability-based abstraction over native contact
  systems, featuring native projection queries, zero-permission contact picking, systemic access with fine-grained
  capabilities, live change observation, native semantic search, and first-class vCard import/export. Designed for
  performance, privacy, and large datasets without memory bloat.
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cap-kit/people">
    <img src="https://img.shields.io/npm/v/@cap-kit/people?color=blue&label=npm&logo=npm&style=flat-square" alt="npm version">
  </a>
  <a href="https://github.com/cap-kit/capacitor-plugins/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/cap-kit/capacitor-plugins/ci.yml?branch=main&label=CI&logo=github&style=flat-square" alt="CI Status" />
  </a>
  <a href="https://capacitorjs.com/">
    <img src="https://img.shields.io/badge/Capacitor-Plugin-blue?logo=capacitor&style=flat-square" alt="Capacitor Plugin">
  </a>
  <a href="https://www.npmjs.com/package/@cap-kit/people">
    <img src="https://img.shields.io/npm/dm/@cap-kit/people?style=flat-square" alt="Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/npm/l/@cap-kit/people?style=flat-square&logo=open-source-initiative&logoColor=white&color=green" alt="License" />
  </a>
  <img src="https://img.shields.io/maintenance/yes/2026?style=flat-square" alt="Maintained" />
</p>
<br>

## Install

```bash
pnpm add @cap-kit/people
# or
npm install @cap-kit/people
# or
yarn add @cap-kit/people
# then run:
npx cap sync
```

## Apple Privacy Manifest

Apple mandates that app developers specify approved reasons for API usage to enhance user privacy.

This plugin includes a skeleton `PrivacyInfo.xcprivacy` file located in `ios/Sources/PeoplePlugin/PrivacyInfo.xcprivacy`.

**You must populate this file if your plugin uses any [Required Reason APIs](https://developer.apple.com/documentation/bundleresources/privacy_manifest_files/describing_use_of_required_reason_api).**

### Example: User Defaults

If your plugin uses `UserDefaults`, you must declare it in the manifest:

```xml
<dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
    <key>NSPrivacyAccessedAPITypeReasons</key>
    <array>
        <string>CA92.1</string>
    </array>
</dict>

```

For detailed steps, please see the [Capacitor Docs](https://capacitorjs.com/docs/ios/privacy-manifest).

## Configuration

<docgen-config>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Configuration options for the People plugin.

| Prop                 | Type                 | Description                                                                                                                                                                                                                              | Default            | Since |
| -------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ----- |
| **`verboseLogging`** | <code>boolean</code> | Enables verbose native logging. When enabled, additional debug information is printed to the native console (Logcat on Android, Xcode on iOS). This option affects native logging behavior only and has no impact on the JavaScript API. | <code>false</code> | 8.0.0 |

### Examples

In `capacitor.config.json`:

```json
{
  "plugins": {
    "People": {
      "verboseLogging": true
    }
  }
}
```

In `capacitor.config.ts`:

```ts
/// <reference types="@cap-kit/people" />

import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  plugins: {
    People: {
      verboseLogging: true,
    },
  },
};

export default config;
```

</docgen-config>

## Permissions

### Android

This plugin requires the following permissions be added to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />

```

Read about [Setting Permissions](https://capacitorjs.com/docs/android/configuration#setting-permissions) in the [Android Guide](https://capacitorjs.com/docs/android) for more information on setting Android permissions.

### iOS

To use the plugin on iOS, you need to add the following keys to your `Info.plist` file:

#### Contacts

- `NSContactsUsageDescription`
- _Privacy - Contacts Usage Description_

Read about [Configuring `Info.plist`](https://capacitorjs.com/docs/ios/configuration#configuring-infoplist) in the [iOS Guide](https://capacitorjs.com/docs/ios) for more information on setting iOS permissions in Xcode.

### Web

On the Web platform, only the zero-permission contact picker is supported via the Contact Picker API when available.  
All systemic access operations (`getContacts`, `getContact`, `searchPeople`, CRUD operations, group management, and `peopleChange` listeners) are not implemented on Web and will reject as unimplemented.

---

### ✅ Correct Usage

All People plugin APIs are based on Promise and follow the standard Capacitor v8 reject paradigm. Use `try / catch` to handle native cancellations and errors.

```ts
import { People, PeopleErrorCode } from '@cap-kit/people';

try {
  const { contact } = await People.pickContact({
    projection: ['name', 'phones', 'emails'],
  });
  console.log('Picked contact:', contact);
} catch (err: any) {
  if (err.code === PeopleErrorCode.CANCELLED) {
    // User canceled selection
    console.log('Picker cancelled');
  } else {
    console.error('Error:', err.message);
  }
}
```

---

### ❌ Incorrect Usage

Do not use checks based on the `success` property in the result, as the methods reject the Promise on error.

```ts
// ❌ DO NOT DO THIS
const result = await People.pickContact();
if (result.success) { ... }
```

---

## Error Handling

All People plugin methods can reject the Promise if they fail. It is recommended to handle standardized error codes using `PeopleErrorCode`.

### Error Codes

All error codes are standardized and exposed via `PeopleErrorCode`:

- `UNAVAILABLE` – Feature not available or OS limitation
- `CANCELLED` – User cancelled an interactive flow (e.g., contact picker)
- `PERMISSION_DENIED` – Permission denied or restricted
- `INIT_FAILED` – Internal initialization or processing failure
- `INVALID_INPUT` – Invalid, missing, or malformed input
- `UNKNOWN_TYPE` – Invalid or unsupported projection/type

These codes are consistent across **iOS**, **Android**, and **Web**.

### Example

```ts
import { People, PeopleErrorCode } from '@cap-kit/people';

try {
  const { contact } = await People.pickContact();
  console.log('Contact selected:', contact);
} catch (err: any) {
  switch (err.code) {
    case PeopleErrorCode.CANCELLED:
      // The user canceled the selection
      console.log('Picker cancelled by user');
      break;

    case PeopleErrorCode.UNAVAILABLE:
      // The user canceled the selection or the picker is not available
      console.log('Picker unavailable or cancelled by user');
      break;

    case PeopleErrorCode.PERMISSION_DENIED:
      // User has denied access to contacts (for methods that require permissions)
      console.error('Permission to access contacts was denied');
      break;

    case PeopleErrorCode.INIT_FAILED:
      // Internal error while processing native data
      console.error('Native initialization or processing failure');
      break;

    default:
      // Generic or unexpected error
      console.error('An unexpected error occurred:', err.message);
      break;
  }
}
```

---

## API

<docgen-index>

- [`createContact(...)`](#createcontact)
- [`updateContact(...)`](#updatecontact)
- [`deleteContact(...)`](#deletecontact)
- [`mergeContacts(...)`](#mergecontacts)
- [`listGroups()`](#listgroups)
- [`createGroup(...)`](#creategroup)
- [`deleteGroup(...)`](#deletegroup)
- [`addPeopleToGroup(...)`](#addpeopletogroup)
- [`removePeopleFromGroup(...)`](#removepeoplefromgroup)
- [`checkPermissions()`](#checkpermissions)
- [`requestPermissions(...)`](#requestpermissions)
- [`pickContact(...)`](#pickcontact)
- [`getContacts(...)`](#getcontacts)
- [`getContact(...)`](#getcontact)
- [`getCapabilities()`](#getcapabilities)
- [`getPluginVersion()`](#getpluginversion)
- [`searchPeople(...)`](#searchpeople)
- [`addListener('peopleChange', ...)`](#addlistenerpeoplechange-)
- [`addListener(string, ...)`](#addlistenerstring-)
- [`removeAllListeners()`](#removealllisteners)
- [Interfaces](#interfaces)
- [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor People plugin interface.

- This interface defines the contract between the JavaScript layer and the
  native implementations (Android and iOS).

### createContact(...)

```typescript
createContact(options: CreateContactOptions) => Promise<CreateContactResult>
```

[CRUD]
Creates a new contact in the device's address book.

- @throws {PeopleError} PERMISSION_DENIED if contacts permission is missing.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#createcontactoptions">CreateContactOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#createcontactresult">CreateContactResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

try {
  const { contact } = await People.createContact({
    contact: {
      name: { given: 'John', family: 'Appleseed' },
      emails: [{ address: 'john.appleseed@example.com', label: 'work' }],
    },
  });
  console.log('Created contact ID:', contact.id);
} catch (error) {
  console.error('Failed to create contact:', error.code);
}
```

---

### updateContact(...)

```typescript
updateContact(options: UpdateContactOptions) => Promise<UpdateContactResult>
```

[CRUD]
Updates an existing contact using a patch-based approach.

- @throws {PeopleError} PERMISSION_DENIED if permission is missing.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#updatecontactoptions">UpdateContactOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#updatecontactresult">UpdateContactResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

try {
  const { contact } = await People.updateContact({
    contactId: 'some-contact-id',
    contact: {
      organization: { company: 'New Company Inc.' },
    },
  });
  console.log('Updated contact company:', contact.organization?.company);
} catch (error) {
  console.error('Update failed:', error.message);
}
```

---

### deleteContact(...)

```typescript
deleteContact(options: DeleteContactOptions) => Promise<void>
```

[CRUD]
Deletes a contact from the device's address book.
Only contacts owned by the app can be deleted.

- @throws {PeopleError} UNAVAILABLE if deletion fails or contact is not app-owned.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#deletecontactoptions">DeleteContactOptions</a></code> |

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

await People.deleteContact({ contactId: 'contact-id-to-delete' });
```

---

### mergeContacts(...)

```typescript
mergeContacts(options: MergeContactsOptions) => Promise<MergeContactsResult>
```

[CRUD]
Merges a source contact into a destination contact.
The source contact is deleted after the merge.

- @throws {PeopleError} PERMISSION_DENIED if permission is missing.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#mergecontactsoptions">MergeContactsOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#mergecontactsresult">MergeContactsResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

const { contact } = await People.mergeContacts({
  sourceContactId: 'duplicate-contact-id',
  destinationContactId: 'main-contact-id',
});
console.log('Final contact state:', contact);
```

---

### listGroups()

```typescript
listGroups() => Promise<ListGroupsResult>
```

[GROUPS]
Lists all available contact groups.

**Returns:** <code>Promise&lt;<a href="#listgroupsresult">ListGroupsResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
const { groups } = await People.listGroups();
```

---

### createGroup(...)

```typescript
createGroup(options: CreateGroupOptions) => Promise<CreateGroupResult>
```

[GROUPS]
Creates a new contact group.

- @example

```typescript
const { group } = await People.createGroup({ name: 'Family' });
```

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#creategroupoptions">CreateGroupOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#creategroupresult">CreateGroupResult</a>&gt;</code>

**Since:** 8.0.0

---

### deleteGroup(...)

```typescript
deleteGroup(options: DeleteGroupOptions) => Promise<void>
```

[GROUPS]
Deletes a contact group.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#deletegroupoptions">DeleteGroupOptions</a></code> |

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

await People.deleteGroup({ groupId: 'group-id-to-delete' });
```

---

### addPeopleToGroup(...)

```typescript
addPeopleToGroup(options: AddPeopleToGroupOptions) => Promise<void>
```

[GROUPS]
Adds contacts to a group.

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#addpeopletogroupoptions">AddPeopleToGroupOptions</a></code> |

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

await People.addPeopleToGroup({
  groupId: 'group-id',
  contactIds: ['contact-id-1', 'contact-id-2'],
});
```

---

### removePeopleFromGroup(...)

```typescript
removePeopleFromGroup(options: RemovePeopleFromGroupOptions) => Promise<void>
```

[GROUPS]
Removes contacts from a group.

| Param         | Type                                                                                  |
| ------------- | ------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#removepeoplefromgroupoptions">RemovePeopleFromGroupOptions</a></code> |

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

await People.removePeopleFromGroup({
  groupId: 'group-id',
  contactIds: ['contact-id-1'],
});
```

---

### checkPermissions()

```typescript
checkPermissions() => Promise<PeoplePluginPermissions>
```

Check the status of permissions.

**Returns:** <code>Promise&lt;<a href="#peoplepluginpermissions">PeoplePluginPermissions</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';
const permissions = await People.checkPermissions();
console.log(permissions.contacts); // Output: 'granted' | 'denied' | 'prompt'
```

---

### requestPermissions(...)

```typescript
requestPermissions(permissions?: { permissions: 'contacts'[]; } | undefined) => Promise<PeoplePluginPermissions>
```

Request permissions.

| Param             | Type                                        | Description                                                                                                                             |
| ----------------- | ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **`permissions`** | <code>{ permissions: 'contacts'[]; }</code> | - An optional object specifying which permissions to request. If not provided, all permissions defined in the plugin will be requested. |

**Returns:** <code>Promise&lt;<a href="#peoplepluginpermissions">PeoplePluginPermissions</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';
const permissions = await People.requestPermissions();
// OR
// const permissions = await People.requestPermissions({ permissions: ['contacts'] });
console.log(permissions.contacts); // Output: 'granted' | 'denied'
```

---

### pickContact(...)

```typescript
pickContact(options?: { projection?: PeopleProjection[] | undefined; } | undefined) => Promise<PickContactResult>
```

[ZERO-PERMISSION]
Launches the native OS contact picker UI.
This method does NOT require any entries in AndroidManifest.xml or Info.plist
as the user explicitly selects the data via the system UI.

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code>{ projection?: PeopleProjection[]; }</code> |

**Returns:** <code>Promise&lt;<a href="#pickcontactresult">PickContactResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
try {
  const { contact } = await People.pickContact({
    projection: ['name', 'phones', 'emails'],
  });
  console.log('User selected:', contact);
} catch (error) {
  if (error.code === 'CANCELLED') {
    console.log('User cancelled the picker.');
  }
}
```

---

### getContacts(...)

```typescript
getContacts(options?: GetContactsOptions | undefined) => Promise<GetContactsResult>
```

[SYSTEMIC-ACCESS]
Queries the entire contact database with specific projection and pagination.
REQUIRES 'contacts' permission.
Use `includeTotal` only when needed: computing `totalCount` may require scanning/counting across the full contacts set and can be expensive on large address books. Default is `false`.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#getcontactsoptions">GetContactsOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#getcontactsresult">GetContactsResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
const result = await People.getContacts({
  projection: ['name', 'phones'],
  limit: 20,
  offset: 0,
});
```

---

### getContact(...)

```typescript
getContact(options: { id: string; projection?: PeopleProjection[]; }) => Promise<{ contact: UnifiedContact; }>
```

Retrieves a single contact by ID.

- @throws {PeopleError} UNAVAILABLE if contact is not found.

| Param         | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`options`** | <code>{ id: string; projection?: PeopleProjection[]; }</code> |

**Returns:** <code>Promise&lt;{ contact: <a href="#unifiedcontact">UnifiedContact</a>; }&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

const { contact } = await People.getContact({ id: 'contact-id', projection: ['name', 'emails'] });
console.log('Contact details:', contact);
```

---

### getCapabilities()

```typescript
getCapabilities() => Promise<PeopleCapabilities>
```

Returns what this device/implementation is capable of.
Useful for UI adaptation (e.g. hiding "Edit" buttons).

**Returns:** <code>Promise&lt;<a href="#peoplecapabilities">PeopleCapabilities</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

const capabilities = await People.getCapabilities();
console.log('Can Read Contacts:', capabilities.canRead);
console.log('Can Write Contacts:', capabilities.canWrite);
```

---

### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native plugin version.

The returned version corresponds to the native implementation
bundled with the application.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```ts
const { version } = await People.getPluginVersion();
```

---

### searchPeople(...)

```typescript
searchPeople(options: { query: string; projection?: PeopleProjection[]; limit?: number; }) => Promise<GetContactsResult>
```

[SYSTEMIC-ACCESS]
Searches the database with projection.
REQUIRES 'contacts' permission.

| Param         | Type                                                                             |
| ------------- | -------------------------------------------------------------------------------- |
| **`options`** | <code>{ query: string; projection?: PeopleProjection[]; limit?: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#getcontactsresult">GetContactsResult</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

const result = await People.searchPeople({ query: 'John', projection: ['name', 'phones'], limit: 10 });
console.log('Fetched contacts:', result.contacts);
```

---

### addListener('peopleChange', ...)

```typescript
addListener(eventName: 'peopleChange', listenerFunc: (payload: PeopleChangeEvent) => void) => Promise<PluginListenerHandle>
```

Listen for changes in the system address book.
REQUIRES 'contacts' permission.

- @returns A promise that resolves to a handle to remove the listener.

| Param              | Type                                                                                  |
| ------------------ | ------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'peopleChange'</code>                                                           |
| **`listenerFunc`** | <code>(payload: <a href="#peoplechangeevent">PeopleChangeEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

const handle = await People.addListener('peopleChange', (event) => {
  console.log('People change detected:', event.type);
});

// To remove the listener later:
// await handle.remove();
```

---

### addListener(string, ...)

```typescript
addListener(eventName: string, listenerFunc: (...args: unknown[]) => void) => Promise<PluginListenerHandle>
```

Registers a listener for plugin events using a generic event name.

Prefer the typed `peopleChange` overload for full payload type safety.

| Param              | Type                                         |
| ------------------ | -------------------------------------------- |
| **`eventName`**    | <code>string</code>                          |
| **`listenerFunc`** | <code>(...args: unknown[]) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 8.0.0

---

### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all registered listeners for this plugin.

**Since:** 8.0.0

#### Example

```typescript
import { People } from '@cap-kit/people';

await People.removeAllListeners();
```

---

### Interfaces

#### CreateContactResult

Result returned by `createContact`.

| Prop          | Type                                                      | Description                                                           |
| ------------- | --------------------------------------------------------- | --------------------------------------------------------------------- |
| **`contact`** | <code><a href="#unifiedcontact">UnifiedContact</a></code> | The full contact object as it was saved, including its new unique ID. |

#### UnifiedContact

Represents a Unified Person in the directory.
Maps to CNContact (iOS) and Aggregated Contact (Android).

| Prop               | Type                                                                                                                  | Description                                            |
| ------------------ | --------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| **`id`**           | <code>string</code>                                                                                                   | The platform-specific unique identifier (UUID or Long) |
| **`name`**         | <code>{ display: string; given?: string; middle?: string; family?: string; prefix?: string; suffix?: string; }</code> | The unified display name                               |
| **`organization`** | <code>{ company?: string; title?: string; department?: string; }</code>                                               |                                                        |
| **`birthday`**     | <code>{ year?: number; month: number; day: number; }</code>                                                           |                                                        |
| **`phones`**       | <code>PhoneNumber[]</code>                                                                                            |                                                        |
| **`emails`**       | <code>EmailAddress[]</code>                                                                                           |                                                        |
| **`addresses`**    | <code>PostalAddress[]</code>                                                                                          |                                                        |
| **`urls`**         | <code>string[]</code>                                                                                                 |                                                        |
| **`note`**         | <code>string</code>                                                                                                   |                                                        |
| **`image`**        | <code>string</code>                                                                                                   | Base64 thumbnail string (iOS only, only if projected). |

#### PhoneNumber

Phone number representation.

| Prop             | Type                | Description                                       |
| ---------------- | ------------------- | ------------------------------------------------- |
| **`label`**      | <code>string</code> | Normalized label (e.g., 'mobile', 'home', 'work') |
| **`number`**     | <code>string</code> | The raw input string                              |
| **`normalized`** | <code>string</code> | E.164 formatted number (if parsing succeeded)     |

#### EmailAddress

Email address representation.

| Prop          | Type                |
| ------------- | ------------------- |
| **`label`**   | <code>string</code> |
| **`address`** | <code>string</code> |

#### PostalAddress

Postal address representation.

| Prop            | Type                |
| --------------- | ------------------- |
| **`label`**     | <code>string</code> |
| **`formatted`** | <code>string</code> |
| **`street`**    | <code>string</code> |
| **`city`**      | <code>string</code> |
| **`region`**    | <code>string</code> |
| **`postcode`**  | <code>string</code> |
| **`country`**   | <code>string</code> |

#### CreateContactOptions

Options for creating a new contact.
The contact data is provided as a partial <a href="#unifiedcontact">UnifiedContact</a>.

| Prop          | Type                                                                                       | Description                                                                                     |
| ------------- | ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| **`contact`** | <code>Partial&lt;Omit&lt;<a href="#unifiedcontact">UnifiedContact</a>, 'id'&gt;&gt;</code> | The contact data to be saved. At least one writable field (e.g., name, email) must be provided. |

#### UpdateContactResult

Result returned by `updateContact`.

| Prop          | Type                                                      | Description                                                |
| ------------- | --------------------------------------------------------- | ---------------------------------------------------------- |
| **`contact`** | <code><a href="#unifiedcontact">UnifiedContact</a></code> | The full contact object after the update has been applied. |

#### UpdateContactOptions

Options for updating an existing contact.
This operation performs a patch, not a full replacement.

| Prop            | Type                                                                                       | Description                                                                               |
| --------------- | ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------- |
| **`contactId`** | <code>string</code>                                                                        | The unique identifier of the contact to update.                                           |
| **`contact`**   | <code>Partial&lt;Omit&lt;<a href="#unifiedcontact">UnifiedContact</a>, 'id'&gt;&gt;</code> | An object containing the fields to be updated. Only the provided fields will be modified. |

#### DeleteContactOptions

Options for deleting a contact.

| Prop            | Type                | Description                                     |
| --------------- | ------------------- | ----------------------------------------------- |
| **`contactId`** | <code>string</code> | The unique identifier of the contact to delete. |

#### MergeContactsResult

Result returned by `mergeContacts`.

| Prop          | Type                                                      | Description                                           |
| ------------- | --------------------------------------------------------- | ----------------------------------------------------- |
| **`contact`** | <code><a href="#unifiedcontact">UnifiedContact</a></code> | The state of the destination contact after the merge. |

#### MergeContactsOptions

Options for merging two contacts.

| Prop                       | Type                | Description                                                      |
| -------------------------- | ------------------- | ---------------------------------------------------------------- |
| **`sourceContactId`**      | <code>string</code> | The identifier of the contact that will be subsumed and deleted. |
| **`destinationContactId`** | <code>string</code> | The identifier of the contact that will be kept and updated.     |

#### ListGroupsResult

Result returned by `listGroups`.

| Prop         | Type                 | Description                                   |
| ------------ | -------------------- | --------------------------------------------- |
| **`groups`** | <code>Group[]</code> | An array of groups found in the address book. |

#### Group

Represents a group in the address book.
A group can be system-generated (e.g., "All Contacts") or user-created.

| Prop           | Type                 | Description                                                                                                                                                                                                       |
| -------------- | -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`id`**       | <code>string</code>  | A unique identifier for the group. This ID is stable and can be used for subsequent operations.                                                                                                                   |
| **`name`**     | <code>string</code>  | The display name of the group. e.g., "Family", "Work", "Book Club"                                                                                                                                                |
| **`source`**   | <code>string</code>  | The source or account where the group originates. On iOS, this could be "iCloud" or "Local". On Android, this corresponds to the account name (e.g., "user@gmail.com"). For logical groups, this will be 'local'. |
| **`readOnly`** | <code>boolean</code> | Indicates if the group is read-only. System groups are typically read-only and cannot be deleted or renamed.                                                                                                      |

#### CreateGroupResult

Result returned by `createGroup`.

| Prop        | Type                                    | Description              |
| ----------- | --------------------------------------- | ------------------------ |
| **`group`** | <code><a href="#group">Group</a></code> | The newly created group. |

#### CreateGroupOptions

Options for creating a new group.

| Prop       | Type                | Description                 |
| ---------- | ------------------- | --------------------------- |
| **`name`** | <code>string</code> | The name for the new group. |

#### DeleteGroupOptions

Options for deleting a group.

| Prop          | Type                | Description                                   |
| ------------- | ------------------- | --------------------------------------------- |
| **`groupId`** | <code>string</code> | The unique identifier of the group to delete. |

#### AddPeopleToGroupOptions

Options for adding people to a group.

| Prop             | Type                  | Description                                          |
| ---------------- | --------------------- | ---------------------------------------------------- |
| **`groupId`**    | <code>string</code>   | The unique identifier of the group.                  |
| **`contactIds`** | <code>string[]</code> | An array of contact identifiers to add to the group. |

#### RemovePeopleFromGroupOptions

Options for removing people from a group.

| Prop             | Type                  | Description                                               |
| ---------------- | --------------------- | --------------------------------------------------------- |
| **`groupId`**    | <code>string</code>   | The unique identifier of the group.                       |
| **`contactIds`** | <code>string[]</code> | An array of contact identifiers to remove from the group. |

#### PeoplePluginPermissions

Permissions status interface.

| Prop           | Type                                                        |
| -------------- | ----------------------------------------------------------- |
| **`contacts`** | <code><a href="#permissionstate">PermissionState</a></code> |

#### PickContactResult

Result returned by pickContact().
Now strictly returns the contact object on success.

| Prop          | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`contact`** | <code><a href="#unifiedcontact">UnifiedContact</a></code> |

#### GetContactsResult

Result returned by getContacts().

| Prop             | Type                          | Description                                    |
| ---------------- | ----------------------------- | ---------------------------------------------- |
| **`contacts`**   | <code>UnifiedContact[]</code> |                                                |
| **`totalCount`** | <code>number</code>           | Total count in the DB (permissions permitting) |

#### GetContactsOptions

Options for querying contacts.

| Prop               | Type                            | Description                                                                                                                                                  | Default               |
| ------------------ | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------- |
| **`projection`**   | <code>PeopleProjection[]</code> | Array of fields to fetch. MISSING fields will not be read from DB (Performance).                                                                             | <code>['name']</code> |
| **`limit`**        | <code>number</code>             | Max number of records to return                                                                                                                              |                       |
| **`offset`**       | <code>number</code>             | Skip count (implement pagination via cursor usually better, but offset for now)                                                                              |                       |
| **`includeTotal`** | <code>boolean</code>            | Whether to compute totalCount across the full contacts set. This may require scanning/counting the full address book and can be expensive on large datasets. | <code>false</code>    |

#### PeopleCapabilities

Capabilities of the People plugin on this device/implementation.

| Prop                  | Type                 |
| --------------------- | -------------------- |
| **`canRead`**         | <code>boolean</code> |
| **`canWrite`**        | <code>boolean</code> |
| **`canObserve`**      | <code>boolean</code> |
| **`canManageGroups`** | <code>boolean</code> |
| **`canPickContact`**  | <code>boolean</code> |

#### PluginVersionResult

Result object returned by the `getPluginVersion()` method.

| Prop          | Type                | Description                       |
| ------------- | ------------------- | --------------------------------- |
| **`version`** | <code>string</code> | The native plugin version string. |

#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

#### PeopleChangeEvent

Event emitted when changes are detected in the device's address book.

| Prop       | Type                                                          | Description                                                  |
| ---------- | ------------------------------------------------------------- | ------------------------------------------------------------ |
| **`ids`**  | <code>string[]</code>                                         | Array of affected contact IDs (always present, may be empty) |
| **`type`** | <code><a href="#peoplechangetype">PeopleChangeType</a></code> | The type of change detected                                  |

### Type Aliases

#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>

#### PeopleProjection

Supported fields for the Projection Engine.
Requesting only what you need reduces memory usage by O(N).
`image` projection support is iOS-only. On Android and Web, requesting `image` is rejected with `UNKNOWN_TYPE` and message `Unsupported projection field: image`.

<code>'name' | 'organization' | 'birthday' | 'phones' | 'emails' | 'addresses' | 'urls' | 'image' | 'note'</code>

#### PeopleChangeType

Payload delivered to listeners registered for "peopleChange".

- `ids`: always present, contains changed contact IDs (may be empty).
- `type`: one of 'insert' | 'update' | 'delete' (default 'update').
  Current native implementations emit `update`.

<code>'insert' | 'update' | 'delete'</code>

</docgen-api>

---

## Contributing

Contributions are welcome! Please read the [contributing guide](CONTRIBUTING.md) before submitting a pull request.

---

## License

MIT
