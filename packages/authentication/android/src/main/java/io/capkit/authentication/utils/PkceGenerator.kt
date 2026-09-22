package io.capkit.authentication.utils

import java.security.MessageDigest
import kotlin.random.Random

/**
 * PKCE (RFC 7636) verifier and challenge generation.
 *
 * Pure and side-effect free: given the same verifier, `generateChallenge`
 * always returns the same base64url(SHA-256(verifier)) value without padding.
 *
 * Default verifier length follows the RFC 7636 recommendation of 43–128 chars;
 * callers may pass an explicit length within that range.
 */
object PkceGenerator {
  /**
   * Unreserved characters allowed in a code_verifier (RFC 7636 section 4.1).
   */
  private const val PKCE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

  private const val DEFAULT_VERIFIER_LENGTH = 64

  /**
   * Generates a cryptographically random code_verifier of the given length.
   *
   * @param length number of characters, defaulting to 64 (within the RFC 7636 range).
   * @return a verifier composed only of the RFC 7636 unreserved characters.
   */
  fun generateVerifier(length: Int = DEFAULT_VERIFIER_LENGTH): String {
    require(length in 43..128) { "PKCE verifier length must be between 43 and 128, got $length" }
    val buffer = StringBuilder(length)
    repeat(length) {
      buffer.append(PKCE_ALPHABET[Random.nextInt(PKCE_ALPHABET.length)])
    }
    return buffer.toString()
  }

  /**
   * Computes the RFC 7636 code_challenge for a verifier:
   * `base64url(sha256(verifier))` with padding removed.
   *
   * @param verifier the previously generated code_verifier.
   * @return a base64url-encoded challenge value without trailing '=' padding.
   */
  fun generateChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64Url.encode(digest)
  }
}
