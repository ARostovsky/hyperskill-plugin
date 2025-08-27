package org.hyperskill.academy.learning.handlers.rename

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.sourceDir
import org.hyperskill.academy.learning.getTask

class TaskRenameProcessor : EduStudyItemRenameProcessor() {

  override fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem? {
    return file.getTask(project)
  }

  override fun getDirectory(element: PsiElement, course: Course): PsiDirectory {
    val directory = element.toPsiDirectory()
    val sourceDir = course.sourceDir
    return if (directory.name == sourceDir) directory.parent ?: directory else directory
  }
}
