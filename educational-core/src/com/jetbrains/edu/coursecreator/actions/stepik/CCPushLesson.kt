package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Modal
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.pushAvailable
import com.jetbrains.edu.coursecreator.LessonType
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.showErrorNotification
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.twelvemonkeys.lang.StringUtil
import org.jetbrains.annotations.NonNls

// educational-core.xml
// TODO i18n rewrite super call after refactoring [CCPushAction]
class CCPushLesson : CCPushAction(LessonType.presentableName) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.getData(CommonDataKeys.PROJECT)
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (project == null || selectedFiles == null || selectedFiles.size != 1) {
      return
    }
    val lessonDir = selectedFiles[0]
    if (!lessonDir.isDirectory) {
      return
    }

    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (course.courseMode != CCUtils.COURSE_MODE || !course.isRemote) return

    val lesson = CCUtils.lessonFromDir(course, lessonDir, project) ?: return
    val section = lesson.section
    if (section != null && section.id <= 0) {
      return
    }

    if (section == null && course.sectionIds.isEmpty()) {
      return
    }

    if (course.id > 0) {
      e.presentation.isEnabledAndVisible = true
      if (lesson.id <= 0) {
        e.presentation.text = getUploadText(itemName)
      }
      else {
        e.presentation.text = getUpdateText(itemName)
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (project == null || selectedFiles == null || selectedFiles.size != 1) {
      return
    }
    val lessonDir = selectedFiles[0]
    if (!lessonDir.isDirectory) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (course.courseMode != CCUtils.COURSE_MODE || !course.isRemote) return
    val lesson = CCUtils.lessonFromDir(course, lessonDir, project) ?: return

    if (course.hasSections && lesson.section == null && lesson.id <= 0) {
      wrapAndPost(project, course, lesson)
      return
    }

    ProgressManager.getInstance().run(object : Modal(project, EduCoreActionBundle.message("push.lesson.uploading"), true) {
      override fun run(indicator: ProgressIndicator) {
        indicator.text = EduCoreActionBundle.message("push.lesson.uploading.to", StepikNames.STEPIK_URL)
        doPush(lesson, project, course)
        YamlFormatSynchronizer.saveRemoteInfo(lesson)
      }
    })
  }

  companion object {
    private val LOG = Logger.getInstance(CCPushLesson::class.java)

    private fun wrapAndPost(project: Project, course: Course, lesson: Lesson) {
      ApplicationManager.getApplication().invokeAndWait {
        val result = Messages.showYesNoDialog(
          project,
          EduCoreBundle.message("notification.wrap.lessons.into.section.message.for.single.lesson"),
          EduCoreBundle.message("notification.wrap.lessons.into.section"),
          EduCoreBundle.message("label.wrap.and.post"),
          EduCoreBundle.message("label.cancel"),
          null
        )
        if (result == Messages.YES) {
          val section = CCUtils.wrapIntoSection(project, course, listOf(lesson), sectionToWrapIntoName(lesson))
          if (section != null) {
            CCPushSection.doPush(project, section, course as EduCourse)
            YamlFormatSynchronizer.saveRemoteInfo(section)
          }
        }
      }
    }

    @NonNls
    private fun sectionToWrapIntoName(lesson: Lesson): String {
      return "Section. " + StringUtil.capitalize(lesson.name)
    }

    // public for tests
    fun doPush(lesson: Lesson, project: Project, course: EduCourse) {
      if (lesson.id > 0) {
        val unit = StepikConnector.getInstance().getUnit(lesson.unitId)
        if (unit == null) {
          LOG.error("Failed to get unit for unit id " + lesson.unitId)
          return
        }
        val positionChanged = lesson.index != unit.position
        if (positionChanged) {
          showErrorNotification(
            project,
            EduCoreBundle.message("error.failed.to.update"),
            EduCoreBundle.message("error.failed.to.update.item.position.changed", CCPushCourse.getUpdateText())
          )



          return
        }
        val sectionId = if (lesson.section != null) lesson.section!!.id else course.sectionIds[0]
        val success = CCStepikConnector.updateLesson(project, lesson, true, sectionId)
        if (success) {
          EduUtils.showNotification(
            project,
            EduCoreActionBundle.message("push.lesson.updated", lesson.name),
            CCStepikConnector.openOnStepikAction("/lesson/" + lesson.id)
          )
        }
      }
      else {
        val sectionId = if (lesson.section != null) lesson.section!!.id else course.sectionIds.first()
        if (!pushAvailable(lesson.container, lesson, project)) return
        val success = CCStepikConnector.postLesson(project, lesson, lesson.index, sectionId)
        if (success) {
          EduUtils.showNotification(
            project,
            EduCoreActionBundle.message("push.lesson.uploaded", lesson.name),
            CCStepikConnector.openOnStepikAction("/lesson/" + lesson.id)
          )
        }
      }
    }
  }
}