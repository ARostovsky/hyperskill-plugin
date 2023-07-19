package com.jetbrains.edu.learning.configuration

import com.intellij.lang.Language
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYCHARM
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.StepikNames

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language, courseType and environment
   */
  fun findConfigurator(courseType: String, environment: String, language: Language): EduConfigurator<*>? =
    when (courseType) {
      CODEFORCES -> CodeforcesLanguageProvider.getConfigurator(language.id)
      MARKETPLACE -> findExtension(PYCHARM, environment, language)?.instance
      else -> findExtension(courseType, environment, language)?.instance
    }

  fun findExtension(courseType: String, environment: String, language: Language): EducationalExtensionPoint<EduConfigurator<*>>? {
    var configurator =
      allExtensions().find { extension ->
        extension.language == language.id &&
        extension.courseType == courseType &&
        extension.environment == environment
      }
    if (configurator == null) {
      configurator = allExtensions().find { extension ->
        extension.language == language.id &&
        compatibleCourseType(extension, courseType) &&
        extension.environment == environment
      }
    }
    return configurator
  }

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<*>>> =
    EducationalExtensionPoint.EP_NAME.extensions.filter { it.instance.isEnabled }

  /**
   * Returns all languages with enabled [EduConfigurator] for [PYCHARM] course type
   */
  val supportedEduLanguages: List<String>
    get() {
      return allExtensions().filter { it.courseType == PYCHARM }.map { it.language }
    }

  private val compatibleCourseTypes: List<String> = listOf(CourseraNames.COURSERA, StepikNames.STEPIK, EduFormatNames.MARKETPLACE)

  private fun compatibleCourseType(extension: EducationalExtensionPoint<EduConfigurator<*>>, courseType: String): Boolean {
    return extension.courseType == PYCHARM && courseType in compatibleCourseTypes
  }

  fun supportedEnvironments(language: Language): List<String> =
    allExtensions().filter { it.language == language.id && it.courseType == PYCHARM }.map { it.environment }.distinct()
}
