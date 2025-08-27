package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.EduFormatNames.SECTION

open class Section : LessonContainer() {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    require(parentItem is Course) { "Course is null for section $name" }
    super.init(parentItem, isRestarted)
  }

  override val course: Course
    get() = parent as? Course ?: error("Course is null for section $name")
  override val itemType: String = SECTION

}
