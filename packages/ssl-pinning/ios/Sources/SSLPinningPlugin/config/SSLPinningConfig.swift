import Foundation
import Capacitor

/**
 Plugin configuration container.

 This struct is responsible for reading and exposing
 static configuration values defined under the
 `SSLPinning` key in capacitor.config.ts.

 Configuration rules:
 - Read once during plugin initialization
 - Treated as immutable runtime input
 - Accessible only from native code

 Validation:
 - Certificate files are validated at load time
 - Missing/invalid certs cause fail-fast errors
 */
public struct SSLPinningConfig {

    // MARK: - Configuration Keys

    /**
     Centralized definition of configuration keys.
     Avoids string duplication and typos.
     */
    private enum Keys {
        static let verboseLogging = "verboseLogging"
        static let fingerprint = "fingerprint"
        static let fingerprints = "fingerprints"
        static let certs = "certs"
        static let certsByDomain = "certsByDomain"
        static let certsManifest = "certsManifest"
        static let excludedDomains = "excludedDomains"
    }

    // MARK: - Public Configuration Values

    /**
     Enables verbose native logging.

     When enabled, the plugin prints additional
     debug information to the Xcode console.

     Default: false
     */
    public let verboseLogging: Bool

    /**
     Default SHA-256 fingerprint used by `checkCertificate()`
     when no fingerprint is provided at runtime.
     */
    public let fingerprint: String?

    /**
     Default SHA-256 fingerprints used by `checkCertificates()`
     when no fingerprints are provided at runtime.
     */
    public let fingerprints: [String]

    /**
     Certificate filenames used for SSL pinning.

     Files are expected to be copied into the app bundle
     subdirectory 'certs'.

     This is the global fallback used when no domain-specific
     certificates are configured via `certsByDomain`.
     */
    public let certs: [String]

    /**
     Per-domain certificate configuration.

     Maps a domain (or subdomain pattern) to a list of
     certificate filenames to use for that domain.
     */
    public let certsByDomain: [String: [String]]

    /**
     Optional manifest file for certificate auto-discovery.

     The manifest is a JSON file containing either:
     - { "certs": ["a.cer", "b.cer"] }
     - { "certsByDomain": { "example.com": ["cert.cer"] } }
     - Both, which extend/override the explicit config values
     */
    public let certsManifest: String?

    /**
     Domains or URL prefixes excluded from SSL pinning.

     Any request whose host matches one of these values
     MUST bypass SSL pinning checks.
     */
    public let excludedDomains: [String]

    // MARK: - Defaults

    private static let defaultVerboseLogging: Bool = false
    private static let defaultFingerprint: String? = nil
    private static let defaultFingerprints: [String] = []
    private static let defaultCerts: [String] = []
    private static let defaultCertsByDomain: [String: [String]] = [:]
    private static let defaultCertsManifest: String? = nil
    private static let defaultExcludedDomains: [String] = []

    // MARK: - Validation Cache

    private let certValidationCache: [String: Bool]

    // MARK: - Initialization

    /**
     Initializes the configuration by reading values
     from the Capacitor PluginConfig.

     - Parameter plugin: The CAPPlugin instance used
     to access typed configuration values.
     */
    init(plugin: CAPPlugin) {
        let config = plugin.getConfig()

        // Verbose logging flag
        self.verboseLogging =
            config.getBoolean(
                Keys.verboseLogging,
                Self.defaultVerboseLogging
            )

        // Synchronize logger state
        SSLPinningLogger.verbose = self.verboseLogging

        // Single fingerprint (optional)
        if let value = config.getString(Keys.fingerprint),
           !value.isEmpty {
            self.fingerprint = value
        } else {
            self.fingerprint = Self.defaultFingerprint
        }

        // Multiple fingerprints (optional)
        self.fingerprints =
            config.getArray(Keys.fingerprints)?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }
            ?? Self.defaultFingerprints

