package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.configFileName
import com.jetbrains.edu.yaml.YamlCodeInsightTest
import kotlin.reflect.KClass

abstract class YamlInspectionsTestBase(private val inspectionClass: KClass<out LocalInspectionTool>) : YamlCodeInsightTest() {

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(inspectionClass.java))
  }

  protected fun testQuickFix(item: StudyItem, fixName: String, configTextBefore: String, configTextAfter: String) {
    testHighlighting(item, configTextBefore)
    val action = myFixture.findSingleIntention(fixName)
    myFixture.launchAction(action)
    val configFile = item.getDir(project.courseDir)!!.findChild(item.configFileName)!!
    myFixture.openFileInEditor(configFile)
    myFixture.checkResult(configTextAfter)
  }

  protected fun testQuickFixIsUnavailable(item: StudyItem, fixName: String, text: String) {
    testHighlighting(item, text)
    check(myFixture.filterAvailableIntentions(fixName).isEmpty()) {
      "Fix `$fixName` should not be possible to apply."
    }
  }

  protected fun testHighlighting(item: StudyItem, configText: String) {
    openConfigFileWithText(item, configText)
    myFixture.checkHighlighting()
  }
}
