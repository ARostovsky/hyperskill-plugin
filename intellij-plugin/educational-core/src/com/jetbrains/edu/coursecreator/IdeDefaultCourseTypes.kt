@file:JvmName("IdeDefaultCourseTypes")
package com.jetbrains.edu.coursecreator

import com.intellij.lang.Language
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CPP
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.GO
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.JAVASCRIPT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PHP
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.RUST

fun getDefaultCourseType(courses: List<CCNewCoursePanel.CourseData>): CCNewCoursePanel.CourseData? =
  courses.find { it.language == Language.findLanguageByID(getDefaultLanguageId()) } ?: courses.firstOrNull()

fun getDefaultLanguageId(): String? = when {
  PlatformUtils.isIntelliJ() || EduUtilsKt.isAndroidStudio() -> KOTLIN
  PlatformUtils.isPyCharm() -> PYTHON
  PlatformUtils.isWebStorm() -> JAVASCRIPT
  PlatformUtils.isCLion() -> CPP
  PlatformUtils.isGoIde() -> GO
  PlatformUtils.isPhpStorm() -> PHP
  // BACKCOMPAT: 2023.2
  @Suppress("DEPRECATION")
  EduUtilsKt.isRustRover() -> RUST
  else -> null
}


