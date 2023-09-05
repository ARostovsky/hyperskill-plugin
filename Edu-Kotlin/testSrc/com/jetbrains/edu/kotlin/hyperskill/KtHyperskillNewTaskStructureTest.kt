package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Task.kt")
      }
      dir("test") {
        file("Tests.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("Tests.kt")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
