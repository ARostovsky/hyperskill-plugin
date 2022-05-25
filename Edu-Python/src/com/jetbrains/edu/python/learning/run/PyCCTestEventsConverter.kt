package com.jetbrains.edu.python.learning.run

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.events.TreeNodeEvent
import com.intellij.ide.IdeCoreBundle
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.checker.TestsOutputParser.TestMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor
import org.jetbrains.annotations.Nls

class PyCCTestEventsConverter(
  testFrameworkName: String,
  consoleProperties: TestConsoleProperties
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

  private val parser = TestsOutputParser()

  // `0` and `1` are the ids of root node and root suit node respectively
  // See `TreeNodeEvent.ROOT_NODE_ID` and `ROOT_SUIT_ID` constants
  private var nextId = 2
  private var isStarted = false

  override fun processServiceMessages(text: @Nls String, outputType: Key<*>, visitor: ServiceMessageVisitor): Boolean {
    val messages = mutableListOf<ServiceMessageBuilder>()
    val processor: (TestMessage) -> Unit = { message ->
      when (message) {
        is TestMessage.TextLine -> {
          messages += if (ProcessOutputType.isStdout(outputType)) {
            createTestStdOutMessage(message.text)
          } else {
            createTestStdErrMessage(message.text)
          }
        }
        is TestMessage.Ok -> {
          val nodeId = nextId++
          messages += createTestStartedMessage(nodeId, message.testName)
          messages += createTestFinishedMessage(nodeId, message.testName)
        }
        is TestMessage.Failed -> {
          val nodeId = nextId++
          messages += createTestStartedMessage(nodeId, message.testName)
          messages += createTestFailedMessage(nodeId, message)
          messages += createTestFinishedMessage(nodeId, message.testName)
        }
      }
    }

    if (!isStarted) {
      messages += createTestSuiteStartedMessage()
      isStarted = true
    }
    parser.processMessage(text, processor)
    if (getProcessFinishedMessage().toRegex() in text) {
      if (nextId == 2) {
        // `nextId == 2` means that converter didn't receive any test result
        val failedMessage = TestMessage.Failed(ROOT_SUITE_NAME, CheckResult.noTestsRun.message)
        messages += createTestFailedMessage(ROOT_SUIT_ID.toInt(), failedMessage)
      }
      messages += createTestSuiteFinishedMessage()
    }

    for (message in messages) {
      super.processServiceMessages(message.toString(), outputType, visitor)
    }
    return true
  }

  private fun createTestSuiteStartedMessage(): ServiceMessageBuilder =
    ServiceMessageBuilder.testSuiteStarted(ROOT_SUITE_NAME)
      .addAttribute(NODE_ID, ROOT_SUIT_ID)
      .addAttribute(PARENT_NODE_ID, TreeNodeEvent.ROOT_NODE_ID)

  private fun createTestSuiteFinishedMessage(): ServiceMessageBuilder =
    ServiceMessageBuilder.testSuiteFinished(ROOT_SUITE_NAME)
      .addAttribute(NODE_ID, ROOT_SUIT_ID)

  private fun createTestStartedMessage(nodeId: Int, name: String): ServiceMessageBuilder =
    ServiceMessageBuilder.testStarted(name)
      .addAttribute(NODE_ID, nodeId.toString())
      .addAttribute(PARENT_NODE_ID, ROOT_SUIT_ID)

  private fun createTestFinishedMessage(nodeId: Int, name: String): ServiceMessageBuilder =
    ServiceMessageBuilder.testFinished(name)
      .addAttribute(NODE_ID, nodeId.toString())

  private fun createTestFailedMessage(nodeId: Int, failed: TestMessage.Failed): ServiceMessageBuilder {
    val builder = ServiceMessageBuilder.testFailed(failed.testName)
      .addAttribute(NODE_ID, nodeId.toString())
      .addAttribute(MESSAGE, failed.message)
    if (failed.expected != null && failed.actual != null) {
      builder
        .addAttribute(ACTUAL, failed.actual)
        .addAttribute(EXPECTED, failed.expected)
    }
    return builder
  }

  private fun createTestStdOutMessage(text: String): ServiceMessageBuilder =
    ServiceMessageBuilder.testStdOut(ROOT_SUITE_NAME)
      .addAttribute(NODE_ID, ROOT_SUIT_ID)
      .addAttribute(OUT, text)

  private fun createTestStdErrMessage(text: String): ServiceMessageBuilder =
    ServiceMessageBuilder.testStdErr(ROOT_SUITE_NAME)
      .addAttribute(NODE_ID, ROOT_SUIT_ID)
      .addAttribute(OUT, text)

  companion object {
    private const val ROOT_SUIT_ID: String = "1"
    private const val ROOT_SUITE_NAME: String = "tests"

    private const val NODE_ID: String = "nodeId"
    private const val PARENT_NODE_ID: String = "parentNodeId"
    private const val OUT: String = "out"
    private const val MESSAGE: String = "message"
    private const val ACTUAL: String = "actual"
    private const val EXPECTED: String = "expected"

    @Nls
    private fun getProcessFinishedMessage(): String {
      return IdeCoreBundle.message("finished.with.exit.code.text.message", "\\d+")
    }
  }
}
