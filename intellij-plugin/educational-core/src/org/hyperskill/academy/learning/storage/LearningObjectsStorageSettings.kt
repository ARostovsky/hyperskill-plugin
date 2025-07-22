package org.hyperskill.academy.learning.storage

import com.intellij.openapi.util.registry.Registry
import java.util.*

private const val REGISTRY_KEY = "hyperskill.learning.objects.storage.type"

/**
 * [getDefaultLearningObjectsStorageType] looks up the registry to find the default type for Learning Objects Storage for newly created projects
 */
fun getDefaultLearningObjectsStorageType(): LearningObjectStorageType {
  val typeFromRegistry = try {
    val selectedOption = Registry.get(REGISTRY_KEY).selectedOption
    LearningObjectStorageType.safeValueOf(selectedOption)
  }
  catch (_: MissingResourceException) {
    null
  }

  return typeFromRegistry ?: LearningObjectStorageType.YAML
}

fun setDefaultLearningObjectsStorageType(value: LearningObjectStorageType) {
  Registry.get(REGISTRY_KEY).selectedOption = value.toString()
}