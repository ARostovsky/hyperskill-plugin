package org.hyperskill.academy.learning.json.migration

import org.hyperskill.academy.learning.json.migration.MigrationNames.JAVA
import org.hyperskill.academy.learning.json.migration.MigrationNames.KOTLIN
import org.hyperskill.academy.learning.json.migration.MigrationNames.SCALA

val LANGUAGE_TASK_ROOTS: Map<String, TaskRoots> = mapOf(
  KOTLIN to TaskRoots("src", "test"),
  JAVA to TaskRoots("src", "test"),
  SCALA to TaskRoots("src", "test"),
  // For test purposes
  "FakeGradleBasedLanguage" to TaskRoots("src", "test")
)

data class TaskRoots(val taskFilesRoot: String, val testFilesRoot: String)
