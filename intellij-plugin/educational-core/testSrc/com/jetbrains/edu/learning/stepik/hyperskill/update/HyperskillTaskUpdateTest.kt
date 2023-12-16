package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.update.TaskUpdater
import com.jetbrains.edu.learning.update.TaskUpdateTestBase
import java.util.*

class HyperskillTaskUpdateTest : TaskUpdateTestBase<HyperskillCourse>() {

  override fun getUpdater(lesson: Lesson): TaskUpdater = HyperskillTaskUpdater(project, lesson)

  fun `test new task created`() {
    initiateLocalCourse()
    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf("TaskFile3.kt" to TaskFile("TaskFile3.kt", "task file 3 text"))
    }
    val newStages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3)
    )
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        addTask(newEduTask)
      }
      stages = newStages
    }
    updateTasks(remoteCourse)

    assertEquals("Task hasn't been added", 3, findLesson(0).taskList.size)
  }

  // TODO EDU-6241 add test for lesson update with index recalculation
  fun `test last task deleted`() {
    initiateLocalCourse()
    val newStages = listOf(HyperskillStage(1, "", 1, true))
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[1])
      }
      stages = newStages
    }
    updateTasks(remoteCourse)

    assertTrue("Task hasn't been deleted", findLesson(0).taskList.size == 1)
  }

  fun `test task description with placeholders have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() { <p>TODO</p>() }") {
            placeholder(index = 0, placeholderText = "TODO")
          }
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))

    val newText = "TODO()"
    val remoteCourse = toRemoteCourse {
      taskList[0].apply {
        descriptionText = "fun foo() { <p>$newText</p> }"
        updateDate = Date(100)
        taskFiles["src/Task.kt"]?.answerPlaceholders?.first()?.placeholderText = newText
      }
    }
    updateTasks(remoteCourse)

    val taskDescription = runReadAction {
      findTask(0, 0).getTaskTextFromTask(project)!!
    }
    assertTrue("Task Description not updated", taskDescription.contains(newText))
  }

  fun `test task type has been updated from unsupported to supported`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1") {
          unsupportedTask("task1", stepId = 1)
          eduTask("task2", stepId = 2) {
            taskFile("TaskFile2.kt", "task file 2 text")
          }
        }
      }
    } as HyperskillCourse

    val newEduTask = EduTask("task1").apply {
      id = 1
      taskFiles = linkedMapOf("TaskFile1.kt" to TaskFile("TaskFile1.kt", "task file 1 text"))
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().apply {
        removeTask(taskList[0])
        addTask(0, newEduTask)
      }
    }
    updateTasks(
      remoteCourse,
      lesson = localCourse.sections.first().lessons.first(),
      remoteLesson = remoteCourse.sections.first().lessons.first()
    )

    assertTrue("UnsupportedTask hasn't been updated to EduTask", findTask(0, 0, 0) is EduTask)
  }

  fun `test choiceTask and its choice options have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1") {
          choiceTask(
            "task1", stepId = 1, choiceOptions = mapOf(
              "Option1" to ChoiceOptionStatus.UNKNOWN,
              "Option2" to ChoiceOptionStatus.UNKNOWN,
              "Option3" to ChoiceOptionStatus.UNKNOWN
            )
          )
          choiceTask(
            "task2", stepId = 2, choiceOptions = mapOf(
              "Option1" to ChoiceOptionStatus.UNKNOWN,
              "Option2" to ChoiceOptionStatus.UNKNOWN,
              "Option3" to ChoiceOptionStatus.UNKNOWN
            )
          )
        }
      }
    } as HyperskillCourse

    val newChoiceTask = ChoiceTask().apply {
      id = 1
      name = "task1"
      descriptionText = "solve task"
      choiceOptions = mapOf(
        "NewOption1" to ChoiceOptionStatus.UNKNOWN,
        "NewOption2" to ChoiceOptionStatus.UNKNOWN,
        "NewOption3" to ChoiceOptionStatus.UNKNOWN
      ).map { ChoiceOption(it.key, it.value) }
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().apply {
        removeTask(taskList[0])
        addTask(0, newChoiceTask)
      }
    }
    updateTasks(
      remoteCourse,
      lesson = localCourse.sections.first().lessons.first(),
      remoteLesson = remoteCourse.sections.first().lessons.first()
    )

    val newChoiceOptions = (findTask(0, 0, 0) as ChoiceTask).choiceOptions
    assertEquals("Choice options for the ChoiceTask have not been updated", "NewOption1", newChoiceOptions[0].text)
  }

  fun `test sortingTask and its options have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1") {
          sortingTask("task1", stepId = 1, options = listOf("0", "1", "2"))
          sortingTask("task2", stepId = 2, options = listOf("0", "1", "2"))
        }
      }
    } as HyperskillCourse

    val newOptions = listOf("3", "4", "5")
    val newSortingTask = SortingTask().apply {
      id = 2
      name = "task2"
      options = newOptions
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().apply {
        removeTask(taskList[1])
        addTask(1, newSortingTask)
      }
    }
    updateTasks(
      remoteCourse,
      lesson = localCourse.sections.first().lessons.first(),
      remoteLesson = remoteCourse.sections.first().lessons.first()
    )

    val newSortingOptions = (findTask(0, 0, 1) as SortingTask).options
    assertEquals("Sorting options for the SortingTask have not been updated", newOptions, newSortingOptions)
  }

  fun `test matchingTask and its options have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1") {
          matchingTask("task1", stepId = 1, options = listOf("0", "1", "2"), captions = listOf("A", "B", "C"))
          matchingTask("task2", stepId = 2, options = listOf("0", "1", "2"), captions = listOf("A", "B", "C"))
        }
      }
    } as HyperskillCourse

    val newOptions = listOf("3", "4", "5")
    val newCaptions = listOf("D", "E", "F")
    val newMatchingTask = MatchingTask().apply {
      id = 2
      name = "task2"
      options = newOptions
      captions = newCaptions
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().apply {
        removeTask(taskList[1])
        addTask(1, newMatchingTask)
      }
    }
    updateTasks(
      remoteCourse,
      lesson = localCourse.sections.first().lessons.first(),
      remoteLesson = remoteCourse.sections.first().lessons.first()
    )

    val matchingTask = (findTask(0, 0, 1) as MatchingTask)
    assertEquals("Matching options for the MatchingTask have not been updated", matchingTask.options, newOptions)
    assertEquals("Matching captions for the MatchingTask have not been updated", matchingTask.captions, newCaptions)
  }

  fun `test remoteEduTask and its checkProfile have been updated`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1") {
          remoteEduTask("task1", stepId = 1, checkProfile = "profile 1")
          remoteEduTask("task2", stepId = 2, checkProfile = "profile 2")
        }
      }
    } as HyperskillCourse

    val newProfile = "profile 2 updated"
    val newRemoteEduTask = RemoteEduTask().apply {
      id = 2
      name = "task2"
      checkProfile = newProfile
    }
    val remoteCourse = toRemoteCourse {
      sections.first().lessons.first().apply {
        removeTask(taskList[1])
        addTask(1, newRemoteEduTask)
      }
    }
    updateTasks(
      remoteCourse,
      lesson = localCourse.sections.first().lessons.first(),
      remoteLesson = remoteCourse.sections.first().lessons.first()
    )

    val newCheckProfile = (findTask(0, 0, 1) as RemoteEduTask).checkProfile
    assertTrue("Sorting options for the SortingTask have not been updated", newCheckProfile == newProfile)
  }

  override fun initiateLocalCourse() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))
  }
}