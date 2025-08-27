package org.hyperskill.academy.csharp.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.rd.platform.util.project
import com.jetbrains.rider.model.riderSolutionLifecycle
import com.jetbrains.rider.projectView.nodes.getVirtualFile
import com.jetbrains.rider.projectView.solution
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.getLesson
import org.hyperskill.academy.learning.getSection
import org.hyperskill.academy.learning.getTask
import org.hyperskill.academy.learning.handlers.rename.EduTaskFileRenameProcessor
import org.hyperskill.academy.learning.handlers.rename.LessonRenameProcessor
import org.hyperskill.academy.learning.handlers.rename.SectionRenameProcessor
import org.hyperskill.academy.learning.handlers.rename.TaskRenameProcessor

class CSharpRenameHandler : RenameHandler {
  override fun invoke(p0: Project, p1: Editor?, p2: PsiFile?, p3: DataContext?) {
    error("Should not be called")
  }

  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
    val element = elements.firstOrNull() ?: return
    val file = dataContext?.getVirtualFile() ?: return
    val processor = when {
      file.getTask(project) != null -> TaskRenameProcessor()
      file.getLesson(project) != null -> LessonRenameProcessor()
      file.getSection(project) != null -> SectionRenameProcessor()
      file.getContainingTask(project) != null -> EduTaskFileRenameProcessor()
      else -> RenamePsiFileProcessor()
    }
    PsiElementRenameHandler.rename(
      element, project, element, CommonDataKeys.EDITOR.getData(dataContext),
      dataContext.getData(PsiElementRenameHandler.DEFAULT_NAME), processor
    )
  }

  override fun isAvailableOnDataContext(p0: DataContext): Boolean {
    return p0.project?.solution?.riderSolutionLifecycle?.isProjectModelReady?.valueOrNull ?: false
  }
}