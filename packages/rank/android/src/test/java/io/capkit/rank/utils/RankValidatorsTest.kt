package io.capkit.rank.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankValidatorsTest {
  @Test
  fun validatePackageName_returnsValue_forValidPackageName() {
    assertEquals("com.example.app", RankValidators.validatePackageName("com.example.app"))
  }

  @Test
  fun validatePackageName_returnsNull_forMissingValue() {
    assertNull(RankValidators.validatePackageName(null))
  }

  @Test
  fun validateSearchTerms_returnsTrimmed_forValidInput() {
    assertEquals("hello world", RankValidators.validateSearchTerms("  hello world  "))
  }

  @Test
  fun validateSearchTerms_returnsNull_forMissingValue() {
    assertNull(RankValidators.validateSearchTerms("   "))
  }
}
