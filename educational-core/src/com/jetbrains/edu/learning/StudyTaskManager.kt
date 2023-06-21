package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization

/**
 * Implementation of class which contains all the information about study in context of current project
 */
@Service(Service.Level.PROJECT)
@State(name = "StudySettings", storages = [Storage(value = "study_project.xml", roamingType = RoamingType.DISABLED)])
class StudyTaskManager(private val project: Project) : DumbAware, Disposable {
  @Volatile
  private var courseLoadedWithError = false

  @Transient
  private var _course: Course? = null

  @get:Transient
  @set:Transient
  var course: Course?
    get() = _course
    set(course) {
      _course = course
      course?.apply {
        project.messageBus.syncPublisher(COURSE_SET).courseSet(this)
      }
    }

  override fun dispose() {}

  companion object {
    val COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener::class.java)

    fun getInstance(project: Project): StudyTaskManager {
      val manager = project.service<StudyTaskManager>()
      if (!project.isDefault && !LightEdit.owns(project) && manager.course == null
          && project.isEduYamlProject() && !manager.courseLoadedWithError) {
        val course = ApplicationManager.getApplication().runReadAction(Computable { loadCourse(project) })
        manager.courseLoadedWithError = course == null
        if (course != null) {
          manager.course = course
        }
        startSynchronization(project)
      }
      return manager
    }
  }
}
