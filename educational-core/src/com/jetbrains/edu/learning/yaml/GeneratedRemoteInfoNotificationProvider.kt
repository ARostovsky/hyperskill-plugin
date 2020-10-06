package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isRemoteConfigFile

class GeneratedRemoteInfoNotificationProvider(val project: Project) :
  EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {

  companion object {
    val KEY: Key<EditorNotificationPanel> = Key.create("Edu.generatedRemoteInfo")
    private const val NOTIFICATION_TEXT: String = "This is a generated file. Not intended for manual editing."
  }

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
    if (isRemoteConfigFile(file)) {
      val panel = EditorNotificationPanel()
      panel.setText(NOTIFICATION_TEXT)
      return panel
    }
    return null
  }
}
