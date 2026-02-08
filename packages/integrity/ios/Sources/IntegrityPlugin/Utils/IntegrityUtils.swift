import Foundation

/**
 Utility helpers for the Integrity plugin (iOS).

 PURPOSE:
 - Host pure Swift utility functions that are:
 - platform-agnostic
 - side-effect free
 - independent from Capacitor APIs

 CURRENT STATE:
 - This type is intentionally empty.
 - iOS does not require explicit JSON serialization helpers
 because Capacitor accepts native `[String: Any]` payloads.

 FUTURE USE CASES:
 - Cross-platform payload normalization
 - Remote attestation artifact encoding
 - Data redaction or canonicalization
 - Shared, testable transformation logic

 CONTRACT:
 - Utilities defined here MUST NOT:
 - access Capacitor APIs
 - reference Plugin or Impl layers
 - perform I/O or side effects
 */
struct IntegrityUtils {

    /**
     Builds a JSON-compatible integrity signal dictionary.

     - Parameters:
     - id: Stable signal identifier.
     - category: High-level signal category.
     - confidence: Confidence level ("low", "medium", "high").
     - description: Optional human-readable description.
     - metadata: Optional diagnostic metadata.
     - includeDebug: Whether debug fields should be included.

     - Returns: A `[String: Any]` dictionary safe to cross the JS bridge.
     */
    static func buildSignal(
        id: String,
        category: String,
        confidence: String,
        description: String?,
        metadata: [String: Any]? = nil,
        includeDebug: Bool
    ) -> [String: Any] {

        var signal: [String: Any] = [
            "id": id,
            "category": category,
            "confidence": confidence
        ]

        if includeDebug, let description {
            signal["description"] = description
        }

        if let metadata {
            signal["metadata"] = metadata
        }

        return signal
    }
}

enum IntegrityCorrelationUtils {

    static func jailbreakCorrelation(
        from signals: [[String: Any]],
        includeDebug: Bool
    ) -> [String: Any]? {

        let jailbreakSignals = signals.filter {
            ($0["category"] as? String) == "jailbreak"
        }

        guard jailbreakSignals.count >= 2 else {
            return nil
        }

        let sources: Set<String> = Set(
            jailbreakSignals.compactMap {
                ($0["metadata"] as? [String: Any])?["source"] as? String
            }
        )

        guard sources.count >= 2 else {
            return nil
        }

        return [
            "id": "ios_jailbreak_multiple_indicators",
            "category": "jailbreak",
            "confidence": "high",
            "description": includeDebug
                ? "Multiple independent jailbreak indicators detected simultaneously"
                : nil,
            "metadata": [
                "indicatorCount": jailbreakSignals.count,
                "sources": Array(sources)
            ]
        ].compactMapValues { $0 }
    }

    static func jailbreakAndHookCorrelation(
        from signals: [[String: Any]],
        includeDebug: Bool
    ) -> [String: Any]? {

        let hasJailbreak = signals.contains {
            ($0["category"] as? String) == "jailbreak"
        }

        let hasHook = signals.contains {
            ($0["category"] as? String) == "hook"
        }

        guard hasJailbreak && hasHook else {
            return nil
        }

        return [
            "id": "ios_jailbreak_and_hook_detected",
            "category": "tamper",
            "confidence": "high",
            "description": includeDebug
                ? "Device appears to be jailbroken and actively instrumented at runtime"
                : nil,
            "metadata": [
                "jailbreakIndicators": signals.filter {
                    ($0["category"] as? String) == "jailbreak"
                }.count,
                "hookIndicators": signals.filter {
                    ($0["category"] as? String) == "hook"
                }.count
            ]
        ].compactMapValues { $0 }
    }
}
