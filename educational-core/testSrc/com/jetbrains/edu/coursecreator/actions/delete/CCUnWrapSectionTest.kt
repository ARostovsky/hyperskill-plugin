package com.jetbrains.edu.coursecreator.actions.delete

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.studyItem.CCRemoveSection
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import junit.framework.TestCase

class CCUnWrapSectionTest : EduActionTestCase() {

  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().messageBus
      .connect(testRootDisposable)
      .subscribe(VirtualFileManager.VFS_CHANGES, CCVirtualFileListener(project, testRootDisposable))
  }

  fun `test unwrap lessons`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson2")
        lesson("lesson3")
      }
      lesson("lesson4")
    }
    val section2 = findFile("section2")
    testAction(CCRemoveSection.ACTION_ID, dataContext(arrayOf(section2)))
    TestCase.assertEquals(4, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..4) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test one lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson2")
      }
      lesson("lesson3")
    }
    val section2 = findFile("section2")
    testAction(CCRemoveSection.ACTION_ID, dataContext(arrayOf(section2)))
    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..3) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test with multiple lesson before and after`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      lesson()
      lesson()
      section("section2") {
        lesson("lesson4")
        lesson("lesson5")
      }
      lesson("lesson6")
      lesson("lesson7")
    }
    val section2 = findFile("section2")
    testAction(CCRemoveSection.ACTION_ID, dataContext(arrayOf(section2)))
    TestCase.assertEquals(7, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNull(section)
    for (i in 1..7) {
      TestCase.assertEquals(i, course.getLesson("lesson$i")!!.index)
    }
  }

  fun `test unwrap lessons tree structure`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson2")
        lesson("lesson3")
      }
      lesson("lesson4")
    }
    val section2 = findFile("section2")
    testAction(CCRemoveSection.ACTION_ID, dataContext(arrayOf(section2)))
    val expectedFileTree = fileTree {
      dir("lesson1")
      dir("lesson2")
      dir("lesson3")
      dir("lesson4")
    }
    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test course has the same named lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson()
      section {
        lesson("lesson1")
        lesson("lesson2")
      }
      lesson()
    }
    val section2 = findFile("section2")
    try {
      testAction(CCRemoveSection.ACTION_ID, dataContext(arrayOf(section2)))
      TestCase.fail("Expected failed to move lesson out message")
    }
    catch (_: Throwable) {}

    TestCase.assertEquals(3, course.items.size)
    val section = course.getSection("section2")
    TestCase.assertNotNull(section)
    TestCase.assertEquals(1, course.getLesson("lesson1")!!.index)
    TestCase.assertEquals(2, course.getSection("section2")!!.index)
    TestCase.assertEquals(3, course.getLesson("lesson2")!!.index)
  }
}
