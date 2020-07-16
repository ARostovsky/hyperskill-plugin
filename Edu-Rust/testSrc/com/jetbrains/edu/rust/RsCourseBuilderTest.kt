package com.jetbrains.edu.rust

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import org.rust.lang.RsLanguage

class RsCourseBuilderTest : CourseGenerationTestBase<RsProjectSettings>() {

  override val defaultSettings: RsProjectSettings = RsProjectSettings(null)

  fun `test new educator course`() {
    val newCourse = newCourse(RsLanguage)
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("lib.rs")
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
        file("task.html")
      }
      file("Cargo.toml", """
          [workspace]
          
          members = [
              "lesson1/*/",
          ]
          
          exclude = [
              "**/*.yaml"
          ]

      """)
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = course(language = RsLanguage, courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
        file("task.html")
      }
    }.assertEquals(rootDir)
  }


  fun `test study course structure`() {
    val course = course(language = RsLanguage) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("task.html")
        file("Cargo.toml")
      }
    }.assertEquals(rootDir)
  }
}
