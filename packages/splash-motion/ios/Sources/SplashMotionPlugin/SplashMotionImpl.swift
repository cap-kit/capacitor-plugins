import Foundation
import UIKit
import Lottie

// The native implementation is an 880-line file with a 569-line class body,
// beyond the SwiftLint style limits (400 lines per file, 350 per type body).
// Splitting the surface composition out of the coordinator is design-level
// work tracked as a follow-up — this sweep keeps the cohesive implementation
// intact. Control comments must sit between doc and declaration, orphaning
// the doc (SwiftLint 0.65.1); the disable:next comments below are
// single-rule and line-scoped, no re-enables.
// swiftlint:disable:next orphaned_doc_comment
/**
 * Platform-specific native implementation for the SplashMotion plugin.
 *
 * Renders a full-screen splash surface (background color, optionally a
 * Lottie animation) with fade transitions, optional haptic feedback and
 * auto-hide scheduling. MUST NOT depend on Capacitor bridge APIs or
 * CAPPluginCall objects.
 *
 * Responsibilities:
 * - Show/hide the splash surface with fade in/out durations.
 * - Render a Lottie animation when `lottie` options are provided.
 * - Fire haptic feedback on visibility transitions.
 * - Auto-hide after `showDuration` when `autoHide` is enabled.
 */
// swiftlint:disable:next type_body_length
final class SplashMotionImpl {

    /**
     Full-screen gradient background view backed by a CAGradientLayer so
     the gradient resizes with its container under Auto Layout.
     */
    private final class GradientView: UIView {
        override static var layerClass: AnyClass {
            CAGradientLayer.self
        }
    }

    // MARK: - Properties

    /// Cached plugin configuration. Applied once during the load phase.
    private var config: SplashMotionConfig?

    /// Dedicated splash window. Retained while the splash is visible.
    private var splashWindow: UIWindow?

    /// Pending auto-hide work item, when scheduled.
    private var autoHideWork: DispatchWorkItem?
    /// Pending activation observer for a haptic deferred until the app
    /// reaches the foreground active state (iOS suppresses haptics before
    /// that). Cleared when the haptic fires or the splash is dismissed.
    private var hapticActivationObserver: NSObjectProtocol?
    /// When the launch splash was requested; drives the minimum hold time.
    private var launchShownAt: Date?
    /// Duration (seconds) of the launch lottie animation, captured when the
    /// animation loads. Clamps the launch minimum hold so the splash is
    /// never released mid-animation.
    private var launchLottieDuration: TimeInterval?
    /// Hide request received before the launch splash was presented.
    private var pendingHide: HideOptions?

    // MARK: - Configuration

    /**
     Applies the plugin configuration to the implementation layer.

     MUST be called exactly once during the plugin load phase.
     */
    func applyConfig(_ config: SplashMotionConfig) {
        self.config = config
        SplashMotionLogger.verbose = config.verboseLogging
        SplashMotionLogger.debug("Configuration applied. Verbose logging:", String(config.verboseLogging))
        // Guard rail: the launch lottie takes precedence over the static
        // launch image (renderLottie/renderStaticImage are mutually
        // exclusive); report the ignored configuration instead of letting
        // it silently do nothing.
        if config.launchLottie != nil && config.launchImageExplicitlyConfigured {
            SplashMotionLogger.warn(
                "launchLottie is configured together with a static launch image (imageResourceName); " +
                "the animated launch lottie takes precedence and the static image is ignored"
            )
        }
    }

    // MARK: - Splash control

    /// Whether the splash surface is currently visible.
    var isVisible: Bool {
        splashWindow?.isHidden == false
    }

