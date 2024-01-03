package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.update.elements.StudyItemUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO EDU-5830 maybe synchronization is needed
@Suppress("DuplicatedCode")
abstract class StudyItemUpdater<T : StudyItem, U : StudyItemUpdate<T>>(protected val project: Project) : ItemUpdater<T> {
  protected abstract suspend fun collect(localItems: List<T>, remoteItems: List<T>): List<U>

  abstract suspend fun update(localItems: List<T>, remoteItems: List<T>)

  companion object {
    @Suppress("UnstableApiUsage")
    suspend fun <T : StudyItem> T.deleteFilesOnDisc(project: Project) {
      val virtualFile = getDir(project.courseDir) ?: return
      withContext(Dispatchers.EDT) {
        writeAction {
          virtualFile.delete(this::class.java)
        }
      }
    }
  }
}