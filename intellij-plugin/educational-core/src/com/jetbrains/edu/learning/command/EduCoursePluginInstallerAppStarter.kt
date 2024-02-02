package com.jetbrains.edu.learning.command

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.InstallAndEnableTaskHeadlessImpl
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import org.apache.commons.cli.CommandLine

/**
 * Adds `installCoursePlugins` command for IDE to install all necessary plugins for given course
 *
 * Expected usages:
 * - `installCoursePlugins [%/path/to/project/dir%] --marketplace %marketplace-course-link%`
 * - `installCoursePlugins [%/path/to/project/dir%] --archive %/path/to/course/archive%`
 *
 * Note, path to project directory is optional for the command itself.
 * It's not prohibited since in case of remote server script,
 * path to project directory is required by the script itself as first positional argument.
 * But the command doesn't use it
 */
class EduCoursePluginInstallerAppStarter : EduAppStarterBase<Args>() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "installCoursePlugins"

  override fun createArgs(cmd: CommandLine): Result<Args, String> = Ok(Args(cmd))

  override suspend fun doMain(course: Course, args: Args): CommandResult {
    val provider = course.compatibilityProvider
    if (provider == null) {
      return CommandResult.Error(course.incompatibleCourseMessage())
    }

    val pluginManager = PluginManager.getInstance()
    val allRequiredPlugins = provider.requiredPlugins().orEmpty() + course.pluginDependencies
    val pluginIds = allRequiredPlugins
      .map { PluginId.getId(it.stringId) }
      .filterTo(hashSetOf()) { pluginManager.findEnabledPlugin(it) == null }

    LOG.info("Installing: $pluginIds")

    var result: CommandResult = CommandResult.Ok
    @Suppress("UnstableApiUsage")
    ProgressManager.getInstance().run(object : InstallAndEnableTaskHeadlessImpl(pluginIds, {}) {
      override fun onThrowable(error: Throwable) {
        result = CommandResult.Error("Failed to install plugins for `${course.name}` course", error)
      }
    })
    return result
  }
}
