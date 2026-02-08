import UIKit

/**
 Detects jailbreak-related applications by probing URL schemes.

 IMPORTANT:
 - This detection is opt-in via native configuration.
 - Requires LSApplicationQueriesSchemes to be declared in Info.plist.
 - This is a LOW confidence heuristic and must never be used alone.
 */
struct IntegrityJailbreakUrlSchemeDetector {

    static func detect(
        schemes: [String],
        includeDebug: Bool
    ) -> [String: Any]? {

        guard !schemes.isEmpty else { return nil }

        for scheme in schemes {
            let urlString = "\(scheme)://"
            guard let url = URL(string: urlString) else { continue }

            if UIApplication.shared.canOpenURL(url) {
                return [
                    "id": "ios_jailbreak_url_scheme",
                    "category": "jailbreak",
                    "confidence": "low",
                    "description": includeDebug
                        ? "Detected jailbreak-related application via URL scheme probing"
                        : nil,
                    "metadata": [
                        "scheme": scheme,
                        "source": "url_scheme"
                    ]
                ].compactMapValues { $0 }
            }
        }

        return nil
    }
}
