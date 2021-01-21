package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.cargo.toolchain.RustToolchain

class RsCheckerFixture : EduCheckerFixture<RsProjectSettings>() {

  private var toolchain: RustToolchain? = null

  override val projectSettings: RsProjectSettings get() = RsProjectSettings(toolchain)

  override fun getSkipTestReason(): String? = if (toolchain == null) "no Rust toolchain found" else super.getSkipTestReason()

  override fun setUp() {
    super.setUp()
    toolchain = RustToolchain.suggest()
  }
}
