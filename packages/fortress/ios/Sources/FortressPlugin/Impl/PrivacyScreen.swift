import Foundation
import UIKit

// Conform to Sendable for Swift 6 strict concurrency
final class PrivacyScreen: @unchecked Sendable {
    private let privacyViewLock = NSLock()
    private var internalPrivacyView: UIView?

    /**
     * Finds the active window using UIWindowScene (iOS 15+ compliant).
     * Marked as @MainActor for safe UI access.
     */
    @MainActor
    private func getActiveWindow() -> UIWindow? {
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
    }

    func lock() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.privacyViewLock.lock()
            defer { self.privacyViewLock.unlock() }

            guard self.internalPrivacyView == nil else { return }

            Task { @MainActor in
                if let window = self.getActiveWindow() {
                    let blurEffect = UIBlurEffect(style: .extraLight)
                    let blurValue = UIVisualEffectView(effect: blurEffect)
                    blurValue.frame = window.bounds
                    blurValue.tag = 9961
                    blurValue.alpha = 0

                    window.addSubview(blurValue)

                    UIView.animate(withDuration: 0.2) {
                        blurValue.alpha = 1.0
                    }
                    self.internalPrivacyView = blurValue
                }
            }
        }
    }

    func unlock() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            // Logica: Usiamo il lock per proteggere l'accesso a internalPrivacyView
            self.privacyViewLock.lock()
            defer { self.privacyViewLock.unlock() }

            Task { @MainActor in
                // Cerchiamo la vista usando sia il riferimento salvato che il tag per sicurezza
                let viewToRemove = self.internalPrivacyView ?? self.getActiveWindow()?.viewWithTag(9961)

                if let blurView = viewToRemove {
                    // Algoritmo: Animazione di dissolvenza (Fade-out)
                    UIView.animate(withDuration: 0.2, animations: {
                        blurView.alpha = 0
                    }, completion: { _ in
                        // Pulizia fisica della gerarchia delle viste
                        blurView.removeFromSuperview()
                    })
                }

                // Reset atomico del riferimento
                self.internalPrivacyView = nil
            }
        }
    }
}
