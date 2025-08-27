package org.hyperskill.academy.rust.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.CodeExecutor.Companion.resultUnchecked
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.ext.findSourceDir
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.rust.messages.EduRustBundle
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.tools.cargo
import org.rust.lang.RsConstants.MAIN_RS_FILE
import org.rust.lang.core.psi.ext.containingCargoTarget
import org.rust.lang.core.psi.rustFile
import org.rust.openapiext.isSuccess

class RsCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    val taskDir = task.getDir(project.courseDir) ?: return resultUnchecked(EduRustBundle.message("error.no.task.dir"))
    val mainVFile = task.findSourceDir(taskDir)?.findChild(MAIN_RS_FILE) ?: return resultUnchecked(
      EduRustBundle.message("error.failed.find.0", MAIN_RS_FILE)
    )
    val target = runReadAction { PsiManager.getInstance(project).findFile(mainVFile)?.rustFile?.containingCargoTarget }
                 ?: return resultUnchecked(EduRustBundle.message("error.failed.find.target.for.0", MAIN_RS_FILE))
    val cargo = project.rustSettings.toolchain?.cargo() ?: return resultUnchecked(EduRustBundle.message("error.no.toolchain"))
    // `--color never` is needed to avoid unexpected color escape codes in output
    val cmd = CargoCommandLine.forTarget(target, "run", listOf("--color", "never")).copy(emulateTerminal = false)

    val processOutput = cargo.toGeneralCommandLine(project, cmd).executeCargoCommandLine(input)
    val output = processOutput.stdout

    return if (processOutput.isSuccess) {
      Ok(output.prepareToCheck())
    }
    else {
      Err(RsStderrAnalyzer.tryToGetCheckResult(processOutput.stdout) ?: CheckResult.failedToCheck)
    }
  }

  private fun String.prepareToCheck(): String {
    var programOutputStarted = false
    val outputBuffer = StringBuilder()

    for (line in lineSequence()) {
      if (programOutputStarted) {
        outputBuffer.appendLine(line)
      }
      else {
        if (line.trimStart().startsWith("Running")) {
          programOutputStarted = true
        }
      }
    }

    return outputBuffer.toString().removeSuffix("\n")
  }
}
