package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.selectedTaskFile

class HyperskillMetricsApplicationActivationListener : ApplicationActivationListener {
  override fun applicationActivated(ideFrame: IdeFrame) {
    val project = ideFrame.project ?: return
    val task = project.selectedTaskFile?.task ?: return
    val course = task.course
    if (!course.isStudy || course !is HyperskillCourse) {
      return
    }
    HyperskillMetricsService.getInstance().taskStarted(task)
  }

  override fun applicationDeactivated(ideFrame: IdeFrame) {
    HyperskillMetricsService.getInstance().taskStopped()
  }
}