package org.hyperskill.academy.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.ui.InlineBannerBase
import com.intellij.util.Alarm
import com.intellij.util.ui.*
import org.hyperskill.academy.learning.actions.*
import org.hyperskill.academy.learning.checker.CheckUtils.getCustomRunConfigurationForRunner
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.projectView.CourseViewUtils.isSolved
import org.hyperskill.academy.learning.taskToolWindow.addActionLinks
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.ui.getUICheckLabel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel

class CheckPanel(private val project: Project, parentDisposable: Disposable) : JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val linkPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())

  val checkActionsPanel: JPanel = JPanel(BorderLayout())

  val checkAdditionalInformationPanel: AdditionalInformationPanel = AdditionalInformationPanel.create(project, this)

  /**
   * FIXME: Should be removed in favor of the [CheckPanel.leftActionsToolbar]
   * @see <a href="https://youtrack.jetbrains.com/issue/EDU-7584">EDU-7584</a>
   */
  private val leftActionsPanel: JPanel = JPanel()
  private val checkButtonWrapper = JPanel(BorderLayout())
  private val rightActionsToolbar: ActionToolbar = createRightActionToolbar()
  private val leftActionsToolbar: ActionToolbar = createLeftActionToolbar()
  private val course = project.course
  private val checkTimeAlarm: Alarm = Alarm(parentDisposable)
  private val asyncProcessIcon = AsyncProcessIcon("Submitting...")

  init {
    leftActionsPanel.layout = BoxLayout(leftActionsPanel, BoxLayout.X_AXIS)
    leftActionsPanel.add(checkButtonWrapper)
    leftActionsPanel.add(leftActionsToolbar.component)
    checkActionsPanel.add(leftActionsPanel, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(rightActionsToolbar.component, BorderLayout.EAST)
    checkActionsPanel.add(linkPanel, BorderLayout.NORTH)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.NORTH)
    add(checkAdditionalInformationPanel, BorderLayout.SOUTH)
    asyncProcessIcon.border = JBUI.Borders.empty(8, 6, 0, 10)
    maximumSize = Dimension(Int.MAX_VALUE, 30)
    border = JBUI.Borders.empty(2, 0, 0, 10)
  }

  fun readyToCheck() {
    addActionLinks(course, linkPanel, 10, 3)
    checkFinishedPanel.removeAll()
    checkDetailsPlaceholder.removeAll()
    checkTimeAlarm.cancelAllRequests()
  }

  fun checkStarted(startSpinner: Boolean) {
    readyToCheck()
    updateBackground()
    if (startSpinner) {
      checkFinishedPanel.add(asyncProcessIcon, BorderLayout.WEST)
    }
  }

  fun updateCheckDetails(task: Task, result: CheckResult? = null) {
    checkFinishedPanel.removeAll()
    checkFinishedPanel.addNextTaskButton(task)
    checkFinishedPanel.addRetryButton(task)

    val checkResult = result ?: restoreSavedResult(task)
    if (checkResult != null) {
      linkPanel.removeAll()
      checkDetailsPlaceholder.add(CheckDetailsPanel(project, task, checkResult, checkTimeAlarm), BorderLayout.SOUTH)
    }
    updateBackground()
  }

  private fun restoreSavedResult(task: Task): CheckResult? {
    val feedback = task.feedback
    if (feedback == null) {
      if (task.status == CheckStatus.Unchecked) {
        return null
      }
      return CheckResult(task.status)
    }
    else {
      if (task.isChangedOnFailed && task.status == CheckStatus.Failed) {
        feedback.message = EduCoreBundle.message("action.retry.shuffle.message")
      }
      return feedback.toCheckResult(task.status)
    }
  }

  private fun updateBackground() {
    UIUtil.setBackgroundRecursively(checkFinishedPanel, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkDetailsPlaceholder, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkButtonWrapper, TaskToolWindowView.getTaskDescriptionBackgroundColor())
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateCheckDetails(task)
    checkAdditionalInformationPanel.updateSize()
  }

  fun addHint(inlineBanner: InlineBannerBase) {
    checkDetailsPlaceholder.add(inlineBanner, BorderLayout.NORTH)
  }

  private fun createRightActionToolbar(): ActionToolbar {
    val actionGroup = ActionManager.getInstance().getAction("HyperskillEducational.CheckPanel.Right") as ActionGroup
    return ActionToolbarImpl(ACTION_PLACE, actionGroup, true).apply {
      targetComponent = this@CheckPanel
      setActionButtonBorder(2, 0)
      minimumButtonSize = Dimension(28, 28)
      border = JBEmptyBorder(5, 0, 0, 0)
    }
  }

  private fun createLeftActionToolbar(): ActionToolbar {
    val actionGroup = ActionManager.getInstance().getAction("HyperskillEducational.CheckPanel.Left") as ActionGroup
    return ActionToolbarImpl(ACTION_PLACE, actionGroup, true).apply {
      targetComponent = this@CheckPanel
      minimumButtonSize = JBDimension(28, 28)
      border = JBEmptyBorder(8, 0, 0, 0)
    }
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    when (task) {
      is TheoryTask -> {}
      else -> {
        val isDefault = !(task.isChangedOnFailed && task.status == CheckStatus.Failed || task.isSolved)
        val isEnabled = !(task.isChangedOnFailed && task.status == CheckStatus.Failed)
        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isDefault = isDefault, isEnabled = isEnabled)
        checkButtonWrapper.add(checkComponent, BorderLayout.WEST)
      }
    }
  }

  private fun JPanel.addNextTaskButton(task: Task) {
    if (!(task.status == CheckStatus.Solved
          || task is TheoryTask
          || task.course is HyperskillCourse)
    ) {
      return
    }

    val nextTask = NavigationUtils.nextTask(task)
    if (nextTask != null || (task.status == CheckStatus.Solved && NavigationUtils.isLastHyperskillProblem(task))) {
      updateCheckButtonWrapper(task) // to update the 'Check' button state
      val hasRunButton = getCustomRunConfigurationForRunner(project, task) != null
      val isDefault = (task is TheoryTask || task.isSolved) && !hasRunButton
      val action = ActionManager.getInstance().getAction(NextTaskAction.ACTION_ID)
      val nextButtonText = nextTask?.let { EduCoreBundle.message("button.next.task.text", nextTask.presentableName) }
      val nextButton = CheckPanelButtonComponent(action = action, isDefault = isDefault, customButtonText = nextButtonText)
      add(nextButton, BorderLayout.WEST)
    }
  }

  private fun JPanel.addRetryButton(task: Task) {
    if (!task.isChangedOnFailed) return

    if (task.status == CheckStatus.Failed) {
      val retryComponent = CheckPanelButtonComponent(
        EduActionUtils.getAction(RetryAction.ACTION_ID) as ActionWithProgressIcon,
        isDefault = true, isEnabled = true
      )
      add(retryComponent, BorderLayout.WEST)
    }
  }

  /**
   * Do not allow the component's height to be less than a preferred height
   */
  override fun getMinimumSize(): Dimension? {
    val minimumSize: Dimension? = super.minimumSize
    val preferredSize: Dimension = super.preferredSize ?: return minimumSize

    return Dimension(
      minimumSize?.width ?: preferredSize.width,
      preferredSize.height
    )
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}
