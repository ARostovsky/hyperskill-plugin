package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.Color

abstract class TaskDescriptionView {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun updateCheckPanel(task: Task?)
  abstract fun updateTaskSpecificPanel()
  abstract fun updateTopPanel(task: Task?)
  abstract fun updateTaskDescription(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateAdditionalTaskTabs()

  abstract fun readyToCheck()
  abstract fun checkStarted(task: Task)
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun checkTooltipPosition(): RelativePoint?

  companion object {

    @JvmStatic
    fun getInstance(project: Project): TaskDescriptionView {
      if (!EduUtils.isEduProject(project)) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return ServiceManager.getService(project, TaskDescriptionView::class.java)
    }

    @JvmStatic
    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }

    @JvmStatic
    fun updateAllTabs(taskDescription: TaskDescriptionView) {
      taskDescription.updateTaskDescription()
      taskDescription.updateAdditionalTaskTabs()
    }
  }
}
