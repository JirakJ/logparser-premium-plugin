/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Settings panel in the IDE preferences under **Tools → LogParser Pro**.
 *
 * Provides controls for all configurable plugin settings including display,
 * search, and export preferences.
 */
class LogParserConfigurable : Configurable {

    private val settings get() = LogParserSettings.getInstance()

    private val maxLinesField = JBTextField(10)
    private val levelCombo = JComboBox(arrayOf("ALL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"))
    private val showLineNumbersBox = JBCheckBox("Show line numbers")
    private val autoRefreshBox = JBCheckBox("Auto-refresh on file change")
    private val dateFormatField = JBTextField(20)
    private val correlationPatternsArea = JBTextArea(3, 30)
    private val themeAwareBox = JBCheckBox("Use theme-aware colors")
    private val wrapLinesBox = JBCheckBox("Wrap long lines in editor")
    private val fontSizeSpinner = JSpinner(SpinnerNumberModel(12, 8, 48, 1))
    private val exportFormatCombo = JComboBox(arrayOf("CSV", "JSON", "Plain Text"))

    override fun getDisplayName(): String = "LogParser Pro"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }

        var row = 0

        // Max lines
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JBLabel("Max lines to parse:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(maxLinesField, gbc)

        // Default filter level
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JBLabel("Default filter level:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(levelCombo, gbc)

        // Date format pattern
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JBLabel("Date format pattern:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(dateFormatField, gbc)

        // Font size
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JBLabel("Font size for log viewer:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(fontSizeSpinner, gbc)

        // Export format
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0
        panel.add(JBLabel("Export format preference:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(exportFormatCombo, gbc)

        // Show line numbers
        row++
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0
        panel.add(showLineNumbersBox, gbc)

        // Auto-refresh
        row++
        gbc.gridy = row
        panel.add(autoRefreshBox, gbc)

        // Theme-aware colors
        row++
        gbc.gridy = row
        panel.add(themeAwareBox, gbc)

        // Wrap long lines
        row++
        gbc.gridy = row
        panel.add(wrapLinesBox, gbc)

        // Custom correlation ID patterns
        row++
        gbc.gridy = row; gbc.gridwidth = 2
        panel.add(JBLabel("Custom correlation ID patterns (comma-separated regexes):"), gbc)

        row++
        gbc.gridy = row; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.3
        correlationPatternsArea.lineWrap = true
        correlationPatternsArea.wrapStyleWord = true
        panel.add(JScrollPane(correlationPatternsArea), gbc)

        // Spacer
        row++
        gbc.gridy = row; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL
        panel.add(JPanel(), gbc)

        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return maxLinesField.text != state.maxLinesToParse.toString() ||
            levelCombo.selectedItem != state.defaultFilterLevel ||
            showLineNumbersBox.isSelected != state.showLineNumbers ||
            autoRefreshBox.isSelected != state.autoRefresh ||
            dateFormatField.text != state.dateFormatPattern ||
            correlationPatternsArea.text != state.customCorrelationPatterns ||
            themeAwareBox.isSelected != state.useThemeAwareColors ||
            wrapLinesBox.isSelected != state.wrapLongLines ||
            (fontSizeSpinner.value as Int) != state.logViewerFontSize ||
            exportFormatCombo.selectedItem != state.exportFormat
    }

    override fun apply() {
        val state = settings.state
        state.maxLinesToParse = maxLinesField.text.toIntOrNull() ?: 100_000
        state.defaultFilterLevel = levelCombo.selectedItem as? String ?: "ALL"
        state.showLineNumbers = showLineNumbersBox.isSelected
        state.autoRefresh = autoRefreshBox.isSelected
        state.dateFormatPattern = dateFormatField.text
        state.customCorrelationPatterns = correlationPatternsArea.text
        state.useThemeAwareColors = themeAwareBox.isSelected
        state.wrapLongLines = wrapLinesBox.isSelected
        state.logViewerFontSize = fontSizeSpinner.value as Int
        state.exportFormat = exportFormatCombo.selectedItem as? String ?: "CSV"
    }

    override fun reset() {
        val state = settings.state
        maxLinesField.text = state.maxLinesToParse.toString()
        levelCombo.selectedItem = state.defaultFilterLevel
        showLineNumbersBox.isSelected = state.showLineNumbers
        autoRefreshBox.isSelected = state.autoRefresh
        dateFormatField.text = state.dateFormatPattern
        correlationPatternsArea.text = state.customCorrelationPatterns
        themeAwareBox.isSelected = state.useThemeAwareColors
        wrapLinesBox.isSelected = state.wrapLongLines
        fontSizeSpinner.value = state.logViewerFontSize
        exportFormatCombo.selectedItem = state.exportFormat
    }
}
