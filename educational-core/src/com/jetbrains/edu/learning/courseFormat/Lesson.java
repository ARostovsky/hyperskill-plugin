package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.visitors.TaskVisitor;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.yaml.YamlDeserializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * To introduce new lesson type it's required to:
 *  - Extend Lesson class
 *  - Go to {@link ItemContainer#items} and update elementTypes in AbstractCollection annotation. Needed for proper xml serialization
 *  - Handle xml migration in {@link com.jetbrains.edu.learning.serialization.converter.xml.BaseXmlConverter#convert}
 *  - Handle yaml deserialization {@link YamlDeserializer#deserializeLesson(com.fasterxml.jackson.databind.ObjectMapper, String)}
 */
public class Lesson extends ItemContainer {
  @Transient public List<Integer> steps;
  @Transient boolean is_public;
  public int unitId = 0;

  @Transient
  private Course myCourse = null;
  @Transient
  private Section mySection = null;

  public Lesson() {
  }

  public void init(@Nullable final Course course, @Nullable final StudyItem section, boolean isRestarted) {
    mySection = section instanceof Section ? (Section)section : null;
    setCourse(course);
    List<Task> tasks = getTaskList();
    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      task.setIndex(i + 1);
      task.init(course, this, isRestarted);
    }
  }

  /**
   * Returns tasks copy. Dedicated methods should be used to modify list of lesson items ([addTask], [removeTask])
   */
  public List<Task> getTaskList() {
    return items.stream().filter(Task.class::isInstance).map(Task.class::cast).collect(Collectors.toList());
  }

  @NotNull
  @Transient
  public Course getCourse() {
    return myCourse;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return mySection == null ? myCourse : mySection;
  }

  @Override
  public String getItemType() {
    return "lesson";
  }

  @NotNull
  @Override
  public String getUIName() {
    return EduCoreBundle.message("study.item.lesson");
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
  }

  public void addTask(@NotNull final Task task) {
    items.add(task);
  }

  public void addTask(int index, @NotNull final Task task) {
    items.add(index, task);
  }

  public void removeTask(@NotNull final Task task) {
    items.remove(task);
  }

  @Nullable
  public Task getTask(@NotNull final String name) {
    return (Task)getItem(name);
  }

  @Nullable
  public Task getTask(int id) {
    for (Task task : getTaskList()) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  @Transient
  public boolean isPublic() {
    return is_public;
  }

  @Transient
  public void setPublic(boolean isPublic) {
    this.is_public = isPublic;
  }

  @Transient
  @Nullable
  public Section getSection() {
    return mySection;
  }

  @Transient
  public void setSection(@Nullable Section section) {
    mySection = section;
  }

  @Nullable
  public VirtualFile getLessonDir(@NotNull final Project project) {
    VirtualFile courseDir = OpenApiExtKt.getCourseDir(project);

    if (mySection == null) {
      return courseDir.findChild(getName());
    }
    else {
      VirtualFile sectionDir = courseDir.findChild(mySection.getName());
      assert sectionDir != null : "Section dir for lesson not found";

      return sectionDir.findChild(getName());
    }
  }

  @NotNull
  public LessonContainer getContainer() {
    if (mySection != null) {
      return mySection;
    }
    return myCourse;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return getLessonDir(project);
  }

  public void visitTasks(@NotNull TaskVisitor visitor) {
    for (Task task : getTaskList()) {
      visitor.visit(task);
    }
  }
}
