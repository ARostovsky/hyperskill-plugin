package org.hyperskill.academy.python.learning.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.ui.ConsoleView

private const val TEST_FRAMEWORK_NAME = "Edu Python Test"

class PyConsoleBuilder(
  private val config: RunConfiguration,
  private val executor: Executor
) : TextConsoleBuilder() {

  override fun addFilter(filter: Filter) {}
  override fun setViewer(isViewer: Boolean) {}

  override fun getConsole(): ConsoleView {
    val consoleProperties = PyConsoleProperties(config, executor)
    return SMTestRunnerConnectionUtil.createConsole(TEST_FRAMEWORK_NAME, consoleProperties)
  }
}

class PyConsoleProperties(
  config: RunConfiguration,
  executor: Executor
) : SMTRunnerConsoleProperties(config, TEST_FRAMEWORK_NAME, executor), SMCustomMessagesParsing {
  init {
    isIdBasedTestTree = true
  }

  override fun createTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties
  ): OutputToGeneralTestEventsConverter = PyTestEventsConverter(testFrameworkName, consoleProperties)
}

