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
 - `enable` arms protection; the blur cover is rendered on the key window on
   `willResignActive` (before the app-switcher snapshot is captured) and
   removed when the app returns to the foreground.
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

    /// Read-only view of the current session blur style.
    /// Exposed for diagnostics and headless tests; never mutated externally.
    var activeBlurEffect: String {
        return blurEffect
    }

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
    ///
    /// The cover is not shown while the app is active; it appears on
    /// willResignActive (app switcher, Control Center, notification shade)
    /// and is removed when the app comes back.
    func enable(blurEffect: String?) {
        if PrivacyScreenValidators.isValidBlurEffect(blurEffect) {
            self.blurEffect = blurEffect ?? "system"
        }
        enabled = true
        DispatchQueue.main.async { [weak self] in
            guard let self = self, self.enabled else { return }
            // If protection is enabled while the app is not fully active
            // (backgrounded or inactive, e.g. launched straight into the
            // switcher), the cover must be visible immediately; otherwise
            // it waits for the next willResignActive event.
            if UIApplication.shared.applicationState != .active {
                self.showOverlay()
            }
        }
    }

    /// Disables protection and removes the cover.
    func disable() {
        enabled = false
        hideOverlay()
    }

    /// Current protection state.
    func isEnabled() -> Bool {
        return enabled
    }

    // MARK: - App lifecycle

    /// Shows the app-switcher cover when protection is enabled.
    ///
    /// Triggered by willResignActive so the cover is on screen before the
    /// system captures the app-switcher snapshot.
    func onAppWillResignActive() {
        guard enabled else { return }
        showOverlay()
    }

    /// Removes the app-switcher cover when the app returns to the foreground.
    func onAppWillEnterForeground() {
        guard enabled else { return }
        hideOverlay()
    }

    /// Removes the cover when the app becomes active again without having
    /// gone to the background (Control Center, notification banner, call
    /// banner). Pairs with willResignActive, which showed it; without this
    /// observer the cover would stay on screen forever in those cases.
    func onAppDidBecomeActive() {
        guard enabled else { return }
        hideOverlay()
    }

    // MARK: - Overlay

    @MainActor
    private func resolvedBlurStyle() -> UIBlurEffect.Style {
        return PrivacyScreenUtils.blurStyle(for: blurEffect)
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
                // No fade-in: the cover must be fully opaque as soon as it is
                // added, so the system snapshot never catches a mid-fade frame.
                blurView.alpha = 1.0
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
                // Prefer the tracked reference; fall back to a scan across ALL
                // windows so a transient key-window mismatch during the
                // foreground transition can never orphan the cover on screen.
                let viewToRemove = self.overlayView
                    ?? UIApplication.shared.connectedScenes
                        .compactMap { $0 as? UIWindowScene }
                        .flatMap { $0.windows }
                        .first { $0.viewWithTag(Self.overlayTag) != nil }?
                        .viewWithTag(Self.overlayTag)

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
