package org.hyperskill.academy.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.moveItemMessage
import javax.swing.JComponent

class CCMoveStudyItemDialog(
  project: Project,
  itemType: StudyItemType,
  thresholdName: String
) : DialogWrapper(project) {

  private val positionPanel: CCItemPositionPanel = CCItemPositionPanel(thresholdName)

  init {
    title = itemType.moveItemMessage
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    positionPanel.attach(this)
  }

  val indexDelta: Int get() = positionPanel.indexDelta
}
