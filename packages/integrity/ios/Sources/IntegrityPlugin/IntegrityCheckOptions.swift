/**
 Options controlling the behavior of `Integrity.check()`.

 This model represents the JavaScript options object
 passed to the native layer.

 Design principles:
 - Decodable from JS input
 - Independent from Capacitor APIs
 - Safe to use inside the native implementation layer
 - Does NOT affect the public JS API shape

 Notes:
 - Default values are applied in the Plugin layer
 - The Impl layer MUST NOT assume non-optional values
 */
struct IntegrityCheckOptions: Decodable {

    /**
     Desired strictness level for integrity checks.

     Supported values:
     - "basic": minimal checks (root/jailbreak, emulator)
     - "standard": adds debug and instrumentation heuristics
     - "strict": enables all available checks

     Defaults to "basic" when not provided.
     */
    let level: String?

    /**
     Whether additional debug information should be
     included in returned integrity signals.

     When enabled, signals MAY include a human-readable
     `description` field intended for diagnostics only.

     Defaults to false.
     */
    let includeDebugInfo: Bool?
}
