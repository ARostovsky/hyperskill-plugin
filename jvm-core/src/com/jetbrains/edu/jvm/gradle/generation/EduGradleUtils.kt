package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtilRt
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createFileFromTemplate
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleUtils {
  @JvmStatic
  fun isConfiguredWithGradle(project: Project): Boolean {
    return File(project.basePath, GradleConstants.DEFAULT_SCRIPT_NAME).exists()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun createProjectGradleFiles(
    projectDir: VirtualFile,
    templates: Map<String, String>,
    templateVariables: Map<String, Any>
  ) {
    for ((name, templateName) in templates) {
      createFileFromTemplate(projectDir, name, templateName, templateVariables)
    }
  }

  @JvmOverloads
  @JvmStatic
  fun setGradleSettings(project: Project, sdk: Sdk?, location: String, distributionType: DistributionType = DistributionType.WRAPPED) {
    val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val existingProject = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(location)
    if (existingProject is GradleProjectSettings) {
      if (existingProject.distributionType == null) {
        existingProject.distributionType = distributionType
      }
      if (existingProject.externalProjectPath == null) {
        existingProject.externalProjectPath = location
      }
      setUpGradleJvm(existingProject, sdk)
      return
    }

    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.distributionType = distributionType
    gradleProjectSettings.isUseAutoImport = true
    gradleProjectSettings.externalProjectPath = location
    // IDEA runner is much more faster and it doesn't write redundant messages into console.
    // Note, it doesn't affect tests - they still are run with gradle runner
    gradleProjectSettings.delegatedBuild = false
    setUpGradleJvm(gradleProjectSettings, sdk)

    val projects = ContainerUtilRt.newHashSet<Any>(systemSettings.getLinkedProjectsSettings())
    projects.add(gradleProjectSettings)
    systemSettings.setLinkedProjectsSettings(projects)
    ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID)
  }

  private fun setUpGradleJvm(projectSettings: GradleProjectSettings, sdk: Sdk?) {
    val javaVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk)

    // Java 13 requires gradle 6.0.
    // If the current bundled gradle version is less than 6.0, let's delegate selection of `gradleJvm` to IDE itself.
    if ((javaVersion == null || javaVersion >= JavaSdkVersion.JDK_13) && GradleVersion.current() < GradleVersion.version("6.0")) {
      projectSettings.gradleJvm = null
    }
  }
}
