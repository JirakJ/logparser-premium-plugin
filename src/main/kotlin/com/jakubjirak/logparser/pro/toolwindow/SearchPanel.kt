/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.search.SearchEngine
import java.awt.BorderLayout
import java.io.File
import javax.swing.*

/**
 * Search tab in the tool window.
 *
 * Provides regex, fuzzy, and simple text search across the currently loaded log file.
 */
class SearchPanel(private val project: Project) {

    private val panel = JPanel(BorderLayout())
    private val searchField = JBTextField(30)
    private val resultsArea = JBTextArea(20, 60)
    private val regexCheckbox = JCheckBox("Regex")
    private val fuzzyCheckbox = JCheckBox("Fuzzy")
    private val caseSensitiveCheckbox = JCheckBox("Case Sensitive")

    init {
        setupUI()
    }

    private fun setupUI() {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.border = JBUI.Borders.empty(12)

        topPanel.add(JBLabel("<html><h3>Search</h3></html>"))
        topPanel.add(Box.createVerticalStrut(8))

        val searchBar = JPanel()
        searchBar.layout = BoxLayout(searchBar, BoxLayout.X_AXIS)
        searchBar.add(JBLabel("Pattern: "))
        searchBar.add(searchField)
        searchBar.add(Box.createHorizontalStrut(8))
        val searchButton = JButton("Search")
        searchButton.addActionListener { performSearch() }
        searchBar.add(searchButton)
        topPanel.add(searchBar)

        topPanel.add(Box.createVerticalStrut(4))
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.X_AXIS)
        optionsPanel.add(regexCheckbox)
        optionsPanel.add(Box.createHorizontalStrut(8))
        optionsPanel.add(fuzzyCheckbox)
        optionsPanel.add(Box.createHorizontalStrut(8))
        optionsPanel.add(caseSensitiveCheckbox)
        topPanel.add(optionsPanel)

        panel.add(topPanel, BorderLayout.NORTH)

        resultsArea.isEditable = false
        resultsArea.font = resultsArea.font.deriveFont(12f)
        panel.add(JBScrollPane(resultsArea), BorderLayout.CENTER)

        // Enter key triggers search
        searchField.addActionListener { performSearch() }
    }

    private fun performSearch() {
        val pattern = searchField.text.trim()
        if (pattern.isEmpty()) return

        val editor = FileEditorManager.getInstance(project)
        val currentFile = editor.selectedFiles.firstOrNull() ?: return
        val ioFile = File(currentFile.path)
        if (!ioFile.exists()) return

        val lines = ioFile.readLines()
        val engine = SearchEngine()

        val query = SearchEngine.SearchQuery(
            pattern = pattern,
            isRegex = regexCheckbox.isSelected,
            isCaseSensitive = caseSensitiveCheckbox.isSelected,
            isFuzzy = fuzzyCheckbox.isSelected
        )

        val results = engine.search(lines, query)

        resultsArea.text = if (results.isEmpty()) {
            "No matches found for: $pattern"
        } else {
            buildString {
                appendLine("Found ${results.size} matches:")
                appendLine()
                results.take(200).forEach { r ->
                    appendLine("Line ${r.lineNumber + 1}: ${r.content}")
                }
                if (results.size > 200) {
                    appendLine("... and ${results.size - 200} more matches")
                }
            }
        }
        resultsArea.caretPosition = 0
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
