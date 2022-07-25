package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

class CheckResultDiffMatcher(private val expected: CheckResultDiff) : BaseMatcher<CheckResultDiff?>() {
  override fun describeTo(description: Description) {
    description.appendValue(expected)
  }

  override fun matches(actual: Any?): Boolean {
    if (actual !is CheckResultDiff) return false
    return expected.title == actual.title && expected.actual == actual.actual && expected.expected == actual.expected
  }

  companion object {
    @JvmStatic
    fun diff(expected: CheckResultDiff): Matcher<CheckResultDiff?> = CheckResultDiffMatcher(expected)
  }
}
