package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.ext.sourceDir
import org.hyperskill.academy.learning.courseFormat.ext.testDirs
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.projectView.CourseViewUtils.modifyTaskChildNode

open class DirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  task: Task?
) : EduNode<Task>(project, value, viewSettings, task) {

  override fun canNavigate(): Boolean = true

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return modifyTaskChildNode(myProject, childNode, item, this::createChildFileNode, this::createChildDirectoryNode)
  }

  open fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return DirectoryNode(myProject, value, settings, item)
  }

  open fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): AbstractTreeNode<*> {
    return originalNode
  }

  override fun updateImpl(data: PresentationData) {
    val course = StudyTaskManager.getInstance(myProject).course ?: return
    val dir = value
    val directoryFile = dir.virtualFile
    val name = directoryFile.name
    if (name == course.sourceDir || course.testDirs.contains(name)) {
      data.presentableText = name
    }
    else {
      val parentValue = parentValue
      data.presentableText = ProjectViewDirectoryHelper.getInstance(myProject).getNodeName(settings, parentValue, dir)
    }
  }
}
