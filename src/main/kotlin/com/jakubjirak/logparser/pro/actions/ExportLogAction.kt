/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.export.ExportFormat
import com.jakubjirak.logparser.pro.export.LogExporter
import java.io.File

/**
 * Exports the currently open log file to CSV / JSON / HTML.
 *
 * Runs parsing and exporting on a background task with progress indication.
 */
class ExportLogAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "No log file is open.", "LogParser Pro")
            return
        }
        val source = File(vFile.path)
        if (!source.isFile) return

        val format = askFormat(project) ?: return
        val target = chooseTargetFile(project, source, format) ?: return

        object : Task.Backgroundable(project, "Exporting log to ${format.name}", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Parsing ${source.name}…"
                val parser = LogParserService.getInstance(project).parseLogFile(source)
                val entries = parser.parseLines()
                indicator.text = "Writing ${target.name}…"
                val text = LogExporter().export(entries, format)
                target.writeText(text)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target)
            }

            override fun onSuccess() {
                Messages.showInfoMessage(project, "Exported to:\n${target.absolutePath}", "LogParser Pro")
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(
                    project,
                    "Export failed: ${error.message}",
                    "LogParser Pro"
                )
            }
        }.queue()
    }

    private fun askFormat(project: com.intellij.openapi.project.Project): ExportFormat? {
        val choices = arrayOf("CSV", "JSON", "HTML")
        val idx = Messages.showDialog(
            project,
            "Choose export format:",
            "LogParser Pro — Export",
            choices,
            0,
            Messages.getQuestionIcon()
        )
        return when (idx) {
            0 -> ExportFormat.CSV
            1 -> ExportFormat.JSON
            2 -> ExportFormat.HTML
            else -> null
        }
    }

    private fun chooseTargetFile(
        project: com.intellij.openapi.project.Project,
        source: File,
        format: ExportFormat
    ): File? {
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.HTML -> "html"
        }
        val descriptor = FileSaverDescriptor(
            "Export Log as ${format.name}",
            "Choose a destination file"
        )
        descriptor.withExtensionFilter(extension)
        val saver = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val suggested = "${source.nameWithoutExtension}.$extension"
        val wrapper = saver.save(null as com.intellij.openapi.vfs.VirtualFile?, suggested) ?: return null
        return wrapper.file
    }
}
