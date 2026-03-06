/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Registers the [LogFileEditor] for `.log`, `.txt`, and `.out` files,
 * as well as any file whose name contains "log".
 *
 * The editor is placed after the default text editor so the user can
 * switch between the raw view and the structured LogParser view.
 */
class LogFileEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension in listOf("log", "txt", "out") ||
            file.name.contains("log", ignoreCase = true)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return LogFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "logparser-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
