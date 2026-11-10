/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.timeline.TimelineAnalyzer
import java.io.File
import java.time.Duration

/**
 * Renders a minute-granularity error timeline for the current log file
 * and flags spike buckets using z-score analysis.
 */
class ShowTimelineAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "No log file is open.", "LogParser Pro")
            return
        }
        val source = File(vFile.path)
        if (!source.isFile) return

        object : Task.Backgroundable(project, "Building timeline", true) {
            private var report: String = ""
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val parser = LogParserService.getInstance(project).parseLogFile(source)
                val entries = parser.parseLines()
                val timeline = TimelineAnalyzer().analyze(entries, Duration.ofMinutes(1))
                report = buildString {
                    appendLine("=== Log Timeline (1-minute buckets) ===")
                    appendLine("Entries: ${timeline.totalEntries} (skipped: ${timeline.skipped})")
                    appendLine("Buckets: ${timeline.buckets.size}  Spikes: ${timeline.spikes.size}")
                    appendLine()
                    val maxTotal = timeline.buckets.maxOfOrNull { it.totalCount } ?: 0
                    timeline.buckets.forEachIndexed { idx, b ->
                        val bar = if (maxTotal == 0) "" else
                            "#".repeat((b.totalCount * 40 / maxTotal).coerceAtLeast(if (b.totalCount > 0) 1 else 0))
                        val marker = if (idx in timeline.spikes) "*" else " "
                        appendLine("$marker ${b.start}  total=${b.totalCount}  err=${b.errorCount}  $bar")
                    }
                }
            }

            override fun onSuccess() {
                Messages.showInfoMessage(project, report, "LogParser Pro — Timeline")
            }
        }.queue()
    }
}
