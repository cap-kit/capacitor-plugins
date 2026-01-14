package io.capkit.test

/**
 * Native implementation for the Test Capacitor plugin.
 *
 * This class contains platform logic only and does not depend on
 * Capacitor plugin APIs.
 */
class Test {
  /**
   * Returns the provided string unchanged.
   *
   * @param value Input string.
   * @return The same string.
   */
  fun echo(value: String): String {
    Logger.debug(value)
    return value
  }
}
