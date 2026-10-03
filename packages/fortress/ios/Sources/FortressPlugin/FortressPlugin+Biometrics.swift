import Foundation
import Capacitor

extension FortressPlugin {
    // MARK: - Standalone Biometrics (no vault or session side effects)

    /**
     Verifies identity without touching vault or session state.

     Accepts `reason` (or legacy `promptMessage`) plus the shared
     `promptOptions`. `allowDeviceCredential`, when present, overrides the
     configured `fallbackStrategy` for this call only.
     */
    @objc func authenticate(_ call: CAPPluginCall) {
        let promptOptions = parsePromptOptions(call)
        let reason = call.getString("reason")
            ?? call.getString("promptMessage")
            ?? promptOptions?.description
            ?? "Authenticate"
        let cancelTitle = promptOptions?.negativeButtonText ?? "Cancel"
        let allowPasscode: Bool = (call.options["allowDeviceCredential"] as? Bool)
            ?? implementation.resolveAllowPasscode()

        implementation.authenticateIdentity(
            reason: reason,
            cancelTitle: cancelTitle,
            allowPasscode: allowPasscode
        ) { [weak self] result in
            switch result {
            case .success:
                call.resolve()
            case .failure(let error):
                self?.handleError(call, error)
            }
        }
    }

    @objc func cancelAuthentication(_ call: CAPPluginCall) {
        implementation.cancelActiveAuthentication()
        call.resolve()
    }

    @objc func isAvailable(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        call.resolve(["isAvailable": status["isBiometricsAvailable"] as? Bool ?? false])
    }

    @objc func isEnrolled(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        call.resolve(["isEnrolled": status["isBiometricsEnabled"] as? Bool ?? false])
    }

    @objc func getBiometricType(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        call.resolve(["biometryType": status["biometryType"] as? String ?? "none"])
    }

    @objc func getBiometricTypes(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        call.resolve(["biometryTypes": status["biometryTypes"] as? [String] ?? []])
    }

    @objc func hasDeviceCredential(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        call.resolve(["hasDeviceCredential": status["isDeviceSecure"] as? Bool ?? false])
    }

    @objc func getBiometricStrengthLevel(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        let strong = status["strongBiometryIsAvailable"] as? Bool ?? false
        let available = status["isBiometricsAvailable"] as? Bool ?? false
        call.resolve(["strengthLevel": strong ? "strong" : (available ? "weak" : "none")])
    }

    @objc func getAuthenticationType(_ call: CAPPluginCall) {
        call.resolve(["authenticationType": implementation.lastAuthenticationType])
    }

    @objc func enroll(_ call: CAPPluginCall) {
        // iOS offers no public biometric enrollment API.
        call.reject(ErrorMessages.unavailable, NativeError.unavailable(ErrorMessages.unavailable).errorCode)
    }
}
