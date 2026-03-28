/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
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
 * Provides quick file open and a list of recently opened log files.
 */
class LogViewerPanel(private val project: Project) {

    private val panel = JPanel(BorderLayout())
    private val recentFiles = DefaultListModel<String>()

    init {
        setupUI()
    }

    private fun setupUI() {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.border = JBUI.Borders.empty(12)

        topPanel.add(JBLabel("<html><h2>LogParser Pro — Log Viewer</h2></html>"))
        topPanel.add(Box.createVerticalStrut(8))

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        val openButton = JButton("Open Log File")
        openButton.addActionListener { openLogFile() }
        buttonPanel.add(openButton)
        buttonPanel.add(Box.createHorizontalGlue())
        topPanel.add(buttonPanel)

        topPanel.add(Box.createVerticalStrut(12))
        topPanel.add(JBLabel("Recent files:"))

        panel.add(topPanel, BorderLayout.NORTH)

        val recentList = JBList(recentFiles)
        recentList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selected = recentList.selectedValue
                if (selected != null) {
                    val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(selected)
                    if (file != null) {
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }
                }
            }
        }
        panel.add(JBScrollPane(recentList), BorderLayout.CENTER)
    }

    private fun openLogFile() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "Select Log File"
            description = "Select a log file to analyse"
            withFileFilter { file: VirtualFile -> file.extension in setOf("log", "out") }
        }

        FileChooser.chooseFiles(descriptor, project, null) { files ->
            files.firstOrNull()?.let { file ->
                FileEditorManager.getInstance(project).openFile(file, true)
                val path = file.path
                if (!recentFiles.contains(path)) {
                    recentFiles.addElement(path)
                }
            }
        }
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
