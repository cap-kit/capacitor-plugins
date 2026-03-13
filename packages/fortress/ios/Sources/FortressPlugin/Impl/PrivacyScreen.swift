import Foundation
import UIKit

/*
 iOS privacy-screen helper.

 Responsibilities:
 - Render a full-screen blur overlay when secure content must be hidden
 - Render optional text and/or image overlay on top of blur
 - Remove the overlay when secure content can be shown
 - Forward tap gestures to unlock callbacks
 */

// MARK: - Overlay Configuration

struct OverlayConfig {
    var text: String = ""
    var showText: Bool = true
    var textColor: UIColor = .white
    var imageName: String = ""
    var showImage: Bool = true
    var backgroundOpacity: CGFloat = -1
    var theme: String = "system"
}

// Conform to Sendable for Swift 6 strict concurrency
final class PrivacyScreen: @unchecked Sendable {
    private let privacyViewLock = NSLock()
    private var internalPrivacyView: UIView?
    private var overlayLabel: UILabel?
    private var overlayImageView: UIImageView?
    private var onTapUnlock: (@MainActor () -> Void)?

    // Privacy overlay configuration
    private var config = OverlayConfig()

    /**
     * Updates the privacy overlay configuration.
     */
    func updateOverlayConfig(_ newConfig: OverlayConfig) {
        self.config = newConfig

        // Update existing overlay elements if visible
        Task { @MainActor in
            self.updateOverlayElements()
        }
    }

    /**
     * Legacy method for backward compatibility (text only).
     */
    func updateOverlayConfig(
        text: String,
        showText: Bool,
        textColor: String,
        backgroundOpacity: Double
    ) {
        var newConfig = OverlayConfig()
        newConfig.text = text
        newConfig.showText = showText

        if !textColor.isEmpty {
            newConfig.textColor = UIColor(hex: textColor) ?? .white
        }

        if backgroundOpacity >= 0 && backgroundOpacity <= 1 {
            newConfig.backgroundOpacity = CGFloat(backgroundOpacity)
        }

        self.updateOverlayConfig(newConfig)
    }

    /**
     * Sets the callback to be called when the user taps on the privacy screen.
     */
    func setOnTapUnlock(_ callback: @MainActor @escaping () -> Void) {
        self.onTapUnlock = callback
    }

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

    /**
     * Updates the overlay label and visibility.
     */
    @MainActor
    private func updateOverlayElements() {
        // Update label
        if let label = overlayLabel {
            label.text = config.text
            label.isHidden = config.text.isEmpty || !config.showText
            label.textColor = config.textColor
        }

        // Update image
        if let imageView = overlayImageView {
            if !config.imageName.isEmpty && config.showImage {
                imageView.image = UIImage(named: config.imageName)
                imageView.isHidden = false
            } else {
                imageView.isHidden = true
            }
        }

        if let blurView = internalPrivacyView as? UIVisualEffectView {
            blurView.effect = UIBlurEffect(style: resolvedBlurStyle())
            if config.backgroundOpacity >= 0 && config.backgroundOpacity <= 1 {
                blurView.backgroundColor = UIColor.black.withAlphaComponent(config.backgroundOpacity)
            } else {
                blurView.backgroundColor = .clear
            }
        }
    }

    @MainActor
    private func resolvedBlurStyle() -> UIBlurEffect.Style {
        switch config.theme {
        case "light":
            return .systemMaterialLight
        case "dark":
            return .systemMaterialDark
        default:
            return .systemMaterial
        }
    }

    /**
     Creates and configures the overlay stack (text + optional image).
     Returns the container view with all overlay elements.
     */
    @MainActor
    private func createOverlayStack() -> UIStackView {
        let stackView = UIStackView()
        stackView.axis = .vertical
        stackView.alignment = .center
        stackView.distribution = .equalSpacing
        stackView.spacing = 16
        stackView.translatesAutoresizingMaskIntoConstraints = false

        // Create image view (top if both visible)
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.isHidden = config.imageName.isEmpty || !config.showImage

        if !config.imageName.isEmpty && config.showImage {
            imageView.image = UIImage(named: config.imageName)
        }

        // Set image constraints
        imageView.widthAnchor.constraint(lessThanOrEqualToConstant: 120).isActive = true
        imageView.heightAnchor.constraint(lessThanOrEqualToConstant: 120).isActive = true

        // Create label (bottom if both visible)
        let label = UILabel()
        label.text = config.text
        label.textColor = config.textColor
        label.font = UIFont.systemFont(ofSize: 24, weight: .semibold)
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        label.isHidden = config.text.isEmpty || !config.showText

        // Add to stack based on configuration
        // Image goes first (top), then text (bottom)
        if !config.imageName.isEmpty && config.showImage {
            stackView.addArrangedSubview(imageView)
        }
        if !config.text.isEmpty && config.showText {
            stackView.addArrangedSubview(label)
        }

        self.overlayImageView = imageView
        self.overlayLabel = label

        return stackView
    }

