package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = FrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT)
abstract class FrameworkLessonYamlMixin : LessonYamlMixin() {
  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class FrameworkLessonBuilder(@JsonProperty(CONTENT) content: List<String?> = emptyList()) : LessonBuilder(content) {
  override fun createLesson() = FrameworkLesson()
}
