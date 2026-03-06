/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.*

/**
 * Factory that creates the LogParser Pro tool window content (bottom panel).
 */
class LogParserToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = LogParserToolWindowContent(project)
        val content = ContentFactory.getInstance()
            .createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Main tool window panel with quick-access buttons and feature overview.
 */
class LogParserToolWindowContent(private val project: Project) {

    private val panel = JPanel(BorderLayout())

    init {
        setupUI()
    }

    private fun setupUI() {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

        mainPanel.add(JBLabel("<html><h2>LogParser Pro</h2></html>"))
        mainPanel.add(Box.createVerticalStrut(12))
        mainPanel.add(createButtonPanel())
        mainPanel.add(Box.createVerticalStrut(16))
        mainPanel.add(createFeatureList())

        panel.add(mainPanel, BorderLayout.NORTH)
    }

    private fun createButtonPanel(): JPanel {
        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)

        val openFileButton = JButton("Open Log File")
        openFileButton.addActionListener { openLogFile() }
        buttonPanel.add(openFileButton)

        return buttonPanel
    }

    private fun createFeatureList(): JBLabel {
        val features = """
            <h3>Features</h3>
            <ul>
              <li>Log viewing &amp; syntax highlighting</li>
              <li>Log level filtering</li>
              <li>Keyword search</li>
              <li>Stack trace folding</li>
              <li>Correlation ID tracking</li>
              <li>AI-powered anomaly detection</li>
              <li>Advanced regex &amp; fuzzy search</li>
              <li>High-performance log indexing</li>
            </ul>
        """.trimIndent()

        return JBLabel("<html>$features</html>")
    }

    private fun openLogFile() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = "Select Log File"
            description = "Select a log file to analyse"
            withFileFilter { file: VirtualFile ->
                file.extension in listOf("log", "txt", "out") || file.name.contains("log")
            }
        }

        FileChooserFactory.getInstance()
            .createFileChooser(descriptor, project, null)
            .choose(null, project).firstOrNull()?.let { file ->
                FileEditorManager.getInstance(project).openFile(file, true)
            }
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
