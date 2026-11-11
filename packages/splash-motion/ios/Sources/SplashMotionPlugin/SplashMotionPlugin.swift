import Foundation
import Capacitor

/**
 Capacitor bridge for the SplashMotion plugin.

 Responsibilities:
 - Parse JavaScript input
 - Call the native implementation
 - Resolve or reject CAPPluginCall
 - Map native errors to JS-facing error codes
 */
@objc(SplashMotionPlugin)
public final class SplashMotionPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    public let identifier = "SplashMotionPlugin"

    /// The name used to reference this plugin in JavaScript.
    public let jsName = "SplashMotion"

    /**
     A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "show", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hide", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation containing platform-specific logic.
    let implementation = SplashMotionImpl()

    /// Configuration instance
    var config: SplashMotionConfig?

    // MARK: - Lifecycle

    override public func load() {
        let cfg = SplashMotionConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)

        implementation.showLaunchSplash()

        SplashMotionLogger.debug("Plugin loaded. Version: ", PluginVersion.number)
    }

    // MARK: - Version

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Splash Motion

    /**
     Shows the splash screen with optional Lottie animation and haptic feedback.

     The `show` options extend the official `@capacitor/splash-screen`
     `ShowOptions` shape (`autoHide`, `fadeInDuration`, `fadeOutDuration`,
     `showDuration`) with the SplashMotion `lottie` and `haptic` objects.
     */
    @objc func show(_ call: CAPPluginCall) {
        let options = ShowOptions(
            autoHide: call.getBool("autoHide"),
            fadeInDuration: call.getInt("fadeInDuration"),
            fadeOutDuration: call.getInt("fadeOutDuration"),
            showDuration: call.getInt("showDuration"),
            lottie: Self.parseLottie(call.getObject("lottie")),
            image: ImageOptions.parse(call.getObject("image")),
            backgroundImage: ImageOptions.parse(call.getObject("backgroundImage")),
            brandingImage: ImageOptions.parse(call.getObject("brandingImage")),
            entrance: EntranceOptions.parse(call.getObject("entrance")),
            backgroundGradient: BackgroundGradientOptions.parse(call.getObject("backgroundGradient")),
            spinner: SpinnerOptions.parse(call.getObject("spinner")),
            haptic: Self.parseHaptic(call.getObject("haptic"))
        )
        implementation.show(options: options)
        call.resolve()
    }

    /**
     Hides the splash screen with an optional fade out and haptic feedback.
     */
    @objc func hide(_ call: CAPPluginCall) {
        let options = HideOptions(
            fadeOutDuration: call.getInt("fadeOutDuration"),
            haptic: Self.parseHaptic(call.getObject("haptic"))
        )
        implementation.hide(options: options)
        call.resolve()
    }

    // MARK: - Option Parsing

    private static func parseLottie(_ options: [AnyHashable: Any]?) -> LottieOptions? {
        guard let options, let source = options["source"] as? String else {
            return nil
        }
        return LottieOptions(
            source: source,
            darkSource: options["darkSource"] as? String,
            loop: options["loop"] as? Bool,
            speed: options["speed"] as? Double
        )
    }

    private static func parseHaptic(_ options: [AnyHashable: Any]?) -> HapticOptions? {
        guard let options else {
            return nil
        }
        return HapticOptions(style: options["style"] as? String)
    }
}
