package com.jetbrains.edu.learning.submissions

import com.intellij.ui.dsl.builder.SegmentedButton
import javax.swing.JButton

fun segmentedButtonRenderer(item: JButton): String = item.text

@Suppress("UnstableApiUsage", "UnusedReceiverParameter")
fun SegmentedButton<JButton>.enableCommunityButton() {}

@Suppress("UnstableApiUsage", "UnusedReceiverParameter")
fun SegmentedButton<JButton>.disableCommunityButton() {}