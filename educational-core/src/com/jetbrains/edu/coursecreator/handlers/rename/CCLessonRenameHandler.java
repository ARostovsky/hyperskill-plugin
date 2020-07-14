package com.jetbrains.edu.coursecreator.handlers.rename;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.coursecreator.StudyItemType;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

public class CCLessonRenameHandler extends CCStudyItemRenameHandler {
  public CCLessonRenameHandler() {
    super(StudyItemType.LESSON);
  }

  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    return EduUtils.isLessonDirectory(project, file);
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiFileSystemItem item) {
    Lesson lesson = EduUtils.getLesson(project, course, item.getVirtualFile());
    if (lesson != null) {
      processRename(lesson, EduNames.LESSON, course, project, item.getVirtualFile());
      Section section = lesson.getSection();
      YamlFormatSynchronizer.saveItem(section != null ? section : course);
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename lesson";
  }
}
