package com.jetbrains.edu.learning.update

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree

open class StudentCourseUpdateTest : CourseUpdateTestBase<Unit>() {
  override val defaultSettings: Unit get() = Unit

  fun `test lesson added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_added")
  }

  fun `test lesson rearranged`() {
    val expectedFileTree = fileTree {
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lessons_rearranged")
  }

  fun `test lesson rearranged in section`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson2") {
          dir("task1") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lessons_rearranged_in_section")
  }


  fun `test lesson renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1_renamed") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_renamed")
  }
  fun `test task added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_added")
  }

  fun `test theory task added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
        dir("Theory") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/theory_task_added")
  }

  fun `test theory task text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
        dir("Theory") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/theory_task_text_changed")
  }


  fun `test task in section added`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
          dir("task2") {
            dir("src") {
              file("Task.java")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_in_section_added")
  }


  fun `test task renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1_renamed") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_renamed")
  }

  fun `test task text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_text_changed")
  }

  fun `test task file renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task_renamed.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_renamed")
  }

  fun `test task file text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  //Changed put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_text_changed")
  }

  private fun testsAdded(setFirstTaskSolved: (course: EduCourse) -> Unit = {}) {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  //put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/tests_added") { course -> setFirstTaskSolved(course) }
  }

  fun `test tests added for solved task`() {
    testsAdded { course -> setFirstTaskSolved(course) }
  }

  fun `test tests added for unchecked task`() {
    testsAdded()
  }

  fun `test theory task file text changed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("Theory") {
          dir("src") {
            file("Task.java", "public class Task {\n  // Changed put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/theory_task_file_changed")
  }

  fun `test task file in section text changed`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  //Changed put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_file_in_section_text_changed")
  }


  fun `test section added`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/section_added")
  }

  fun `test sections rearranged`() {
    val expectedFileTree = fileTree {
      dir("section2") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/sections_rearranged")
  }

  fun `test lesson added into section`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
        dir("lesson2") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }

      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_added_into_section")
  }

  fun `test section renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("section1_renamed") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/section_renamed")
  }

  fun `test lesson in section renamed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      dir("section1") {
        dir("lesson1_renamed") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_in_section_renamed")
  }

  fun `test task removed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/task_removed")
  }

  fun `test lesson removed`() {
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java", "public class Task {\n  // put your task here\n}")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_removed")
  }

  fun `test lesson removed from section`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/lesson_removed_from_section")
  }

  fun `test section removed`() {
    val expectedFileTree = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.java", "public class Task {\n  // put your task here\n}")
            }
            dir("test") {
              file("Tests.java")
            }
            file("task.md")
          }
        }
      }
    }

    doTest(expectedFileTree, "testData/stepik/updateCourse/section_removed")
  }

  private fun setFirstTaskSolved(course: EduCourse) {
    ((course.items[0] as Lesson).items[0] as EduTask).status = CheckStatus.Solved
  }
}
