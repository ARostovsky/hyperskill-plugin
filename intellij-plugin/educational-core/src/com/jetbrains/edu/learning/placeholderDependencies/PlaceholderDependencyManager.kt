package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object PlaceholderDependencyManager {

  private val LOG = Logger.getInstance(PlaceholderDependencyManager::class.java)

  fun updateDependentPlaceholders(project: Project, task: Task) {
    for (taskFile in task.taskFiles.values) {
      val virtualFile = taskFile.findTaskFileInDir(task.getDir(project.courseDir) ?: break) ?: continue
      EditorNotifications.getInstance(project).updateNotifications(virtualFile)
    }

    if (task.status != CheckStatus.Unchecked) {
      return
    }
    if (task.placeholderDependencies.isEmpty()) {
      return
    }

    if (task.hasChangedFiles(project)) {
      return
    }

    val unsolvedTasks = task.getUnsolvedTaskDependencies()
    if (unsolvedTasks.isNotEmpty()) {
      //everything will be handled by editor notification
      return
    }

    for (dependency in task.placeholderDependencies) {
      val replacementText = getReplacementText(project, dependency)
      val placeholderToReplace = dependency.answerPlaceholder
      runUndoTransparentWriteAction {
        replacePlaceholderText(project, placeholderToReplace, replacementText)
      }
    }
  }

  private fun replacePlaceholderText(project: Project, placeholderToReplace: AnswerPlaceholder, replacementText: String) {
    val document = placeholderToReplace.taskFile.getDocument(project)
    if (document == null) {
      LOG.error("Failed to find document for `${placeholderToReplace.taskFile.name}`")
      return
    }
    val startOffset = placeholderToReplace.offset
    val endOffset = placeholderToReplace.endOffset
    document.replaceString(startOffset, endOffset, replacementText)
    placeholderToReplace.isInitializedFromDependency = true
  }

  private fun getReplacementText(project: Project, dependency: AnswerPlaceholderDependency): String {
    val course = dependency.answerPlaceholder.taskFile.task.course
    val dependencyPlaceholder = dependency.resolve(course)!!
    val dependencyTask = dependencyPlaceholder.taskFile.task
    val dependencyLesson = dependencyTask.lesson
    return if (dependencyLesson is FrameworkLesson && dependencyLesson.currentTaskIndex != dependencyTask.index - 1) {
      dependencyPlaceholder.studentAnswer ?: error("Student answer should be not null here")
    }
    else {
      val document = dependencyPlaceholder.taskFile.getDocument(project)!!
      val startOffset = dependencyPlaceholder.offset
      val endOffset = dependencyPlaceholder.endOffset
      document.getText(TextRange.create(startOffset, endOffset))
    }
  }
}
