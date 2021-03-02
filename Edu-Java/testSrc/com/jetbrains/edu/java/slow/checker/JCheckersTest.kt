package com.jetbrains.edu.java.slow.checker

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class JCheckersTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = JavaLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static int foo() {
              return 42;
            }
          }
        """)
        javaTaskFile("test/Test.java", """
          import org.junit.Assert;

          public class Test {
            @org.junit.Test
            public void test() {
              Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
            }
          }
        """)
      }
      outputTask("OutputTask") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskWithWindowsLineSeparators") {
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println("OK");
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\r\n")
        }
      }
      outputTask("OutputTaskWithSeveralFiles") {
        javaTaskFile("src/Utils.java", """
          public class Utils {
            public static String ok() {
              return "OK";
            }
          }
        """)
        javaTaskFile("src/Task.java", """
          public class Task {
            public static void main(String[] args) {
              System.out.println(Utils.ok());
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
    }
  }

  fun `test java course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
