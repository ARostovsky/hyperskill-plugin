package com.jetbrains.edu.learning

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduUtils.isEduProject
import com.jetbrains.edu.learning.EduUtils.isStudentProject
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.taskDescriptionHintBlocks
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveAll
import java.io.IOException

// educational-core.xml
@Suppress("ComponentNotRegistered")
class EduProjectComponent(private val project: Project) : ProjectComponent {
  private var busConnection: MessageBusConnection? = null

  override fun projectOpened() {
    if (project.isDisposed || !isEduProject(project)) {
      return
    }

    if (!isUnitTestMode) {
      EduDocumentListener.setGlobalListener(project)
      selectProjectView(true)
    }
    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      val course = StudyTaskManager.getInstance(project)?.course
      if (course == null) {
        LOG.warn("Opened project is with null course")
        return@runWhenProjectIsInitialized
      }

      val propertiesComponent = PropertiesComponent.getInstance(project)
      if (CCUtils.isCourseCreator(project) && !propertiesComponent.getBoolean(HINTS_IN_DESCRIPTION_PROPERTY)) {
        moveHintsToTaskDescription(course)
        propertiesComponent.setValue(HINTS_IN_DESCRIPTION_PROPERTY, true)
      }

      setupProject(course)
      ApplicationManager.getApplication().invokeLater {
        ApplicationManager.getApplication()
          .runWriteAction { EduCounterUsageCollector.eduProjectOpened(course) }
      }
    }

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      TaskDescriptionView.updateAllTabs(TaskDescriptionView.getInstance(project))
    })

    // we need opened project to get project for a course using `CourseExt.getProject`,
    // that's why can't use `ProjectComponent#projectClosed`
    connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectClosing(project: Project) {
        val course = StudyTaskManager.getInstance(project).course
        if (!isUnitTestMode && isStudentProject(project)) {
          saveAll(project)
        }

        if (PropertiesComponent.getInstance(project).getBoolean(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW)) {
          removeProjectFromRecentProjects(project)
        }
      }
    })

    connection.subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appWillBeClosed(isRestart: Boolean) {
        val projects = ProjectManager.getInstance().openProjects
        for (project in projects) {
          if (PropertiesComponent.getInstance(project).getBoolean(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW)) {
            // force closing project -> IDE will not try to reopen course preview in the next session
            ProjectManager.getInstance().closeProject(project)
            removeProjectFromRecentProjects(project)
          }
        }
      }
    })

    busConnection = connection
  }

  private fun removeProjectFromRecentProjects(project: Project) {
    val basePath = project.basePath
    if (basePath != null) {
      RecentProjectsManager.getInstance().removePath(basePath)
      RecentProjectsManager.getInstance().updateLastProjectPath()
    }
  }

  private fun setupProject(course: Course) {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.warn(String.format("Failed to refresh gradle project: configurator for `%s` is null", course.languageID))
      return
    }

    if (project.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED) == true) {
      configurator.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    // Android Studio creates `gradlew` not via VFS so we have to refresh project dir
    VfsUtil.markDirtyAndRefresh(false, true, true, project.courseDir)
  }

  // In general, it's hack to select proper Project View pane for course projects
  // Should be replaced with proper API
  private fun selectProjectView(retry: Boolean) {
    ToolWindowManager.getInstance(project).invokeLater(Runnable {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW)
      // Since 2020.1 project view tool window can be uninitialized here yet
      if (toolWindow == null) {
        if (retry) {
          selectProjectView(false)
        }
        else {
          LOG.warn("Failed to show Course View because Project View is not initialized yet")
        }
        return@Runnable
      }
      val projectView = ProjectView.getInstance(project)
      if (projectView != null) {
        val selectedViewId = ProjectView.getInstance(project).currentViewId
        if (CourseViewPane.ID != selectedViewId) {
          projectView.changeView(CourseViewPane.ID)
        }
      }
      else {
        LOG.warn("Failed to select Project View")
      }
    })
  }

  @VisibleForTesting
  fun moveHintsToTaskDescription(course: Course) {
    course.visitLessons { lesson ->
      for (task in lesson.taskList) {
        val text = StringBuffer(task.descriptionText)
        val hintBlocks = task.taskDescriptionHintBlocks()
        text.append(hintBlocks)
        task.descriptionText = text.toString()
        val file = task.getDescriptionFile(project)
        if (file != null) {
          ApplicationManager.getApplication().runWriteAction {
            try {
              VfsUtil.saveText(file, text.toString())
            }
            catch (e: IOException) {
              LOG.warn(e.message)
            }
          }
        }

        for (value in task.taskFiles.values) {
          for (placeholder in value.answerPlaceholders) {
            placeholder.hints = emptyList()
          }
        }
      }
    }
  }

  override fun initComponent() {
    if (!isUnitTestMode && isStudentProject(project)) {
      VirtualFileManager.getInstance().addVirtualFileListener(UserCreatedFileListener(project), project)
    }
  }

  override fun disposeComponent() {
    busConnection?.disconnect()
  }

  override fun getComponentName(): String {
    return "StudyTaskManager"
  }

  companion object {
    private val LOG = Logger.getInstance(EduProjectComponent::class.java.name)
    private const val HINTS_IN_DESCRIPTION_PROPERTY = "HINTS_IN_TASK_DESCRIPTION"

    fun getInstance(project: Project): EduProjectComponent {
      return project.getComponent(EduProjectComponent::class.java)
    }
  }
}
