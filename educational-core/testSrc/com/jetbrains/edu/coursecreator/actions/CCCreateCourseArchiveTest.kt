package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.GENERATED_FILES_FOLDER
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.yaml.configFileName
import junit.framework.TestCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CCCreateCourseArchiveTest : EduActionTestCase() {

  fun `test local course archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test coursera course archive`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    } as CourseraCourse
    course.submitManually = false
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test coursera course archive submit manually`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    } as CourseraCourse
    course.submitManually = true
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test local course with author`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    course.setAuthorsAsString(arrayOf("EduTools Dev", "EduTools QA", "EduTools"))
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test framework lesson archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson("my lesson") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test sections`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test custom files`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
            taskFile("test.py", "some test")
            taskFile("additional.py", "my test", visible = false)
            taskFile("visibleAdditional.py", "my test")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test remote course archive`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }.asRemote()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse("Jan 01, 1970 03:00:00 AM")
    course.updateDate = date
    for (lesson in course.lessons) {
      lesson.updateDate = date
      for (task in lesson.taskList) {
        task.updateDate = date
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test placeholder dependencies`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """) {
            placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
            placeholder(1, dependency = "lesson1#task1#fizz.kt#2")
          }
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test throw exception if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    course.description = "my summary"
    val placeholder = course.lessons.first().taskList.first().taskFiles["fizz.kt"]?.answerPlaceholders?.firstOrNull()
                      ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    assertThrows(BrokenPlaceholderException::class.java, ThrowableRunnable<BrokenPlaceholderException> {
      CourseArchiveCreator.loadActualTexts(project, course)
    })
  }

  fun `test navigate to yaml if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    course.description = "my summary"
    createConfigFiles(project)

    val task = course.lessons.first().taskList.first() ?: error("Cannot find task")
    val placeholder = task.taskFiles["fizz.kt"]?.answerPlaceholders?.firstOrNull() ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    assertNull(FileEditorManagerEx.getInstanceEx(project).currentFile)

    // It is not important, what would be passed to the constructor, except the first argument - project
    // Inside `compute()`, exception would be thrown, so we will not reach the moment of creating the archive
    CourseArchiveCreator(project, course.getDir(project), File(""), false).compute()

    val navigatedFile = FileEditorManagerEx.getInstanceEx(project).currentFile ?: error("Navigated file should not be null here")
    assertEquals(task.configFileName, navigatedFile.name)
  }

  fun `test course additional files`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
      }
      additionalFiles {
        taskFile("additional.txt", "file text")
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    TestCase.assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test course with choice tasks`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("task.txt")
        }
      }
    }
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test task with custom name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    val task = course.lessons.first().taskList.first()
    task.customPresentableName = "custom name"

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test peek solution is hidden for course`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.solutionsHidden = true
    course.description = "my summary"
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test peek solution is hidden for task`() {
    val task = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }.findTask("lesson1", "task1")
    task.solutionHidden = true
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test gradle properties in archive`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("gradle.properties", "some.awesome.property=true")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test mp3 audio file in archive`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.mp4")
    }
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)

  }

  fun `test mp4 video file in archive`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.mp4")
    }
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  fun `test img file in archive`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.img")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  // EDU-2765
  fun `test pdf file in archive`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.pdf")
    }

    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  private fun generateJson(): String {
    @Suppress("DEPRECATION")
    val baseDir = myFixture.project.baseDir
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val errorMessage = CCCreateCourseArchive.createCourseArchive(myFixture.project, "course",
                                                                 myFixture.project.basePath + "/" + GENERATED_FILES_FOLDER,
                                                                 false)
    assertNull(errorMessage)
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    val generated = baseDir.findChild(GENERATED_FILES_FOLDER)
    assertNotNull(generated)
    val archive = generated!!.findChild("course.zip")
    assertNotNull(archive)
    val courseFolder = generated.findChild("course")
    assertNotNull(courseFolder)
    val jsonFile = courseFolder!!.findChild(EduNames.COURSE_META_FILE)
    assertNotNull(jsonFile)
    return FileUtil.loadFile(File(jsonFile!!.path), true).replace(Regex("\\n\\n"), "\n")
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/createCourseArchive"
  }

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

}
