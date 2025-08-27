package org.hyperskill.academy.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.serialization.SerializationUtils.Json.DESCRIPTION_FORMAT

class To10VersionJsonStepOptionConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    val descriptionFormat = stepOptionsJson.get(DESCRIPTION_FORMAT).asText()
    stepOptionsJson.put(DESCRIPTION_FORMAT, descriptionFormat.uppercase())
    return stepOptionsJson
  }
}
