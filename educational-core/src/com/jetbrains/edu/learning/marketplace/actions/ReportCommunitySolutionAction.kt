package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.diff.chains.SimpleDiffRequestChain.DiffRequestProducerWrapper
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.DumbAwareActionButton
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToReportCommunitySolutionNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls

class ReportCommunitySolutionAction : DumbAwareActionButton() {

  @Suppress("DialogTitleCapitalization")
  override fun updateButton(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return

    e.presentation.apply {
      isVisible = Registry.`is`(ShareMySolutionsAction.REGISTRY_KEY, false)
                  && project.isMarketplaceCourse()
                  && project.isStudentProject()
                  && e.userDataAvailable(TASK_ID_KEY)
                  && e.userDataAvailable(SUBMISSION_ID_KEY)

      if (!isVisible) return@apply

      isEnabled = e.getChainDiffVirtualFile()?.getUserData(IS_REPORTED) != true
      if (!isEnabled) {
        text = EduCoreBundle.message("marketplace.report.solutions.action.tooltip.text")
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val chainDiffVirtualFile = e.getChainDiffVirtualFile() ?: return
    val submissionTime = chainDiffVirtualFile.getSubmissionTime()

    if (!showYesNoDialog(project, submissionTime)) return

    val submissionId = chainDiffVirtualFile.getUserDataFromChain(SUBMISSION_ID_KEY) ?: return

    runInBackground(project, EduCoreBundle.message("marketplace.report.solutions.background.title")) {
      val isSuccessful = MarketplaceSubmissionsConnector.getInstance().reportSolution(submissionId)
      if (!isSuccessful) {
        showFailedToReportCommunitySolutionNotification(project)
        return@runInBackground
      }
      val taskId = chainDiffVirtualFile.getUserDataFromChain(TASK_ID_KEY) ?: return@runInBackground
      SubmissionsManager.getInstance(project).removeCommunitySubmission(taskId, submissionId)
      chainDiffVirtualFile.putUserData(IS_REPORTED, true)
      MarketplaceNotificationUtils.showSuccessReportCommunitySolutionNotification(project)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun showYesNoDialog(project: Project?, time: String?): Boolean = when (Messages.showYesNoDialog(
    project,
    EduCoreBundle.message("marketplace.report.solutions.dialog.text", time.toString()),
    EduCoreBundle.message("marketplace.report.solutions.dialog.title"),
    EduCoreBundle.message("marketplace.report.solutions.dialog.yes.text"),
    EduCoreBundle.message("marketplace.report.solutions.dialog.no.text"),
    AllIcons.General.Warning
  )) {
    Messages.YES -> true
    else -> false
  }

  private fun AnActionEvent.getChainDiffVirtualFile(): ChainDiffVirtualFile? = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile

  private fun <T> ChainDiffVirtualFile.getUserDataFromChain(key: Key<T>): T? = chain.getUserData(key)

  private fun <T> AnActionEvent.userDataAvailable(key: Key<T>): Boolean = getChainDiffVirtualFile()?.getUserDataFromChain(key) != null

  private fun ChainDiffVirtualFile.getSubmissionTime(): String {
    val diffRequestProducerWrapper = chain.requests.first() as DiffRequestProducerWrapper
    val diffName = diffRequestProducerWrapper.name
    return diffName.substringAfter(SOLUTION_PREFIX)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.ReportCommunitySolution"

    private val SOLUTION_PREFIX = EduCoreBundle.message("submissions.compare.community", "")
    private val IS_REPORTED: Key<Boolean> = Key.create("isReported")

    val TASK_ID_KEY: Key<Int> = Key.create("taskId")
    val SUBMISSION_ID_KEY: Key<Int> = Key.create("submissionId")
  }
}