package com.jetbrains.edu.cpp.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeVisitor
import com.jetbrains.edu.cpp.findCMakeCommand
import com.jetbrains.edu.cpp.getCMakeProjectName
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.courseFormat.TaskFile
import org.jetbrains.annotations.Nls

class NoTaskProjectNameInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
    object : CMakeVisitor() {
      override fun visitFile(file: PsiFile) {
        return
      }
    }

  private class AddDefaultProjectNameFix(file: PsiFile, val taskFile: TaskFile) : LocalQuickFixOnPsiElement(file) {
    override fun getFamilyName(): String = "CMake"

    @Nls
    override fun getText(): String = EduCppBundle.message("project.name.not.set.fix.description")

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val mockFile = PsiFileFactory.getInstance(project).createFileFromText(
        "mock",
        CMakeListsFileType.INSTANCE, "project(${getCMakeProjectName(taskFile.task)})\n"
      )
      val projectCommand = mockFile.findCMakeCommand("project")!! // must be set in mock file
      val cmakeMinimumRequiredCommand = file.findCMakeCommand("cmake_minimum_required")
      if (cmakeMinimumRequiredCommand != null) {
        file.addAfter(projectCommand, cmakeMinimumRequiredCommand)
      }
      else {
        file.addBefore(projectCommand, file.firstChild)
      }
    }

  }
}
