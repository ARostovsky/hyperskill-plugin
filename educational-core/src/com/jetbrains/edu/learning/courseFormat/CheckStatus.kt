package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.EduNames.*

enum class CheckStatus(val rawStatus: String) {
  Unchecked(UNCHECKED),
  Solved(CORRECT),
  Failed(WRONG);

  companion object {
    @JvmStatic
    fun String.toCheckStatus(): CheckStatus = when (this) {
      CORRECT -> Solved
      WRONG -> Failed
      else -> Unchecked
    }
  }
}