package org.hyperskill.academy.learning.format.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.logger
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.yaml.InjectableValueKey
import org.hyperskill.academy.learning.yaml.YamlDeepLoader.loadCourse
import org.hyperskill.academy.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames
import org.hyperskill.academy.learning.yaml.getEduValue
import org.hyperskill.academy.learning.yaml.migrate.YamlMigrationStep
import org.hyperskill.academy.learning.yaml.migrate.YamlMigrator
import org.hyperskill.academy.learning.yaml.setEduValue
import org.junit.Test

class YamlMigrationTest : EduTestCase() {

  @Test
  fun `folders are passed correctly to migration methods`() {
    courseWithFiles(createYamlConfigs = true) {
      section {
        lesson {
          eduTask()
          eduTask()
        }
      }
      lesson {
        eduTask()
        eduTask()
      }
      lesson {}
    }

    class ConvertNamesToFolders : YamlMigrationStep {
      override fun migrateSection(mapper: ObjectMapper, config: ObjectNode, parentCourse: Course, folderName: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "section in $folderName/")

      override fun migrateLesson(mapper: ObjectMapper, config: ObjectNode, parentItem: StudyItem, folderName: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "lesson in $folderName/")

      override fun migrateTask(mapper: ObjectMapper, config: ObjectNode, parentLesson: Lesson, folderName: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "task in $folderName/")
    }

    YamlMigrator.withMigrationSteps(mapOf(CURRENT_YAML_VERSION + 1 to ConvertNamesToFolders())) {
      val migratedCourse = loadCourse(project) ?: kotlin.test.fail("failed to load course")

      assertEquals("section in section1/", migratedCourse.items[0].presentableName)

      assertEquals("lesson in lesson1/", (migratedCourse.items[0] as ItemContainer).items[0].presentableName)
      assertEquals("lesson in lesson1/", migratedCourse.items[1].presentableName)
      assertEquals("lesson in lesson2/", migratedCourse.items[2].presentableName)

      assertEquals("task in task1/", migratedCourse.allTasks[0].presentableName)
      assertEquals("task in task2/", migratedCourse.allTasks[1].presentableName)
      assertEquals("task in task1/", migratedCourse.allTasks[2].presentableName)
      assertEquals("task in task2/", migratedCourse.allTasks[3].presentableName)
    }
  }

  @Test
  fun `several migration steps together`() {
    courseWithFiles(courseMode = CourseMode.STUDENT, createYamlConfigs = true) {
      section {
        lesson {
          eduTask()
          eduTask()
        }
      }
      lesson {
        eduTask()
        eduTask()
      }
    }

    class StepMigrateCourse : YamlMigrationStep {
      override fun migrateCourse(mapper: ObjectMapper, config: ObjectNode): ObjectNode =
        config.put(YamlMixinNames.TITLE, "MigratedCourse")
    }

    class StepMigrateTask : YamlMigrationStep {
      override fun migrateTask(mapper: ObjectMapper, config: ObjectNode, parentLesson: Lesson, taskFolder: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "task in ${parentLesson.presentableName} in course ${parentLesson.course.name}")
    }

    class StepMigrateSectionAndLesson : YamlMigrationStep {
      override fun migrateSection(mapper: ObjectMapper, config: ObjectNode, parentCourse: Course, sectionFolder: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "section in ${parentCourse.name}")

      override fun migrateLesson(mapper: ObjectMapper, config: ObjectNode, parentItem: StudyItem, lessonFolder: String): ObjectNode =
        config.put(YamlMixinNames.CUSTOM_NAME, "lesson in ${parentItem.presentableName}")
    }

    YamlMigrator.withMigrationSteps(
      mapOf(
        CURRENT_YAML_VERSION + 1 to StepMigrateCourse(),
        CURRENT_YAML_VERSION + 2 to StepMigrateTask(),
        CURRENT_YAML_VERSION + 3 to StepMigrateSectionAndLesson()
      )
    ) {
      val migratedCourse = loadCourse(project) ?: kotlin.test.fail("failed to load course")
      assertEquals("MigratedCourse", migratedCourse.presentableName)
      assertEquals("section in MigratedCourse", migratedCourse.items[0].presentableName)

      assertEquals("lesson in section in MigratedCourse", (migratedCourse.items[0] as ItemContainer).items[0].presentableName)
      assertEquals("lesson in MigratedCourse", migratedCourse.items[1].presentableName)

      assertEquals("task in lesson in section in MigratedCourse in course MigratedCourse", migratedCourse.allTasks[0].presentableName)
      assertEquals("task in lesson in section in MigratedCourse in course MigratedCourse", migratedCourse.allTasks[1].presentableName)
      assertEquals("task in lesson in MigratedCourse in course MigratedCourse", migratedCourse.allTasks[2].presentableName)
      assertEquals("task in lesson in MigratedCourse in course MigratedCourse", migratedCourse.allTasks[3].presentableName)
    }
  }

