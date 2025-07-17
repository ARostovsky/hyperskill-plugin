package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.StepikLanguage
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory
import com.jetbrains.edu.learning.submissions.SolutionFile

object HyperskillSubmitConnector {
  fun submitCodeTask(project: Project, task: CodeTask): Result<StepikBasedSubmission, String> {
    val connector = task.getStepikBasedConnector()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    // getLanguage() needed for backwards compatibility
    val submissionLanguage = task.submissionLanguage ?: getLanguage(task).onError { return Err(it) }

    val configurator = task.course.configurator
    val codeTaskText = configurator?.getCodeTaskFile(project, task)?.getText(project)
    if (codeTaskText == null) {
      LOG.error("Unable to create submission: file with code is not found for the task ${task.name}")
      return Err(EduCoreBundle.message("error.failed.to.post.solution.to", connector.platformName))
    }
    val submission = HyperskillSubmissionFactory.createCodeTaskSubmission(attempt, codeTaskText, submissionLanguage)
    return connector.postSubmission(submission)
  }

  // will be fixed or removed in EDU-4781
  private fun getLanguage(task: CodeTask): Result<String, String> {
    val course = task.course
    return when {
      course is HyperskillCourse -> getHyperskillLanguage(task)
      course.isStepikRemote -> getStepikLanguage(task)
      else -> error("Task ${task.name} doesn't belong to Hyperskill nor Stepik courses")
    }
  }

  private fun getHyperskillLanguage(task: CodeTask): Result<String, String> {
    val course = task.course
    val defaultLanguage = HyperskillLanguages.getLanguageName(course.languageId)
    if (defaultLanguage == null) {
      val languageDisplayName = course.languageDisplayName
      return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
    }
    return Ok(defaultLanguage)
  }

  private fun getStepikLanguage(task: CodeTask): Result<String, String> {
    val course = task.course

    val courseLanguageId = course.languageById?.id
    if (courseLanguageId != null) {
      val defaultLanguage = StepikLanguage.langOfId(courseLanguageId, course.languageVersion).langName
      if (defaultLanguage != null) {
        return Ok(defaultLanguage)
      }
    }

    val languageDisplayName = course.languageDisplayName
    return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
  }

  fun submitRemoteEduTask(task: RemoteEduTask, files: List<SolutionFile>): Result<StepikBasedSubmission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val taskSubmission = HyperskillSubmissionFactory.createRemoteEduTaskSubmission(task, attempt, files)
    return connector.postSubmission(taskSubmission)
  }

  private val LOG: Logger = logger<HyperskillSubmitConnector>()
}