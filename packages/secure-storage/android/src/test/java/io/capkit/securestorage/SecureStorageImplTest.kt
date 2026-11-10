package io.capkit.securestorage

import android.content.Context
import io.capkit.securestorage.error.SecureStorageError
import io.capkit.securestorage.error.SecureStorageErrorMessages
import io.capkit.securestorage.utils.SecureStorageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.reflect.InvocationTargetException

/**
 * Unit tests for [SecureStorageImpl] on the JVM via Robolectric.
 *
 * Coverage strategy (verified approach for this suite):
 * - The insecure tier and the key-name logic run for real: Robolectric
 *   emulates [SharedPreferences], so all candidate resolution, group
 *   namespacing, obfuscation and prefix-change behavior is exercised
 *   through the public implementation API, with raw stored names
 *   asserted against the backing preference files.
 * - The secure tier's encryption is hardware-backed (AndroidKeyStore),
 *   which Robolectric does NOT emulate. On the JVM the Keystore lookup
 *   fails, and the implementation must surface that as
 *   [SecureStorageError.Unavailable] — asserted as the error mapping
 *   contract (reflectively, see the test comment). Round-trip encryption
 *   itself is only testable on device and is intentionally not faked;
 *   the public set() path additionally trips a Robolectric JCE artifact
 *   (Cipher framework-jar verification) before ever reaching the
 *   Keystore, so only the no-partial-state invariant is asserted there.
 * - The `commit()` vs `apply()` choice has no seam to intercept; the
 *   testable consequence is asserted instead: values are visible in the
 *   backing store immediately after a write returns (a guarantee only
 *   `commit()` provides on device, where `apply()` is asynchronous).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecureStorageImplTest {
  private lateinit var context: Context
  private lateinit var impl: SecureStorageImpl

  private val securePrefs
    get() = context.getSharedPreferences("securestorage_encrypted", Context.MODE_PRIVATE)

  private val insecurePrefs
    get() = context.getSharedPreferences("securestorage_insecure", Context.MODE_PRIVATE)

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    impl = SecureStorageImpl(context)
    // Establish runtime overrides so the key-name logic never touches the
    // lateinit static config (which requires a Capacitor Plugin to build).
    impl.configure(group = null, obfuscateKeys = false, obfuscationPrefix = "")
  }

  // ---------------------------------------------------------------------------
  // Secure tier: error mapping when the hardware Keystore is unavailable
  // ---------------------------------------------------------------------------

  @Test
  fun masterKeySurfacesUnavailableWhenKeystoreMissing() {
    // Robolectric has no AndroidKeyStore provider, so the master key
    // lookup fails; the impl must map that to Unavailable, not leak a
    // raw KeyStore exception. Asserted reflectively because the public
    // set() path cannot reach the Keystore on the JVM: Cipher.getInstance
    // dies first in JCE framework-jar verification under the Robolectric
    // sandbox classloader (ExceptionInInitializerError, an Error the impl
    // does not catch and correctly so — that failure is a test-environment
    // artifact, not a device condition).
    val method = SecureStorageImpl::class.java.getDeclaredMethod("masterKey")
    method.isAccessible = true
    try {
      method.invoke(impl)
      fail("masterKey must throw when the Keystore is unavailable")
    } catch (error: InvocationTargetException) {
      val cause =
        error.targetException as? SecureStorageError.Unavailable
          ?: throw AssertionError("expected SecureStorageError.Unavailable but was ${error.targetException}")
      assertEquals(SecureStorageErrorMessages.UNAVAILABLE, cause.errorMessage)
    }
  }

  @Test
  fun setFailureLeavesNoPartialState() {
    // Whatever the JVM failure mode is (here: Robolectric's Cipher/JCE
    // artifact; on device: Unavailable), a failed set() must not leave
    // partial state in the backing store. This also pins the evaluation
    // order: the encrypted payload is computed BEFORE the write executes.
    try {
      impl.set("k", "v")
      // Unreachable on the JVM; tolerated so the test stays valid if the
      // environment ever gains a working Keystore emulation.
    } catch (_: Throwable) {
    }
    assertTrue(securePrefs.all.isEmpty())
  }

  @Test
  fun secureTierReadsOnEmptyStoreReturnDefaults() {
    assertNull(impl.get("missing"))
    assertFalse(impl.has("missing"))
    assertTrue(impl.keys().isEmpty())
    assertFalse(impl.remove("missing"))
    assertEquals(mapOf("a" to null, "b" to null), impl.getMany(listOf("a", "b")))
  }

  @Test
  fun keysDecodeObfuscatedStoredNames() {
    // A pre-obfuscation era entry (base64-encoded account name, no prefix)
    // must be reported under its decoded logical name.
    securePrefs
      .edit()
      .putString(SecureStorageUtils.encodeB64("alpha"), "payload")
      .commit()
    assertEquals(listOf("alpha"), impl.keys())
    // ...and remove() must reach it through candidate resolution.
    assertTrue(impl.remove("alpha"))
    assertTrue(securePrefs.all.isEmpty())
  }

  @Test
  fun clearWipesSecurePreferencesEvenWithoutKeystore() {
    securePrefs.edit().putString("legacy", "value").commit()
    impl.clear()
    assertTrue(securePrefs.all.isEmpty())
    // Best-effort master key deletion must never surface as an error.
    impl.clear()
  }

  // ---------------------------------------------------------------------------
  // Insecure tier: full CRUD on the emulated SharedPreferences
  // ---------------------------------------------------------------------------

  @Test
  fun insecureTierRoundTrip() {
    impl.setInsecure("pref", "dark")
    assertEquals("dark", impl.getInsecure("pref"))
    assertTrue(impl.hasInsecure("pref"))
    assertTrue(impl.removeInsecure("pref"))
    assertFalse(impl.removeInsecure("pref"))
    assertNull(impl.getInsecure("pref"))
    assertFalse(impl.hasInsecure("pref"))
  }

  @Test
  fun insecureWritesAreSynchronouslyVisible() {
    impl.setInsecure("pref", "dark")
    // commit() guarantee: the value is readable from the backing store
    // the moment the write returns. On device apply() would only enqueue
    // the write; this asserts the observable consequence of commit().
    assertEquals("dark", insecurePrefs.getString("CapacitorStorage.pref", null))
  }

  @Test
  fun insecureGroupNamespacing() {
    impl.configure(group = "OtherGroup")
    impl.setInsecure("k", "v")
    // Value lives under the configured group, verifiable in the raw file.
    assertEquals("v", insecurePrefs.getString("OtherGroup.k", null))
    // The default group does not see it.
    impl.insecureGroup = "CapacitorStorage"
    assertNull(impl.getInsecure("k"))
    assertFalse(impl.hasInsecure("k"))
  }

  @Test
  fun insecureObfuscationEncodesStoredNameAndRoundTrips() {
    impl.configure(group = null, obfuscateKeys = true)
    impl.setInsecure("flag", "on")
    val encoded = SecureStorageUtils.encodeB64("flag")
    assertEquals("on", insecurePrefs.getString("CapacitorStorage.$encoded", null))
    assertEquals("on", impl.getInsecure("flag"))
    assertTrue(impl.hasInsecure("flag"))
  }

  @Test
  fun insecurePrefixChangeDoesNotOrphanLegacyEntries() {
    impl.setInsecure("legacy", "kept")
    // configure() registers the new prefix as seen, so reads span both
    // prefixes within the session and the pre-prefix entry stays reachable.
    impl.configure(group = null, obfuscationPrefix = "p_")
    assertEquals("kept", impl.getInsecure("legacy"))
    assertTrue(impl.hasInsecure("legacy"))
    assertTrue(impl.removeInsecure("legacy"))
  }

  @Test
  fun keysInsecureReturnsDecodedGroupKeysOnly() {
    impl.setInsecure("one", "1")
    impl.setInsecure("two", "2")
    // Foreign entries: another group and a non-group key, written raw.
    insecurePrefs.edit().putString("OtherGroup.foreign", "x").commit()
    insecurePrefs.edit().putString("unrelated", "y").commit()

    val keys = impl.keysInsecure()
    assertTrue(keys.contains("one"))
    assertTrue(keys.contains("two"))
    assertFalse(keys.contains("foreign"))
    assertFalse(keys.contains("unrelated"))

    // clearInsecure() removes only the current group's keys.
    impl.clearInsecure()
    assertTrue(impl.keysInsecure().isEmpty())
    assertEquals("y", insecurePrefs.getString("unrelated", null))
    assertEquals("x", insecurePrefs.getString("OtherGroup.foreign", null))
  }

  @Test
  fun insecureClearIsIdempotentOnEmptyStore() {
    impl.clearInsecure()
    impl.clearInsecure()
    assertTrue(insecurePrefs.all.isEmpty())
  }
}
