package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.JdkBundle
import com.jetbrains.edu.java.messages.EduJavaBundle
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import java.io.File

class JLanguageSettings : JdkLanguageSettings() {

  override fun setupProjectSdksModel(model: ProjectSdksModel) {
    val jdkBundle = JdkBundle.createBundled()
    if (jdkBundle != null && jdkBundle.isJdk) {
      val bundledJdkPath = jdkBundle.homeLocation.absolutePath
      if (model.sdks.none { it.homePath == bundledJdkPath }) {
        model.addSdk(JavaSdk.getInstance(), bundledJdkPath, null)
      }
    }
  }

  private val JdkBundle.homeLocation get(): File {
    // `JdkBundle#getLocation` returns only location of bundled jdk/jre root folder
    // but we need to get directory where `javac` is located, and for macOS these folders are not the same
    // See implementation of `com.intellij.util.JdkBundle#createBundle`
    return if (SystemInfo.isMac) File(location, "Contents/Home") else location
  }

  override fun getLanguageVersions() = JavaSdkVersion.values().filter { it.isAtLeast(DEFAULT_JAVA) }.map { it.description }

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    if (course != null) {
      val courseJavaVersionDescription = course.languageVersion ?: DEFAULT_JAVA.description
      val courseJavaVersion = courseJavaVersionDescription.toJavaSdkVersion()
                              ?: return ValidationMessage(EduJavaBundle.message("unsupported.java.version", courseJavaVersionDescription))

      val jdkItem = myJdkSettings.jdkItem
      // BACKCOMPAT: 2019.3
      @Suppress("DEPRECATION")
      if (jdkItem !is JdkComboBox.SuggestedJdkItem) {
        val providedJavaVersion = jdkItem?.jdk?.versionString ?: return ValidationMessage(EduJavaBundle.message("no.java.sdk"))

        val javaSdkVersion = JavaSdkVersion.fromVersionString(providedJavaVersion)
                             ?: return ValidationMessage(EduJavaBundle.message("failed.to.get.java.version"))
        if (javaSdkVersion.isAtLeast(courseJavaVersion)) {
          return null
        }
        return ValidationMessage(
          EduJavaBundle.message("update.java.version.before.link", courseJavaVersionDescription),
          EduJavaBundle.message("update.java.version.link.text"),
          "",
          "https://www.oracle.com/technetwork/java/javase/downloads/index.html"
        )
      }
    }
    return super.validate(course, courseLocation)
  }

  override fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    val suitableJdk = findSuitableJdk(course, sdksModel) ?: return
    jdkComboBox.selectedJdk = suitableJdk
  }

  companion object {
    private val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8

    @JvmStatic
    fun findSuitableJdk(course: Course, sdkModel: ProjectSdksModel): Sdk? {
      val courseJavaVersion = (course.languageVersion ?: DEFAULT_JAVA.description).toJavaSdkVersion() ?: return null
      val jdks = sdkModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }
      return jdks.find { JavaSdkVersion.fromVersionString(it.versionString ?: "")?.isAtLeast(courseJavaVersion) ?: false }
    }

    private fun String.toJavaSdkVersion() = JavaSdkVersion.values().find { it.description == this }
  }
}