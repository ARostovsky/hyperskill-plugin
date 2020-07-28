package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.PlaceholderPainter;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.messages.EduCoreActionBundle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CCDeleteAllAnswerPlaceholdersAction extends CCAnswerPlaceholderAction {

  public CCDeleteAllAnswerPlaceholdersAction() {
    super(() -> EduCoreActionBundle.message("delete.all.answer.placeholders"),
          () -> EduCoreActionBundle.message("delete.all.answer.placeholders.description"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    CCState state = getState(e);
    if (state == null) return;
    if (!state.getTaskFile().getAnswerPlaceholders().isEmpty()) {
      e.getPresentation().setEnabledAndVisible(true);
    }
    super.update(e);
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull CCState state) {
    final ClearPlaceholders action = new ClearPlaceholders(state.getProject(), state.getTaskFile(), state.getEditor());
    EduUtils.runUndoableAction(state.getProject(), EduCoreActionBundle.message("delete.all.answer.placeholders"), action,
                               UndoConfirmationPolicy.REQUEST_CONFIRMATION);
  }

  private static class ClearPlaceholders extends TaskFileUndoableAction {
    private final List<AnswerPlaceholder> myPlaceholders;
    private final TaskFile myTaskFile;

    public ClearPlaceholders(@NotNull Project project, @NotNull TaskFile taskFile, @NotNull Editor editor) {
      super(project, taskFile, editor);
      myTaskFile = taskFile;
      myPlaceholders = new ArrayList<>(taskFile.getAnswerPlaceholders());
    }

    @Override
    public boolean performUndo() {
      myTaskFile.getAnswerPlaceholders().addAll(myPlaceholders);
      PlaceholderPainter.showPlaceholders(getProject(), myTaskFile);
      return true;
    }

    @Override
    public void performRedo() {
      PlaceholderPainter.hidePlaceholders(myTaskFile);
      myTaskFile.getAnswerPlaceholders().clear();
    }

    @Override
    public boolean isGlobal() {
      return true;
    }
  }
}
