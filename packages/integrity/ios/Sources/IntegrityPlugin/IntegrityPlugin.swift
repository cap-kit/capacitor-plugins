import Foundation
import Capacitor
import UIKit

/**
 Capacitor bridge for the Integrity plugin (iOS).

 Responsibilities:
 - Parse JavaScript input
 - Invoke the native implementation
 - Resolve or reject CAPPluginCall exactly once
 - Map native IntegrityError to JS-facing error codes

 Forbidden:
 - Platform-specific business logic
 - System API usage
 - Throwing uncaught exceptions
 */
@objc(IntegrityPlugin)
public final class IntegrityPlugin: CAPPlugin, CAPBridgedPlugin {

    // MARK: - Plugin metadata

    /// The unique identifier for the plugin.
    ///
    /// CONTRACT:
    /// - MUST match the plugin registration name used by Capacitor
    /// - MUST remain stable across releases (breaking change otherwise)
    public let identifier = "IntegrityPlugin"

    /// The name used to reference this plugin in JavaScript.
    ///
    /// CONTRACT:
    /// - MUST match `registerPlugin({ name })` on the JS side
    /// - Changing this value breaks JS ↔ native binding
    public let jsName = "Integrity"

    /**
     A list of methods exposed by this plugin.
     - `check`:
     - `presentBlockPage`:
     - `getPluginVersion`: A method that returns the version of the plugin.

     CONTRACT:
     - Every method listed here MUST:
     - be implemented exactly once
     - resolve or reject its CAPPluginCall exactly once
     - return a Promise on the JS side

     NOTE:
     - No method in this list may perform platform logic directly.
     - All business logic MUST be delegated to the Impl layer.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "check", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "presentBlockPage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    // MARK: - Properties

    /// Native implementation instance.
    ///
    /// NOTE:
    /// - Owned by the Plugin layer
    /// - Lifetime == plugin lifetime
    /// - MUST NOT be recreated per call
    ///
    /// WARNING:
    /// - The implementation MUST NOT access:
    ///   - CAPPluginCall
    ///   - bridge
    ///   - view controllers
    private let implementation = IntegrityImpl()

    // Configuration instance
    //
    // CONTRACT:
    // - Initialized once during load()
    // - Treated as immutable afterwards
    // - NEVER read directly by the Impl layer
    private var config: IntegrityConfig?

    // MARK: - Event-related properties

    /// Buffer for integrity signals captured before a JS listener is registered.
    ///
    /// NOTE:
    /// - Stored in-memory only
    /// - Flushed when the first listener becomes available
    /// - Cleared immediately after delivery
    private var bufferedSignals: [[String: Any]] = []

    /// Canonical event name emitted to the JavaScript layer.
    ///
    /// CONTRACT:
    /// - MUST remain stable (breaking change otherwise)
    /// - MUST match the JS-side event subscription
    private static let integritySignalEvent = "integritySignal"

    // MARK: - Lifecycle

    /**
     Plugin lifecycle entry point.

     CONTRACT:
     - Called exactly once by Capacitor
     - This is the ONLY valid place to:
     - read plugin configuration
     - create IntegrityConfig
     - inject configuration into the Impl layer
     - register system event observers (NotificationCenter)

     WARNING:
     - Calling applyConfig(_:) outside this method is FORBIDDEN
     - Observers registered here MUST be detached in `deinit`
     */
    override public func load() {
        // Initialize IntegrityConfig
        let cfg = IntegrityConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)
        IntegrityLogger.debug("Integrity plugin loaded")

