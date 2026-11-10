/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

/**
 * Log Viewer tab in the tool window.
 *
 * Automatically tracks `.log` and `.out` files opened in the IDE editor
 * and displays them in a list for quick navigation.
 */
class LogViewerPanel(private val project: Project) {

    private val panel = JPanel(BorderLayout())
    private val logFilesModel = DefaultListModel<String>()
    private val supportedExtensions = setOf("log", "out")

    init {
        setupUI()
        scanOpenFiles()
        subscribeToEditorEvents()
    }

    private fun setupUI() {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.border = JBUI.Borders.empty(12)

        topPanel.add(JBLabel("<html><h2>LogParser Pro — Log Viewer</h2></html>"))
        topPanel.add(Box.createVerticalStrut(4))
        topPanel.add(JBLabel("Open log files in the editor — they appear here automatically."))
        topPanel.add(Box.createVerticalStrut(12))
        topPanel.add(JBLabel("Open log files:"))

        panel.add(topPanel, BorderLayout.NORTH)

        val fileList = JBList(logFilesModel)
        fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selected = fileList.selectedValue ?: return@addListSelectionListener
                val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(selected)
                if (file != null) {
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            }
        }
        panel.add(JBScrollPane(fileList), BorderLayout.CENTER)
    }

    private fun scanOpenFiles() {
        FileEditorManager.getInstance(project).openFiles
            .filter { it.extension in supportedExtensions }
            .forEach { addFile(it) }
    }

    private fun subscribeToEditorEvents() {
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    if (file.extension in supportedExtensions) {
                        addFile(file)
                    }
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (file.extension in supportedExtensions) {
                        removeFile(file)
                    }
                }
            }
        )
    }

    private fun addFile(file: VirtualFile) {
        val path = file.path
        if (!logFilesModel.contains(path)) {
            logFilesModel.addElement(path)
        }
    }

    private fun removeFile(file: VirtualFile) {
        logFilesModel.removeElement(file.path)
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
