package com.jetbrains.edu.remote

import com.intellij.codeWithMe.ClientId
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.client.session
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class EduRemoteUidRetrieverService(private val project: Project, private val scope: CoroutineScope) {
  private val checkInterval = 5.seconds
  private val isStarted = AtomicBoolean(false)
  private val fileWithUid: File
    get() = File(System.getProperty(INFO_FILE_PATH_PROPERTY_NAME) ?: INFO_FILE_PATH)

  fun start() {
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return

    if (!isStarted.compareAndSet(false, true)) {
      LOG.info("There was an attempt to start the service again, while it is already running")
      return
    }
    LOG.info("EduRemoteUidRetrieverService started")

    scope.launch {
      while (true) {
        if (fileWithUid.exists()) {
          service<EduRemoteUidHolderService>().userUid = fileWithUid.extractUUID()
          break
        }
        else {
          LOG.warn(
            "The user UID file (${fileWithUid.absolutePath}) was not found in the file system. " +
            "Will re-check after a delay of $checkInterval"
          )
        }
        delay(checkInterval)
      }

      if (project.isOpen) {
        submissionsManager.prepareSubmissionsContentWhenLoggedIn {
          openLastSubmittedTaskIfNeeded()
        }
      }
    }
  }

  private fun openLastSubmittedTaskIfNeeded() {
    val lastSubmission = SubmissionsManager.getInstance(project).getLastSubmission()
    val course = project.course ?: return

    val taskToOpen = course.allTasks.find { task -> task.id == lastSubmission?.taskId }

    invokeLater {
      ClientId.withClientId(project.session(ClientId.current).clientId) {
        if (taskToOpen == null) {
          NavigationUtils.openFirstTask(course, project)
        }
        else {
          NavigationUtils.navigateToTask(project, taskToOpen)
        }
      }
    }
  }

  private fun File.extractUUID(): String? {
    val fileText = readText()
    val matcher = PATTERN.matcher(fileText)

    return if (matcher.find()) {
      val uid = matcher.group(1)
      LOG.info("The user's UID ($uid) was successfully retrieved and stored")
      uid
    }
    else {
      LOG.warn(
        "The user UID file (${fileWithUid.absolutePath}) was located in the file system, however, " +
        "it does not contain any user UID information"
      )
      null
    }
  }

  companion object {
    @NonNls
    private const val INFO_FILE_PATH: String = "/idea/podinfo/labels"

    @NonNls
    private const val INFO_FILE_PATH_PROPERTY_NAME: String = "edu.remote.ide.info.file.path"

    @NonNls
    private const val JB_UID_PROPERTY_NAME_IN_FILE: String = "jbUid"

    private val LOG = logger<EduRemoteUidRetrieverService>()
    private val PATTERN = """$JB_UID_PROPERTY_NAME_IN_FILE="(\S+)"""".toPattern()
  }
}
