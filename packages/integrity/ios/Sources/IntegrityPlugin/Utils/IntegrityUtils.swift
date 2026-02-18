import Foundation

struct IntegrityUtils {
    static func buildSignal(
        id: String,
        category: String,
        confidence: String,
        description: String? = nil,
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
