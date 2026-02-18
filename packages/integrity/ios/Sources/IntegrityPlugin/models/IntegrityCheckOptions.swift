/// Options controlling the behavior of `Integrity.check()`.
///
/// This model represents the JavaScript options object passed to the native layer.
/// Default values are applied in the Plugin layer.
struct IntegrityCheckOptions: Decodable {
    // MARK: - Properties

    /// Desired strictness level for integrity checks.
    ///
    /// Supported values:
    /// - "basic": minimal checks (root/jailbreak, emulator)
    /// - "standard": adds debug and instrumentation heuristics
    /// - "strict": enables all available checks
    let level: String?

    /// Whether additional debug information should be included in returned signals.
    ///
    /// When enabled, signals may include a diagnostic `description` field.
    let includeDebugInfo: Bool?
}
