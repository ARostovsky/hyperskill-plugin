package com.jetbrains.edu.sql.jvm.gradle.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import com.jetbrains.edu.sql.jvm.gradle.SqlTestLanguage
import com.jetbrains.edu.sql.jvm.gradle.sqlCourse
import com.jetbrains.edu.sql.jvm.gradle.sqlTestLanguage

class SqlGradleCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test new educator course with java tests`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE)
    newCourse.sqlTestLanguage = SqlTestLanguage.JAVA
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("SqlTest.java")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test new educator course with kotlin tests`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE)
    newCourse.sqlTestLanguage = SqlTestLanguage.KOTLIN
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = sqlCourse(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/SqlTest.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = sqlCourse {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/SqlTest.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }
}
