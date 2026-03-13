import Foundation
import Capacitor

extension FortressPlugin {

    @objc func getRuntimeConfig(_ call: CAPPluginCall) {
        guard let runtimeConfig = self.config else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }
        call.resolve(runtimeConfigSnapshot(runtimeConfig))
    }

    @objc func configure(_ call: CAPPluginCall) {
        guard var runtimeConfig = self.config else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }

        let incomingOverrides = Dictionary(
            uniqueKeysWithValues: call.options.map { (String(describing: $0.key), $0.value) }
        )
        runtimeConfig.applyRuntimeOverrides(incomingOverrides)

        self.config = runtimeConfig
        implementation.configure(runtimeConfig)
        runtimeConfigStore.saveOverrides(runtimeConfigSnapshot(runtimeConfig))
        implementation.setRuntimeEnablePrivacyScreen(runtimeConfig.enablePrivacyScreen)

        if runtimeConfig.enablePrivacyScreen {
            let isLocked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(isLocked)
        } else {
            implementation.setPrivacyScreenVisible(false)
        }

        call.resolve()
    }

    @objc func resetRuntimeConfig(_ call: CAPPluginCall) {
        guard let baselineConfig = staticConfigBaseline else {
            call.reject(ErrorMessages.initFailed, NativeError.initFailed(ErrorMessages.initFailed).errorCode)
            return
        }

        config = baselineConfig
        implementation.configure(baselineConfig)
        runtimeConfigStore.clearOverrides()
        implementation.setRuntimeEnablePrivacyScreen(baselineConfig.enablePrivacyScreen)

        if baselineConfig.enablePrivacyScreen {
            let isLocked = (try? implementation.isLocked()) ?? true
            implementation.setPrivacyScreenVisible(isLocked)
        } else {
            implementation.setPrivacyScreenVisible(false)
        }

        call.resolve()
    }
}
