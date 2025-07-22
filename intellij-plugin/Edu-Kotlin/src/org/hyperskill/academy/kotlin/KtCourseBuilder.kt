package org.hyperskill.academy.kotlin

import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.learning.courseFormat.Course

open class KtCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = KOTLIN_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = KtConfigurator.TASK_KT
  override fun mainTemplateName(course: Course): String = KtConfigurator.MAIN_KT
  override fun testTemplateName(course: Course): String = KtConfigurator.TESTS_KT

  override fun getSupportedLanguageVersions(): List<String> = listOf("1.2", "1.3", "1.4", "1.5", "1.6", "1.7")

  override fun getLanguageSettings() = KtLanguageSettings()

  companion object {
    const val KOTLIN_BUILD_GRADLE_TEMPLATE_NAME = "kotlin-build.gradle"
  }
}
