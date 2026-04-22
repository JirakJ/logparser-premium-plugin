/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
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
import com.jakubjirak.logparser.pro.grouping.ExceptionGrouper
import java.io.File

/**
 * Groups stack traces in the current log file into unique exception signatures
 * and shows a ranked report.
 */
class GroupExceptionsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "No log file is open.", "LogParser Pro")
            return
        }
        val source = File(vFile.path)
        if (!source.isFile) return

        object : Task.Backgroundable(project, "Grouping exceptions", true) {
            private var report: String = ""
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val parser = LogParserService.getInstance(project).parseLogFile(source)
                val entries = parser.parseLines()
                val groups = ExceptionGrouper().group(entries)
                report = buildString {
                    appendLine("=== Exception Groups (${groups.size}) ===")
                    appendLine("Total traces: ${groups.sumOf { it.count }}")
                    appendLine()
                    groups.take(25).forEachIndexed { idx, g ->
                        appendLine("#${idx + 1}  (x${g.count})  ${g.exceptionClass ?: "unknown"}")
                        if (!g.rootCause.isNullOrEmpty()) appendLine("    caused by: ${g.rootCause}")
                        if (g.firstSeen != null) appendLine("    first: ${g.firstSeen}  last: ${g.lastSeen}")
                        appendLine("    sample lines: ${g.sampleLineNumbers.joinToString(", ")}")
                        appendLine()
                    }
                    if (groups.size > 25) appendLine("… ${groups.size - 25} more groups omitted")
                }
            }

            override fun onSuccess() {
                Messages.showInfoMessage(project, report, "LogParser Pro — Exception Groups")
            }
        }.queue()
    }
}
