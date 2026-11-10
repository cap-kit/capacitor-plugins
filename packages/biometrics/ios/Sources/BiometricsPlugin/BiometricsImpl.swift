import Foundation
import LocalAuthentication
import Security

/**
 Native iOS implementation for the Biometrics plugin.

 Pure platform logic, isolated from the Capacitor bridge:
 - MUST NOT access CAPPluginCall.
 - MUST NOT depend on Capacitor bridge APIs directly.

 All iOS biometry is strong-class, so `strongBiometryIsAvailable` mirrors
 availability and weak strength is unreachable.
 */
final class BiometricsImpl: @unchecked Sendable {

    // MARK: - Properties

    /// Cached plugin configuration containing logging flags.
    private var config: BiometricsConfig?

    /// Credential class of the last successful ceremony in this session.
    var lastAuthenticationType = "unknown"

    /**
     Reference box for the in-flight authentication context.

     `LAContext.evaluatePolicy` has no external cancel handle: the only way
     to dismiss an ongoing system prompt programmatically is
     `invalidate()` on the same context.
     */
    private final class ActiveContextBox: @unchecked Sendable {
        private let lock = NSLock()
        private var context: LAContext?

        func set(_ context: LAContext?) {
            lock.lock()
            defer { lock.unlock() }
            self.context = context
        }

        func invalidate() {
            lock.lock()
            let current = context
            context = nil
            lock.unlock()
            current?.invalidate()
        }
    }

    private let activeContext = ActiveContextBox()

    private final class CompletionRelay: @unchecked Sendable {
        private let completion: (Result<Void, Error>) -> Void

        init(completion: @escaping (Result<Void, Error>) -> Void) {
            self.completion = completion
        }

        func call(_ result: Result<Void, Error>) {
            completion(result)
        }
    }

    // MARK: - Configuration

    /**
     Applies static plugin configuration.

     MUST be called exactly once from the Plugin bridge layer during `load()`.
     */
    func applyConfig(_ config: BiometricsConfig) {
        self.config = config
        BiometricsLogger.verbose = config.verboseLogging

        BiometricsLogger.debug(
            "Configuration applied. Verbose logging:",
            config.verboseLogging
        )
    }

    // MARK: - Authentication

    /**
     Policy selection for the system ceremony.

     When the device passcode may satisfy the ceremony the broader
     policy is used; otherwise the ceremony accepts biometrics only.
     */
    static func policy(forAllowPasscode allowPasscode: Bool) -> LAPolicy {
        return allowPasscode
            ? .deviceOwnerAuthentication
            : .deviceOwnerAuthenticationWithBiometrics
    }

    /**
     Fallback button title for the system prompt.

     The system fallback title stays at its default when the passcode
     is offered; an empty title hides the fallback button when
     biometrics-only is requested.
     */
    static func fallbackTitle(forAllowPasscode allowPasscode: Bool) -> String? {
        return allowPasscode ? nil : ""
    }

    /**
     Credential class reported for a successful ceremony.

     When the passcode may satisfy the ceremony the concrete method is
     unknown; a biometrics-only ceremony always resolved through
     biometry.
     */
    static func authenticationType(forAllowPasscode allowPasscode: Bool) -> String {
        return allowPasscode ? "unknown" : "biometric"
    }

    /**
     Runs the system biometric ceremony.

     - Parameter reason: Message displayed to the user.
     - Parameter cancelTitle: Localized cancel button title.
     - Parameter allowPasscode: When true the device passcode is offered as
       fallback; otherwise biometrics only.
     */
    func authenticate(
        reason: String,
        cancelTitle: String = "Cancel",
        allowPasscode: Bool = true,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let context = LAContext()
        context.localizedCancelTitle = cancelTitle
        context.localizedFallbackTitle = Self.fallbackTitle(forAllowPasscode: allowPasscode)

        let policy: LAPolicy = Self.policy(forAllowPasscode: allowPasscode)

        var error: NSError?
        guard context.canEvaluatePolicy(policy, error: &error) else {
            if let laError = error {
                completion(.failure(Self.mapLAError(laError)))
                return
            }
            completion(.failure(BiometricsError.unavailable(BiometricsErrorMessages.unavailable)))
            return
        }

        let relay = CompletionRelay(completion: completion)
        activeContext.set(context)

        context.evaluatePolicy(policy, localizedReason: reason) { success, authError in
            self.activeContext.set(nil)
            if success {
                self.lastAuthenticationType = Self.authenticationType(forAllowPasscode: allowPasscode)
                relay.call(.success(()))
                return
            }

            if let nsError = authError as NSError? {
                relay.call(.failure(Self.mapLAError(nsError)))
                return
            }

            relay.call(.failure(BiometricsError.initFailed(BiometricsErrorMessages.internalError)))
        }
    }

