package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.collect.Lists;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.api.CourseAdditionalInfo;
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikUnit;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jetbrains.edu.coursecreator.CCUtils.collectAdditionalLessonInfo;
import static com.jetbrains.edu.learning.EduUtils.showNotification;
import static com.jetbrains.edu.learning.EduUtils.showOAuthDialog;
import static com.jetbrains.edu.learning.courseFormat.ext.CourseExt.getHasSections;

public class CCStepikConnector {
  private static final Logger LOG = Logger.getInstance(CCStepikConnector.class.getName());
  public static final String FAILED_TITLE = "Failed to publish ";
  public static final String PUBLISHING_COURSE_TITLE = "Publishing additional course data";
  public static final String PUSH_COURSE_GROUP_ID = "Push.course";
  private static final String JETBRAINS_USER_ID = "17813950";
  private static final List<Integer> TESTER_USER_IDS = Lists.newArrayList(17869355);

  private CCStepikConnector() { }

  // POST methods:

  public static void postCourseWithProgress(@NotNull final Project project, @NotNull final EduCourse course) {
    ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Modal(project, "Uploading Course", true) {
      @Override
      public void run(@NotNull final ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        if (checkIfAuthorized(project, "post course")) {
          postCourse(project, course);
        }
      }
    });
  }

  public static void postCourse(@NotNull final Project project, @NotNull EduCourse course) {
    final StepikUser user = EduSettings.getInstance().getUser();
    if (user == null) {
      // we check that user isn't null before `postCourse` call
      LOG.warn("User is null when posting the course");
      return;
    }
    updateProgress("Uploading course to " + StepikNames.STEPIK_URL);
    final StepikUserInfo currentUser = StepikConnector.getInstance().getCurrentUserInfo(user);
    if (currentUser != null) {
      final List<StepikUserInfo> courseAuthors = course.getAuthors();
      for (final StepikUserInfo courseAuthor : courseAuthors) {
        currentUser.setFirstName(courseAuthor.getFirstName());
        currentUser.setLastName(courseAuthor.getLastName());
      }
      course.setAuthors(Collections.singletonList(currentUser));
    }

    final EduCourse courseOnRemote = StepikConnector.getInstance().postCourse(course);
    if (courseOnRemote == null) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(course, true));
      return;
    }
    final List<StudyItem> items = course.getItems();
    courseOnRemote.setItems(Lists.newArrayList(items));
    courseOnRemote.setAuthors(course.getAuthors());
    courseOnRemote.setCourseMode(CCUtils.COURSE_MODE);
    courseOnRemote.setEnvironment(course.getEnvironment());
    courseOnRemote.setLanguage(course.getLanguage());

    if (!ApplicationManager.getApplication().isInternal() && !isTestAccount(currentUser)) {
      addJetBrainsUserAsAdmin(courseOnRemote.getAdminsGroup());
    }

    boolean success = getHasSections(course) ? postSections(project, courseOnRemote) : postTopLevelLessons(project, courseOnRemote);
    success = postCourseAdditionalInfo(course, project, courseOnRemote.getId()) && success;

    StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
    courseOnRemote.init(null, null, true);
    String message = "Course is " + (success ? "" : "partially ") + "published";
    showNotification(project, message, openOnStepikAction("/course/" + courseOnRemote.getId()));
  }

  public static boolean postCourseAdditionalInfo(@NotNull EduCourse course, @NotNull final Project project, int courseId) {
    if (!checkIfAuthorized(project, "post course additional information")) return false;

    updateProgress(PUBLISHING_COURSE_TITLE);
    String errors = CCUtils.checkIgnoredFiles(project);
    if (errors != null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post additional files." + errors);
      return false;
    }
    final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(course, project);
    CourseAdditionalInfo courseAdditionalInfo = new CourseAdditionalInfo(additionalFiles, course.getSolutionsHidden());
    int code = StepikConnector.getInstance().postCourseAttachment(courseAdditionalInfo, courseId);
    if (code != HttpStatus.SC_CREATED) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post additional files");
      return false;
    }
    return true;
  }

  /**
   * This method should be used for courses with sections only
   */
  private static boolean postSections(@NotNull Project project, @NotNull EduCourse course) {
    course.sortItems();
    final List<Section> sections = course.getSections();
    assert course.getLessons().isEmpty() : "postSections method should be used for courses with sections only";
    int i = 1;
    boolean success = true;
    for (Section section : sections) {
      checkCanceled();
      section.setPosition(i++);
      List<Lesson> lessons = section.getLessons();

      final int sectionId = postSectionInfo(project, section, course.getId());
      success = sectionId != -1 && postLessons(project, course, sectionId, lessons) && success;
    }
    return success;
  }

  private static boolean postTopLevelLessons(@NotNull Project project, @NotNull EduCourse course) {
    final int sectionId = postSectionForTopLevelLessons(project, course);
    return sectionId != -1 && postLessons(project, course, sectionId, course.getLessons());
  }

  public static int postSectionForTopLevelLessons(@NotNull Project project, @NotNull EduCourse course) {
    Section section = new Section();
    section.setName(course.getName());
    section.setPosition(1);
    int sectionId = postSectionInfo(project, section, course.getId());
    course.setSectionIds(Collections.singletonList(sectionId));
    return sectionId;
  }

  public static boolean postSection(@NotNull Project project, @NotNull Section section) {
    EduCourse course = (EduCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final int sectionId = postSectionInfo(project, section, course.getId());
    return sectionId != -1 && postLessons(project, course, sectionId, section.getLessons());
  }

  public static int postSectionInfo(@NotNull Project project, @NotNull Section section, int courseId) {
    if (!checkIfAuthorized(project, "post section")) return -1;

    section.setCourseId(courseId);
    final Section postedSection = StepikConnector.getInstance().postSection(section);
    if (postedSection == null) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(section, true));
      return -1;
    }
    section.setId(postedSection.getId());
    section.setUpdateDate(postedSection.getUpdateDate());
    return postedSection.getId();
  }

  private static boolean postLessons(@NotNull Project project, @NotNull EduCourse course, int sectionId, @NotNull List<Lesson> lessons) {
    int position = 1;
    boolean success = true;
    for (Lesson lesson : lessons) {
      updateProgress("Publishing lesson " + lesson.getIndex());
      success = postLesson(project, lesson, position, sectionId) && success;
      checkCanceled();
      position += 1;
    }
    return success;
  }

  public static boolean postLesson(@NotNull final Project project, @NotNull final Lesson lesson, int position, int sectionId) {
    Lesson postedLesson = postLessonInfo(project, lesson, sectionId, position);
    if (postedLesson == null) return false;

    boolean success = true;
    for (Task task : lesson.getTaskList()) {
      checkCanceled();
      success = postTask(project, task, postedLesson.getId()) && success;
    }
    if (!updateLessonAdditionalInfo(lesson, project)) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(lesson, true));
      return false;
    }
    return success;
  }

  public static Lesson postLessonInfo(@NotNull Project project, @NotNull Lesson lesson, int sectionId, int position) {
    if (!checkIfAuthorized(project, "postLesson")) return null;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final Lesson postedLesson = StepikConnector.getInstance().postLesson(lesson);
    if (postedLesson == null) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(lesson, true));
      return null;
    }
    if (sectionId != -1) {
      postedLesson.unitId = postUnit(postedLesson.getId(), position, sectionId, project);
    }

    lesson.setId(postedLesson.getId());
    lesson.unitId = postedLesson.unitId;
    lesson.setUpdateDate(postedLesson.getUpdateDate());
    return postedLesson;
  }

  public static int postUnit(int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "postUnit")) return lessonId;

    final StepikUnit unit = StepikConnector.getInstance().postUnit(lessonId, position, sectionId);
    if (unit == null || unit.getId() == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post unit");
      return -1;
    }
    return unit.getId();
  }

  public static boolean postTask(@NotNull final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return false;
    // TODO: add meaningful comment to final Success notification that Code tasks were not pushed
    if (task instanceof CodeTask) return true;

    final StepSource stepSource = StepikConnector.getInstance().postTask(project, task, lessonId);
    if (stepSource == null) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(task, task.getLesson(), true));
      return false;
    }
    task.setId(stepSource.getId());
    task.setUpdateDate(stepSource.getUpdateDate());
    return true;
  }

  // UPDATE methods:

  public static boolean updateCourseInfo(@NotNull final Project project, @NotNull final EduCourse course) {
    if (!checkIfAuthorized(project, "update course")) return false;
    // Course info parameters such as isPublic() and isCompatible can be changed from Stepik site only
    // so we get actual info here
    EduCourse courseInfo = StepikConnector.getInstance().getCourseInfo(course.getId());
    if (courseInfo != null) {
      course.setPublic(courseInfo.isPublic());
      course.setCompatible(courseInfo.isCompatible());
    }
    else {
      LOG.warn("Failed to get current course info");
    }
    int responseCode = StepikConnector.getInstance().updateCourse(course);

    if (responseCode == HttpStatus.SC_FORBIDDEN) {
      showNoRightsToUpdateNotification(project, course);
      return false;
    }
    if (responseCode != HttpStatus.SC_OK) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(course, false));
      return false;
    }
    return true;
  }

  public static boolean updateCourseAdditionalInfo(@NotNull Project project, @NotNull Course course) {
    if (!checkIfAuthorized(project, "update course additional information")) return false;

    EduCourse courseInfo = StepikConnector.getInstance().getCourseInfo(course.getId());
    assert courseInfo != null;
    updateProgress(PUBLISHING_COURSE_TITLE);
    String errors = CCUtils.checkIgnoredFiles(project);
    if (errors != null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update additional files." + errors);
      return false;
    }
    final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(courseInfo, project);
    CourseAdditionalInfo courseAdditionalInfo = new CourseAdditionalInfo(additionalFiles, course.getSolutionsHidden());
    return StepikConnector.getInstance().updateCourseAttachment(courseAdditionalInfo, courseInfo) == HttpStatus.SC_CREATED;
  }

  public static boolean updateSectionForTopLevelLessons(@NotNull EduCourse course) {
    Section section = new Section();
    section.setName(course.getName());
    section.setPosition(1);
    section.setId(course.getSectionIds().get(0));
    return updateSectionInfo(section);
  }

  public static boolean updateSection(@NotNull Section section, @NotNull Course course, @NotNull Project project) {
    section.setCourseId(course.getId());
    boolean updated = updateSectionInfo(section);
    if (!updated) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(section, false));
      return false;
    }
    for (Lesson lesson : section.getLessons()) {
      checkCanceled();
      if (lesson.getId() > 0) {
        updateLesson(project, lesson, false, section.getId());
      }
      else {
        postLesson(project, lesson, lesson.getIndex(), section.getId());
      }
    }

    return true;
  }

  public static boolean updateSectionInfo(@NotNull Section section) {
    section.units.clear();
    return StepikConnector.getInstance().updateSection(section) != null;
  }

  public static boolean updateLesson(@NotNull final Project project,
                                     @NotNull final Lesson lesson,
                                     boolean showNotification,
                                     int sectionId) {
    Lesson postedLesson = updateLessonInfo(project, lesson, showNotification, sectionId);
    return postedLesson != null &&
           updateLessonTasks(project, lesson, postedLesson.steps) &&
           updateLessonAdditionalInfo(lesson, project);
  }

  public static Lesson updateLessonInfo(@NotNull final Project project,
                                        @NotNull final Lesson lesson,
                                        boolean showNotification, int sectionId) {
    if (!checkIfAuthorized(project, "update lesson")) return null;
    // TODO: support case when lesson was removed from Stepik

    final Lesson updatedLesson = StepikConnector.getInstance().updateLesson(lesson);
    if (updatedLesson == null && showNotification) {
      showErrorNotification(project, FAILED_TITLE, getErrorMessage(lesson, false));
      return null;
    }
    if (sectionId != -1) {
      updateUnit(lesson.unitId, lesson.getId(), lesson.getIndex(), sectionId, project);
    }

    return updatedLesson;
  }

  public static boolean updateLessonAdditionalInfo(@NotNull final Lesson lesson, @NotNull Project project) {
    if (!checkIfAuthorized(project, "update lesson additional information")) return false;

    LessonAdditionalInfo info = collectAdditionalLessonInfo(lesson, project);
    if (info.isEmpty()) {
      StepikConnector.getInstance().deleteLessonAttachment(lesson.getId());
      return true;
    }
    updateProgress("Publishing additional data for " + lesson.getName());
    return StepikConnector.getInstance().updateLessonAttachment(info, lesson) == HttpStatus.SC_CREATED;
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "updateUnit")) return;

    final StepikUnit unit = StepikConnector.getInstance().updateUnit(unitId, lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update unit");
    }
  }

  private static boolean updateLessonTasks(@NotNull Project project, @NotNull Lesson localLesson, @NotNull List<Integer> steps) {
    final Set<Integer> localTasksIds = localLesson.getTaskList()
      .stream()
      .map(task -> task.getId())
      .filter(id -> id > 0)
      .collect(Collectors.toSet());

    final List<Integer> taskIdsToDelete = steps.stream()
      .filter(id -> !localTasksIds.contains(id))
      .collect(Collectors.toList());

    // Remove all tasks from Stepik which are not in our lessons now
    for (Integer step : taskIdsToDelete) {
      StepikConnector.getInstance().deleteTask(step);
    }

    boolean success = true;
    for (Task task : localLesson.getTaskList()) {
      checkCanceled();
      success = (task.getId() > 0 ? updateTask(project, task) : postTask(project, task, localLesson.getId())) && success;
    }
    return success;
  }

  public static boolean updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorized(project, "update task")) return false;
    VirtualFile taskDir = task.getDir(OpenApiExtKt.getCourseDir(project));
    if (taskDir == null) return false;
    final Course course = task.getLesson().getCourse();

    final int responseCode = StepikConnector.getInstance().updateTask(project, task);

    switch (responseCode) {
      case HttpStatus.SC_OK:
        StepSource step = StepikConnector.getInstance().getStep(task.getId());
        if (step != null) {
          task.setUpdateDate(step.getUpdateDate());
        }
        else {
          LOG.warn(String.format("Failed to get step for task '%s' with id %d while setting an update date", task.getName(), task.getId()));
        }
        return true;
      case HttpStatus.SC_NOT_FOUND:
        // TODO: support case when lesson was removed from Stepik too
        return postTask(project, task, task.getLesson().getId());
      case HttpStatus.SC_FORBIDDEN:
        showNoRightsToUpdateNotification(project, (EduCourse)course);
        return false;
      default:
        showErrorNotification(project, FAILED_TITLE, getErrorMessage(task, task.getLesson(), false));
        return false;
    }
  }

  // GET methods:

  public static int getTaskPosition(final int taskId) {
    StepSource step = StepikConnector.getInstance().getStep(taskId);
    return step != null ? step.getPosition() : -1;
  }

  // helper methods:

  private static boolean isTestAccount(@Nullable StepikUserInfo user) {
    return user != null && TESTER_USER_IDS.contains(user.getId());
  }

  private static void addJetBrainsUserAsAdmin(@NotNull String groupId) {
    final int responseCode = StepikConnector.getInstance().postMember(JETBRAINS_USER_ID, groupId);
    if (responseCode != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to add JetBrains as admin ");
    }
  }

  // TODO function is needed to be refactored after refactoring [showStepikNotification]
  public static boolean checkIfAuthorized(@NotNull Project project, @NotNull String failedActionName) {
    checkCanceled();
    if (!EduSettings.isLoggedIn()) {
      showStepikNotification(project, failedActionName);
      return false;
    }
    return true;
  }

  private static void updateProgress(@NotNull String text) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
      indicator.setText2(text);
    }
  }

  public static void showErrorNotification(@NotNull Project project,
                                           @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String title,
                                           @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message) {
    LOG.info(message);
    final Notification notification = new Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR);
    notification.notify(project);
  }

  private static void showNoRightsToUpdateNotification(@NotNull final Project project, @NotNull final EduCourse course) {
    String message = "You don't have permission to update the course <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
    Notification notification = new Notification(PUSH_COURSE_GROUP_ID, FAILED_TITLE, message, NotificationType.ERROR,
                                                 createPostCourseNotificationListener(project, course));
    notification.notify(project);
  }

  @NotNull
  public static NotificationListener.Adapter createPostCourseNotificationListener(@NotNull Project project,
                                                                                  @NotNull EduCourse course) {
    return new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        notification.expire();
        course.convertToLocal();
        postCourseWithProgress(project, course);
      }
    };
  }

  public static AnAction openOnStepikAction(@NotNull @NonNls String url) {
    return new AnAction(EduCoreBundle.message("action.open.on.text", StepikNames.STEPIK)) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(StepikNames.STEPIK_URL + url);
      }
    };
  }

  // TODO function is needed to be refactored for localization
  public static void showStepikNotification(@NotNull Project project, @NotNull String failedActionName) {
    String text = "Log in to Stepik to " + failedActionName;
    Notification notification = new Notification("Stepik", "Failed to " + failedActionName, text, NotificationType.ERROR);
    notification.addAction(new DumbAwareAction("Log in") {

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        StepikAuthorizer.doAuthorize(() -> showOAuthDialog());
        notification.expire();
      }
    });

    notification.notify(project);
  }

  private static void checkCanceled() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
    }
  }

  private static String getPrintableType(StudyItem item) {
    if (item instanceof Course) return EduNames.COURSE;
    if (item instanceof Section) return EduNames.SECTION;
    if (item instanceof FrameworkLesson) return EduNames.FRAMEWORK_LESSON;
    if (item instanceof Lesson) return EduNames.LESSON;
    if (item instanceof Task) return EduNames.TASK;
    return "item";
  }

  private static String getItemInfo(StudyItem item, boolean isNew) {
    String id = isNew ? "" : " (id = " + item.getId() + ")";
    return getPrintableType(item) + " `" + item.getName() + "`" + id;
  }

  private static String getErrorMessage(StudyItem item, boolean isNew) {
    return "Failed to " + (isNew ? "post " : "update ") + getItemInfo(item, isNew);
  }

  private static String getErrorMessage(StudyItem item, StudyItem parent, boolean isNew) {
    return getErrorMessage(item, isNew) + " in " + getItemInfo(parent, false);
  }
}
