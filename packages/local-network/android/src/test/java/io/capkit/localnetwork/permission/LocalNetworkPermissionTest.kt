package io.capkit.localnetwork.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionTest {
  @Test
  fun permissionConstantsMatchAndroidApi37() {
    assertEquals("android.permission.ACCESS_LOCAL_NETWORK", LocalNetworkPermission.NAME)
    assertEquals("localNetwork", LocalNetworkPermission.ALIAS)
    assertEquals(37, LocalNetworkPermission.ANDROID_API_LOCAL_NETWORK)
  }

  @Test
  fun notEnforcedBelowApi37() {
    assertFalse(LocalNetworkPermission.isEnforced(24))
    assertFalse(LocalNetworkPermission.isEnforced(36))
  }

  @Test
  fun enforcedOnApi37AndAbove() {
    assertTrue(LocalNetworkPermission.isEnforced(37))
    assertTrue(LocalNetworkPermission.isEnforced(38))
  }
}
