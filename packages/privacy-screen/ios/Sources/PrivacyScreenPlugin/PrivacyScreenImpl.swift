import Foundation
import UIKit

/**
 Native iOS implementation for the PrivacyScreen plugin.

 Pure platform logic, isolated from the Capacitor bridge:
 - MUST NOT access CAPPluginCall.
 - MUST NOT depend on Capacitor bridge APIs directly.
 - MUST perform UI operations on the main thread.

 Behavior (extracted from the Fortress privacy engine, adapted to the
 standalone contract):
 - `enable` renders a full-screen blur overlay on the key window so app
   content never appears in the app-switcher snapshot.
 - The blur style follows the `ios.blurEffect` session option
   (`light` / `dark`, defaulting to the system style). A `none` value maps
   to the system style: unlike the official plugin, this standalone does
   not re-show the launch storyboard, it keeps a neutral blur.
 */
final class PrivacyScreenImpl: @unchecked Sendable {

    // MARK: - Properties

    /// Cached plugin configuration containing logging flags.
    private var config: PrivacyScreenConfig?

    /// Current protection state. Source of truth for `isEnabled`.
    private var enabled = false

    /// Session blur style requested via `enable({ ios: { blurEffect } })`.
    private var blurEffect: String = "system"

    private let overlayLock = NSLock()
    private var overlayView: UIView?

    private static let overlayTag = 9961

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     MUST be called exactly once from the Plugin bridge layer during `load()`.
     */
    func applyConfig(_ config: PrivacyScreenConfig) {
        self.config = config
        PrivacyScreenLogger.verbose = config.verboseLogging
        PrivacyScreenLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Protection control

    /// Enables protection with the requested blur style.
    func enable(blurEffect: String?) {
        if let blurEffect, ["light", "dark", "none"].contains(blurEffect) {
            self.blurEffect = blurEffect
        }
        enabled = true
        showOverlay()
    }

    /// Disables protection and removes the overlay.
    func disable() {
        enabled = false
        hideOverlay()
    }

    /// Current protection state.
    func isEnabled() -> Bool {
        return enabled
    }

    // MARK: - Overlay

    @MainActor
    private func resolvedBlurStyle() -> UIBlurEffect.Style {
        switch blurEffect {
        case "light":
            return .systemMaterialLight
        case "dark":
            return .systemMaterialDark
        default:
            return .systemMaterial
        }
    }

    @MainActor
    private func keyWindow() -> UIWindow? {
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
    }

    private func showOverlay() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.overlayLock.lock()
            defer { self.overlayLock.unlock() }

            guard self.overlayView == nil else { return }

            Task { @MainActor in
                guard let window = self.keyWindow() else { return }

                let blurView = UIVisualEffectView(effect: UIBlurEffect(style: self.resolvedBlurStyle()))
                blurView.frame = window.bounds
                blurView.tag = Self.overlayTag
                blurView.alpha = 0
                blurView.isUserInteractionEnabled = false

                window.addSubview(blurView)
                UIView.animate(withDuration: 0.2) {
                    blurView.alpha = 1.0
                }
                self.overlayView = blurView
            }
        }
    }

    private func hideOverlay() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            self.overlayLock.lock()
            defer { self.overlayLock.unlock() }

            Task { @MainActor in
                let viewToRemove = self.overlayView ?? self.keyWindow()?.viewWithTag(Self.overlayTag)

                if let blurView = viewToRemove {
                    UIView.animate(withDuration: 0.2, animations: {
                        blurView.alpha = 0
                    }, completion: { _ in
                        blurView.removeFromSuperview()
                    })
                }

                self.overlayView = nil
            }
        }
    }
}
