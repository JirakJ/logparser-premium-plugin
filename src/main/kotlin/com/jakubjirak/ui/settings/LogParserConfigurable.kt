/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.jakubjirak.parsers.LogLevel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * Persistent settings for LogParser Pro, accessible via
 * **Settings → Tools → LogParser Pro**.
 */
@Service(Service.Level.APP)
@State(name = "com.jakubjirak.logparser.pro.Settings", storages = [Storage("com-jakubjirak-logparser-pro.xml")])
class LogParserSettings : PersistentStateComponent<LogParserSettings.State> {

    /** Serialisable settings state. */
    data class State(
        var maxLinesToParse: Int = 100_000,
        var defaultFilterLevel: String = "INFO",
        var showLineNumbers: Boolean = true,
        var autoRefresh: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(): LogParserSettings = service()
    }
}

/** Settings panel in the IDE preferences under **Tools → LogParser Pro**. */
class LogParserConfigurable : Configurable {

    private val settings get() = LogParserSettings.getInstance()

    private val maxLinesField = JBTextField(10)
    private val levelCombo = JComboBox(LogLevel.entries.map { it.name }.toTypedArray())
    private val showLineNumbersBox = JBCheckBox("Show line numbers")
    private val autoRefreshBox = JBCheckBox("Auto-refresh on file change")

    override fun getDisplayName(): String = "LogParser Pro"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        gbc.gridx = 0; gbc.gridy = 0
        panel.add(JBLabel("Max lines to parse:"), gbc)
        gbc.gridx = 1
        panel.add(maxLinesField, gbc)

        gbc.gridx = 0; gbc.gridy = 1
        panel.add(JBLabel("Default filter level:"), gbc)
        gbc.gridx = 1
        panel.add(levelCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2
        panel.add(showLineNumbersBox, gbc)

        gbc.gridy = 3
        panel.add(autoRefreshBox, gbc)

        gbc.gridy = 4; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL
        panel.add(JPanel(), gbc)

        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return maxLinesField.text != state.maxLinesToParse.toString() ||
            levelCombo.selectedItem != state.defaultFilterLevel ||
            showLineNumbersBox.isSelected != state.showLineNumbers ||
            autoRefreshBox.isSelected != state.autoRefresh
    }

    override fun apply() {
        val state = settings.state
        state.maxLinesToParse = maxLinesField.text.toIntOrNull() ?: 100_000
        state.defaultFilterLevel = levelCombo.selectedItem as? String ?: "INFO"
        state.showLineNumbers = showLineNumbersBox.isSelected
        state.autoRefresh = autoRefreshBox.isSelected
    }

    override fun reset() {
        val state = settings.state
        maxLinesField.text = state.maxLinesToParse.toString()
        levelCombo.selectedItem = state.defaultFilterLevel
        showLineNumbersBox.isSelected = state.showLineNumbers
        autoRefreshBox.isSelected = state.autoRefresh
    }
}
