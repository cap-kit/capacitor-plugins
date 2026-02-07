import Foundation

/**
 Helper responsible for assembling the final integrity report payload.

 Responsibilities:
 - Aggregate signals
 - Compute integrity score
 - Build environment metadata
 - Produce a JS-bridge-safe dictionary

 This type contains NO platform logic.
 */
struct IntegrityReportBuilder {

    /**
     Builds the final integrity report returned to the JavaScript layer.

     This method is responsible for:
     - aggregating all collected integrity signals
     - computing a numeric integrity score
     - deriving a boolean compromise state
     - attaching immutable environment metadata
     - producing a JSON-bridge-safe payload

     IMPORTANT:
     - This builder MUST remain platform-agnostic.
     - No platform-specific heuristics or weighting logic
     may be introduced here.
     */
    static func buildReport(
        signals: [[String: Any]],
        isEmulator: Bool,
        platform: String
    ) -> [String: Any] {

        // Compute the aggregate integrity score based on signal confidence.
        let score = computeScore(from: signals)

        return [
            // Ordered list of all detected integrity signals.
            "signals": signals,

            // Numeric integrity score derived from signal confidence.
            "score": score,

            // Convenience flag indicating whether the device
            // should be considered compromised.
            "compromised": score >= 30,

            // Static environment metadata describing the runtime context.
            "environment": buildEnvironment(
                platform: platform,
                isEmulator: isEmulator
            ),

            // Millisecond-precision UNIX timestamp of report generation.
            "timestamp": currentTimestamp()
        ]
    }

    // MARK: - Scoring

    /**
     Computes a numeric integrity score from a list of signals.

     Scoring policy:
     - high   → +30 points
     - medium → +15 points
     - low    → +5 points

     NOTES:
     - Signals without a valid `confidence` field are ignored.
     - This scoring model is intentionally simple and heuristic-based.
     - Platform-specific adjustments MUST NOT be implemented here.
     */
    private static func computeScore(
        from signals: [[String: Any]]
    ) -> Int {
        signals.reduce(0) { acc, signal in
            guard let confidence = signal["confidence"] as? String else {
                return acc
            }

            switch confidence {
            case "high": return acc + 30
            case "medium": return acc + 15
            case "low": return acc + 5
            default: return acc
            }
        }
    }

    // MARK: - Environment

    /**
     Builds a static environment descriptor attached to every report.

     This metadata is informational only and MUST NOT be used
     to infer compromise on its own.
     */
    private static func buildEnvironment(
        platform: String,
        isEmulator: Bool
    ) -> [String: Any] {
        [
            // Platform identifier (e.g. "ios", "android").
            "platform": platform,

            // Whether the app is running inside an emulator or simulator.
            "isEmulator": isEmulator,

            // Debug build flag reserved for future use.
            // Currently always false on iOS.
            "isDebugBuild": false
        ]
    }

    // MARK: - Timestamp

    /**
     Returns the current UNIX timestamp in milliseconds.

     This timestamp represents the moment the integrity
     report was assembled, not when individual signals
     were detected.
     */
    private static func currentTimestamp() -> Int {
        Int(Date().timeIntervalSince1970 * 1000)
    }
}
