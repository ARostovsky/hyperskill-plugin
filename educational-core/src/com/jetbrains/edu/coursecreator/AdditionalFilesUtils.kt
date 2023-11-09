package com.jetbrains.edu.coursecreator

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction
import com.jetbrains.edu.coursecreator.actions.CourseArchiveIndicator
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.api.TaskAdditionalInfo
import com.jetbrains.edu.learning.stepik.collectTaskFiles
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import java.io.IOException

object AdditionalFilesUtils {
  private val LOG = Logger.getInstance(AdditionalFilesUtils::class.java)

  fun collectAdditionalFiles(course: Course, project: Project, indicator: CourseArchiveIndicator? = null): List<EduFile> {
    ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }

    val fileVisitor = additionalFilesVisitor(project, course, indicator)
    VfsUtilCore.visitChildrenRecursively(project.courseDir, fileVisitor)
    return fileVisitor.additionalTaskFiles
  }

  private fun isExcluded(file: VirtualFile, courseIgnoreRules: CourseIgnoreRules, course: Course, project: Project): Boolean =
    courseIgnoreRules.isIgnored(file) || excludedByConfigurator(file, course, project)

  private fun excludedByConfigurator(file: VirtualFile, course: Course, project: Project): Boolean =
    course.configurator?.excludeFromArchive(project, course, file) ?: false

  @Suppress("DEPRECATION") // https://youtrack.jetbrains.com/issue/EDU-4930
  fun collectAdditionalLessonInfo(lesson: Lesson, project: Project): LessonAdditionalInfo {
    val nonPluginTasks = lesson.taskList.filter { !it.isPluginTaskType }
    val taskInfo = nonPluginTasks.associateBy(Task::id) {
      TaskAdditionalInfo(it.name, it.customPresentableName, collectTaskFiles(project, it))
    }
    val courseFiles: List<EduFile> = if (lesson.course is HyperskillCourse) collectAdditionalFiles(lesson.course, project) else listOf()
    return LessonAdditionalInfo(lesson.customPresentableName, taskInfo, courseFiles)
  }

  fun getChangeNotesVirtualFile(project: Project): VirtualFile? {
    return project.courseDir.findChild(EduNames.CHANGE_NOTES)
  }

  private fun additionalFilesVisitor(project: Project, course: Course, indicator: CourseArchiveIndicator?) =
    object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      // we take the course ignore rules once, and we are sure they are not changed while course archive is being created
      private val courseIgnoreRules = CourseIgnoreRules.loadFromCourseIgnoreFile(project)

      val additionalTaskFiles = mutableListOf<EduFile>()
      var archiveLocation = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchiveAction.LAST_ARCHIVE_LOCATION)

      override fun visitFile(file: VirtualFile): Boolean {
        if (FileUtil.toSystemDependentName(file.path) == archiveLocation || isExcluded(file, courseIgnoreRules, course, project)) {
          return false
        }

        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          return file.getTask(project) == null
        }

        addToAdditionalFiles(file, project, indicator)
        return false
      }

      private fun addToAdditionalFiles(file: VirtualFile, project: Project, indicator: CourseArchiveIndicator?) {
        try {
          createAdditionalTaskFile(file, project, indicator)?.also { taskFile -> additionalTaskFiles.add(taskFile) }
        }
        catch (e: FileTooBigException) {
          throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong(), false)
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }

      private fun createAdditionalTaskFile(file: VirtualFile, project: Project, indicator: CourseArchiveIndicator?): EduFile? {
        val taskFile = file.getTaskFile(project)
        if (taskFile != null) return null

        val path = VfsUtilCore.getRelativePath(file, project.courseDir) ?: return null
        val contents = if (file.isToEncodeContent) {
          BinaryContentsFromDisk(file, indicator)
        }
        else {
          TextualContentsFromDisk(file, indicator)
        }
        return EduFile(path, contents)
      }
    }
}
