package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.update.elements.TaskUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class TaskUpdaterBase<T : Lesson>(project: Project, protected val lesson: T) : StudyItemUpdater<Task, TaskUpdate>(project) {

  suspend fun collect(remoteLesson: Lesson): List<TaskUpdate> = collect(lesson.taskList, remoteLesson.taskList)

  protected suspend fun Task.isChanged(remoteTask: Task): Boolean {
    val newTaskFiles = remoteTask.taskFiles
    val taskDescriptionText = descriptionText.ifEmpty {
      withContext(Dispatchers.EDT) {
        readAction { getDescriptionFile(project)?.getTextFromTaskTextFile() ?: "" }
      }
    }

    return when {
      name != remoteTask.name -> true
      index != remoteTask.index -> true
      taskFiles.size != newTaskFiles.size -> true
      taskDescriptionText != remoteTask.descriptionText -> true
      descriptionFormat != remoteTask.descriptionFormat -> true
      javaClass != remoteTask.javaClass -> true

      this is RemoteEduTask && remoteTask is RemoteEduTask -> {
        checkProfile != remoteTask.checkProfile
      }

      else -> {
        newTaskFiles.any { (newFileName, newTaskFile) ->
          isTaskFileChanged(taskFiles[newFileName] ?: return@any true, newTaskFile)
        }
      }
    }
  }

  private fun isTaskFileChanged(taskFile: TaskFile, newTaskFile: TaskFile): Boolean {
    if (taskFile.contents.textualRepresentation != newTaskFile.contents.textualRepresentation) return true
    val taskFilePlaceholders = taskFile.answerPlaceholders
    val newTaskFilePlaceholders = newTaskFile.answerPlaceholders
    if (taskFilePlaceholders.size != newTaskFilePlaceholders.size) return true
    if (newTaskFilePlaceholders.isNotEmpty()) {
      for ((i, newPlaceholder) in newTaskFilePlaceholders.withIndex()) {
        val existingPlaceholder = taskFilePlaceholders[i]
        if (arePlaceholdersDiffer(existingPlaceholder, newPlaceholder)) return true
      }
    }
    return false
  }

  private fun arePlaceholdersDiffer(placeholder: AnswerPlaceholder, newPlaceholder: AnswerPlaceholder): Boolean =
    newPlaceholder.length != placeholder.initialState.length
    || newPlaceholder.offset != placeholder.initialState.offset
    || newPlaceholder.placeholderText != placeholder.placeholderText
    || newPlaceholder.possibleAnswer != placeholder.possibleAnswer
    || newPlaceholder.placeholderDependency.toString() != placeholder.placeholderDependency.toString()
}