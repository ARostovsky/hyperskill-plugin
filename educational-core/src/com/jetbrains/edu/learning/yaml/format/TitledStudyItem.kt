package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder for any StudyItem, should be filled with actual content later
 */
class TitledStudyItem(title: String) : StudyItem(title) {
  override fun getItemType(): String {
    throw NotImplementedError()
  }

  override fun getParent(): StudyItem {
    throw NotImplementedError()
  }

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override fun getUIName(): String {
    throw NotImplementedError()
  }

  override fun setName(name: String?) {
    throw NotImplementedError()
  }

  override fun getId(): Int {
    throw NotImplementedError()
  }

  override fun getDir(project: Project): VirtualFile {
    throw NotImplementedError()
  }

  override fun getCourse(): Course {
    throw NotImplementedError()
  }
}