package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.TaskFileBuilder
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, ENCRYPTED_TEXT, LEARNER_CREATED)
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {

  @JsonProperty(ENCRYPTED_TEXT)
  @Encrypt
  fun getTextToSerialize(): String {
    throw NotImplementedError()
  }

  @JsonProperty(LEARNER_CREATED)
  private var myLearnerCreated = false
}

private class StudentTaskFileBuilder(
  @JsonProperty(TEXT) val textFromConfig: String?,
  @Encrypt @JsonProperty(ENCRYPTED_TEXT) val encryptedTextFromConfig: String?,
  @JsonProperty(LEARNER_CREATED) val learnerCreated: Boolean = false,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true
) : TaskFileBuilder(name, placeholders, visible) {
  override fun createTaskFile(): TaskFile {
    return super.createTaskFile().apply {
      if (encryptedTextFromConfig != null) {
        setText(encryptedTextFromConfig)
      }
      else {
        setText(textFromConfig)
      }
      isLearnerCreated = learnerCreated
    }
  }
}

