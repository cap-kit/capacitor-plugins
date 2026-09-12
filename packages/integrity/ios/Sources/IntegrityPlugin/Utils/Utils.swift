import Foundation

struct Utils {
    static func buildSignal(
        id: String,
        category: String,
        confidence: String,
        includeDebug: Bool,
        description: String? = nil,
        metadata: [String: Any]? = nil
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
