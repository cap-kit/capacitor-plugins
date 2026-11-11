import Foundation
import UIKit

/**
 Utility helpers for the SplashMotion plugin.

 Keeping a dedicated utilities file helps maintain a clean
 separation between core logic and helper code.
 */
struct SplashMotionUtils {

    /**
     Returns the application's key window, searching connected foreground
     scenes first (iOS 13+ scene lifecycle) and falling back to the legacy
     `UIApplicationDelegate.window` when no scene window is available.

     - Returns: the key window, or nil when none can be resolved.
     */
    static func keyWindow() -> UIWindow? {
        if #available(iOS 15.0, *) {
            let scenes = UIApplication.shared.connectedScenes
            let windowScene = scenes.first { $0.activationState == .foregroundActive } as? UIWindowScene
            if let keyWindow = windowScene?.windows.first(where: { $0.isKeyWindow }) {
                return keyWindow
            }
        }
        return UIApplication.shared.delegate?.window ?? nil
    }

    /**
     Maps a validated haptic style name to the matching impact feedback style.

     Callers must only pass values accepted by
     `SplashMotionValidators.normalizeHapticStyle`.
     */
    static func impactStyle(for hapticStyle: String) -> UIImpactFeedbackGenerator.FeedbackStyle {
        switch hapticStyle {
        case "light":
            return .light
        case "heavy":
            return .heavy
        default:
            return .medium
        }
    }
}
