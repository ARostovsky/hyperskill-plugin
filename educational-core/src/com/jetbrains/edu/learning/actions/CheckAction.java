package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.impl.NotificationSettings;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker;
import com.jetbrains.edu.learning.checker.remote.RemoteTaskCheckerManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.projectView.ProgressUtil;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import com.jetbrains.edu.learning.taskDescription.ui.check.CheckPanel;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class CheckAction extends DumbAwareAction {
  public static final String ACTION_ID = "Educational.Check";
  private static final Logger LOG = Logger.getInstance(CheckAction.class);

  protected final Ref<Boolean> myCheckInProgress = new Ref<>(false);

  public CheckAction() {
    super(EduCoreBundle.message("check"), EduCoreBundle.message("check.solution"), null);
  }

  public CheckAction(String text) {
    super(text, text, null);
  }

  private CheckAction(String text, String description) {
    super(text, description, null);
  }

  public static CheckAction createCheckAction(@NotNull Task task) {
    if (task instanceof TheoryTask) {
      return new CheckAction(EduCoreBundle.message("check.run"), EduCoreBundle.message("check.run.solution"));
    }
    return task.getCheckAction();
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    if (DumbService.isDumb(project)) {
      showCheckUnavailablePopup(project);
      return;
    }
    CheckDetailsView.getInstance(project).clear();
    FileDocumentManager.getInstance().saveAllDocuments();
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (virtualFile == null) {
      return;
    }
    Task task = EduUtils.getTaskForFile(project, virtualFile);
    if (task == null) {
      return;
    }
    EduCounterUsageCollector.checkTask(task.getStatus());
    for (CheckListener listener : CheckListener.EP_NAME.getExtensionList()) {
      listener.beforeCheck(project, task);
    }

    StudyCheckTask checkTask = new StudyCheckTask(project, task);
    if (checkTask.isHeadless()) {
      // It's hack to make checker tests work properly.
      // `com.intellij.openapi.progress.ProgressManager.run(com.intellij.openapi.progress.Task)` executes task synchronously
      // if the task run in headless environment (e.g. in unit tests).
      // It blocks EDT and any next `ApplicationManager.getApplication().invokeAndWait()` call will hang because of deadlock
      Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().run(checkTask));
      //noinspection TestOnlyProblems
      EduUtils.waitAndDispatchInvocationEvents(future);
    }
    else {
      ProgressManager.getInstance().run(checkTask);
    }
  }

  private static void showCheckUnavailablePopup(Project project) {
    Balloon balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        ActionUtil.getUnavailableMessage(EduCoreBundle.message("check"), false),
        null,
        UIUtil.getToolTipActionBackground(),
        EduBrowserHyperlinkListener.INSTANCE)
      .createBalloon();

    balloon.show(TaskDescriptionView.getInstance(project).checkTooltipPosition(), Balloon.Position.above);
  }

  @Override
  public void update(AnActionEvent e) {
    if (CheckPanel.ACTION_PLACE.equals(e.getPlace())) {
      //action is being added only in valid context
      //no project in event in this case, so just enable it
      return;
    }

    final Presentation presentation = e.getPresentation();
    EduUtils.updateAction(e);

    Project project = e.getProject();
    if (project == null) {
      return;
    }

    final EduEditor studyEditor = EduUtils.getSelectedEduEditor(project);
    if (studyEditor != null) {
      final Task task = studyEditor.getTaskFile().getTask();
      if (task instanceof TheoryTask) {
        presentation.setText(EduCoreBundle.message("check.run"));
        presentation.setDescription(EduCoreBundle.message("check.run.solution"));
      }
      else {
        presentation.setText(EduCoreBundle.message("check"));
        presentation.setDescription(EduCoreBundle.message("check.solution"));
      }
    }
    if (presentation.isEnabled()) {
      presentation.setEnabled(!myCheckInProgress.get());
      return;
    }
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    if (virtualFile == null || FileEditorManager.getInstance(project).getSelectedTextEditor() == null) {
      return;
    }
    if (EduUtils.isTestsFile(project, virtualFile)) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private class StudyCheckTask extends com.intellij.openapi.progress.Task.Backgroundable {
    private final Project myProject;
    private final Task myTask;
    @Nullable private final TaskChecker myChecker;
    private CheckResult myResult;
    private String TEST_RESULTS_DISPLAY_ID = "Test Results: Run";

    public StudyCheckTask(@NotNull Project project, @NotNull Task task) {
      super(project, EduCoreBundle.message("checking.solution"), true);
      myProject = project;
      myTask = task;
      final Course course = task.getLesson().getCourse();
      EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
      if (configurator != null) {
        TaskCheckerProvider checkerProvider = configurator.getTaskCheckerProvider();
        myChecker = checkerProvider.getTaskChecker(task, project);
      }
      else {
        myChecker = null;
      }
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      indicator.setIndeterminate(true);
      myCheckInProgress.set(true);
      TaskDescriptionView.getInstance(myProject).checkStarted();

      long start = System.currentTimeMillis();
      NotificationSettings notificationSettings = turnOffTestRunnerNotifications();
      CheckResult localCheckResult = myChecker == null ? CheckResult.NO_LOCAL_CHECK : myChecker.check(indicator);
      ApplicationManager.getApplication()
        .invokeLater(() -> NotificationsConfigurationImpl.getInstanceImpl().changeSettings(notificationSettings));
      long end = System.currentTimeMillis();
      LOG.info(String.format("Checking of %s task took %d ms", myTask.getName(), end - start));
      if (localCheckResult.getStatus() == CheckStatus.Failed) {
        myResult = localCheckResult;
        return;
      }
      RemoteTaskChecker remoteChecker = RemoteTaskCheckerManager.remoteCheckerForTask(myProject, myTask);
      myResult = remoteChecker == null ? localCheckResult : remoteChecker.check(myProject, myTask, indicator);
    }

    @Override
    public void onSuccess() {
      String message = myResult.getMessage();
      CheckStatus status = myResult.getStatus();
      final String details = myResult.getDetails();
      if (myTask.getCourse().isStudy()) {
        myTask.setStatus(status);
        YamlFormatSynchronizer.saveItem(myTask);
      }
      if (status == CheckStatus.Failed) {
        if (myChecker != null) {
          myChecker.onTaskFailed(message, details);
        }
      }
      else if (status == CheckStatus.Solved) {
        if (myChecker != null) {
          myChecker.onTaskSolved(message);
        }
      }
      TaskDescriptionView.getInstance(myProject).checkFinished(myTask, myResult);
      ApplicationManager.getApplication().invokeLater(() -> {
        ProgressUtil.updateCourseProgress(myProject);
        ProjectView.getInstance(myProject).refresh();

        for (CheckListener listener : CheckListener.EP_NAME.getExtensions()) {
          listener.afterCheck(myProject, myTask, myResult);
        }
      });
      finishChecking();
    }

    @Override
    public void onCancel() {
      finishChecking();
      TaskDescriptionView.getInstance(myProject).readyToCheck();
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
      super.onThrowable(error);
      myResult = CheckResult.FAILED_TO_CHECK;
      TaskDescriptionView.getInstance(myProject).checkFinished(myTask, myResult);
      finishChecking();
    }

    private void finishChecking() {
      if (myChecker != null) {
        myChecker.clearState();
      }
      myCheckInProgress.set(false);
    }

    private NotificationSettings turnOffTestRunnerNotifications() {
      NotificationsConfigurationImpl notificationsConfiguration = NotificationsConfigurationImpl.getInstanceImpl();
      NotificationSettings testRunnerSettings = NotificationsConfigurationImpl.getSettings(TEST_RESULTS_DISPLAY_ID);
      notificationsConfiguration.changeSettings(TEST_RESULTS_DISPLAY_ID, NotificationDisplayType.NONE, false, false);
      return testRunnerSettings;
    }
  }
}
