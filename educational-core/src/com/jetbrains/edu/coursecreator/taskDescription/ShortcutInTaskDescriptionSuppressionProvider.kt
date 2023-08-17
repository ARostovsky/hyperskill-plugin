package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.codeInspection.DefaultXmlSuppressionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.taskToolWindow.containsShortcut

class ShortcutInTaskDescriptionSuppressionProvider : DefaultXmlSuppressionProvider() {

  override fun isSuppressedFor(element: PsiElement, inspectionId: String): Boolean =
    inspectionId == "CheckDtdRefs" && element.text.containsShortcut()

  override fun isProviderAvailable(file: PsiFile): Boolean =
    CCUtils.isCourseCreator(file.project) && EduUtilsKt.isTaskDescriptionFile(file.name)
}