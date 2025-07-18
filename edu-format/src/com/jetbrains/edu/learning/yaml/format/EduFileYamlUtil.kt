@file:Suppress("unused")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.message
import com.jetbrains.edu.learning.json.mixins.HighlightLevelValueFilter
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.json.mixins.TrueValueFilter
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HIGHLIGHT_LEVEL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_BINARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROPAGATABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE
import com.jetbrains.edu.learning.yaml.format.student.IsBinaryFilter
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageBinaryContents
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageTextualContents

/**
 * Base mixin class used to deserialize task and additional files item.
 */
abstract class EduFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var name: String
}

@JsonDeserialize(builder = AdditionalFileBuilder::class)
@JsonPropertyOrder(NAME, IS_BINARY)
abstract class AdditionalFileYamlMixin : EduFileYamlMixin() {

  private val isBinary: Boolean?
    @JsonProperty(IS_BINARY)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IsBinaryFilter::class)
    get() = throw NotImplementedInMixin()
}

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin : EduFileYamlMixin() {
  @JsonProperty(VISIBLE)
  private var isVisible = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(EDITABLE)
  private var isEditable = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(PROPAGATABLE)
  private val isPropagatable = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = HighlightLevelValueFilter::class)
  @JsonProperty(HIGHLIGHT_LEVEL)
  private var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
}

/**
 * A base class for building task files and additional files
 */
open class EduFileBuilder(
  @JsonProperty(NAME) val name: String?
) {
  protected open fun setupEduFile(eduFile: EduFile) {
    eduFile.name = name ?: formatError(message("yaml.editor.invalid.file.without.name"))
  }
}

@JsonPOJOBuilder(buildMethodName = "buildAdditionalFile", withPrefix = "")
class AdditionalFileBuilder(
  @JsonProperty(IS_BINARY) val isBinary: Boolean? = false,
  name: String?
) : EduFileBuilder(name) {

  fun buildAdditionalFile(): EduFile {
    val additionalFile = EduFile()
    setupEduFile(additionalFile)
    setupAdditionalFile(additionalFile)
    return additionalFile
  }

  private fun setupAdditionalFile(eduFile: EduFile) {
    eduFile.contents = if (isBinary == true) {
      TakeFromStorageBinaryContents
    }
    else {
      TakeFromStorageTextualContents
    }
  }
}

@JsonPOJOBuilder(buildMethodName = "buildTaskFile", withPrefix = "")
open class TaskFileBuilder(
  name: String?,
  @JsonProperty(VISIBLE) val visible: Boolean = true,
  @JsonProperty(EDITABLE) val editable: Boolean = true,
  @JsonProperty(PROPAGATABLE) val propagatable: Boolean = true,
  @JsonProperty(HIGHLIGHT_LEVEL) val errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
) : EduFileBuilder(name) {

  @Suppress("unused") //used for deserialization
  fun buildTaskFile(): TaskFile {
    val taskFile = TaskFile()
    setupEduFile(taskFile)
    setupTaskFile(taskFile)
    return taskFile
  }

  protected open fun setupTaskFile(taskFile: TaskFile) {
    taskFile.isVisible = visible
    taskFile.isEditable = editable
    taskFile.isPropagatable = propagatable
    if (errorHighlightLevel != EduFileErrorHighlightLevel.ALL_PROBLEMS && errorHighlightLevel != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      taskFile.errorHighlightLevel = errorHighlightLevel
    }
  }
}