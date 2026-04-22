/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.diff.LogDiffEngine
import java.io.File

/**
 * Diffs the currently open log file against a user-chosen second log file
 * and shows a summary of new / removed signatures.
 */
class DiffLogsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val leftVFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "Open the baseline log first.", "LogParser Pro")
            return
        }
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select Log to Diff Against")
        val rightVFile = FileChooser.chooseFile(descriptor, project, null) ?: return
        val left = File(leftVFile.path)
        val right = File(rightVFile.path)
        if (!left.isFile || !right.isFile) return

        object : Task.Backgroundable(project, "Computing log diff", true) {
            private var report: String = ""
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val service = LogParserService.getInstance(project)
                val leftEntries = service.parseLogFile(left).parseLines()
                val rightEntries = service.parseLogFile(right).parseLines()
                val result = LogDiffEngine().diff(leftEntries, rightEntries)
                report = buildString {
                    appendLine("=== Log Diff ===")
                    appendLine("A: ${left.name}  B: ${right.name}")
                    appendLine()
                    appendLine("Added (in B only): ${result.added.size}")
                    result.added.take(10).forEach {
                        appendLine("  (x${it.count}) ${it.sample.level} ${it.sample.message.take(120)}")
                    }
                    appendLine()
                    appendLine("Removed (in A only): ${result.removed.size}")
                    result.removed.take(10).forEach {
                        appendLine("  (x${it.count}) ${it.sample.level} ${it.sample.message.take(120)}")
                    }
                    appendLine()
                    appendLine("Common signatures: ${result.common.size}")
                }
            }

            override fun onSuccess() {
                Messages.showInfoMessage(project, report, "LogParser Pro — Diff")
            }
        }.queue()
    }
}
