import Foundation
import LocalAuthentication

/**
 * Biometric authentication implementation using iOS LocalAuthentication.
 *
 * Supports Face ID, Touch ID, and device passcode as fallback.
 *
 * Architectural rules:
 * - Pure Swift implementation
 * - Stateless - no internal state
 * - Uses LAContext for biometric operations
 * - Throws NativeError for all failure cases
 */
struct BiometricAuth {

    /**
     Reference box for the in-flight authentication context.

     `LAContext.evaluatePolicy` has no external cancel handle: the only way
     to dismiss an ongoing system prompt programmatically is
     `invalidate()` on the same context. The box is a reference type so it
     stays shared across the value-type copies captured by completions.
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

    // MARK: - Biometric Type

    /**
     * Available biometric types on the device.
     */
    enum BiometricType {
        case none
        case touchID
        case faceID
    }

    // MARK: - Error Types

    enum BiometricError: Swift.Error {
        case notAvailable
        case notEnrolled
        case lockout
        case cancelled
        case failed
        case passcodeNotSet
    }

    // MARK: - Public API

    /**
     * Checks whether biometric authentication is available on the device.
     *
     * @return true if biometrics can be used, false otherwise
     */
    func canAuthenticate() -> Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    /**
     * Returns the type of biometric available on the device.
     *
     * @return The biometric type (faceID, touchID, or none)
     */
    func getBiometricType() -> BiometricType {
        let context = LAContext()
        var error: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return .none
        }

        switch context.biometryType {
        case .faceID:
            return .faceID
        case .touchID:
            return .touchID
        case .opticID:
            return .faceID // Treat OpticID as FaceID for compatibility
        case .none:
            return .none
        @unknown default:
            return .none
        }
    }

    /**
     * Checks whether device passcode is available as fallback.
     *
     * @return true if passcode authentication is available
     */
    func canAuthenticateWithPasscode() -> Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error)
    }

    /**
     * Performs biometric authentication with passcode fallback.
     *
     * @param reason The message displayed to the user
     * @param allowPasscode Whether to allow device passcode as fallback
     * @param completion Result callback invoked on authentication completion
     */
    func authenticate(
        reason: String,
        cancelTitle: String = "Cancel",
        allowPasscode: Bool = true,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let context = LAContext()
        context.localizedCancelTitle = cancelTitle
        context.localizedFallbackTitle = allowPasscode ? "Use Passcode" : ""

        var error: NSError?
        let policy: LAPolicy = allowPasscode
            ? .deviceOwnerAuthentication
            : .deviceOwnerAuthenticationWithBiometrics

        guard context.canEvaluatePolicy(policy, error: &error) else {
            if let laError = error {
                completion(.failure(mapLAError(laError)))
                return
            }
            completion(.failure(NativeError.unavailable(ErrorMessages.unavailable)))
            return
        }

        let relay = CompletionRelay(completion: completion)
        activeContext.set(context)

        context.evaluatePolicy(policy, localizedReason: reason) { success, authError in
            self.activeContext.set(nil)
            if success {
                relay.call(.success(()))
                return
            }

            if let nsError = authError as NSError? {
                relay.call(.failure(self.mapLAError(nsError)))
                return
            }

            relay.call(.failure(BiometricError.failed))
        }
    }

    /**
     * Performs biometric-only authentication (no passcode fallback).
     *
     * @param reason The message displayed to the user
     * @param completion Result callback invoked on authentication completion
     */
    func authenticateBiometricOnly(
        reason: String,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        authenticate(reason: reason, allowPasscode: false, completion: completion)
    }

    /**
     Cancels the ongoing authentication prompt, if any.

     Invalidating the context dismisses the system dialog; the in-flight
     completion then fires with a cancellation error. No-op when idle.
     */
    func cancelActiveAuthentication() {
        activeContext.invalidate()
    }

    func checkStatus() -> [String: Any] {
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
            case .biometryNotEnrolled:
                isBiometricsAvailable = true
                isBiometricsEnabled = false
            case .biometryNotAvailable:
                isBiometricsAvailable = false
                isBiometricsEnabled = false
            case .passcodeNotSet:
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

        let biometryType = resolveBiometryType(context: biometricContext, available: isBiometricsAvailable)

        return [
            "isBiometricsAvailable": isBiometricsAvailable,
            "isBiometricsEnabled": isBiometricsEnabled,
            "isDeviceSecure": isDeviceSecure,
            "biometryType": biometryType,
            "biometryTypes": biometryType == "none" ? [] : [biometryType],
            "strongBiometryIsAvailable": isBiometricsAvailable
        ]
    }

    private func resolveBiometryType(context: LAContext, available: Bool) -> String {
        guard available else {
            return "none"
        }

        if #available(iOS 11.0, *) {
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

        return "none"
    }

    // MARK: - Private Helpers

    private func mapLAError(_ error: NSError) -> NativeError {
        guard let laError = error as? LAError else {
            return .initFailed(ErrorMessages.initFailed)
        }

        switch laError.code {
        case .biometryNotAvailable, .biometryNotEnrolled, .biometryLockout, .passcodeNotSet:
            return .unavailable(ErrorMessages.unavailable)
        case .userCancel, .userFallback, .authenticationFailed, .systemCancel, .appCancel:
            return .cancelled(ErrorMessages.cancelled)
        default:
            return .initFailed(ErrorMessages.initFailed)
        }
    }
}
