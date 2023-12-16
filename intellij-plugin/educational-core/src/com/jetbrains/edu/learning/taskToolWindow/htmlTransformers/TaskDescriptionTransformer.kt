package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers

import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps.*

private val TaskDescriptionHtmlTransformer = HtmlTransformer.pipeline(
  CssHtmlTransformer,
  MediaThemesTransformer,
  ExternalLinkIconsTransformer,
  CodeHighlighter,
  HintsWrapper
)

val TaskDescriptionTransformer = StringHtmlTransformer.pipeline(
  TaskDescriptionHtmlTransformer.toStringTransformer(),
  ResourceWrapper
)