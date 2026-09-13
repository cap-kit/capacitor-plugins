package io.capkit.authentication.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file JwtDecoderTest.kt
 * RED-GREEN contract for decoding the Apple `id_token` JWT payload without
 * signature validation: Base64URL extraction of `sub`, `email` and nonce claims.
 * Malformed JWT, bad base64
 * or non-JSON payload must yield `null` — never crash (security baseline).
 */
class JwtDecoderTest {
  private fun encodeSegment(json: String): String =
    java.util.Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString(json.toByteArray(Charsets.UTF_8))

  private fun buildToken(payload: String): String =
    "${encodeSegment("""{"alg":"RS256","kid":"abc"}""")}.$payload.signature"

  @Test
  fun `decodes sub email and nonce from a well formed id token`() {
    val token =
      buildToken(
        encodeSegment(
          """{"iss":"https://appleid.apple.com","sub":"001234.45678","email":"ada@example.com","nonce":"nc-1"}""",
        ),
      )

    val claims = JwtDecoder.decode(token)

    assertNotNull(claims)
    assertEquals("001234.45678", claims!!.sub)
    assertEquals("ada@example.com", claims.email)
    assertEquals("nc-1", claims.nonce)
  }

  @Test
  fun `returns an empty-ish claims object when optional claims are absent`() {
    val token = buildToken(encodeSegment("""{"sub":"001234.45678"}"""))

    val claims = JwtDecoder.decode(token)

    assertNotNull(claims)
    assertEquals("001234.45678", claims!!.sub)
    assertNull(claims.email)
    assertNull(claims.nonce)
  }

  @Test
  fun `returns null for a malformed jwt with too few segments`() {
    assertNull(JwtDecoder.decode("just-onepart"))
    assertNull(JwtDecoder.decode("header.onlytwo"))
  }

  @Test
  fun `returns null for a token with bad base64 in the payload`() {
    val token = "${encodeSegment("""{"alg":"RS256"}""")}.!!!not-base64!!!.sig"
    assertNull(JwtDecoder.decode(token))
  }

  @Test
  fun `returns null for a payload that is not JSON`() {
    val token = buildToken(encodeSegment("not-json"))
    assertNull(JwtDecoder.decode(token))
  }

  @Test
  fun `returns null for an empty or blank token`() {
    assertNull(JwtDecoder.decode(""))
    assertNull(JwtDecoder.decode("   "))
  }

  @Test
  fun `decodes private relay email claim`() {
    val token =
      buildToken(
        encodeSegment(
          """{"iss":"https://appleid.apple.com","sub":"001234.45678","email":"abc123@privaterelay.appleid.com","is_private_email":true}""",
        ),
      )

    val claims = JwtDecoder.decode(token)

    assertNotNull(claims)
    assertEquals("abc123@privaterelay.appleid.com", claims!!.email)
  }
}
