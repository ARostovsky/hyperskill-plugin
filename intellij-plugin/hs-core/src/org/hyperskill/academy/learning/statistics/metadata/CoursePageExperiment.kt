package org.hyperskill.academy.learning.statistics.metadata

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable
import org.hyperskill.academy.learning.statistics.metadata.CourseSubmissionMetadataManager.Companion.EXPERIMENT_ID
import org.hyperskill.academy.learning.statistics.metadata.CourseSubmissionMetadataManager.Companion.EXPERIMENT_VARIANT
import org.hyperskill.academy.learning.statistics.metadata.CourseSubmissionMetadataManager.Companion.MAX_VALUE_LENGTH


@Serializable
data class CoursePageExperiment(val experimentId: String, val experimentVariant: String) {

  fun toMetadataMap(): Map<String, String> = mapOf(
    EXPERIMENT_ID to experimentId,
    EXPERIMENT_VARIANT to experimentVariant,
  )

  companion object {
    private val LOG = logger<CoursePageExperiment>()

    fun fromParams(params: Map<String, String>): CoursePageExperiment? {
      val experimentId = params[EXPERIMENT_ID] ?: return null
      val experimentVariant = params[EXPERIMENT_VARIANT] ?: return null
      if (experimentVariant.length > MAX_VALUE_LENGTH) {
        LOG.warn("Experiment variant is too long: $experimentVariant. Max supported length is $MAX_VALUE_LENGTH")
        return null
      }
      return CoursePageExperiment(experimentId, experimentVariant)
    }
  }
}
