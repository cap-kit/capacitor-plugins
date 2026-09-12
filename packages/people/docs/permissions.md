# People — Permissions

This guide covers the permissions required by the People plugin on each platform: Android manifest entries, iOS `Info.plist` keys, and Web platform behavior.

## Android

This plugin requires the following permissions be added to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />

```

Read about [Setting Permissions](https://capacitorjs.com/docs/android/configuration#setting-permissions) in the [Android Guide](https://capacitorjs.com/docs/android) for more information on setting Android permissions.

## iOS

To use the plugin on iOS, you need to add the following keys to your `Info.plist` file:

### Contacts

- `NSContactsUsageDescription`
- _Privacy - Contacts Usage Description_

Read about [Configuring `Info.plist`](https://capacitorjs.com/docs/ios/configuration#configuring-infoplist) in the [iOS Guide](https://capacitorjs.com/docs/ios) for more information on setting iOS permissions in Xcode.

## Web

On the Web platform, only the zero-permission contact picker is supported via the Contact Picker API when available.  
All systemic access operations (`getContacts`, `getContact`, `searchPeople`, CRUD operations, group management, and `peopleChange` listeners) are not implemented on Web and will reject as unimplemented.