  @Test
  fun `passing external actions to a migrator`() {
    courseWithFiles(courseMode = CourseMode.STUDENT, createYamlConfigs = true) {
      lesson {
        eduTask()
      }
    }

    val taskFilesSearcherKey = InjectableValueKey<(lesson: Lesson, taskFolder: String) -> List<String>>("task_files_searcher")

    class AddTaskFilesToTaskNameMigrationStep : YamlMigrationStep {
      override fun migrateTask(mapper: ObjectMapper, config: ObjectNode, parentLesson: Lesson, taskFolder: String): ObjectNode {
        val taskFilesSearcher = mapper.getEduValue(taskFilesSearcherKey)
        if (taskFilesSearcher == null) {
          logger<YamlMigrationTest>().error("Failed to migrate task because task file searcher is not provided")
          return config
        }

        val taskFiles = taskFilesSearcher(parentLesson, taskFolder)
        return config.put(YamlMixinNames.CUSTOM_NAME, taskFiles.sorted().joinToString(separator = ","))
      }
    }

    YamlMigrator.withMigrationSteps(
      steps = mapOf(
        CURRENT_YAML_VERSION + 1 to AddTaskFilesToTaskNameMigrationStep()
      ),
      mapperSetup = {
        setEduValue(taskFilesSearcherKey) { lesson, taskFolder -> findTaskFiles(lesson, taskFolder) }
      }
    ) {
      val migratedCourse = loadCourse(project) ?: kotlin.test.fail("failed to load course")

      assertEquals(migratedCourse.allTasks[0].presentableName, "task-info.yaml,task.html")
    }
  }

  @Test
  fun `migrate steps from yaml version plus 1 to current version`() {
    courseWithFiles(createYamlConfigs = true) {
      lesson {
        eduTask()
      }
    }

    class CallMeStep : YamlMigrationStep {
      var called = false
      override fun migrateCourse(mapper: ObjectMapper, config: ObjectNode): ObjectNode {
        called = true
        return config
      }
    }

    val dontCallMe = object : YamlMigrationStep {
      override fun migrateCourse(mapper: ObjectMapper, config: ObjectNode): ObjectNode {
        error("This migration step should not be called")
      }
    }

    val callMe1 = CallMeStep()
    val callMe2 = CallMeStep()

    YamlMigrator.withMigrationSteps(
      steps = mapOf(
        CURRENT_YAML_VERSION to dontCallMe,
        CURRENT_YAML_VERSION + 1 to callMe1,
        CURRENT_YAML_VERSION + 2 to callMe2
      )
    ) {
      loadCourse(project) ?: kotlin.test.fail("failed to load course")

      assertTrue("This migration step should be called", callMe1.called)
      assertTrue("This migration step should be called", callMe2.called)
    }
  }

  private fun findTaskFiles(lesson: Lesson, taskFolder: String): List<String> {
    val lessonDir = lesson.getDir(project.courseDir) ?: kotlin.test.fail("Failed to get lesson dir")
    val taskDir = lessonDir.findChild(taskFolder) ?: kotlin.test.fail("Failed to get task dir")

    return taskDir.children.map { it.name }
  }
}