    /**
     Shows the privacy blur overlay on the active window with optional text and/or image.
     */
    func lock() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.privacyViewLock.lock()
            defer { self.privacyViewLock.unlock() }

            guard self.internalPrivacyView == nil else { return }

            Task { @MainActor in
                if let window = self.getActiveWindow() {
                    let blurEffect = UIBlurEffect(style: self.resolvedBlurStyle())
                    let blurView = UIVisualEffectView(effect: blurEffect)
                    blurView.frame = window.bounds
                    blurView.tag = 9961
                    blurView.alpha = 0
                    blurView.isUserInteractionEnabled = true

                    // Apply custom background opacity if set
                    if self.config.backgroundOpacity >= 0 && self.config.backgroundOpacity <= 1 {
                        blurView.backgroundColor = UIColor.black.withAlphaComponent(self.config.backgroundOpacity)
                    }

                    // Create and configure overlay stack (text + optional image)
                    let overlayStack = self.createOverlayStack()

                    blurView.contentView.addSubview(overlayStack)
                    NSLayoutConstraint.activate([
                        overlayStack.centerXAnchor.constraint(
                            equalTo: blurView.contentView.centerXAnchor
                        ),
                        overlayStack.centerYAnchor.constraint(
                            equalTo: blurView.contentView.centerYAnchor
                        ),
                        overlayStack.leadingAnchor.constraint(
                            greaterThanOrEqualTo: blurView.contentView.leadingAnchor,
                            constant: 20
                        ),
                        overlayStack.trailingAnchor.constraint(
                            lessThanOrEqualTo: blurView.contentView.trailingAnchor,
                            constant: -20
                        )
                    ])

                    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.handleTap))
                    blurView.addGestureRecognizer(tapGesture)

                    window.addSubview(blurView)

                    UIView.animate(withDuration: 0.2) {
                        blurView.alpha = 1.0
                    }
                    self.internalPrivacyView = blurView
                }
            }
        }
    }

    @MainActor
    @objc private func handleTap() {
        onTapUnlock?()
    }

    /**
     Hides and removes the privacy blur overlay from the active window.
     */
    func unlock() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            self.privacyViewLock.lock()
            defer { self.privacyViewLock.unlock() }

            Task { @MainActor in
                let viewToRemove = self.internalPrivacyView ?? self.getActiveWindow()?.viewWithTag(9961)

                if let blurView = viewToRemove {
                    UIView.animate(withDuration: 0.2, animations: {
                        blurView.alpha = 0
                    }, completion: { _ in
                        blurView.removeFromSuperview()
                    })
                }

                self.internalPrivacyView = nil
                self.overlayLabel = nil
                self.overlayImageView = nil
            }
        }
    }
}

// MARK: - UIColor Hex Extension

extension UIColor {
    convenience init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else {
            return nil
        }

        let length = hexSanitized.count
        var red: CGFloat = 0.0
        var green: CGFloat = 0.0
        var blue: CGFloat = 0.0
        var alpha: CGFloat = 1.0

        if length == 6 {
            red = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
            green = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
            blue = CGFloat(rgb & 0x0000FF) / 255.0
        } else if length == 8 {
            red = CGFloat((rgb & 0xFF000000) >> 24) / 255.0
            green = CGFloat((rgb & 0x00FF0000) >> 16) / 255.0
            blue = CGFloat((rgb & 0x0000FF00) >> 8) / 255.0
            alpha = CGFloat(rgb & 0x000000FF) / 255.0
        } else {
            return nil
        }

        self.init(red: red, green: green, blue: blue, alpha: alpha)
    }
}
