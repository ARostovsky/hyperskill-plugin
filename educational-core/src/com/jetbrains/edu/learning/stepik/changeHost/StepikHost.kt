package com.jetbrains.edu.learning.stepik.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_DEFAULT_URL
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HOST_ORDINAL_PROPERTY
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_RELEASE_URL
import com.jetbrains.edu.learning.stepik.StepikOAuthBundle.value

enum class StepikHost(val url: String, val clientId: String, val clientSecret: String) {

  PRODUCTION(STEPIK_DEFAULT_URL, value("stepikClientId"), value("stepikClientSecret")),
  RELEASE(STEPIK_RELEASE_URL, value("stepikNonProductionClientId"), value("stepikNonProductionClientSecret"));

  override fun toString(): String {
    return url
  }

  companion object {
    fun getSelectedHost(): StepikHost = values()[PropertiesComponent.getInstance().getInt(STEPIK_HOST_ORDINAL_PROPERTY, PRODUCTION.ordinal)]
  }
}