        // Register passive system observers
        addEventObservers()
    }

    /// Plugin teardown.
    ///
    /// NOTE:
    /// - Invoked when the plugin instance is deallocated
    /// - Responsible for detaching all NotificationCenter observers
    deinit {
        removeEventObservers()
    }

    // MARK: - Event emission and buffering

    /**
     Emits an integrity signal to JavaScript or buffers it if no listeners exist.

     - If at least one listener is registered, the signal is emitted immediately.
     - Otherwise, the signal is queued in memory until a listener becomes available.

     - Parameter signal: A fully-formed integrity signal payload.
     */
    private func emitOrBufferSignal(_ signal: [String: Any]) {
        if hasListeners(IntegrityPlugin.integritySignalEvent) {
            notifyListeners(IntegrityPlugin.integritySignalEvent, data: signal, retainUntilConsumed: true)
        } else {
            bufferedSignals.append(signal)
        }
    }

    /**
     Flushes all buffered integrity signals to JavaScript listeners.

     NOTE:
     - This method is idempotent.
     - Signals are delivered in FIFO order.
     - Buffer is cleared immediately after dispatch.
     */
    @objc private func flushBufferedSignals() {
        if hasListeners(IntegrityPlugin.integritySignalEvent) && !bufferedSignals.isEmpty {
            for signal in bufferedSignals {
                notifyListeners(IntegrityPlugin.integritySignalEvent, data: signal, retainUntilConsumed: true)
            }
            bufferedSignals.removeAll()
        }
    }

    // MARK: - NotificationCenter registration

    /**
     Registers passive system observers required for real-time integrity signals.

     NOTE:
     - Observers are scoped strictly to the plugin lifecycle.
     - No polling, timers, or background tasks are introduced.
     */
    private func addEventObservers() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleDidBecomeActiveNotification(_:)),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(flushBufferedSignals),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
    }

    /**
     Detaches all NotificationCenter observers.

     NOTE:
     - This method is invoked exclusively from `deinit`.
     - The SwiftLint rule is suppressed intentionally as the call is lifecycle-safe.
     */
    private func removeEventObservers() {
        // swiftlint:disable notification_center_detachment
        NotificationCenter.default.removeObserver(self)
        // swiftlint:enable notification_center_detachment
    }

    // MARK: - Error mapping

    /**
     Maps native `IntegrityError` values to JS-facing error codes.

     CONTRACT:
     - Error codes MUST be stable and documented
     - Error codes MUST match across platforms
     - Platform-specific error codes are FORBIDDEN
     */
    private func reject(
        _ call: CAPPluginCall,
        error: IntegrityError
    ) {
        let code: String

        switch error {
        case .unavailable:
            code = "UNAVAILABLE"
        case .permissionDenied:
            code = "PERMISSION_DENIED"
        case .initFailed:
            code = "INIT_FAILED"
        case .unknownType:
            code = "UNKNOWN_TYPE"
        }

        call.reject(error.message, code)
    }

    // MARK: - Integrity check

    /**
     Executes a baseline integrity check.

     CONTRACT:
     - Resolves exactly once on success
     - Rejects exactly once on failure
     - Never throws outside this scope

     NOTE:
     - All option normalization happens here.
     - The Impl layer MUST receive fully normalized options.
     */
    @objc func check(_ call: CAPPluginCall) {
        do {
            let options =
                try call.decode(IntegrityCheckOptions.self)

            let normalizedOptions = IntegrityCheckOptions(
                level: options.level ?? "basic",
                includeDebugInfo: options.includeDebugInfo ?? false
            )

            let result =
                try implementation.performCheck(
                    options: normalizedOptions
                )

            call.resolve(result)

        } catch let error as IntegrityError {
            reject(call, error: error)
        } catch {
            call.reject(
                "Unexpected native error during integrity check.",
                "INIT_FAILED"
            )
        }
    }

    // MARK: - Present block page

    /**
     Presents the configured integrity block page, if enabled.

     CONTRACT:
     - UI decisions are delegated to the host app
     - This method only performs presentation when explicitly requested

     WARNING:
     - UI work MUST be dispatched to the main thread
     - Failure to access the root view controller MUST reject the call
     */
    @objc func presentBlockPage(_ call: CAPPluginCall) {
        guard
            let blockPage = config?.blockPage,
            blockPage.enabled,
            let baseURL = blockPage.url
        else {
            call.resolve(["presented": false])
            return
        }

        let reason = call.getString("reason")
        let dismissible = call.getBool("dismissible") ?? false

        let finalURL =
            reason != nil
            ? "\(baseURL)?reason=\(reason!)"
            : baseURL

        DispatchQueue.main.async {
            // CONTRACT:
            // All UIKit interactions MUST occur on the main thread.
            guard let rootVC = self.bridge?.viewController else {
                call.reject(
                    "View controller not available",
                    "UNAVAILABLE"
                )
                return
            }

            let blockVC = IntegrityBlockViewController(
                url: finalURL,
                dismissible: dismissible
            )
            let nav = UINavigationController(rootViewController: blockVC)
            rootVC.present(nav, animated: true) {
                call.resolve(["presented": true])
            }
        }
    }

    // MARK: - Version

    /**
     Returns the native plugin version.

     - Used exclusively for diagnostics and compatibility checks.
     - Must not be used for feature detection.
     */
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

    // MARK: - Notification handlers

    /**
     Handles application foreground transitions.

     PURPOSE:
     - Detects runtime integrity changes (e.g. debugger attachment)
     when the app becomes active.

     NOTE:
     - Failures are logged but NOT propagated to JS.
     - This is a background signal, not a direct API invocation.
     */
    @objc private func handleDidBecomeActiveNotification(_ notification: Notification) {
        // Trigger an integrity check on app foreground (e.g., for debugger attachment)
        let options = IntegrityCheckOptions(
            // Use standard level for event-driven checks
            level: "standard",
            includeDebugInfo: false
        )

        do {
            let result = try implementation.performCheck(options: options)
            emitOrBufferSignal(result)
        } catch {
            IntegrityLogger.error(
                "handleDidBecomeActiveNotification: Error during integrity check:",
                error.localizedDescription
            )
        }
    }
}
