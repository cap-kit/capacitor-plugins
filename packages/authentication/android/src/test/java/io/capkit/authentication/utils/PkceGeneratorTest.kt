package io.capkit.authentication.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JUnit4 tests for [PkceGenerator].
 *
 * PKEI (RFC 7636) verifier/challenge generation is pure and side-effect free,
 * the only executable proof tier under `pnpm test` on this plugin.
 */
class PkceGeneratorTest {
  @Test
  fun `generateVerifier returns a string of the requested length`() {
    val verifier = PkceGenerator.generateVerifier(64)

    assertEquals(64, verifier.length)
  }

  @Test
  fun `generateVerifier uses only the RFC 7636 unreserved charset`() {
    val verifier = PkceGenerator.generateVerifier(64)

    val allowed = Regex("[A-Za-z0-9._~-]+")
    assertTrue("verifier contained invalid PKCE characters: $verifier", allowed.matches(verifier))
  }

  @Test
  fun `generateChallenge matches the RFC 7636 appendix A vector`() {
    val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

    val challenge = PkceGenerator.generateChallenge(verifier)

    assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)
  }

  @Test
  fun `generateChallenge produces a base64url value without padding`() {
    val challenge = PkceGenerator.generateChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")

    assertFalse("challenge must not contain padding '='", challenge.contains('='))
    assertFalse("challenge must not contain base64url-invalid '+'", challenge.contains('+'))
    assertFalse("challenge must not contain base64url-invalid '/'", challenge.contains('/'))
  }

  @Test
  fun `distinct verifiers yield distinct challenges`() {
    val a = PkceGenerator.generateChallenge("verifier-one-aaaaaaaaaaaaaaaa")
    val b = PkceGenerator.generateChallenge("verifier-two-bbbbbbbbbbbbbbbb")

    assertFalse("different verifiers must produce different challenges", a == b)
  }
}
