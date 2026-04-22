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
import com.jakubjirak.logparser.pro.io.GzipLogReader
import com.jakubjirak.logparser.pro.privacy.SensitiveDataRedactor
import java.io.File

/**
 * Writes a redacted copy of the current log file with PII/secrets masked.
 *
 * Uses [SensitiveDataRedactor] with the default preset rules. The source
 * file is never modified.
 */
class RedactSecretsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "No log file is open.", "LogParser Pro")
            return
        }
        val source = File(vFile.path)
        if (!source.isFile) return

        val descriptor = FileSaverDescriptor(
            "Save Redacted Log",
            "Choose destination for the redacted copy"
        )
        descriptor.withExtensionFilter("log")
        val saver = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val wrapper = saver.save(
            null as com.intellij.openapi.vfs.VirtualFile?,
            "${source.nameWithoutExtension}.redacted.log"
        ) ?: return
        val target = wrapper.file

        object : Task.Backgroundable(project, "Redacting secrets", true) {
            private var redactionCount: Int = 0
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val redactor = SensitiveDataRedactor()
                target.bufferedWriter().use { writer ->
                    GzipLogReader.open(source).useLines { lines ->
                        lines.forEach { line ->
                            val r = redactor.redact(line)
                            redactionCount += r.redactions.size
                            writer.write(r.text)
                            writer.newLine()
                        }
                    }
                }
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target)
            }

            override fun onSuccess() {
                Messages.showInfoMessage(
                    project,
                    "Redacted $redactionCount value(s) → ${target.absolutePath}",
                    "LogParser Pro — Redact Secrets"
                )
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(project, "Redaction failed: ${error.message}", "LogParser Pro")
            }
        }.queue()
    }
}
