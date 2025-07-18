package com.jetbrains.edu.learning.yaml.format.remote

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.CourseBuilder
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_CONTENT_PATH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = RemoteCourseBuilder::class)
abstract class RemoteCourseYamlMixin : CourseYamlMixin()

@JsonPOJOBuilder(withPrefix = "")
class RemoteCourseBuilder(
  @JsonProperty(TYPE) courseType: String?,
  @JsonProperty(TITLE) title: String,
  @JsonProperty(SUMMARY) summary: String,
  @JsonProperty(FEEDBACK_LINK) yamlFeedbackLink: String?,
  @JsonProperty(PROGRAMMING_LANGUAGE) displayProgrammingLanguageName: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) language: String,
  @JsonProperty(ENVIRONMENT) yamlEnvironment: String?,
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(SOLUTIONS_HIDDEN) areSolutionsHidden: Boolean?,
  @JsonProperty(TAGS) yamlContentTags: List<String> = emptyList(),
  @JsonProperty(CUSTOM_CONTENT_PATH) customContentPath: String = "",
) : CourseBuilder(
  courseType,
  title,
  summary,
  displayProgrammingLanguageName,
  programmingLanguageVersion,
  language,
  yamlEnvironment,
  content,
  areSolutionsHidden,
  yamlContentTags,
  pathToContent = customContentPath,
) {

  override fun makeCourse(): Course {
    return when (courseType) {
      HYPERSKILL_TYPE_YAML -> HyperskillCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType ?: "Unknown", EduFormatNames.COURSE))
    }
  }
}