    /**
     Dismisses the ongoing authentication prompt, if any.
     */
    func cancelActiveAuthentication() {
        activeContext.invalidate()
    }

    // MARK: - Secure storage (biometry-bound)

    /**
     Stores a value in the biometry-bound secure store.

     Prompt-free and synchronous: the system applies the access
     control at read time instead of at write time.
     */
    func setSecureData(
        key: String,
        value: String,
        accessControl: SecAccessControlCreateFlags,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        completion(Result { try BiometricsSecureStore.storeValue(value, key: key, accessControl: accessControl) })
    }

    /**
     Reads a value from the biometry-bound secure store.

     The Keychain read blocks while the biometric prompt is up, so it
     runs on a background queue and relays the result on the main
     queue (Capgo mechanism).
     */
    func getSecureData(
        key: String,
        reason: String?,
        fallbackTitle: String?,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Result {
                try BiometricsSecureStore.readValue(key: key, reason: reason, fallbackTitle: fallbackTitle)
            }
            DispatchQueue.main.async {
                completion(result)
            }
        }
    }

    /**
     Deletes the value stored for `key` without prompting (idempotent).
     */
    func deleteSecureData(key: String, completion: @escaping (Result<Void, Error>) -> Void) {
        completion(Result { try BiometricsSecureStore.deleteValue(key: key) })
    }

    /**
     Reports whether a value exists for `key` without prompting.

     `errSecInteractionNotAllowed` counts as present.
     */
    func isSecureDataSaved(key: String, completion: @escaping (Result<Bool, Error>) -> Void) {
        completion(.success(BiometricsSecureStore.itemExists(key: key)))
    }

    // MARK: - Capability status

    /**
     Capability snapshot backing every availability getter.
     */
    func status() -> [String: Any] {
        let deviceContext = LAContext()
        var deviceError: NSError?
        let isDeviceSecure = deviceContext.canEvaluatePolicy(.deviceOwnerAuthentication, error: &deviceError)

        let biometricContext = LAContext()
        var biometricError: NSError?
        let canEvaluateBiometrics = biometricContext.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &biometricError
        )

        let isBiometricsAvailable: Bool
        let isBiometricsEnabled: Bool

        if canEvaluateBiometrics {
            isBiometricsAvailable = true
            isBiometricsEnabled = true
        } else if let laError = biometricError as? LAError {
            switch laError.code {
            case .biometryNotEnrolled, .passcodeNotSet:
                isBiometricsAvailable = true
                isBiometricsEnabled = false
            default:
                isBiometricsAvailable = false
                isBiometricsEnabled = false
            }
        } else {
            isBiometricsAvailable = false
            isBiometricsEnabled = false
        }

        let biometryType = Self.resolveBiometryType(context: biometricContext, available: isBiometricsAvailable)

        return [
            "isBiometricsAvailable": isBiometricsAvailable,
            "isBiometricsEnabled": isBiometricsEnabled,
            "isDeviceSecure": isDeviceSecure,
            "biometryType": biometryType,
            "biometryTypes": biometryType == "none" ? [] : [biometryType],
            "strongBiometryIsAvailable": isBiometricsAvailable
        ]
    }

    /**
     Resolves the reported biometry type for a capability snapshot.

     All iOS biometry is strong-class, so the type is reported
     verbatim when available and as "none" otherwise.
     */
    static func resolveBiometryType(context: LAContext, available: Bool) -> String {
        guard available else {
            return "none"
        }

        switch context.biometryType {
        case .faceID:
            return "faceId"
        case .touchID:
            return "touchId"
        case .opticID:
            return "iris"
        case .none:
            return "none"
        @unknown default:
            return "none"
        }
    }

    // MARK: - Private Helpers

    /**
     Maps an `LAContext` evaluation error to a plugin error.

     Availability-class failures (no biometry, lockout, missing
     passcode) map to `.unavailable`; user-driven or system
     cancellations and failed ceremonies map to `.cancelled`;
     anything else falls back to `.initFailed`.
     */
    static func mapLAError(_ error: NSError) -> BiometricsError {
        guard let laError = error as? LAError else {
            return .initFailed(BiometricsErrorMessages.initFailed)
        }

        switch laError.code {
        case .biometryNotAvailable, .biometryNotEnrolled, .biometryLockout, .passcodeNotSet:
            return .unavailable(BiometricsErrorMessages.unavailable)
        case .userCancel, .userFallback, .authenticationFailed, .systemCancel, .appCancel:
            return .cancelled(BiometricsErrorMessages.cancelled)
        default:
            return .initFailed(BiometricsErrorMessages.initFailed)
        }
    }
}
