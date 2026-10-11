package io.capkit.authentication.apple

import io.capkit.authentication.model.NameParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * @file FormPostParserTest.kt
 * RED-GREEN contract for parsing the Apple OAuth `form_post` callback body into
 * structured parameters: `code`, `id_token`, `state` and `user`.
 * Malformed or missing input must yield `null` /
 * no crash (security baseline). Pure, no Android runtime deps.
 */
class FormPostParserTest {
  @Test
  fun `parses code id_token and state from a well formed body`() {
    val result =
      FormPostParser.parse(
        """{"code":"auth-code-1","id_token":"header.payload.signature","state":"st-1"}""",
      )

    assertNotNull(result)
    result!!
    assertEquals("auth-code-1", result.authorizationCode)
    assertEquals("header.payload.signature", result.idToken)
    assertEquals("st-1", result.state)
    assertNull(result.user)
  }

  @Test
  fun `parses user from nested JSON object with name parts`() {
    val result =
      FormPostParser.parse(
        """{"code":"auth-code-1","user":{"email":"ada@example.com","name":{"firstName":"Ada","lastName":"Lovelace"}}}""",
      )

    assertNotNull(result)
    assertEquals("auth-code-1", result!!.authorizationCode)
    assertNotNull(result.user)
    assertEquals("ada@example.com", result.user?.email)
    assertEquals(NameParts(firstName = "Ada", lastName = "Lovelace"), result.user?.name)
  }

  @Test
  fun `parses user from a stringified JSON payload as Apple delivers it`() {
    val result =
      FormPostParser.parse(
        """{"code":"auth-code-1","user":"{\"email\":\"ada@example.com\",\"name\":{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}}"}""",
      )

    assertNotNull(result)
    assertEquals("auth-code-1", result!!.authorizationCode)
    assertNotNull(result.user)
    assertEquals("ada@example.com", result.user?.email)
    assertEquals(NameParts(firstName = "Ada", lastName = "Lovelace"), result.user?.name)
  }

  @Test
  fun `returns null when body is not valid JSON`() {
    assertNull(FormPostParser.parse("not-json"))
  }

  @Test
  fun `returns null when body is empty`() {
    assertNull(FormPostParser.parse(""))
    assertNull(FormPostParser.parse("   "))
  }

  @Test
  fun `returns null when no code or id token present`() {
    assertNull(FormPostParser.parse("""{"state":"st-1"}"""))
    assertNull(FormPostParser.parse("""{}"""))
  }

  @Test
  fun `surfaces id token when present without code`() {
    val result = FormPostParser.parse("""{"id_token":"header.payload.signature"}""")
    assertNotNull(result)
    assertEquals("header.payload.signature", result!!.idToken)
    assertNull(result.authorizationCode)
  }

  @Test
  fun `tolerates unknown keys`() {
    val result =
      FormPostParser.parse(
        """{"code":"auth-code-1","futureKey":42,"extra":"x"}""",
      )
    assertNotNull(result)
    assertEquals("auth-code-1", result!!.authorizationCode)
  }
}
