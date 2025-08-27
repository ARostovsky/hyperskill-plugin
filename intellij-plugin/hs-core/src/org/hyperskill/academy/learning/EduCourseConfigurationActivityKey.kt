package org.hyperskill.academy.learning

import com.intellij.platform.backend.observation.ActivityKey
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls

object EduCourseConfigurationActivityKey : ActivityKey {
  override val presentableName: @Nls String
    get() = EduCoreBundle.message("course.configuration")
}
