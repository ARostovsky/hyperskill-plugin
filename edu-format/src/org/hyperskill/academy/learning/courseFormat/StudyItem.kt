package org.hyperskill.academy.learning.courseFormat

import java.util.*

/**
 * Base class for all items in course: section, lesson, task
 *
 * For each base type of study item (`Course`, `Section`, `Lesson`, `Task`)
 * there is the corresponding element in `StudyItemType` enum.
 *
 * @see Section
 * @see Lesson
 * @see FrameworkLesson
 * @see org.hyperskill.academy.learning.courseFormat.tasks.Task
 *
 * @see org.hyperskill.academy.coursecreator.StudyItemType
 */
abstract class StudyItem() {
  // from 1 to number of items
  var index: Int = -1
  var name: String = ""
  var updateDate: Date = Date(0)
  var id: Int = 0 // id on remote resource (Stepik, Marketplace)
  var contentTags: List<String> = listOf()

  @Transient
  private var _parent: ItemContainer? = null

  open var parent: ItemContainer
    get() = _parent ?: error("Parent is null for StudyItem $name")
    set(value) {
      _parent = value
    }

  abstract val course: Course
  abstract val itemType: String     // used in json/yaml serialization/deserialization

  // Non unique lesson/task/section names can be received from stepik. In this case unique directory name is generated,
  // but original non unique name is displayed
  @get:Deprecated("Should be used only for deserialization. Use {@link StudyItem#getPresentableName()} instead")
  var customPresentableName: String? = null

  @Suppress("DEPRECATION")
  val presentableName: String
    get() = customPresentableName ?: name

  constructor(name: String) : this() {
    this.name = name
  }

  abstract fun init(parentItem: ItemContainer, isRestarted: Boolean)

  fun getRelativePath(root: StudyItem): String {
    if (this == root) return ""
    val parents = mutableListOf<String>()
    var currentParent = parent
    while (currentParent != root) {
      parents.add(currentParent.name)
      currentParent = currentParent.parent
    }
    parents.reverse()
    if (parents.isEmpty()) return name
    parents.add(name)
    return parents.joinToString("/")
  }

  val pathInCourse: String
    get() = getRelativePath(course)
}
