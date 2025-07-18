package com.jetbrains.edu.python.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import org.junit.Test

class PyCourseBuilderTest : CourseGenerationTestBase<PyProjectSettings>() {

  override val defaultSettings: PyProjectSettings = PyProjectSettings()

  @Test
  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/python_course.json")
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Our first program") {
          file("hello_world.py")
          file("tests.py")
          file("task.html")
        }
        dir("Comments") {
          file("comments.py")
          file("tests.py")
          file("task.html")
        }
      }
      dir("Variables") {
        dir("Variable definition") {
          file("variable_definition.py")
          file("tests.py")
          file("task.html")
        }
      }
      file("test_helper.py")
    }

    expectedFileTree.assertEquals(rootDir)
  }

}
