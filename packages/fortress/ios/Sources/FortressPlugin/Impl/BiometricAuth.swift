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
        allowPasscode: Bool = true,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
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

        context.evaluatePolicy(policy, localizedReason: reason) { success, authError in
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

    // MARK: - Private Helpers

    private func mapLAError(_ error: NSError) -> NativeError {
        guard let laError = error as? LAError else {
            return .initFailed(ErrorMessages.initFailed)
        }

        switch laError.code {
        case .biometryNotAvailable:
            return .unavailable(ErrorMessages.unavailable)
        case .biometryNotEnrolled:
            return .unavailable(ErrorMessages.unavailable)
        case .biometryLockout:
            return .unavailable(ErrorMessages.unavailable)
        case .userCancel:
            return .cancelled(ErrorMessages.cancelled)
        case .userFallback:
            return .cancelled(ErrorMessages.cancelled)
        case .authenticationFailed:
            return .cancelled(ErrorMessages.cancelled)
        case .passcodeNotSet:
            return .unavailable(ErrorMessages.unavailable)
        case .systemCancel:
            return .cancelled(ErrorMessages.cancelled)
        case .appCancel:
            return .cancelled(ErrorMessages.cancelled)
        default:
            return .initFailed(ErrorMessages.initFailed)
        }
    }
}