        // Certificate files (optional)
        let explicitCerts: [String] =
            config.getArray(Keys.certs)?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }
            ?? Self.defaultCerts

        // Per-domain certificates (optional)
        let explicitCertsByDomain = Self.parseCertsByDomain(config.getObject(Keys.certsByDomain))

        // Manifest (optional)
        let manifestPath = config.getString(Keys.certsManifest)

        // Load and merge manifest if present
        let (manifestCerts, manifestCertsByDomain) = Self.loadManifest(manifestPath)

        // Merge: explicit config takes precedence over manifest
        self.certs = explicitCerts.isEmpty ? manifestCerts : explicitCerts

        var mergedCertsByDomain = manifestCertsByDomain
        for (key, value) in explicitCertsByDomain {
            mergedCertsByDomain[key] = value
        }
        self.certsByDomain = mergedCertsByDomain

        // Store manifest path
        self.certsManifest = manifestPath

        // Excluded domains (optional)
        self.excludedDomains =
            config.getArray(Keys.excludedDomains)?
            .compactMap { $0 as? String }
            .filter { !$0.isEmpty }
            ?? Self.defaultExcludedDomains

        // Validate certificate files at load time (fail-fast)
        self.certValidationCache = Self.validateCertFiles(
            certs: self.certs,
            certsByDomain: self.certsByDomain
        )

        // Check for invalid certificates and log errors
        for (certFile, isValid) in self.certValidationCache {
            if !isValid {
                SSLPinningLogger.error("Certificate validation failed: \(certFile)")
            }
        }
    }

    // MARK: - Public Helpers

    /**
     Checks if a certificate file exists and is valid.
     */
    public func isCertValid(_ fileName: String) -> Bool {
        return certValidationCache[fileName] ?? false
    }

    /**
     Returns all certificate file names from config (global + domain-specific).
     */
    public func getAllCertFileNames() -> Set<String> {
        let domainCerts = certsByDomain.values.flatMap { $0 }
        return Set(certs + domainCerts)
    }

    // MARK: - Private Helpers

    private static func parseCertsByDomain(_ object: [String: Any]?) -> [String: [String]] {
        guard let obj = object else { return [:] }

        var result: [String: [String]] = [:]
        for (key, value) in obj {
            if let certArray = value as? [String] {
                let filtered = certArray.filter { !$0.isEmpty }
                if !filtered.isEmpty {
                    result[key] = filtered
                }
            }
        }
        return result
    }

    private static func loadManifest(_ manifestPath: String?) -> ([String], [String: [String]]) {
        guard let path = manifestPath, !path.isEmpty else {
            return ([], [:])
        }

        guard let url = Bundle.main.url(forResource: path.replacingOccurrences(of: ".json", with: ""), withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            SSLPinningLogger.error("Failed to load manifest: \(path)")
            return ([], [:])
        }

        return parseManifestJson(data)
    }

    private static func parseManifestJson(_ data: Data) -> ([String], [String: [String]]) {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            SSLPinningLogger.error("Failed to parse manifest JSON")
            return ([], [:])
        }

        var certsList: [String] = []
        var certsByDomainMap: [String: [String]] = [:]

        // Parse global certs
        if let certsArray = json["certs"] as? [String] {
            certsList = certsArray.filter { !$0.isEmpty }
        }

        // Parse certsByDomain
        if let domainObj = json["certsByDomain"] as? [String: [String]] {
            for (domain, certList) in domainObj {
                let filtered = certList.filter { !$0.isEmpty }
                if !filtered.isEmpty {
                    certsByDomainMap[domain] = filtered
                }
            }
        }

        return (certsList, certsByDomainMap)
    }

    /**
     Validates all configured certificate files at load time.
     Returns a map of filename -> isValid.
     */
    private static func validateCertFiles(
        certs: [String],
        certsByDomain: [String: [String]]
    ) -> [String: Bool] {
        var result: [String: Bool] = [:]
        let allCerts = Array(Set(certs + certsByDomain.values.flatMap { $0 }))

        for certFile in allCerts {
            let isValid = isCertFileValid(certFile)
            result[certFile] = isValid
        }

        return result
    }

    /**
     Checks if a certificate file exists and can be parsed.
     */
    private static func isCertFileValid(_ fileName: String) -> Bool {
        let fileURL = URL(fileURLWithPath: fileName)
        let name = fileURL.deletingPathExtension().lastPathComponent
        let ext = fileURL.pathExtension.isEmpty ? "cer" : fileURL.pathExtension

        guard let path = Bundle.main.path(forResource: name, ofType: ext),
              let data = try? Data(contentsOf: URL(fileURLWithPath: path)),
              let _ = SecCertificateCreateWithData(nil, data as CFData) else {
            return false
        }

        return true
    }
}
