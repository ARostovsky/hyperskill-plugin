package org.hyperskill.academy.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.hyperskill.academy.coursecreator.CCStudyItemPathInputValidator
import org.hyperskill.academy.coursecreator.framework.CCFrameworkLessonManager
import org.hyperskill.academy.coursecreator.handlers.StudyItemRefactoringHandler
import org.hyperskill.academy.coursecreator.presentableTitleName
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.studyItemType
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

abstract class EduStudyItemRenameProcessor : RenamePsiFileProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (!(element is PsiDirectory || element is PsiDirectoryContainer)) return false
    val project = element.project
    val course = project.course ?: return false
    val file = getDirectory(element.toPsiDirectory(), course).virtualFile
    return getStudyItem(project, course, file) != null
  }

  override fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): RenameDialog {
    val course = project.course ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)
    val file = getDirectory(element.toPsiDirectory(), course).virtualFile
    val item = getStudyItem(project, course, file) ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)
    return createRenameDialog(project, element, nameSuggestionContext, editor, Factory(item))
  }

  override fun findReferences(
    element: PsiElement,
    searchScope: SearchScope,
    searchInCommentsAndStrings: Boolean
  ): MutableCollection<PsiReference> {
    val references = super.findReferences(element, searchScope, searchInCommentsAndStrings)
    return StudyItemRefactoringHandler.processUsageReferences(references)
  }

  protected open fun getDirectory(element: PsiElement, course: Course): PsiDirectory = element.toPsiDirectory()

  protected abstract fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem?

  protected fun PsiElement.toPsiDirectory(): PsiDirectory {
    return (this as? PsiDirectoryContainer)?.directories?.first() ?: (this as PsiDirectory)
  }

  private class Factory(private val item: StudyItem) : RenameDialogFactory {

    override fun createRenameDialog(
      project: Project,
      element: PsiElement,
      nameSuggestionContext: PsiElement?,
      editor: Editor?
    ): EduRenameDialogBase {
      return object : EduRenameDialogBase(project, element, nameSuggestionContext, editor) {

        init {
          title = EduCoreBundle.message("action.rename", item.studyItemType.presentableTitleName)
        }

        override fun performRename(newName: String) {
          StudyItemRefactoringHandler.processBeforeRename(project, item, newName)
          super.performRename(newName)
          CCFrameworkLessonManager.getInstance(project).migrateRecordsRename(item, newName)
          item.name = newName
          YamlFormatSynchronizer.saveItem(item.parent)
          item.course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
        }

        @Throws(ConfigurationException::class)
        override fun canRun() {
          if (item.course.isStudy) {
            throw ConfigurationException(EduCoreBundle.message("error.invalid.rename.message"))
          }
          val itemDir = item.getDir(project.courseDir)
          val validator = CCStudyItemPathInputValidator(project, item.course, item.studyItemType, itemDir?.parent, item.name)
          if (!validator.checkInput(newName)) {
            throw ConfigurationException(validator.getErrorText(newName))
          }
        }
      }
    }
  }
}
