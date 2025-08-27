package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps

import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.jsoup.nodes.Document

//TODO: remove when all Marketplace courses cut headers
object CutOutHeaderTransformer : HtmlTransformer {
  private val HEADER_TAG_NAMES = listOf("h1", "h2", "h3")

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    // Find the first header element that appears once and remove it
    HEADER_TAG_NAMES.asSequence()
      .map { html.getElementsByTag(it) }
      .firstOrNull { it.size == 1 && it.prev().isEmpty() }
      ?.first() // Get the single element
      ?.remove()

    return html
  }
}