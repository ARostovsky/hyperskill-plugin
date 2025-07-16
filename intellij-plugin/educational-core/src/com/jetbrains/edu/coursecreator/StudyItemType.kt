package com.jetbrains.edu.coursecreator

import com.intellij.openapi.keymap.KeymapUtil
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

enum class StudyItemType {
  COURSE_TYPE,
  SECTION_TYPE,
  LESSON_TYPE,
  TASK_TYPE;
}

val StudyItemType.presentableName: String
  @Nls
  get() = when (this) {
    COURSE_TYPE -> message("item.course")
    SECTION_TYPE -> message("item.section")
    LESSON_TYPE -> message("item.lesson")
    TASK_TYPE -> message("item.task")
  }


val StudyItemType.presentableTitleName: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("item.course.title")
    SECTION_TYPE -> message("item.section.title")
    LESSON_TYPE -> message("item.lesson.title")
    TASK_TYPE -> message("item.task.title")
  }

val StudyItemType.newItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("item.new.course.title")
    SECTION_TYPE -> message("item.new.section.title")
    LESSON_TYPE -> message("item.new.lesson.title")
    TASK_TYPE -> message("item.new.task.title")
  }

val StudyItemType.selectItemTypeMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE_TYPE -> message("item.select.type.course")
    SECTION_TYPE -> message("item.select.type.section")
    LESSON_TYPE -> message("item.select.type.lesson")
    TASK_TYPE -> message("item.select.type.task")
  }

val StudyItemType.pressEnterToCreateItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() {
    val enter = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    return when (this) {
      COURSE_TYPE -> message("item.hint.press.enter.to.create.course", enter)
      SECTION_TYPE -> message("item.hint.press.enter.to.create.section", enter)
      LESSON_TYPE -> message("item.hint.press.enter.to.create.lesson", enter)
      TASK_TYPE -> message("item.hint.press.enter.to.create.task", enter)
    }
  }

val StudyItemType.moveItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("dialog.title.move.course")
    SECTION_TYPE -> message("dialog.title.move.section")
    LESSON_TYPE -> message("dialog.title.move.lesson")
    TASK_TYPE -> message("dialog.title.move.task")
  }

@Nls(capitalization = Nls.Capitalization.Sentence)
fun StudyItemType.failedToFindItemMessage(@NonNls itemName: String): String =
  when (this) {
    COURSE_TYPE -> message("item.failed.to.find.course", itemName)
    SECTION_TYPE -> message("item.failed.to.find.section", itemName)
    LESSON_TYPE -> message("item.failed.to.find.lesson", itemName)
    TASK_TYPE -> message("item.failed.to.find.task", itemName)
  }
