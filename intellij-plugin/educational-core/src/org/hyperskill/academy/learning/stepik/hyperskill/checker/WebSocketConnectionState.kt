package org.hyperskill.academy.learning.stepik.hyperskill.checker

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.project.Project
import okhttp3.WebSocket
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.api.SubmissionsList
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillCheckConnector.EVALUATION_STATUS
import org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillCheckConnector.toCheckResult
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.submissions.SubmissionsManager

/**
 * Communication protocol with Hyperskill WS is the following:
 *
 * 1. Retrieve token needed to authorize via API
 * 2. Send token to server when connection opens: {"connect":{"token":"<actual token>"},"id":1}
 * 3. Receive OK message: {"id":1,"result":{"client":"<clientId>","version":"2.3.1","expires":true,"ttl":899}}
 * 4. Send a message to subscribe to submission events: {"method":1,"subscribe":{"channel":"submission#<userId>-0"},"id":2}
 * 5. Receive OK message: {"id":2,"result":{}}
 * 6. Start receiving messages with submission events: {"push":{"channel":"submission#6242591-0","pub":{"data": <submissionsData>}}}
 *
 * Useful links for troubleshooting:
 * @see <a href="https://centrifugal.dev/docs/getting-started/introduction">Centrifugo docs</a>
 * @see <a href="https://github.com/centrifugal/protocol/blob/master/definitions/client.proto">Centrifugo client protocol github</a>
 * @see <a href="https://github.com/centrifugal/centrifuge-java">Centrifugo Java client github</a>
 *
 */
abstract class WebSocketConnectionState(protected val project: Project, protected val task: CodeTask, val isTerminal: Boolean = false) {
  abstract fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState

  open fun getResult(): Result<CheckResult, SubmissionError> {
    return Err(SubmissionError.NoSubmission("Submission result check via web sockets failed"))
  }
}

class InitialState(project: Project, task: CodeTask, private val token: String) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    webSocket.send(OpenMessage(token))
    return WaitingForConnectionState(project, task)
  }
}

private class WaitingForConnectionState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    webSocket.send(SubscribeToSubmissionsMessage(HyperskillSettings.INSTANCE.account!!.userInfo.id))
    return WaitingForSubscriptionState(project, task)
  }
}


private class WaitingForSubscriptionState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return when (val result: Result<StepikBasedSubmission, String> = HyperskillSubmitConnector.submitCodeTask(project, task)) {
      is Ok -> ReceivingSubmissionsState(project, task, result.value)
      is Err -> ErrorState(project, task)
    }
  }
}

private class ReceivingSubmissionsState(project: Project, task: CodeTask, val submission: StepikBasedSubmission) : WebSocketConnectionState(
  project,
  task
) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    val objectMapper = HyperskillConnector.getInstance().objectMapper
    val data = objectMapper.readTree(message).get("push").get("pub").get("data") ?: return this
    for (receivedSubmission in objectMapper.treeToValue(data, SubmissionsList::class.java).submissions) {
      if (receivedSubmission.status == EVALUATION_STATUS) continue
      if (submission.id == receivedSubmission.id) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, receivedSubmission)
        return SubmissionReceivedState(project, task, receivedSubmission)
      }
    }
    return this
  }

  override fun getResult(): Result<CheckResult, SubmissionError> {
    return Err(SubmissionError.WithSubmission(submission, "No check result received"))
  }
}

private class SubmissionReceivedState(project: Project, task: CodeTask, private val submission: StepikBasedSubmission) :
  WebSocketConnectionState(
    project, task, true
  ) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return this
  }

  override fun getResult(): Result<CheckResult, SubmissionError> {
    return Ok(submission.toCheckResult())
  }
}

private class ErrorState(project: Project, task: CodeTask) : WebSocketConnectionState(project, task, true) {
  override fun handleEvent(webSocket: WebSocket, message: String): WebSocketConnectionState {
    return this
  }
}

private fun WebSocket.send(message: WebSocketMessage) {
  send(HyperskillConnector.getInstance().objectMapper.writeValueAsString(message))
}

@Suppress("unused")
private open class WebSocketMessage(@field:JsonProperty("id") val id: Int)

@Suppress("unused")
private class OpenMessage(token: String) : WebSocketMessage(1) {
  @JsonProperty("connect")
  val connect = mapOf("token" to token)
}

@Suppress("unused")
private class SubscribeToSubmissionsMessage(userId: Int) : WebSocketMessage(2) {
  @JsonProperty("method")
  val method = 1

  @JsonProperty("subscribe")
  val subscribe = mapOf("channel" to "submission#$userId-0")
}