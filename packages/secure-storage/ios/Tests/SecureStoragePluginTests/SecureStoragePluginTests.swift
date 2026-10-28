import XCTest
@testable import SecureStoragePlugin

/**
 Functional tests for the SecureStorage plugin native implementation.

 These tests validate the core behavior of the implementation
 independently from the Capacitor bridge:

 - Key-name encoding/decoding and candidate resolution (pure logic).
 - The insecure tier backed by UserDefaults (fully runnable headless).
 - Keychain CRUD. Bare SPM test runners lack the keychain entitlement
   (errSecMissingEntitlement, -34018), so those tests probe availability
   first and skip cleanly where Keychain writes are not permitted.

 WHY 10 TESTS SKIP (verified 2026-10-28, keep this block in sync with
 the probe below):

 `pnpm verify:ios` runs the package scheme via `xcodebuild test` with no
 host application, so the process that executes these tests is the
 simulator runtime's own xctest agent
 (iPhoneSimulator.platform/Developer/Library/Xcode/Agents/xctest).
 That agent is ad-hoc signed carrying only the
 `com.apple.security.get-task-allow` entitlement — it has no
 `keychain-access-groups` / `application-identifier`, and the Security
 server therefore rejects every Keychain WRITE with
 errSecMissingEntitlement (-34018). The probe below observes exactly
 that: the mapped error is `initFailed("Internal error. (-34018)")`.

 Granting entitlements to the TEST BUNDLE does not help (verified with
 `xcodebuild ... CODE_SIGN_ENTITLEMENTS=...`): keychain access is
 enforced against the requesting PROCESS, and the process is the
 platform agent, not the bundle. The remaining remedies are all
 outside this package's scope: re-signing the root-owned Xcode-installed
 agent (a global machine modification) or adding a host-application
 Xcode project to a pure-SPM fork distributed as a Swift package +
 tarball. Reads of MISSING items are also gated behind the same probe
 because SecItem queries are answered by the same Security server and
 fail identically in this context.

 Skipped tests (all Keychain-write dependent):
   testDefaultAccessWinsWhenPerItemAccessMissing, testSetGetRoundTrip,
   testSetOverwritesPreviousValue, testGetMissingReturnsNil,
   testRemoveReportsExistance, testKeysReturnsDecodedNames,
   testGetManyMapsMissingToNull, testClearRemovesEverything,
   testObfuscatedKeysRoundTrip, testClearIsIdempotent.

 They run for real on device/simulator builds with a host app (the
 Capacitor example app), where the host's bundle identifier provides
 the keychain access group. Do NOT force them green here by weakening
 the probe: a "passing" claim would be false, and a Keychain stub would
 test a fake, not the plugin.
 */
final class SecureStoragePluginTests: XCTestCase {

    private var impl: SecureStorageImpl!

    override func setUp() {
        super.setUp()
        impl = SecureStorageImpl()
        clearAll()
        impl.clearInsecure()
    }

    override func tearDown() {
        clearAll()
        impl.clearInsecure()
        super.tearDown()
    }

    /// Some platforms remove a single matching item per SecItemDelete call;
    /// wipe by looping so the harness always starts from an empty service.
    private func clearAll() {
        var attempts = 0
        while attempts < 50 {
            attempts += 1
            do {
                try impl.clear()
            } catch {
                return
            }
            if ((try? impl.keys()) ?? []).isEmpty {
                return
            }
        }
    }

    /// Probe whether Keychain writes are permitted in this test context.
    /// Returns false with errSecMissingEntitlement (-34018) on bare SPM
    /// test runners without a host application.
    private func keychainWritable() -> Bool {
        let probe = "__capkit_probe_\(UUID().uuidString)"
        do {
            try impl.set(key: probe, value: "probe", access: nil)
            _ = try impl.remove(key: probe)
            return true
        } catch {
            return false
        }
    }

    // MARK: - Key-name encoding (pure logic)

    func testEncodeB64RoundTrip() {
        let original = "clave Usuario#1"
        let encoded = SecureStorageUtils.encodeB64(original)
        XCTAssertFalse(encoded.contains("+"))
        XCTAssertFalse(encoded.contains("/"))
        XCTAssertEqual(SecureStorageUtils.decodeB64(encoded), original)
    }

    func testDecodeB64RejectsPlainNames() {
        XCTAssertNil(SecureStorageUtils.decodeB64("plain_key_name"))
    }

    func testEncodeNameAppliesPrefixAndObfuscation() {
        impl.runtimeObfuscationPrefix = "p_"
        impl.runtimeObfuscateKeys = true
        XCTAssertEqual(impl.encodeName("key"), "p_\(SecureStorageUtils.encodeB64("key"))")
        impl.runtimeObfuscateKeys = false
        XCTAssertEqual(impl.encodeName("key"), "p_key")
    }

    func testNameCandidatesSpanBothEncodings() {
        impl.runtimeObfuscateKeys = true
        let candidates = impl.nameCandidates("key")
        XCTAssertTrue(candidates.contains(SecureStorageUtils.encodeB64("key")))
        XCTAssertTrue(candidates.contains("key"))
    }

    func testDecodeNameStripsPrefixWithoutMisdecoding() {
        impl.runtimeObfuscationPrefix = "p_"
        XCTAssertEqual(impl.decodeName("p_plain"), "plain")
    }

    // MARK: - Accessibility mapping (pure logic)

    func testResolveAccessibleMapping() {
        XCTAssertEqual(impl.resolveAccessible("whenUnlocked"), kSecAttrAccessibleWhenUnlocked)
        XCTAssertEqual(
            impl.resolveAccessible("afterFirstUnlockThisDeviceOnly"),
            kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        )
        XCTAssertNil(impl.resolveAccessible("bogus"))
        XCTAssertNil(impl.resolveAccessible(nil))
    }

