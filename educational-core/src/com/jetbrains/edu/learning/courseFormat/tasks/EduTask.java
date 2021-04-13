package com.jetbrains.edu.learning.courseFormat.tasks;

import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Original Edu plugin tasks with local tests and answer placeholders
 */
public class EduTask extends Task {
  public static final String EDU_TASK_TYPE = "edu";
  public static final String PYCHARM_TASK_TYPE = "pycharm";

  public EduTask() {
  }

  public EduTask(@NotNull final String name) {
    super(name);
  }

  public EduTask(@NotNull final String name, int id, int position, @NotNull Date updateDate, @NotNull CheckStatus status) {
    super(name, id, position, updateDate, status);
  }

  @Override
  public String getItemType() {
    return EDU_TASK_TYPE;
  }

  @Override
  public boolean isToSubmitToStepik() {
    return myStatus != CheckStatus.Unchecked;
  }

  @Override
  public boolean supportSubmissions() {
    return true;
  }
}
