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
import com.intellij.openapi.vfs.VirtualFile

/**
 * Opens a file chooser dialog to select a log file and opens it in the editor.
 *
 * Filters to `.log` and `.out` files by default.
 */
class OpenLogFileAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "Select Log File"
            description = "Select a log file to analyse"
            withFileFilter { file: VirtualFile ->
                file.extension in setOf("log", "out")
            }
        }

        FileChooser.chooseFiles(descriptor, project, null) { files ->
            files.firstOrNull()?.let { file ->
                FileEditorManager.getInstance(project).openFile(file, true)
            }
        }
    }
}
