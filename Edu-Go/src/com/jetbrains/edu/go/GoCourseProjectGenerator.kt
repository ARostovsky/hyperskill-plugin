package com.jetbrains.edu.go

import com.goide.sdk.GoSdkService
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil.createDirectoryIfMissing
import com.intellij.openapi.vfs.VfsUtil.getUserHomeDir
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseProjectGenerator(builder: GoCourseBuilder, course: Course) :
  CourseProjectGenerator<GoProjectSettings>(builder, course) {
  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    val modFileName = "go.mod"
    GeneratorUtils.createChildFile(baseDir, modFileName, getInternalTemplateText(modFileName))
  }

  override fun afterProjectGenerated(project: Project, projectSettings: GoProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    GoSdkService.getInstance(project).setSdk(projectSettings.sdk)
    val homePath = getUserHomeDir()?.path ?: return
    invokeAndWaitIfNeeded {
      runWriteAction { createDirectoryIfMissing(joinPaths(homePath, EduNames.GO)) }
    }
  }
}
