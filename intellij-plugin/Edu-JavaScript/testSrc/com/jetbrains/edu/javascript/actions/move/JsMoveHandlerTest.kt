package com.jetbrains.edu.javascript.actions.move

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course

class JsMoveHandlerTest : MoveHandlerTestBase(JavascriptLanguage.INSTANCE) {

  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.js") }
    doTest(findTarget) {
      javaScriptTaskFile("foo.js", "function foo<caret>(a, b) {}")
      javaScriptTaskFile("bar.js")
    }
  }

  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.js") }
    doTest(findTarget) {
      javaScriptTaskFile("foo.js", "class Foo<caret> {}")
      javaScriptTaskFile("bar.js")
    }
  }
}
