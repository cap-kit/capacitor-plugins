import Foundation
import UIKit

/**
 Utility helpers for resolving iOS Settings URLs.

 This type contains ONLY mapping logic and must not:
 - interact with Capacitor
 - open URLs
 - perform side effects
 */
enum SettingsUtils {

    static func resolveSettingsURL(for option: String?) -> URL? {
        guard let option else {
            return nil
        }

        let settingsPaths: [String: String] = [
            "about": "App-prefs:General&path=About",
            "autoLock": "App-prefs:General&path=AUTOLOCK",
            "bluetooth": "App-prefs:Bluetooth",
            "dateTime": "App-prefs:General&path=DATE_AND_TIME",
            "facetime": "App-prefs:FACETIME",
            "general": "App-prefs:General",
            "keyboard": "App-prefs:General&path=Keyboard",
            "iCloud": "App-prefs:CASTLE",
            "iCloudStorageBackup": "App-prefs:CASTLE&path=STORAGE_AND_BACKUP",
            "international": "App-prefs:General&path=INTERNATIONAL",
            "locationServices": "App-prefs:Privacy&path=LOCATION",
            "music": "App-prefs:MUSIC",
            "notes": "App-prefs:NOTES",
            "notifications": "App-prefs:NOTIFICATIONS_ID",
            "phone": "App-prefs:Phone",
            "photos": "App-prefs:Photos",
            "managedConfigurationList": "App-prefs:General&path=ManagedConfigurationList",
            "reset": "App-prefs:General&path=Reset",
            "ringtone": "App-prefs:Sounds&path=Ringtone",
            "sounds": "App-prefs:Sounds",
            "softwareUpdate": "App-prefs:General&path=SOFTWARE_UPDATE_LINK",
            "store": "App-prefs:STORE",
            "tracking": "App-prefs:Privacy&path=USER_TRACKING",
            "wallpaper": "App-prefs:Wallpaper",
            "wifi": "App-prefs:WIFI",
            "tethering": "App-prefs:INTERNET_TETHERING",
            "doNotDisturb": "App-prefs:DO_NOT_DISTURB",
            "touchIdPasscode": "App-prefs:TOUCHID_PASSCODE",
            "screenTime": "App-prefs:SCREEN_TIME",
            "accessibility": "App-prefs:ACCESSIBILITY",
            "vpn": "App-prefs:VPN"
        ]

        if let path = settingsPaths[option] {
            return URL(string: path)
        }

        if option == "app" {
            return URL(string: UIApplication.openSettingsURLString)
        }

        if option == "appNotification" {
            if #available(iOS 16.0, *) {
                return URL(string: UIApplication.openNotificationSettingsURLString)
            }
            return URL(string: UIApplication.openSettingsURLString)
        }

        return nil
    }
}
