package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun createCourseStructure(project: Project, module: Module, baseDir: VirtualFile, settings: JdkProjectSettings) {
      GeneratorUtils.removeModule(project, module)

    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
    super.createCourseStructure(project, module, baseDir, settings)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    val jdk = setJdk(project, projectSettings)
    setupGradleSettings(project, jdk)
    super.afterProjectGenerated(project, projectSettings)
  }

  protected open fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
    val gradleCourseBuilder = myCourseBuilder as GradleCourseBuilderBase
    EduGradleUtils.createProjectGradleFiles(baseDir, gradleCourseBuilder.templates,
                                            gradleCourseBuilder.templateVariables(project))
  }

  private fun setJdk(project: Project, settings: JdkProjectSettings): Sdk? {
    val jdk = getJdk(settings)

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      settings.model.apply()
    } catch (e: ConfigurationException) {
      LOG.error(e)
    }

    runWriteAction {
      ProjectRootManager.getInstance(project).projectSdk = jdk
      addAnnotations(ProjectRootManager.getInstance(project).projectSdk?.sdkModificator)
    }
    return jdk
  }

  private fun addAnnotations(sdkModificator: SdkModificator?) {
    sdkModificator?.apply {
      JavaSdkImpl.attachJdkAnnotations(this)
      this.commitChanges()
    }
  }

  protected open fun getJdk(settings: JdkProjectSettings): Sdk? {
    return settings.jdkItem?.jdk
  }

  companion object {

    private val LOG = Logger.getInstance(GradleCourseProjectGenerator::class.java)
    // Unfortunately, org.jetbrains.plugins.gradle.service.project.GradleStartupActivity#SHOW_UNLINKED_GRADLE_POPUP is private
    // so create own const
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
