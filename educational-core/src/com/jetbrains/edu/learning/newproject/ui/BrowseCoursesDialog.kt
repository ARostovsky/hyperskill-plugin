package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import javax.swing.JComponent

class BrowseCoursesDialog(val courses: List<Course>, customToolbarActions: DefaultActionGroup? = null) : OpenCourseDialogBase() {
  val panel = CoursesPanel(courses, this, customToolbarActions) { setEnabledViewAsEducator(it) }

  init {
    title = "Select Course"
    init()
    panel.addCourseValidationListener(object : CoursesPanel.CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        isOKActionEnabled = canStartCourse
      }
    })
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return panel
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(panel.selectedCourse, panel.locationString, panel.projectSettings)

  override fun createCenterPanel(): JComponent = panel

  override fun setError(error: ErrorState) {
    panel.updateErrorInfo(error)
  }

  fun setCoursesComparator(comparator: Comparator<Course>) {
    panel.setCoursesComparator(comparator)
  }

  fun setEmptyText(text: String) {
    panel.setEmptyText(text)
  }
}
