/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Registers the [LogFileEditor] for `.log` and `.out` files.
 *
 * Intentionally does **not** accept `.txt` files to avoid interfering
 * with the default text editor for regular text files.
 *
 * The editor is placed after the default text editor so the user can
 * switch between the raw view and the structured LogParser view.
 */
class LogFileEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension in SUPPORTED_EXTENSIONS
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return LogFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "logparser-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("log", "out")
    }
}
