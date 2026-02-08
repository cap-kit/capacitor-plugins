import Foundation
import DeviceCheck

/**
 Handles remote attestation signals using Apple's DeviceCheck (App Attest) framework.

 IMPORTANT:
 - App Attest is NOT implemented yet.
 - This component explicitly reports unavailability instead of failing silently.
 - The emitted signal is observational only and LOW confidence.
 */
struct IntegrityRemoteAttestor {

    /**
     Returns a LOW confidence signal indicating that App Attest
     is currently not available or not implemented.

     This signal is emitted only when strict mode is requested.
     */
    static func getAppAttestSignal(options: IntegrityCheckOptions) -> [String: Any]? {

        guard options.level == "strict" else {
            return nil
        }

        return [
            "id": "ios_app_attest_unavailable",
            "category": "environment",
            "confidence": "low",
            "description": options.includeDebugInfo == true
                ? "Apple App Attest is not implemented or not available on this device"
                : nil,
            "metadata": [
                "attestation": "unsupported",
                "framework": "DeviceCheck",
                "reason": "not_implemented"
            ]
        ].compactMapValues { $0 }
    }
}
