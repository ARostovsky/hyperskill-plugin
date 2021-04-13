package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.MAPPER
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.REMOTE_MAPPER
import com.jetbrains.edu.learning.yaml.errorHandling.*
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [YamlLoader.loadItem].
 */
object YamlDeserializer {

  fun deserializeItem(configFile: VirtualFile, project: Project?, mapper: ObjectMapper = MAPPER): StudyItem? {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return try {
      when (configName) {
        COURSE_CONFIG -> mapper.deserializeCourse(configFileText)
        SECTION_CONFIG -> mapper.deserializeSection(configFileText)
        LESSON_CONFIG -> mapper.deserializeLesson(configFileText)
        TASK_CONFIG -> mapper.deserializeTask(configFileText)
        else -> loadingError(unknownConfigMessage(configFile.name))
      }
    }
    catch (e: Exception) {
      if (project != null) {
        processErrors(project, configFile, e)
      }
      return null
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(project: Project,
                                                                  contentList: MutableList<T>,
                                                                  mapper: ObjectMapper = MAPPER): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItem(configFile, project, mapper) as? T ?: continue
      deserializeItem.name = titledItem.name
      deserializeItem.index = titledItem.index
      content.add(deserializeItem)
    }

    return content
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.learning.yaml.format.CourseBuilder]
   */
  @VisibleForTesting
  fun ObjectMapper.deserializeCourse(configFileText: String): Course {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val courseMode = asText(treeNode.get("mode"))
    val course = treeToValue(treeNode, Course::class.java)
    course.courseMode = if (courseMode != null) EduNames.STUDY else CCUtils.COURSE_MODE
    return course
  }

  private fun ObjectMapper.readNode(configFileText: String): JsonNode =
    when (val tree = readTree(configFileText)) {
      null -> JsonNodeFactory.instance.objectNode()
      is MissingNode -> JsonNodeFactory.instance.objectNode()
      else -> tree
    }

  @VisibleForTesting
  fun ObjectMapper.deserializeSection(configFileText: String): Section {
    val jsonNode = readNode(configFileText)
    return treeToValue(jsonNode, Section::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeLesson(configFileText: String): Lesson {
    val treeNode = readNode(configFileText)
    val type = asText(treeNode.get("type"))
    val clazz = when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      CheckiOStation().itemType -> CheckiOStation::class.java // for student projects only
      Lesson().itemType, null -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.LESSON))
    }
    return treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String): Task {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = asText(treeNode.get("type")) ?: formatError("Task type not specified")

    val clazz = when (type) {
      "edu" -> EduTask::class.java
      "output" -> OutputTask::class.java
      TheoryTask.THEORY -> TheoryTask::class.java
      "video" -> VideoTask::class.java
      "choice" -> ChoiceTask::class.java
      IDE_TASK_TYPE -> IdeTask::class.java
      // for student mode
      CodeTask.CODE -> CodeTask::class.java
      "checkiO" -> CheckiOMission::class.java
      CODEFORCES_TASK_TYPE -> CodeforcesTask::class.java
      CODEFORCES_TASK_TYPE_WITH_FILE_IO -> CodeforcesTaskWithFileIO::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.TASK))
    }
    return treeToValue(treeNode, clazz)
  }

  fun deserializeRemoteItem(configFile: VirtualFile): StudyItem {
    val configName = configFile.name
    val configFileText = configFile.document.text
    return when (configName) {
      REMOTE_COURSE_CONFIG -> deserializeCourseRemoteInfo(configFileText)
      REMOTE_LESSON_CONFIG -> REMOTE_MAPPER.readValue(configFileText, Lesson::class.java)
      REMOTE_SECTION_CONFIG,
      REMOTE_TASK_CONFIG -> REMOTE_MAPPER.readValue(configFileText, RemoteStudyItem::class.java)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun deserializeCourseRemoteInfo(configFileText: String): Course {
    val treeNode = REMOTE_MAPPER.readTree(configFileText)
    val type = asText(treeNode.get("type"))

    val clazz = when {
      type == CodeforcesNames.CODEFORCES_COURSE_TYPE -> CodeforcesCourse::class.java
      treeNode.get(YamlMixinNames.HYPERSKILL_PROJECT) != null -> HyperskillCourse::class.java
      else -> EduCourse::class.java
    }

    return REMOTE_MAPPER.treeToValue(treeNode, clazz)
  }

  private fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }

  private val StudyItem.childrenConfigFileNames: Array<String>
    get() = when (this) {
      is Course -> arrayOf(SECTION_CONFIG, LESSON_CONFIG)
      is Section -> arrayOf(LESSON_CONFIG)
      is Lesson -> arrayOf(TASK_CONFIG)
      else -> error("Unexpected StudyItem: ${javaClass.simpleName}")
    }

  fun StudyItem.getConfigFileForChild(project: Project, childName: String): VirtualFile? {
    val dir = getDir(project.courseDir) ?: error(noDirForItemMessage(name))
    val itemDir = dir.findChild(childName)
    val configFile = childrenConfigFileNames.map { itemDir?.findChild(it) }.firstOrNull { it != null }
    if (configFile != null) {
      return configFile
    }

    val message = if (itemDir == null) "Directory for item '$childName' not found" else "Config file for item '${childName}' not found"
    val parentConfig = dir.findChild(configFileName) ?: error("Config file for currently loading item ${name} not found")
    showError(project, null, parentConfig, message)

    return null
  }

  private fun processErrors(project: Project, configFile: VirtualFile, e: Exception) {
    @Suppress("DEPRECATION")
    // suppress deprecation for MarkedYAMLException as it is actually thrown from com.fasterxml.jackson.dataformat.yaml.YAMLParser.nextToken
    when (e) {
      is MissingKotlinParameterException -> {
        val parameterName = e.parameter.name
        if (parameterName == null) {
          showError(project, e, configFile)
        }
        else {
          showError(project, e, configFile, "${NameUtil.nameToWordsLowerCase(parameterName).joinToString("_")} is empty")
        }
      }
      is InvalidYamlFormatException -> showError(project, e, configFile, e.message)
      is MismatchedInputException -> {
        showError(project, e, configFile)
      }
      is com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException -> {
        val message = yamlParsingErrorNotificationMessage(e.problem, e.contextMark?.line)
        if (message != null) {
          showError(project, e, configFile, message)
        }
        else {
          showError(project, e, configFile)
        }
      }
      is JsonMappingException -> {
        val causeException = e.cause
        if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
          showError(project, e, configFile)
        }
        else {
          showError(project, causeException, configFile, causeException.message)
        }
      }
      else -> throw e
    }
  }

  private fun yamlParsingErrorNotificationMessage(problem: String?, line: Int?) =
    if (problem != null && line != null) "$problem at line ${line + 1}" else null

  fun showError(project: Project,
                originalException: Exception?,
                configFile: VirtualFile,
                cause: String = "invalid config") {
    // to make test failures more comprehensible
    if (isUnitTestMode && project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION) == true) {
      if (originalException != null) {
        throw ProcessedException(cause, originalException)
      }
    }
    runInEdt {
      val editor = configFile.getEditor(project)
      if (editor != null) {
        editor.headerComponent = InvalidFormatPanel(project, cause)
      }
      else {
        val notification = InvalidConfigNotification(project, configFile, cause)
        notification.notify(project)
      }
    }
  }

  @VisibleForTesting
  class ProcessedException(message: String, originalException: Exception?) : Exception(message, originalException)
}

