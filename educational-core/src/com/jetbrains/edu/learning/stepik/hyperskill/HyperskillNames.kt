package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import org.jetbrains.ide.BuiltInServerManager

const val HYPERSKILL = "Hyperskill"
const val HYPERSKILL_TYPE = HYPERSKILL
const val HYPERSKILL_PROBLEMS = "Problems"
const val HYPERSKILL_URL_PROPERTY = "Hyperskill URL"
const val HYPERSKILL_DEFAULT_URL = "https://hyperskill.org/"
const val HYPERSKILL_PROJECTS_URL = "https://hyperskill.org/projects"
val HYPERSKILL_PROFILE_PATH = "${HYPERSKILL_URL}profile/"
private val port = BuiltInServerManager.getInstance().port
val REDIRECT_URI_DEFAULT = "http://localhost:$port/api/edu/hyperskill/oauth"

var CLIENT_ID = HyperskillOAuthBundle.valueOrDefault("clientId", "")
var CLIENT_SECRET = HyperskillOAuthBundle.valueOrDefault("clientSecret", "")
const val HYPERSKILL_PROJECT_NOT_SUPPORTED = "Selected project is not supported yet. " +
                                             "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select another project</a> "
const val FAILED_TO_CREATE_PROJECT = "A project wasn't found or created"
const val LOADING_PROJECT_STAGES = "Loading Project Stages"

val HYPERSKILL_LANGUAGES = mapOf("java" to "${EduNames.JAVA} 11", "kotlin" to EduNames.KOTLIN, "python" to EduNames.PYTHON,
                                 "javascript" to EduNames.JAVASCRIPT)

const val CONTINUE_ON_HYPERSKILL = "Continue on <a href=\"https://hyperskill.org/learning-path\">${EduNames.JBA}</a>."

const val SUCCESS_MESSAGE = "<html>${CheckUtils.CONGRATULATIONS} $CONTINUE_ON_HYPERSKILL</html>"

val HYPERSKILL_URL: String
  get() = PropertiesComponent.getInstance().getValue(HYPERSKILL_URL_PROPERTY, HYPERSKILL_DEFAULT_URL)

val AUTHORISATION_CODE_URL: String
  get() = "${HYPERSKILL_URL}oauth2/authorize/?" +
          "client_id=$CLIENT_ID&redirect_uri=${URLUtil.encodeURIComponent(REDIRECT_URI)}&grant_type=code&scope=read+write&response_type=code"

val REDIRECT_URI: String
  get() = if (EduUtils.isAndroidStudio()) {
    getCustomServer().handlingUri
  }
  else {
    REDIRECT_URI_DEFAULT
  }

private fun createCustomServer(): CustomAuthorizationServer {
  return CustomAuthorizationServer.create(HYPERSKILL, "/api/edu/hyperskill/oauth")
  { code, _ -> if (HyperskillConnector.getInstance().login(code)) null else "Failed to login to ${EduNames.JBA}" }
}

private fun getCustomServer(): CustomAuthorizationServer {
  val startedServer = CustomAuthorizationServer.getServerIfStarted(HYPERSKILL)
  return startedServer ?: createCustomServer()
}