    func testDefaultAccessWinsWhenPerItemAccessMissing() throws {
        impl.setDefaultAccess("whenUnlocked")
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "default-access-key", value: "v", access: nil)
        XCTAssertEqual(try impl.get(key: "default-access-key"), "v")
    }

    // MARK: - Synchronization flag

    func testSynchronizeToggle() {
        impl.setSynchronize(false)
        XCTAssertEqual(impl.isSynchronized(), false)
        impl.setSynchronize(true)
        XCTAssertEqual(impl.isSynchronized(), true)
    }

    // MARK: - Insecure tier (UserDefaults, headless-safe)

    func testInsecureRoundTrip() {
        impl.setInsecure(key: "pref", value: "dark")
        XCTAssertEqual(impl.getInsecure(key: "pref"), "dark")
        XCTAssertEqual(impl.hasInsecure(key: "pref"), true)
        XCTAssertEqual(impl.removeInsecure(key: "pref"), true)
        XCTAssertEqual(impl.removeInsecure(key: "pref"), false)
        XCTAssertNil(impl.getInsecure(key: "pref"))
    }

    func testInsecureGroupNamespacing() {
        impl.configure(group: "OtherGroup")
        impl.setInsecure(key: "k", value: "v")
        // Value lives under the configured group.
        XCTAssertEqual(UserDefaults.standard.string(forKey: "OtherGroup.k"), "v")
        // Default group does not see it.
        impl.insecureGroup = "CapacitorStorage"
        XCTAssertNil(impl.getInsecure(key: "k"))
    }

    func testInsecureKeysAndClear() {
        impl.setInsecure(key: "one", value: "1")
        impl.setInsecure(key: "two", value: "2")
        let keys = impl.keysInsecure()
        XCTAssertTrue(keys.contains("one"))
        XCTAssertTrue(keys.contains("two"))
        impl.clearInsecure()
        XCTAssertTrue(impl.keysInsecure().isEmpty)
    }

    func testInsecureObfuscatedCandidates() {
        impl.runtimeObfuscateKeys = true
        impl.setInsecure(key: "flag", value: "on")
        XCTAssertEqual(impl.getInsecure(key: "flag"), "on")
        XCTAssertTrue(impl.hasInsecure(key: "flag"))
    }

    func testInsecurePrefixChangeDoesNotOrphanEntries() {
        impl.setInsecure(key: "legacy", value: "kept")
        // The public configure() API registers the new prefix as seen, so
        // reads still span the legacy prefix within the session.
        impl.configure(group: nil, obfuscationPrefix: "p_")
        XCTAssertEqual(impl.getInsecure(key: "legacy"), "kept")
        XCTAssertTrue(impl.hasInsecure(key: "legacy"))
        XCTAssertEqual(impl.removeInsecure(key: "legacy"), true)
    }

    // MARK: - Secure tier (Keychain, entitlement-gated)

    func testSetGetRoundTrip() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "k1", value: "value-1", access: nil)
        XCTAssertEqual(try impl.get(key: "k1"), "value-1")
        XCTAssertEqual(try impl.has(key: "k1"), true)
    }

    func testSetOverwritesPreviousValue() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "k1", value: "first", access: nil)
        try impl.set(key: "k1", value: "second", access: nil)
        XCTAssertEqual(try impl.get(key: "k1"), "second")
    }

    func testGetMissingReturnsNil() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain unavailable in this test context")
        XCTAssertNil(try impl.get(key: "missing"))
        XCTAssertEqual(try impl.has(key: "missing"), false)
    }

    func testRemoveReportsExistance() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "k1", value: "v", access: nil)
        XCTAssertEqual(try impl.remove(key: "k1"), true)
        XCTAssertEqual(try impl.remove(key: "k1"), false)
        XCTAssertNil(try impl.get(key: "k1"))
    }

    func testKeysReturnsDecodedNames() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "alpha", value: "1", access: nil)
        try impl.set(key: "beta", value: "2", access: nil)
        let keys = try impl.keys()
        XCTAssertTrue(keys.contains("alpha"))
        XCTAssertTrue(keys.contains("beta"))
    }

    func testGetManyMapsMissingToNull() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain unavailable in this test context")
        try impl.set(key: "present", value: "v", access: nil)
        let values = try impl.getMany(keys: ["present", "absent"])
        XCTAssertEqual(values["present"] ?? "missing", "v")
        // Double-optional: an absent key maps to an explicit null value.
        XCTAssertNil(values["absent"].flatMap { $0 })
    }

    func testClearRemovesEverything() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        try impl.set(key: "a", value: "1", access: nil)
        try impl.set(key: "b", value: "2", access: nil)
        try impl.clear()
        XCTAssertTrue(try impl.keys().isEmpty)
    }

    func testObfuscatedKeysRoundTrip() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain writes unavailable in this test context")
        impl.runtimeObfuscateKeys = true
        try impl.set(key: "secret-key", value: "payload", access: nil)
        XCTAssertEqual(try impl.get(key: "secret-key"), "payload")
        // Candidates span both encodings, so a plain-named legacy entry is still found.
        XCTAssertEqual(try impl.has(key: "secret-key"), true)
        let keys = try impl.keys()
        XCTAssertTrue(keys.contains("secret-key"), "keys() must decode base64 account names")
    }

    func testClearIsIdempotent() throws {
        try XCTSkipUnless(keychainWritable(), "Keychain unavailable in this test context")
        try impl.clear()
        try impl.clear()
        XCTAssertTrue(try impl.keys().isEmpty)
    }
}
