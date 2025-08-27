package org.hyperskill.academy.learning.courseFormat

enum class CheckResultSeverity {
  Info, Warning, Error;

  fun isWaring() = this == Warning
  fun isInfo() = this == Info
}