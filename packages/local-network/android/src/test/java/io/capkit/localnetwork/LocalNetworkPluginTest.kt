package io.capkit.localnetwork

import com.getcapacitor.PluginMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPluginTest {
  /**
   * Every method declared in `src/definitions.ts` must be annotated with
   * [PluginMethod], otherwise the Capacitor bridge answers the JavaScript
   * call with `UNIMPLEMENTED` on Android.
   *
   * Only methods declared by this class are inspected: the [Plugin] base
   * class annotates its own listener and permission methods.
   */
  @Test
  fun exposesEveryMethodDeclaredInTheJavaScriptInterface() {
    val exposed =
      LocalNetworkPlugin::class.java.declaredMethods
        .filter { it.isAnnotationPresent(PluginMethod::class.java) }
        .map { it.name }
        .toSet()

    assertEquals(setOf("checkPermissions", "requestPermissions", "getPluginVersion"), exposed)
  }

  /**
   * `BuildConfig.PLUGIN_VERSION` comes from `package.json` at build time;
   * an empty or stale value means the build script never injected it.
   */
  @Test
  fun versionIsASemanticVersion() {
    val pattern = Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z.\-]+)?$""")

    assertTrue(
      "Unexpected BuildConfig.PLUGIN_VERSION: ${BuildConfig.PLUGIN_VERSION}",
      pattern.matches(BuildConfig.PLUGIN_VERSION),
    )
  }
}
