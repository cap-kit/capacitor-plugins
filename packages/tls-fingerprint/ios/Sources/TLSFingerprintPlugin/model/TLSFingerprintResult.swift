import Foundation

/**
 Canonical result model for TLS fingerprint validation operations on iOS.

 This struct is exchanged between the native implementation (TLSFingerprintImpl)
 and the bridge (TLSFingerprintPlugin) and serialized to JavaScript via a JSObject.

 Fields mirror the public JS payload:
 - actualFingerprint: server fingerprint used for matching
 - fingerprintMatched: whether the fingerprint check succeeded (true) or not (false)
 - matchedFingerprint: the fingerprint that matched (only present for fingerprint mode)
 - excludedDomain: indicates an excluded-domain bypass (true when applicable)
 - mode: active mode: "fingerprint" | "excluded"
 - error: human-readable error (empty on success/match)
 - errorCode: canonical error code string (empty on success)
 */
struct TLSFingerprintResult {
    let actualFingerprint: String?
    let fingerprintMatched: Bool
    let matchedFingerprint: String?
    let excludedDomain: Bool?
    let mode: String?
    let error: String?
    let errorCode: String?

    init(
        actualFingerprint: String? = nil,
        fingerprintMatched: Bool,
        matchedFingerprint: String? = nil,
        excludedDomain: Bool? = nil,
        mode: String? = nil,
        error: String? = nil,
        errorCode: String? = nil
    ) {
        self.actualFingerprint = actualFingerprint
        self.fingerprintMatched = fingerprintMatched
        self.matchedFingerprint = matchedFingerprint
        self.excludedDomain = excludedDomain
        self.mode = mode
        self.error = error
        self.errorCode = errorCode
    }

    func toDictionary() -> [String: Any] {
        var dict: [String: Any] = [
            "fingerprintMatched": fingerprintMatched
        ]

        if let actualFingerprint = actualFingerprint {
            dict["actualFingerprint"] = actualFingerprint
        }

        if let matchedFingerprint = matchedFingerprint {
            dict["matchedFingerprint"] = matchedFingerprint
        }

        if let excludedDomain = excludedDomain {
            dict["excludedDomain"] = excludedDomain
        }

        if let mode = mode {
            dict["mode"] = mode
        }

        if let error = error {
            dict["error"] = error
        }

        if let errorCode = errorCode {
            dict["errorCode"] = errorCode
        }

        return dict
    }
}
