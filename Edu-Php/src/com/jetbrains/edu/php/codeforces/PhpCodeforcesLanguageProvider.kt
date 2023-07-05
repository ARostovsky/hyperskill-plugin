package com.jetbrains.edu.php.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.php.PhpConfigurator
import com.jetbrains.edu.php.PhpProjectSettings
import javax.swing.Icon

class PhpCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<PhpProjectSettings> = PhpConfigurator()
  override val languageId: String = EduNames.PHP
  override val templateFileName: String = "codeforces.Php main.php"
  override val displayTemplateName: String = "main.php"
  override val languageIcon: Icon = EducationalCoreIcons.PhpLogo
}