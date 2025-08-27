package org.hyperskill.academy.learning.yaml.format.student

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.errorHandling.YamlLoadingException
import org.hyperskill.academy.learning.yaml.format.TaskChangeApplier


class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    if (existingItem.solutionHidden != deserializedItem.solutionHidden && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.visibility.cannot.be.changed"))
    }
    super.applyChanges(existingItem, deserializedItem)
    if (existingItem.status != deserializedItem.status && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.status.cannot.be.changed"))
    }
    when (existingItem) {

      is EduTask -> {
        if (existingItem is RemoteEduTask) {
          existingItem.checkProfile = (deserializedItem as RemoteEduTask).checkProfile
        }
        existingItem.record = deserializedItem.record
      }
    }
  }

  override fun applyTaskFileChanges(existingTaskFile: TaskFile, deserializedTaskFile: TaskFile) {
    super.applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
    existingTaskFile.text = deserializedTaskFile.contents.textualRepresentation
  }

  override fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.not.allowed.to.change.task"))
  }
}