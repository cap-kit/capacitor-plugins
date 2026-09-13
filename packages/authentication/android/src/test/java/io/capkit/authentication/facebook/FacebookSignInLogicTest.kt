package io.capkit.authentication.facebook

import io.capkit.authentication.error.AuthenticationError
import io.capkit.authentication.model.SocialAuthResultUser
import io.capkit.authentication.utils.ErrorCodeMapper
import io.capkit.authentication.vault.TokenBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @file FacebookSignInLogicTest.kt
 * RED-GREEN contract for the pure Facebook sign-in orchestration core:
 * the single-flight gate, the typed
 * outcome mapping onto [FacebookErrorMapper] (ten codes, zero new), and the vault
 * persist/clear hooks. The SDK-dependent dialog surface (CallbackManager /
 * LoginManager / GraphRequest) is bound in the wiring slice through
 * [FacebookSdkGateway]; the JVM core depends only on that seam.
 */
class FacebookSignInLogicTest {
  private class RecordingGateway : FacebookSdkGateway {
    val launchedScopes = mutableListOf<List<String>>()
    var logOutCalls = 0

    override fun launchLogin(scopes: List<String>) {
      launchedScopes += scopes
    }

    override fun logOut() {
      logOutCalls++
    }
  }

  @Test
  fun `begin launches the gateway with the configured scopes`() {
    val gateway = RecordingGateway()
    val impl = FacebookSignInImpl(gateway, persist = {}, clearVault = {})

    val error = impl.begin(listOf("public_profile", "email"))

    assertEquals(null, error)
    assertEquals(listOf(listOf("public_profile", "email")), gateway.launchedScopes)
    assertTrue(impl.isInFlight)
  }

  @Test
  fun `begin while in flight rejects CONFLICT and does not relaunch the gateway`() {
    val gateway = RecordingGateway()
    val impl = FacebookSignInImpl(gateway, persist = {}, clearVault = {})

    impl.begin(listOf("email"))
    val error = impl.begin(listOf("email"))

    assertTrue(error is AuthenticationError.Conflict)
    assertEquals("CONFLICT", ErrorCodeMapper.mapCode(error!!))
    assertEquals(1, gateway.launchedScopes.size)
    assertTrue(impl.isInFlight)
  }

  @Test
  fun `complete success persists the mapped bundle and releases the gate`() {
    val gateway = RecordingGateway()
    val persisted = mutableListOf<TokenBundle>()
    val impl = FacebookSignInImpl(gateway, persist = { persisted += it }, clearVault = {})

    impl.begin(listOf("public_profile"))
    val bundle =
      impl.completeSuccess(
        accessToken = "fb-at-1",
        userId = "12345",
        grantedPermissions = listOf(" public_profile ", "email"),
        declinedPermissions = listOf("user_photos"),
        expiresAtMillis = 1700000123456L,
        profile =
          SocialAuthResultUser(
            id = "12345",
            email = "grace@example.com",
            picture = "https://example.com/fb.jpg",
          ),
      )

    assertEquals("fb-at-1", bundle.accessToken)
    assertEquals(1, persisted.size)
    assertEquals(bundle, persisted.single())
    assertEquals(listOf("public_profile", "email"), persisted.single().grantedPermissions)
    assertEquals(1700000123L, persisted.single().expiresAt)
    assertEquals("https://example.com/fb.jpg", persisted.single().user?.picture)
    assertFalse(impl.isInFlight)
  }

  @Test
  fun `complete cancellation releases the gate without persisting`() {
    val gateway = RecordingGateway()
    val persisted = mutableListOf<TokenBundle>()
    val impl = FacebookSignInImpl(gateway, persist = { persisted += it }, clearVault = {})

    impl.begin(listOf("email"))
    impl.completeCancellation()

    assertFalse(impl.isInFlight)
    assertEquals(0, persisted.size)
  }

  @Test
  fun `complete failure releases the gate and maps the error onto the ten-code set`() {
    val gateway = RecordingGateway()
    val impl = FacebookSignInImpl(gateway, persist = {}, clearVault = {})

    impl.begin(listOf("email"))
    val graphError = impl.completeFailure(FacebookSignInError.GRAPH_NETWORK)
    assertEquals("UNAVAILABLE", ErrorCodeMapper.mapCode(graphError))
    assertFalse(impl.isInFlight)

    impl.begin(listOf("email"))
    val refreshError = impl.completeFailure(FacebookSignInError.REFRESH_NOT_SUPPORTED)
    assertEquals("INVALID_INPUT", ErrorCodeMapper.mapCode(refreshError))
    assertEquals("Facebook issues no refresh token", refreshError.errorMessage)
    assertFalse(impl.isInFlight)
  }

  @Test
  fun `sign out calls the gateway logout and clears the vault`() {
    val gateway = RecordingGateway()
    var vaultCleared = 0
    val impl = FacebookSignInImpl(gateway, persist = {}, clearVault = { vaultCleared++ })

    impl.signOut()

    assertEquals(1, gateway.logOutCalls)
    assertEquals(1, vaultCleared)
  }
}
