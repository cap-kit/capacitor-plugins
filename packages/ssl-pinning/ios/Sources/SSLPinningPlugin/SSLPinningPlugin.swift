import Foundation
import Capacitor

/**
 * @file SSLPinningPlugin.swift
 * This file defines the implementation of the Capacitor plugin `SSLPinningPlugin` for iOS.
 * The plugin provides an interface between JavaScript and native iOS code, allowing Capacitor applications
 * to interact with SSL certificate verification functionality.
 *
 * Documentation Reference: https://capacitorjs.com/docs/plugins/ios
 */

@objc(SSLPinningPlugin)
public class SSLPinningPlugin: CAPPlugin, CAPBridgedPlugin {

    // Configuration instance
    private var config: SSLPinningConfig?

    /// An instance of the implementation class that contains the plugin's core functionality.
    private let implementation = SSLPinningImpl()

    /// The unique identifier for the plugin, used by Capacitor's internal mechanisms.
    public let identifier = "SSLPinningPlugin"

    /// The JavaScript name used to reference this plugin in Capacitor applications.
    public let jsName = "SSLPinning"

    /**
     * A list of methods exposed by this plugin. These methods can be called from the JavaScript side.
     * - `checkCertificate`: Validates an SSL certificate for a given URL.
     * - `checkCertificates`: Validates multiple SSL certificates for given URLs.
     * - `getPluginVersion`: Retrieves the current version of the plugin.
     */
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkCertificate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkCertificates", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    /**
     Plugin initialization.
     Loads config and sets up lifecycle observers.
     */
    override public func load() {
        let cfg = SSLPinningConfig(plugin: self)
        self.config = cfg
        implementation.applyConfig(cfg)
    }

    // MARK: - SSL Pinning Methods

    /**
     Validates an SSL certificate for a given URL.
     - Parameters:
     - call: The CAPPluginCall object containing the call details from JavaScript.
     */
    @objc func checkCertificate(_ call: CAPPluginCall) {
        let url = call.getString("url", "")
        let fingerprintValue = call.getString("fingerprint", "")
        let fingerprintArg = fingerprintValue.isEmpty ? nil : fingerprintValue

        if url.isEmpty {
            call.resolve([
                "fingerprintMatched": false,
                "error": "Missing url"
            ])
            return
        }

        implementation.checkCertificate(
            urlString: url,
            fingerprintFromArgs: fingerprintArg
        ) { result in
            call.resolve(result)
        }
    }

    /**
     Validates multiple SSL certificates for given URLs.
     - Parameters:
     - call: The CAPPluginCall object containing the call details from JavaScript.
     */
    @objc func checkCertificates(_ call: CAPPluginCall) {
        let url = call.getString("url", "")

        let fingerprints = call
            .getArray("fingerprints", [])
            .compactMap { $0 as? String }

        if url.isEmpty {
            call.resolve([
                "fingerprintMatched": false,
                "error": "Missing url"
            ])
            return
        }

        implementation.checkCertificates(
            urlString: url,
            fingerprintsFromArgs: fingerprints.isEmpty ? nil : fingerprints
        ) { result in
            call.resolve(result)
        }
    }

    // MARK: - Version

    /// Retrieves the plugin version synchronized from package.json.
    @objc func getPluginVersion(_ call: CAPPluginCall) {
        // Standardized enum name across all CapKit plugins
        call.resolve([
            "version": PluginVersion.number
        ])
    }

}
