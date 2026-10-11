package io.capkit.authentication.apple

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file FormPostValueDecoderTest.kt
 * RED-GREEN contract for URL-decoding an Apple OAuth `form_post` payload captured
 * by the WebView: the raw `application/x-www-form-urlencoded` body arrives
 * percent-encoded (Apple percent-encodes every value, including the stringified
 * `user` JSON), and the decoder MUST restore the original values before the
 * payload is handed to [FormPostParser]. Pure, no Android
 * runtime deps; malformed input yields null/empty — never a crash.
 */
class FormPostValueDecoderTest {
  @Test
  fun `decodes a plain form body with simple values`() {
    val result = FormPostValueDecoder.decodeFormBody("state=st-1&code=auth-code-1")

    assertEquals(2, result.size)
    assertEquals("st-1", result["state"])
    assertEquals("auth-code-1", result["code"])
  }

  @Test
  fun `decodes percent encoded values including plus signs`() {
    val result =
      FormPostValueDecoder.decodeFormBody(
        "id_token=a.b.c&scope=name+email&percent=%7E%21%40",
      )

    assertEquals("a.b.c", result["id_token"])
    assertEquals("name email", result["scope"])
    assertEquals("~!@", result["percent"])
  }

  @Test
  fun `decodes the stringified user JSON exactly as Apple delivers it`() {
    val rawUser =
      "%7B%22email%22%3A%22ada%40example.com%22%2C%22name%22%3A%7B%22firstName%22%3A%22Ada%22%2C%22lastName%22%3A%22Lovelace%22%7D%7D"
    val result = FormPostValueDecoder.decodeFormBody("code=auth-code-1&user=$rawUser")

    assertEquals("auth-code-1", result["code"])
    assertEquals(
      """{"email":"ada@example.com","name":{"firstName":"Ada","lastName":"Lovelace"}}""",
      result["user"],
    )
  }

  @Test
  fun `decodes a value whose content contains encoded reserved characters`() {
    val result =
      FormPostValueDecoder.decodeFormBody(
        "state=st%26x%3D1&code=abc%2Fdef%2Bghi%20jkl",
      )

    assertEquals("st&x=1", result["state"])
    assertEquals("abc/def+ghi jkl", result["code"])
  }

  @Test
  fun `returns an empty map for null or blank bodies`() {
    assertEquals(emptyMap<String, String>(), FormPostValueDecoder.decodeFormBody(null))
    assertEquals(emptyMap<String, String>(), FormPostValueDecoder.decodeFormBody(""))
    assertEquals(emptyMap<String, String>(), FormPostValueDecoder.decodeFormBody("   "))
  }

  @Test
  fun `tolerates malformed pairs without crashing`() {
    val result = FormPostValueDecoder.decodeFormBody("state=st-1&&code=%ZZ&")

    assertEquals("st-1", result["state"])
    assertEquals(1, result.size)
  }

  @Test
  fun `builds a form post JSON string embedding the user as raw JSON`() {
    val result =
      FormPostValueDecoder.toFormPostJson(
        mapOf(
          "code" to "auth-code-1",
          "id_token" to "header.payload.signature",
          "state" to "st-1",
          "user" to """{"email":"ada@example.com"}""",
        ),
      )

    assertEquals(
      """{"code":"auth-code-1","id_token":"header.payload.signature","state":"st-1","user":{"email":"ada@example.com"}}""",
      result,
    )
  }

  @Test
  fun `builds a form post JSON string without user when absent`() {
    val result =
      FormPostValueDecoder.toFormPostJson(
        mapOf("code" to "auth-code-1", "state" to "st-1"),
      )

    assertEquals("""{"code":"auth-code-1","state":"st-1"}""", result)
  }

  @Test
  fun `returns null when the decoded map cannot form a JSON object`() {
    assertNull(FormPostValueDecoder.toFormPostJson(emptyMap()))
  }

  @Test
  fun `keeps the credential when the embedded user JSON is malformed`() {
    // An optional, malformed profile field must never destroy a valid sign-in:
    // the user stays decodable-looking (string) so FormPostParser drops the
    // profile while preserving code/id_token (security baseline).
    val json =
      FormPostValueDecoder.toFormPostJson(
        mapOf("code" to "auth-code-1", "state" to "st-1", "user" to "{not-json"),
      )

    val parsed = FormPostParser.parse(checkNotNull(json))
    assertEquals("auth-code-1", parsed?.authorizationCode)
    assertEquals("st-1", parsed?.state)
    assertNull(parsed?.user)
  }

  @Test
  fun `decoded form body round trips through the parser for Apple delivery`() {
    val rawBody =
      "code=auth-code-1&id_token=header.payload.signature&state=st-1&user=%7B%22email%22%3A%22ada%40example.com%22%7D"
    val decoded = FormPostValueDecoder.decodeFormBody(rawBody)
    val json = FormPostValueDecoder.toFormPostJson(decoded)

    val parsed = FormPostParser.parse(checkNotNull(json))
    assertEquals("auth-code-1", parsed?.authorizationCode)
    assertEquals("header.payload.signature", parsed?.idToken)
    assertEquals("st-1", parsed?.state)
    assertEquals("ada@example.com", parsed?.user?.email)
  }
}
