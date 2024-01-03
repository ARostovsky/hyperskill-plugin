package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.update.elements.LessonCreationInfo
import com.jetbrains.edu.learning.update.elements.LessonDeletionInfo
import com.jetbrains.edu.learning.update.elements.LessonUpdate
import com.jetbrains.edu.learning.update.elements.LessonUpdateInfo

abstract class LessonUpdater(project: Project, private val container: ItemContainer) : StudyItemUpdater<Lesson, LessonUpdate>(project) {
  protected abstract fun createTaskUpdater(lesson: Lesson): TaskUpdater

  suspend fun collect(remoteContainer: ItemContainer): List<LessonUpdate> {
    // EDU-6560 Implement new framework lesson update logic for Hyperskill and Marketplace
    val localLessons = container.items.filterIsInstance<Lesson>().filter { it !is FrameworkLesson }
    val remoteLessons = remoteContainer.items.filterIsInstance<Lesson>().filter { it !is FrameworkLesson }
    return collect(localLessons, remoteLessons)
  }

  override suspend fun collect(localItems: List<Lesson>, remoteItems: List<Lesson>): List<LessonUpdate> {
    val updates = mutableListOf<LessonUpdate>()

    val localLessons = localItems.toMutableSet()
    val remoteLessons = remoteItems.toMutableSet()

    while (localLessons.isNotEmpty() || remoteLessons.isNotEmpty()) {
      if (localLessons.isEmpty()) {
        // new lessons
        remoteLessons.forEach { remoteLesson ->
          updates.add(LessonCreationInfo(container, remoteLesson))
        }
        remoteLessons.clear()
      }
      if (remoteLessons.isEmpty()) {
        // lessons to be deleted
        localLessons.forEach { localLesson ->
          updates.add(LessonDeletionInfo(localLesson))
        }
        localLessons.clear()
      }

      // tasks to be updated
      val localLesson = localLessons.firstOrNull() ?: continue
      val remoteLesson = remoteLessons.find { it.id == localLesson.id }
      if (remoteLesson == null) {
        updates.add(LessonDeletionInfo(localLesson))
        localLessons.remove(localLesson)
      }
      else {
        val taskUpdater = createTaskUpdater(localLesson)
        val taskUpdates = taskUpdater.collect(remoteLesson)
        if (taskUpdates.isNotEmpty() || localLesson.isOutdated(remoteLesson) || localLesson.isChanged(remoteLesson)) {
          updates.add(LessonUpdateInfo(localLesson, remoteLesson, taskUpdates))
        }

        localLessons.remove(localLesson)
        remoteLessons.remove(remoteLesson)
      }
    }

    return updates
  }

  override suspend fun update(localItems: List<Lesson>, remoteItems: List<Lesson>) {
    TODO("Not yet implemented")
  }

  private fun Lesson.isChanged(remoteLesson: Lesson): Boolean = when {
    name != remoteLesson.name -> true
    index != remoteLesson.index -> true
    taskList.size != remoteLesson.taskList.size -> true
    else -> false
  }
}