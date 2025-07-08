package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.marketplace.lti.LtiCourseMetadataProcessor.Companion.LTI_COURSERA_COURSE
import com.jetbrains.edu.learning.marketplace.lti.LtiCourseMetadataProcessor.Companion.LTI_LAUNCH_ID
import com.jetbrains.edu.learning.marketplace.lti.LtiCourseMetadataProcessor.Companion.LTI_LMS_DESCRIPTION
import com.jetbrains.edu.learning.marketplace.lti.LtiCourseMetadataProcessor.Companion.LTI_STUDY_ITEM_ID
import com.jetbrains.edu.learning.navigation.NavigationProperties
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class LtiCourseMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `lti metadata`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "description"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettingsDTO("id", "description", LTIOnlineService.STANDALONE, null),
      LTISettingsManager.getInstance(project).settings
    )
    assertEquals(NavigationProperties(-1), StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Test
  fun `lti metadata with study item id`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "description",
      LTI_STUDY_ITEM_ID to "12345"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettingsDTO("id", "description", LTIOnlineService.STANDALONE, null),
      LTISettingsManager.getInstance(project).settings
    )
    assertEquals(NavigationProperties(12345), StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Test
  fun `missing required parameters`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_STUDY_ITEM_ID to "12345"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(LTISettingsManager.getInstance(project).settings)
    assertNull(StudyItemSelectionService.getInstance(project).studyItemSettings.value)
  }

  @Test
  fun `coursera_course affects return link`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "moodle",
      LTI_COURSERA_COURSE to "cats"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettingsDTO("id", "moodle", LTIOnlineService.STANDALONE, "https://www.coursera.org/learn/cats"),
      LTISettingsManager.getInstance(project).settings
    )
  }
}
