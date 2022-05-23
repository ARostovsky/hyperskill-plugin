package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Update {@link StepikChangeRetriever#isEqualTo(AnswerPlaceholder, AnswerPlaceholder)} if you added new property that has to be compared
 */
public class AnswerPlaceholderDependency {
  private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("(([^#]+)#)?([^#]+)#([^#]+)#([^#]+)#(\\d+)");

  @Nullable
  private String mySectionName;
  private String myLessonName;
  private String myTaskName;
  private String myFileName;
  private int myPlaceholderIndex;
  private boolean myIsVisible = true;
  transient private AnswerPlaceholder myAnswerPlaceholder = null;

  public AnswerPlaceholderDependency() {
  }

  public AnswerPlaceholderDependency(@NotNull AnswerPlaceholder answerPlaceholder,
                                     @Nullable String sectionName,
                                     @NotNull String lessonName,
                                     @NotNull String taskName,
                                     @NotNull String fileName,
                                     int placeholderIndex,
                                     boolean isVisible) {
    mySectionName = sectionName;
    myLessonName = lessonName;
    myTaskName = taskName;
    myFileName = fileName;
    myPlaceholderIndex = placeholderIndex;
    myAnswerPlaceholder = answerPlaceholder;
    myIsVisible = isVisible;
  }

  @Nullable
  public AnswerPlaceholder resolve(@NotNull Course course) {
    Lesson lesson = course.getLesson(mySectionName, myLessonName);
    if (lesson == null) {
      return null;
    }
    Task task = lesson.getTask(myTaskName);
    if (task == null) {
      return null;
    }
    TaskFile taskFile = task.getTaskFile(myFileName);
    if (taskFile == null) {
      return null;
    }
    if (!EduUtils.indexIsValid(myPlaceholderIndex, taskFile.getAnswerPlaceholders())) {
      return null;
    }
    return taskFile.getAnswerPlaceholders().get(myPlaceholderIndex);
  }

  public AnswerPlaceholder getAnswerPlaceholder() {
    return myAnswerPlaceholder;
  }

  public void setAnswerPlaceholder(AnswerPlaceholder answerPlaceholder) {
    myAnswerPlaceholder = answerPlaceholder;
  }

  @Nullable
  public static AnswerPlaceholderDependency create(@NotNull AnswerPlaceholder answerPlaceholder, @NotNull String text) {
    return create(answerPlaceholder, text, true);
  }

  @Nullable
  public static AnswerPlaceholderDependency create(@NotNull AnswerPlaceholder answerPlaceholder, @NotNull String text, boolean isVisible)
    throws InvalidDependencyException {
    if (StringUtil.isEmptyOrSpaces(text)) {
      return null;
    }
    Task task = answerPlaceholder.getTaskFile().getTask();
    Course course = task.getCourse();
    Matcher matcher = DEPENDENCY_PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new InvalidDependencyException(text);
    }
    try {
      String sectionName = matcher.group(2);
      String lessonName = matcher.group(3);
      String taskName = matcher.group(4);
      String file = FileUtil.toSystemIndependentName(matcher.group(5));
      int placeholderIndex = Integer.parseInt(matcher.group(6)) - 1;
      AnswerPlaceholderDependency dependency = new AnswerPlaceholderDependency(answerPlaceholder, sectionName, lessonName, taskName, file, placeholderIndex, isVisible);
      AnswerPlaceholder targetPlaceholder = dependency.resolve(course);
      if (targetPlaceholder == null) {
        throw new InvalidDependencyException(text, EduCoreBundle.message("exception.placeholder.non.existing"));
      }
      if (targetPlaceholder.getTaskFile().getTask() == task) {
        throw new InvalidDependencyException(text, EduCoreBundle.message("exception.placeholder.wrong.reference.to.source"));
      }
      if (refersToNextTask(task, targetPlaceholder.getTaskFile().getTask())) {
        throw new InvalidDependencyException(text, EduCoreBundle.message("exception.placeholder.wrong.reference.to.next"));
      }
      return dependency;
    }
    catch (NumberFormatException e) {
      throw new InvalidDependencyException(text);
    }
  }

  private static boolean refersToNextTask(@NotNull Task sourceTask, @NotNull Task targetTask) {
    Lesson sourceLesson = sourceTask.getLesson();
    Lesson targetLesson = targetTask.getLesson();
    if (sourceLesson == targetLesson) {
      return targetTask.getIndex() > sourceTask.getIndex();
    }
    if (sourceLesson.getSection() == targetLesson.getSection()) {
      return targetLesson.getIndex() > sourceLesson.getIndex();
    }
    return getIndexInCourse(targetLesson) > getIndexInCourse(sourceLesson);

  }

  private static int getIndexInCourse(@NotNull Lesson lesson) {
    Section section = lesson.getSection();
    return section != null ? section.getIndex() : lesson.getIndex();
  }

  @Nullable
  public String getSectionName() {
    return mySectionName;
  }

  public void setSectionName(@Nullable String sectionName) {
    mySectionName = sectionName;
  }

  public String getLessonName() {
    return myLessonName;
  }

  public void setLessonName(String lessonName) {
    myLessonName = lessonName;
  }

  public String getTaskName() {
    return myTaskName;
  }

  public void setTaskName(String taskName) {
    myTaskName = taskName;
  }

  public String getFileName() {
    return myFileName;
  }

  public void setFileName(String fileName) {
    myFileName = fileName;
  }

  public int getPlaceholderIndex() {
    return myPlaceholderIndex;
  }

  public void setPlaceholderIndex(int placeholderIndex) {
    myPlaceholderIndex = placeholderIndex;
  }

  public boolean isVisible() {
    return myIsVisible;
  }

  public void setVisible(boolean visible) {
    myIsVisible = visible;
  }

  @Override
  public String toString() {
    String section = mySectionName != null ? mySectionName + "#" : "";
    return section + StringUtil.join(ContainerUtil.newArrayList(myLessonName, myTaskName, myFileName, myPlaceholderIndex + 1), "#");
  }

  public static class InvalidDependencyException extends IllegalStateException {
    private final String myCustomMessage;

    public InvalidDependencyException(String dependencyText) {
      super(EduCoreBundle.message("exception.placeholder.invalid.dependency.detailed", dependencyText));
      myCustomMessage = EduCoreBundle.message("exception.placeholder.invalid.dependency");
    }

    public InvalidDependencyException(String dependencyText, String customMessage) {
      super(EduCoreBundle.message("exception.placeholder.invalid.dependency.detailed.with.custom.message", dependencyText, customMessage));
      myCustomMessage = customMessage;
    }

    public String getCustomMessage() {
      return myCustomMessage;
    }
  }
}
