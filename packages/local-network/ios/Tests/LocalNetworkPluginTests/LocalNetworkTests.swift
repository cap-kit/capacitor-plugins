import XCTest
@testable import LocalNetworkPlugin

class LocalNetworkTests: XCTestCase {
    /// Every method declared in `src/definitions.ts` must be registered in
    /// `pluginMethods`, otherwise the Capacitor bridge answers the JavaScript
    /// call with `UNIMPLEMENTED` on iOS.
    func testRegistersEveryMethodDeclaredInTheJavaScriptInterface() {
        let plugin = LocalNetworkPlugin()
        let registered = Set(plugin.pluginMethods.map { $0.name })

        XCTAssertEqual(registered, ["checkPermissions", "requestPermissions", "getPluginVersion"])
    }

    /// `PluginVersion.number` is generated from `package.json` by
    /// `scripts/sync-version.mjs`; an empty or stale value means the generated
    /// file was never refreshed.
    func testGeneratedVersionIsASemanticVersion() {
        let pattern = #"^\d+\.\d+\.\d+(-[0-9A-Za-z.\-]+)?$"#

        XCTAssertNotNil(
            PluginVersion.number.range(of: pattern, options: .regularExpression),
            "Unexpected PluginVersion.number: \(PluginVersion.number)"
        )
    }
}
