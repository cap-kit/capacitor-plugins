import Foundation
import Capacitor

extension FortressPlugin {

    @objc func checkStatus(_ call: CAPPluginCall) {
        let status = implementation.checkBiometricStatus()
        lastSecurityStatus = status
        call.resolve(status)
    }

    /**
     Overrides the detected biometry type for development/testing flows.

     Accepted values: `none`, `touchId`, `faceId`, `fingerprint`, `iris`.
     */
    @objc func setBiometryType(_ call: CAPPluginCall) {
        guard let biometryType = call.getString("biometryType"),
              ["none", "touchId", "faceId", "fingerprint", "iris"].contains(biometryType) else {
            reject(call, error: .invalidInput(ErrorMessages.invalidInput))
            return
        }

        implementation.setBiometryType(biometryType)
        let status = implementation.checkBiometricStatus()
        lastSecurityStatus = status
        notifyListeners("onSecurityStateChanged", data: status)
        call.resolve()
    }

    /**
     Overrides biometric enrollment state for development/testing flows.
     */
    @objc func setBiometryIsEnrolled(_ call: CAPPluginCall) {
        guard let isBiometricsEnabled = call.getBool("isBiometricsEnabled") else {
            reject(call, error: .invalidInput(ErrorMessages.invalidInput))
            return
        }

        implementation.setBiometryIsEnrolled(isBiometricsEnabled)
        let status = implementation.checkBiometricStatus()
        lastSecurityStatus = status
        notifyListeners("onSecurityStateChanged", data: status)
        call.resolve()
    }

    /**
     Overrides device secure-state for development/testing flows.
     */
    @objc func setDeviceIsSecure(_ call: CAPPluginCall) {
        guard let isDeviceSecure = call.getBool("isDeviceSecure") else {
            reject(call, error: .invalidInput(ErrorMessages.invalidInput))
            return
        }

        implementation.setDeviceIsSecure(isDeviceSecure)
        let status = implementation.checkBiometricStatus()
        lastSecurityStatus = status
        notifyListeners("onSecurityStateChanged", data: status)
        call.resolve()
    }
}
