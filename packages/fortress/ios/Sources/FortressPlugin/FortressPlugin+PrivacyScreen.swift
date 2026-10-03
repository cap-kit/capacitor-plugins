import Foundation
import Capacitor

extension FortressPlugin {
    // MARK: - Privacy Screen (manual runtime control — Ionic API parity)

    /**
     Enables privacy-screen protection independently of the vault lock state.

     Accepts the official `PrivacyScreenConfig` shape (`android` / `ios`
     display knobs) for API compatibility. Visual style is governed by the
     Fortress overlay configuration (`privacyOverlay*`); the knobs are
     validated for shape and stored client-side, protection itself is applied
     through the Fortress overlay + snapshot masking.
     */
    @objc func enable(_ call: CAPPluginCall) {
        // Capacitor flattens the JS argument: `{ android, ios }` arrive top-level.
        // Knobs are compat-accepted (style stays Fortress-driven); log when present.
        if call.getObject("android") != nil || call.getObject("ios") != nil {
            Logger.debug("Privacy enable with platform config (compat-accepted)")
        }
        implementation.setPrivacyScreenManualOverride(true)
        implementation.setPrivacyScreenVisible(true)
        call.resolve(["success": true])
    }

    @objc func disable(_ call: CAPPluginCall) {
        implementation.setPrivacyScreenManualOverride(false)
        implementation.setPrivacyScreenVisible(false)
        call.resolve(["success": true])
    }

    @objc func isEnabled(_ call: CAPPluginCall) {
        call.resolve(["enabled": implementation.isPrivacyScreenEnabled()])
    }
}
