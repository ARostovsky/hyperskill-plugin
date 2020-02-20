package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode

object CodeforcesContestsProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    checkIsBackgroundThread()
    val taskTextLanguage = CodeforcesSettings.getInstance().preferableTaskTextLanguage ?: TaskTextLanguage.ENGLISH
    return if (isUnitTestMode) emptyList() else CodeforcesContestLoader.getContestInfos(locale = taskTextLanguage.locale)
  }
}