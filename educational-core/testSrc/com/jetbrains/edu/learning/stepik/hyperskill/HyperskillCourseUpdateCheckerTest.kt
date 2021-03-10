package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.stepik.hyperskill.update.SyncHyperskillCourseAction
import com.jetbrains.edu.learning.update.CourseUpdateCheckerTestBase
import java.util.*
import kotlin.test.assertNotEquals

class HyperskillCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun configureResponse(stagesResponse: String, courseResponse: String = "course_response.json") {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse(courseResponse)
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      STAGES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse(stagesResponse)
    }
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      STEPS_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("steps_response_111.json")
    }
  }

  fun `test check scheduled for upToDate course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse()
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 1, 2) {}
  }

  fun `test check scheduled for newly created course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 0, 1) {}
  }

  fun `test no isUpToDate check for newly created course at project opening`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    testNoCheck(HyperskillCourseUpdateChecker.getInstance(project))
  }

  fun `test check scheduled for not upToDate course with notification`() {
    configureResponse("stages_response.json")
    val course = course(courseProducer = ::HyperskillCourse) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 111) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    createCourseStructure(course)
    course.hyperskillProject = HyperskillProject()
    course.updateDate = Date(0)
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)
    HyperskillSettings.INSTANCE.updateAutomatically = false

    doTest(HyperskillCourseUpdateChecker.getInstance(project), false, 1, 2, 2) {}
  }

  fun `test course updated at sync action`() {
    configureResponse("stages_response.json", courseResponse = "course_response_lite.json")
    val course = createHyperskillCourse(false)
    course.addLesson(FrameworkLesson())
    course.hyperskillProject!!.title = "Outdated title"

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("update.notification.title"), notification.title)
      }
    })

    SyncHyperskillCourseAction().synchronizeCourse(project)
    assertTrue(notificationShown)
    assertEquals("Phone Book", course.hyperskillProject!!.title)
    assertNotEquals(Date(0), course.updateDate)
  }

  fun `test notification shown for up to date course at sync action`() {
    configureResponse("stages_empty_response.json", courseResponse = "course_response_lite.json")
    createHyperskillCourse(true)

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("update.nothing.to.update"), notification.title)
      }
    })

    SyncHyperskillCourseAction().synchronizeCourse(project)
    assertTrue(notificationShown)
  }

  private fun createHyperskillCourse(isNewlyCreated: Boolean = false): HyperskillCourse {
    val course = course(courseProducer = ::HyperskillCourse) { } as HyperskillCourse
    course.apply {
      id = 1
      hyperskillProject = HyperskillProject().apply {
        id = 1
        title = "Phone Book"
      }
    }

    createCourseStructure(course)
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, isNewlyCreated)
    StudyTaskManager.getInstance(project).course = course
    HyperskillCourseUpdateChecker.getInstance(project).course = course
    return course
  }

  override fun checkNotification(notificationListener: NotificationListener,
                                 isCourseUpToDate: Boolean) {
    if (isCourseUpToDate) {
      if (notificationListener.notificationShown) {
        assertEquals(EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
                     notificationListener.notificationText)
      }
    }
    else {
      assertTrue("Notification wasn't shown", notificationListener.notificationShown)
      assertEquals(EduCoreBundle.message("update.content.request"), notificationListener.notificationText)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "stepik/hyperskill/"

  override fun tearDown() {
    try {
      val updateChecker = HyperskillCourseUpdateChecker.getInstance(project)
      updateChecker.invocationNumber = 0
      updateChecker.cancelCheckRequests()
      HyperskillSettings.INSTANCE.updateAutomatically = true
    }
    finally {
      super.tearDown()
    }
  }

  companion object {
    private val COURSES_REQUEST_RE = """/api/projects?.*""".toRegex()
    private val STAGES_REQUEST_RE = """/api/stages?.*""".toRegex()
    private val STEPS_REQUEST_RE = """/api/steps?.*""".toRegex()
  }
}