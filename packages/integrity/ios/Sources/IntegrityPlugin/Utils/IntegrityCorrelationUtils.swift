import Foundation

struct IntegrityCorrelationUtils {
    static func jailbreakCorrelation(
        from signals: [[String: Any]],
        includeDebug: Bool
    ) -> [String: Any]? {
        let hasJailbreak = signals.contains { ($0["category"] as? String) == "jailbreak" }
        let hasTamper = signals.contains { ($0["category"] as? String) == "tamper" }

        guard hasJailbreak && hasTamper else { return nil }

        return IntegrityUtils.buildSignal(
            id: "ios_jailbreak_correlation_confirmed",
            category: "jailbreak",
            confidence: "high",
            description: "Multiple jailbreak/tamper indicators detected simultaneously",
            metadata: ["source": "jailbreak+tamper"],
            includeDebug: includeDebug
        )
    }

    static func jailbreakAndHookCorrelation(
        from signals: [[String: Any]],
        includeDebug: Bool
    ) -> [String: Any]? {
        let hasJailbreak = signals.contains { ($0["category"] as? String) == "jailbreak" }
        let hasHook = signals.contains { ($0["category"] as? String) == "hook" }

        guard hasJailbreak && hasHook else { return nil }

        return IntegrityUtils.buildSignal(
            id: "ios_jailbreak_hook_correlation_confirmed",
            category: "hook",
            confidence: "high",
            description: "Jailbreak and instrumentation indicators detected simultaneously",
            metadata: ["source": "jailbreak+hook"],
            includeDebug: includeDebug
        )
    }
}
