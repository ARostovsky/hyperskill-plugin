package com.jetbrains.edu.rust.courseGeneration

import com.jetbrains.edu.coursecreator.archive.CourseArchiveTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test
import org.rust.lang.RsLanguage

class RsCourseArchiveTest : CourseArchiveTestBase() {

  @Test
  fun `test cargo config 1`() {
    val course = courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson {
        eduTask {
          taskFile(
            "Cargo.toml", """
            [package]
            name = "task1"
            version = "0.1.0"
            edition = "2018"            
          """
          )
          taskFile(
            "src/lib.rs", """
            // TODO: replace this with an actual task
            pub fn sum(a: i32, b: i32) -> i32 {
                a + b
            }
          """
          )
        }
      }
      additionalFile(
        "Cargo.toml", """
        [workspace]
        members = ["lesson1/*/"]
      """
      )
      additionalFile(
        ".cargo/config.toml", """
        [build]
        rustflags = ["-Adead_code", "-Aunused_variables"]
      """
      )
    }
    doTest(course)
  }

  @Test
  fun `test cargo config 2`() {
    val course = courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
    ) {
      lesson {
        eduTask {
          taskFile(
            "Cargo.toml", """
            [package]
            name = "task1"
            version = "0.1.0"
            edition = "2018"            
          """
          )
          taskFile(
            "src/lib.rs", """
            // TODO: replace this with an actual task
            pub fn sum(a: i32, b: i32) -> i32 {
                a + b
            }
          """
          )
        }
      }
      additionalFile(
        "Cargo.toml", """
        [workspace]
        members = ["lesson1/*/"]
      """
      )
      additionalFile(
        ".cargo/config", """
        [build]
        rustflags = ["-Adead_code", "-Aunused_variables"]
      """
      )
    }
    doTest(course)
  }
}