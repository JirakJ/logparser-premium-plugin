/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Settings panel in the IDE preferences under **Tools → LogParser Pro**.
 *
 * Exposes the options consumed by the log editor: line limit, level colours,
 * font size and the export format used by the editor's Export button.
 */
class LogParserConfigurable : Configurable {

    private val settings get() = LogParserSettings.getInstance()

    private val maxLinesField = JBTextField(10)
    private val themeAwareBox = JBCheckBox("Use theme-aware colors")
    private val fontSizeSpinner = JSpinner(SpinnerNumberModel(LogParserSettings.DEFAULT_FONT_SIZE, 8, 48, 1))
    private val exportFormatCombo = JComboBox(arrayOf("CSV", "JSON", "Plain Text"))

    override fun getDisplayName(): String = "LogParser Pro"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }
        addRow(panel, gbc, 0, "Max lines to parse:", maxLinesField)
        addRow(panel, gbc, 1, "Font size for log viewer:", fontSizeSpinner)
        addRow(panel, gbc, 2, "Editor export format:", exportFormatCombo)

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0
        panel.add(themeAwareBox, gbc)

        gbc.gridy = 4; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL
        panel.add(JPanel(), gbc)

        reset()
        return panel
    }

    private fun addRow(panel: JPanel, gbc: GridBagConstraints, row: Int, label: String, field: JComponent) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JBLabel(label), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(field, gbc)
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return parseMaxLines(maxLinesField.text) != state.maxLinesToParse ||
            themeAwareBox.isSelected != state.useThemeAwareColors ||
            (fontSizeSpinner.value as Int) != state.logViewerFontSize ||
            exportFormatCombo.selectedItem != state.exportFormat
    }

    override fun apply() {
        val state = settings.state
        state.maxLinesToParse = parseMaxLines(maxLinesField.text)
        state.useThemeAwareColors = themeAwareBox.isSelected
        state.logViewerFontSize = fontSizeSpinner.value as Int
        state.exportFormat = exportFormatCombo.selectedItem as? String ?: "CSV"
    }

    override fun reset() {
        val state = settings.state
        maxLinesField.text = state.maxLinesToParse.toString()
        themeAwareBox.isSelected = state.useThemeAwareColors
        fontSizeSpinner.value = state.logViewerFontSize
        exportFormatCombo.selectedItem = state.exportFormat
    }

    companion object {
        /** Parses the "max lines" field; blank, non-numeric or non-positive input falls back to the default. */
        fun parseMaxLines(text: String): Int =
            text.trim().toIntOrNull()?.takeIf { it > 0 } ?: LogParserSettings.DEFAULT_MAX_LINES
    }
}