    /**
     Shows the splash surface.

     When a splash is already visible, the surface is kept and only the
     auto-hide schedule and haptic feedback are refreshed.

     - Parameter options: Parsed show options.
     */
    func show(options: ShowOptions) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }

            if self.isVisible {
                SplashMotionLogger.debug("show() called while visible: refreshing schedule")
                self.scheduleAutoHide(options: options)
                self.fireHaptic(options.haptic)
                return
            }
            self.presentSplashWhenSceneReady(options: options, attempt: 0)
        }
    }

    /**
     Presents the splash once a window scene is available.

     Scene-based apps connect their window scene right after
     `didFinishLaunching` returns; the plugin `load()` phase runs before
     that, so `connectedScenes` can still be empty at launch. Retry on the
     main queue for a bounded time so the splash window gets a scene
     assigned and actually renders (a scene-less window never displays).
     Non-scene apps fall through after the timeout and present without a
     scene, matching the pre-scene behavior.
     */
    private func presentSplashWhenSceneReady(
        options: ShowOptions,
        attempt: Int
    ) {
        if Self.foregroundWindowScene() == nil && attempt < Constants.maxSceneWaitRetries {
            DispatchQueue.main.asyncAfter(deadline: .now() + Constants.sceneWaitRetryInterval) {
                [weak self] in
                self?.presentSplashWhenSceneReady(options: options, attempt: attempt + 1)
            }
            return
        }
        presentSplash(options: options)
        // The launch hide() can arrive while the splash presentation is
        // still pending (scene wait): apply the remembered request now.
        if let pending = pendingHide {
            pendingHide = nil
            dismissHonoringMinimumHold(options: pending)
        }
    }

    /**
     Hides the splash surface with a fade out.

     - Parameter options: Parsed hide options.
     */
    func hide(options: HideOptions) {
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.cancelAutoHide()
            if self.isVisible {
                self.dismissHonoringMinimumHold(options: options)
            } else if self.launchShownAt != nil {
                // Launch splash presentation still pending (scene wait):
                // remember the request and apply it once presented, so the
                // hide call is not consumed by a window that does not exist
                // yet (the splash would otherwise stay on screen forever).
                self.pendingHide = options
            } else {
                self.dismissSplash(fadeOutDuration: options.fadeOutDuration, haptic: options.haptic)
            }
        }
    }

    /**
     Dismisses the splash honoring the launch minimum hold: when the launch
     splash was shown with `launchAutoHide` off, `launchShowDuration` is the
     minimum time the splash stays visible, so an early hide() (fast gate,
     quick WebView boot) is deferred until the minimum elapses. Manual
     show()/hide() cycles are unaffected: the launch marker is cleared on
     the first dismissal.
     */
    private func dismissHonoringMinimumHold(options: HideOptions) {
        let remaining = launchMinimumHoldRemaining()
        if remaining > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + remaining) { [weak self] in
                self?.dismissLaunchSplash(options: options)
            }
            return
        }
        dismissLaunchSplash(options: options)
    }

    private func dismissLaunchSplash(options: HideOptions) {
        launchShownAt = nil
        launchLottieDuration = nil
        pendingHide = nil
        dismissSplash(fadeOutDuration: options.fadeOutDuration, haptic: options.haptic)
    }

    /**
     Remaining time the launch splash must stay visible under the minimum
     hold rule; zero when there is no launch marker or auto-hide is on
     (in that mode launchShowDuration already controls the schedule).
     Config durations are milliseconds; the result is seconds. The hold is
     clamped to the launch lottie duration so a loaded launch animation
     always plays to completion before the splash is released.
     */
    private func launchMinimumHoldRemaining() -> TimeInterval {
        guard let shownAt = launchShownAt, let config, !config.launchAutoHide else { return 0 }
        let minimumSeconds = max(
            Double(SplashMotionValidators.clampDuration(config.launchShowDuration, fallback: 0)) / 1000.0,
            launchLottieDuration ?? 0
        )
        let elapsed = -shownAt.timeIntervalSinceNow
        return max(0, minimumSeconds - elapsed)
    }

    // MARK: - Launch behavior

    /**
     Shows the launch splash when static configuration requests it, honoring
     `launchShowDuration`, `launchAutoHide`, `launchLottie` and `launchHaptic`.

     Called from the plugin load phase.
     */
    func showLaunchSplash() {
        guard let config else { return }

        let options = ShowOptions(
            autoHide: config.launchAutoHide,
            fadeInDuration: 0,
            fadeOutDuration: config.launchFadeOutDuration,
            showDuration: config.launchShowDuration,
            lottie: config.launchLottie,
            haptic: config.launchHaptic
        )

        SplashMotionLogger.debug("Showing launch splash. Duration:", String(config.launchShowDuration))
        // With launchAutoHide off, launchShowDuration acts as the minimum
        // time the splash stays visible (see hide(options:)), measured from
        // this request.
        launchShownAt = Date()
        show(options: options)
    }

    // MARK: - Native mechanics

    // The surface composition is a single cohesive unit: background, content
    // layers and entrance animation are applied to the same view hierarchy.
    // swiftlint:disable:next function_body_length
    private func presentSplash(options: ShowOptions) {
        let isDark = Self.isDarkMode()

        let splashViewController = UIViewController()
        guard let contentView = splashViewController.view else { return }
        applyBackground(to: contentView, options: options, isDark: isDark)

        // All content layers live inside a single transparent container so
        // the entrance animation covers them together, above the background.
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(container)
        NSLayoutConstraint.activate([
            container.topAnchor.constraint(equalTo: contentView.topAnchor),
            container.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
            container.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            container.trailingAnchor.constraint(equalTo: contentView.trailingAnchor)
        ])

        renderImageLayer(
            into: container,
            image: options.backgroundImage ?? config?.backgroundImage,
            isDark: isDark,
            fillsSurface: true,
            defaultGravity: nil,
            defaultMargin: 0
        )

        if let lottie = options.lottie {
            renderLottie(into: container, options: lottie)
        } else {
            renderStaticImage(into: container, override: options.image, isDark: isDark)
        }

        renderImageLayer(
            into: container,
            image: options.brandingImage ?? config?.brandingImage,
            isDark: isDark,
            fillsSurface: false,
            defaultGravity: Constants.defaultBrandingGravity,
            defaultMargin: Constants.defaultBrandingMargin
        )

        renderSpinner(into: container, options: options)

        let window = UIWindow(frame: UIScreen.main.bounds)
        // Scene-based apps (iOS 13+ with UIApplicationSceneManifest): a
        // UIWindow without an assigned scene is never attached to the
        // display and stays invisible. Assign the foreground scene before
        // making the window key.
        window.windowScene = Self.foregroundWindowScene() ?? Self.anyWindowScene()
        window.rootViewController = splashViewController
        window.windowLevel = .alert + 1
        contentView.alpha = 0
        window.makeKeyAndVisible()

        self.splashWindow = window

        let fadeInDuration = SplashMotionValidators.clampDuration(
            options.fadeInDuration,
            fallback: Self.defaultFadeDuration
        )
        UIView.animate(withDuration: Double(fadeInDuration) / 1000.0) {
            contentView.alpha = 1
        }

        applyEntrance(to: container, entrance: options.entrance ?? config?.entrance)

        fireHaptic(options.haptic)
        scheduleAutoHide(options: options)
        SplashMotionLogger.debug("Splash shown")
    }

    /**
     Resolves a usable foreground window scene for the splash window:
     foreground-active first, then foreground-inactive (the state during
     launch activation). Scenes in other states (unattached, background)
     are not usable for display.
     */
    private static func foregroundWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first { $0.activationState == .foregroundActive }
            ?? scenes.first { $0.activationState == .foregroundInactive }
    }

    /**
     Fallback scene resolution for when the wait budget expired without a
     usable foreground scene: any foreground scene first, then the first
     scene, mirroring the pre-scene behavior when nothing matches.
     */
    private static func anyWindowScene() -> UIWindowScene? {
        foregroundWindowScene()
            ?? UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
    }

    /**
     Renders the loading spinner as the topmost centered layer of the
     content container, mirroring the official plugin behavior
     (UIActivityIndicatorView, official style mapping, optional hex tint).
     It lives inside the container so the entrance animation covers it, and
     it disappears with the surface on hide.
     */
    private func renderSpinner(into container: UIView, options: ShowOptions) {
        let perCall = options.spinner
        let show: Bool
        let styleName: String?
        let colorHex: String?
        if let perCall {
            // Per-call convention: the object replaces the whole static
            // spinner configuration; passing it implies the spinner is
            // shown unless `show` is explicitly false.
            show = perCall.show ?? true
            styleName = perCall.style
            colorHex = perCall.color
        } else {
            show = config?.showSpinner ?? false
            styleName = config?.iosSpinnerStyle
            colorHex = config?.spinnerColor
        }
        guard show else { return }

        let spinner = UIActivityIndicatorView(
            style: SplashMotionValidators.parseSpinnerStyle(styleName)
        )
        if let color = SplashMotionValidators.parseColor(colorHex) {
            spinner.color = color
        }
        spinner.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: container.centerYAnchor)
        ])
        spinner.startAnimating()
        SplashMotionLogger.debug("Spinner rendered")
    }

    /**
     Applies the splash surface background: a vertical gradient when
     configured and valid, otherwise the flat background color (with dark
     mode support).
     */
    private func applyBackground(to view: UIView, options: ShowOptions, isDark: Bool) {
        if let gradient = options.backgroundGradient ?? config?.backgroundGradient,
           let colors = SplashMotionValidators.resolveGradient(
               top: gradient.top,
               bottom: gradient.bottom,
               topDark: gradient.topDark,
               bottomDark: gradient.bottomDark,
               isDark: isDark
           ) {
            let gradientView = GradientView()
            gradientView.translatesAutoresizingMaskIntoConstraints = false
            view.addSubview(gradientView)
            NSLayoutConstraint.activate([
                gradientView.topAnchor.constraint(equalTo: view.topAnchor),
                gradientView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
                gradientView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
                gradientView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
            ])
            if let gradientLayer = gradientView.layer as? CAGradientLayer {
                gradientLayer.colors = [colors.0.cgColor, colors.1.cgColor]
                gradientLayer.startPoint = CGPoint(x: 0.5, y: 0.0)
                gradientLayer.endPoint = CGPoint(x: 0.5, y: 1.0)
            }
            return
        }

        view.backgroundColor = resolveSplashBackgroundColor(isDark: isDark)
    }

    // Six parameters describe one image layer (source, theme, fill mode,
    // gravity and margin); bundling them would hide the call sites instead
    // of clarifying them.
    // swiftlint:disable:next orphaned_doc_comment
    /**
     Renders one image layer inside the content container.

     When `fillsSurface` is true the view fills the whole container and
     gravity/margin are ignored (background layer and legacy static image).
     Otherwise the view keeps its intrinsic image size and is positioned by
     gravity.
     */
    // swiftlint:disable:next function_parameter_count
    private func renderImageLayer(
        into container: UIView,
        image: ImageOptions?,
        isDark: Bool,
        fillsSurface: Bool,
        defaultGravity: String?,
        defaultMargin: Double
    ) {
        guard let image else { return }

        let source = isDark ? (image.darkSource ?? image.source) : image.source
        guard let uiImage = Self.resolveUIImage(for: source) else {
            SplashMotionLogger.debug("Splash image resource not found:", source)
            return
        }

        let imageView = UIImageView(image: uiImage)
        imageView.contentMode = SplashMotionValidators.parseContentMode(image.scaleType)
        imageView.alpha = SplashMotionValidators.normalizeAlpha(image.alpha)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(imageView)

        if fillsSurface {
            NSLayoutConstraint.activate([
                imageView.topAnchor.constraint(equalTo: container.topAnchor),
                imageView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                imageView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                imageView.trailingAnchor.constraint(equalTo: container.trailingAnchor)
            ])
            return
        }

        let margin = SplashMotionValidators.normalizeMargin(image.margin, fallback: defaultMargin)
        let anchor = SplashMotionValidators.parseGravity(image.gravity ?? defaultGravity)

        var constraints: [NSLayoutConstraint] = []
        switch anchor.vertical {
        case .top:
            constraints.append(imageView.topAnchor.constraint(equalTo: container.topAnchor, constant: margin))
        case .bottom:
            constraints.append(imageView.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -margin))
        case .center:
            constraints.append(imageView.centerYAnchor.constraint(equalTo: container.centerYAnchor))
        }
        switch anchor.horizontal {
        case .leading:
            constraints.append(imageView.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: margin))
        case .trailing:
            constraints.append(imageView.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -margin))
        case .center:
            constraints.append(imageView.centerXAnchor.constraint(equalTo: container.centerXAnchor))
        }
        NSLayoutConstraint.activate(constraints)
    }

    /**
     Resolves a splash image source into a UIImage: a value containing "/"
     is decoded from the web asset tree, a plain name is first looked up in
     the asset catalog / bundle root.
     */
    private static func resolveUIImage(for source: String) -> UIImage? {
        if source.contains("/") {
            guard let url = bundleFileURL(for: source) else { return nil }
            return UIImage(contentsOfFile: url.path)
        }
        return UIImage(named: source)
    }

    /**
     Renders the static splash image, either the per-call override or the
     static configuration (with dark-mode resource selection).
     */
    private func renderStaticImage(into container: UIView, override: ImageOptions?, isDark: Bool) {
        guard let config else { return }

        let image: ImageOptions
        if let override {
            image = override
        } else {
            // Legacy behavior (no override, no imageGravity): the image
            // view fills the surface and androidScaleType scales it,
            // unchanged from the official plugin (default FIT_XY). With a
            // gravity, the view keeps its intrinsic image size and a null
            // scale type falls back to CENTER_CROP.
            let fillsSurface = config.imageGravity == nil
            let scaleType: String?
            if fillsSurface && config.androidScaleType == nil {
                scaleType = "FIT_XY"
            } else {
                scaleType = config.androidScaleType
            }
            image = ImageOptions(
                source: isDark ? (config.imageResourceNameDark ?? config.imageResourceName) : config.imageResourceName,
                gravity: config.imageGravity,
                scaleType: scaleType
            )
        }

        let fillsSurface = override == nil && config.imageGravity == nil
        renderImageLayer(
            into: container,
            image: image,
            isDark: isDark,
            fillsSurface: fillsSurface,
            defaultGravity: Constants.defaultContentGravity,
            defaultMargin: 0
        )
    }

    /**
     Applies the content entrance animation (fade or scale) to the whole
     content container.
     */
    private func applyEntrance(to container: UIView, entrance: EntranceOptions?) {
        let type = SplashMotionValidators.normalizeEntranceType(entrance?.type)
        guard type != "none" else { return }

        let duration = Double(SplashMotionValidators.clampEntranceDuration(entrance?.durationMs)) / 1000.0
        if type == "fade" {
            container.alpha = 0
            UIView.animate(withDuration: duration) {
                container.alpha = 1
            }
        } else {
            container.alpha = 0
            container.transform = CGAffineTransform(scaleX: 0.85, y: 0.85)
            UIView.animate(withDuration: duration) {
                container.alpha = 1
                container.transform = .identity
            }
        }
        SplashMotionLogger.debug("Entrance animation applied:", type)
    }

    private func renderLottie(into parentView: UIView, options: LottieOptions) {
        // Launch clamp: only the launch surface (config launchLottie while
        // the launch marker is active) extends the launch minimum hold.
        let isLaunchSurface = launchShownAt != nil && config?.launchLottie != nil

        // Dark mode picks the darkSource variant when provided.
        let source = Self.isDarkMode() ? (options.darkSource ?? options.source) : options.source

        if source.hasSuffix(".lottie") {
            renderDotLottie(into: parentView, source: source, options: options, isLaunchSurface: isLaunchSurface)
            return
        }

        if source.hasPrefix("http://") || source.hasPrefix("https://") {
            guard let url = URL(string: source) else {
                SplashMotionLogger.error("Invalid Lottie URL: " + source)
                return
            }
            SplashMotionLogger.debug("Lottie loading from URL")
            // Download closure is dispatched on the main queue by Lottie.
            LottieAnimation.loadedFrom(url: url) { [weak self] animation in
                guard let self, let animation else {
                    SplashMotionLogger.error("Lottie animation failed to load from URL: " + source)
                    return
                }
                self.configureAndPlay(
                    LottieAnimationView(animation: animation),
                    parentView: parentView,
                    options: options,
                    isLaunchSurface: isLaunchSurface
                )
            }
            return
        }

        guard let animationData = Self.bundleAssetData(for: source),
              let animation = try? LottieAnimation.from(data: animationData) else {
            SplashMotionLogger.error("Lottie resource not found in bundle: " + source)
            return
        }
        SplashMotionLogger.debug("Lottie loading from bundle:", source)
        configureAndPlay(
            LottieAnimationView(animation: animation),
            parentView: parentView,
            options: options,
            isLaunchSurface: isLaunchSurface
        )
    }

    /**
     Renders a dotLottie (.lottie) archive: a zip container whose selected
     animation JSON is extracted first and then parsed as a normal
     composition. Local sources are read from the app bundle; remote sources
     are downloaded with URLSession and the result is dispatched back to the
     main queue.
     */
    private func renderDotLottie(
        into parentView: UIView,
        source: String,
        options: LottieOptions,
        isLaunchSurface: Bool
    ) {
        let handleArchive: (Data?) -> Void = { [weak self] zipData in
            guard let zipData,
                  let jsonData = SplashMotionZipReader.extractAnimationJSON(from: zipData),
                  let animation = try? LottieAnimation.from(data: jsonData) else {
                SplashMotionLogger.error("dotLottie archive contains no animation JSON: " + source)
                return
            }
            DispatchQueue.main.async {
                let animationView = LottieAnimationView(animation: animation)
                self?.configureAndPlay(
                    animationView,
                    parentView: parentView,
                    options: options,
                    isLaunchSurface: isLaunchSurface
                )
            }
        }

        if source.hasPrefix("http://") || source.hasPrefix("https://") {
            guard let url = URL(string: source) else {
                SplashMotionLogger.error("Invalid dotLottie URL: " + source)
                return
            }
            SplashMotionLogger.debug("dotLottie loading from URL")
            URLSession.shared.dataTask(with: url) { data, _, _ in
                handleArchive(data)
            }.resume()
            return
        }

        guard let bundleURL = Self.bundleFileURL(for: source),
              let zipData = try? Data(contentsOf: bundleURL) else {
            SplashMotionLogger.error("dotLottie resource not found in bundle: " + source)
            return
        }
        SplashMotionLogger.debug("dotLottie loading from bundle:", source)
        handleArchive(zipData)
    }

    /**
     Applies layout, speed, loop mode and playback to a ready Lottie view.

     The animation view is pinned to the full parent surface so a
     full-canvas splash composition (e.g. 375x748 with a baked background)
     covers the whole screen. The composition scales with aspect fit, so
     devices whose screen aspect differs from the composition letterbox;
     configure backgroundColor/backgroundColorDark to the composition's
     baked background color to make those bands invisible.
     */
    private func configureAndPlay(
        _ animationView: LottieAnimationView,
        parentView: UIView,
        options: LottieOptions,
        isLaunchSurface: Bool = false
    ) {
        animationView.translatesAutoresizingMaskIntoConstraints = false
        animationView.contentMode = .scaleAspectFit
        parentView.addSubview(animationView)

        NSLayoutConstraint.activate([
            animationView.topAnchor.constraint(equalTo: parentView.topAnchor),
            animationView.bottomAnchor.constraint(equalTo: parentView.bottomAnchor),
            animationView.leadingAnchor.constraint(equalTo: parentView.leadingAnchor),
            animationView.trailingAnchor.constraint(equalTo: parentView.trailingAnchor)
        ])

        let speed = CGFloat(options.speed ?? Self.defaultLottieSpeed)
        animationView.animationSpeed = max(0.1, min(speed, 10.0))
        animationView.loopMode = (options.loop ?? Self.defaultLottieLoop) ? .loop : .playOnce
        // Launch clamp: record the launch animation's real duration so the
        // launch minimum hold never releases the splash mid-animation.
        if isLaunchSurface, let duration = animationView.animation?.duration {
            launchLottieDuration = duration
        }
        animationView.play()
    }

    /**
     Resolves the splash background color honoring dark mode: the dark
     variant wins when the system is in dark mode and is configured.
     */
    private func resolveSplashBackgroundColor(isDark: Bool) -> UIColor {
        guard let config else { return .systemBackground }
        if isDark,
           let darkHex = config.backgroundColorDark,
           let darkColor = SplashMotionValidators.parseColor(darkHex) {
            return darkColor
        }
        if let hex = config.backgroundColor,
           let color = SplashMotionValidators.parseColor(hex) {
            return color
        }
        return .systemBackground
    }

    /**
     Detects the current system theme: the key window trait collection
     first, falling back to the main screen.
     */
    private static func isDarkMode() -> Bool {
        let style = SplashMotionUtils.keyWindow()?.traitCollection.userInterfaceStyle
            ?? UIScreen.main.traitCollection.userInterfaceStyle
        return style == .dark
    }

    /**
     Resolves a JS-facing asset path into bundle data.

     Web asset sources such as `public/lottie/splash.json` live inside the
     `public` folder reference of the app bundle (Capacitor copies the web
     dir preserving its structure), so the lookup uses a subdirectory path.
     A source without the `public/` prefix is resolved from the bundle root
     (useful for resources shipped by the app itself).
     */
    private static func bundleAssetData(for source: String) -> Data? {
        let relativePath = source.hasPrefix("public/") ? String(source.dropFirst("public/".count)) : source
        let directory = (relativePath as NSString).deletingLastPathComponent
        let fileName = (relativePath as NSString).lastPathComponent
        let baseName = (fileName as NSString).deletingPathExtension
        let fileExtension = (fileName as NSString).pathExtension

        let url: URL?
        if directory.isEmpty {
            url = Bundle.main.url(forResource: baseName, withExtension: fileExtension)
        } else {
            url = Bundle.main.url(forResource: baseName, withExtension: fileExtension, subdirectory: "public/" + directory)
        }
        guard let url else { return nil }
        return try? Data(contentsOf: url)
    }

    /**
     Resolves a JS-facing source path into a bundle URL.

     Web asset sources such as `public/lottie/splash.lottie` live inside the
     `public` folder reference of the app bundle, so the lookup uses a
     subdirectory path. Used for both dotLottie archives and image assets.
     */
    private static func bundleFileURL(for source: String) -> URL? {
        let relativePath = source.hasPrefix("public/") ? String(source.dropFirst("public/".count)) : source
        let directory = (relativePath as NSString).deletingLastPathComponent
        let fileName = (relativePath as NSString).lastPathComponent
        let baseName = (fileName as NSString).deletingPathExtension
        let fileExtension = (fileName as NSString).pathExtension

        if directory.isEmpty {
            return Bundle.main.url(forResource: baseName, withExtension: fileExtension)
        }
        return Bundle.main.url(forResource: baseName, withExtension: fileExtension, subdirectory: "public/" + directory)
    }

    private func dismissSplash(fadeOutDuration: Int?, haptic: HapticOptions?) {
        guard let window = splashWindow, let rootView = window.rootViewController?.view else {
            return
        }

        // A haptic deferred until activation is meaningless once the splash
        // is gone: drop it instead of firing into an empty surface.
        cancelPendingHapticActivation()

        fireHaptic(haptic)

        let duration = SplashMotionValidators.clampDuration(
            fadeOutDuration,
            fallback: Self.defaultFadeDuration
        )

        UIView.animate(
            withDuration: Double(duration) / 1000.0,
            animations: {
                rootView.alpha = 0
            },
            completion: { [weak self] _ in
                window.isHidden = true
                window.rootViewController = nil
                if self?.splashWindow === window {
                    self?.splashWindow = nil
                }
                SplashMotionLogger.debug("Splash hidden")
            }
        )
    }

    private func scheduleAutoHide(options: ShowOptions) {
        cancelAutoHide()

        let autoHide = options.autoHide ?? Self.defaultAutoHide
        guard autoHide else { return }

        let showDuration = SplashMotionValidators.clampDuration(
            options.showDuration,
            fallback: Self.defaultShowDuration
        )

        let work = DispatchWorkItem { [weak self] in
            self?.dismissSplash(fadeOutDuration: options.fadeOutDuration, haptic: nil)
        }
        autoHideWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(showDuration) / 1000.0, execute: work)
    }

    private func cancelAutoHide() {
        autoHideWork?.cancel()
        autoHideWork = nil
    }

    // MARK: - Haptics

    private func fireHaptic(_ haptic: HapticOptions?) {
        guard let haptic else { return }

        // UIKit suppresses UIFeedbackGenerator output unless the app is in
        // the foreground active state (expected behavior per Apple, dev
        // forums thread 681512). During launch the splash presents while
        // the scene is still foregroundInactive, so an immediate impact
        // would be silently swallowed: defer it until the app becomes
        // active, keeping the haptic inside the splash window (the launch
        // minimum hold guarantees the surface is still up by then).
        if UIApplication.shared.applicationState == .active {
            performHapticImpact(haptic)
            return
        }
        guard hapticActivationObserver == nil else { return }
        hapticActivationObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard let self else { return }
            self.cancelPendingHapticActivation()
            // The splash may have been dismissed while inactive: only fire
            // when the surface is still visible.
            guard self.splashWindow != nil else { return }
            self.performHapticImpact(haptic)
        }
    }

    private func performHapticImpact(_ haptic: HapticOptions) {
        let style = SplashMotionValidators.normalizeHapticStyle(haptic.style)
        let generator = UIImpactFeedbackGenerator(style: SplashMotionUtils.impactStyle(for: style))
        generator.prepare()
        generator.impactOccurred()
        SplashMotionLogger.debug("Haptic fired:", style)
    }

    private func cancelPendingHapticActivation() {
        if let token = hapticActivationObserver {
            NotificationCenter.default.removeObserver(token)
        }
        hapticActivationObserver = nil
    }

    // MARK: - Constants

    private enum Constants {
        static let defaultFadeDuration = 200
        static let defaultShowDuration = 3000
        static let defaultAutoHide = true
        /// Scene-wait retry budget: ~2s at ~16ms per attempt.
        static let maxSceneWaitRetries = 120
        static let sceneWaitRetryInterval: Double = 0.016
        static let defaultLottieSpeed: Double = 1.0
        static let defaultLottieLoop = true
        static let defaultContentGravity = "center"
        static let defaultBrandingGravity = "bottomEnd"
        static let defaultBrandingMargin: Double = 16
    }

    /// Compile-time-checked access to the shared constants.
    private static let defaultFadeDuration = Constants.defaultFadeDuration
    private static let defaultShowDuration = Constants.defaultShowDuration
    private static let defaultAutoHide = Constants.defaultAutoHide
    private static let defaultLottieSpeed = Constants.defaultLottieSpeed
    private static let defaultLottieLoop = Constants.defaultLottieLoop

    // Cohesive native implementation; file_length is style-only and splitting
    // is design-level work tracked as a follow-up.
    // swiftlint:disable:next file_length
}
