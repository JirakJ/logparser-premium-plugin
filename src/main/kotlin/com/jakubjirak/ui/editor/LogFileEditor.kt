/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.jakubjirak.core.LogParserService
import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogFilter
import com.jakubjirak.parsers.LogLevel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Custom file editor that displays parsed log entries in a filterable table.
 *
 * Provides colour-coded log levels, keyword search, level filtering,
 * and a status bar showing entry counts and file size.
 */
@Suppress("TooManyFunctions")
class LogFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val userDataHolder = UserDataHolderBase()
    private val panel = JPanel(BorderLayout())

    private var allEntries: List<LogEntry> = emptyList()
    private var filteredEntries: List<LogEntry> = emptyList()
    private val tableModel = LogTableModel()
    private val table = JBTable(tableModel)
    private val statusLabel = JBLabel()

    private var selectedLevels: Set<LogLevel> = emptySet()
    private var searchKeyword: String = ""

    init {
        setupUI()
        loadLogFile()
    }

    private fun setupUI() {
        panel.add(createToolbar(), BorderLayout.NORTH)

        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        table.setDefaultRenderer(Any::class.java, LogLevelCellRenderer())
        table.columnModel.getColumn(0).preferredWidth = 60
        table.columnModel.getColumn(1).preferredWidth = 180
        table.columnModel.getColumn(2).preferredWidth = 60
        table.columnModel.getColumn(3).preferredWidth = 600

        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        panel.add(createStatusBar(), BorderLayout.SOUTH)
    }

    private fun createToolbar(): JPanel {
        val toolbar = JPanel()
        toolbar.layout = BoxLayout(toolbar, BoxLayout.X_AXIS)
        toolbar.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)

        toolbar.add(JBLabel("Search: "))
        val searchField = JBTextField(20)
        searchField.addActionListener {
            searchKeyword = searchField.text.trim()
            applyFilter()
        }
        toolbar.add(searchField)
        toolbar.add(Box.createHorizontalStrut(12))

        toolbar.add(JBLabel("Level: "))
        val levelNames = arrayOf("ALL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE")
        val levelCombo = JComboBox(levelNames)
        levelCombo.addActionListener {
            val selected = levelCombo.selectedItem as String
            selectedLevels = if (selected == "ALL") {
                emptySet()
            } else {
                setOf(LogLevel.valueOf(selected))
            }
            applyFilter()
        }
        toolbar.add(levelCombo)
        toolbar.add(Box.createHorizontalStrut(12))

        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { loadLogFile() }
        toolbar.add(refreshButton)

        return toolbar
    }

    private fun createStatusBar(): JPanel {
        val statusBar = JPanel(BorderLayout())
        statusBar.border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
        statusBar.add(statusLabel, BorderLayout.WEST)
        return statusBar
    }

    private fun loadLogFile() {
        val ioFile = java.io.File(file.path)
        if (!ioFile.exists()) return

        val service = LogParserService.getInstance(project)
        val parser = service.parseLogFile(ioFile)
        allEntries = parser.parseLines()
        applyFilter()
    }

    private fun applyFilter() {
        val filter = LogFilter(
            levels = selectedLevels,
            keywords = if (searchKeyword.isEmpty()) emptyList() else listOf(searchKeyword)
        )
        filteredEntries = allEntries.filter { it.matches(filter) }
        tableModel.fireTableDataChanged()
        updateStatusBar()
    }

    private fun updateStatusBar() {
        val errors = allEntries.count { it.level == LogLevel.ERROR }
        val warnings = allEntries.count { it.level == LogLevel.WARN }
        val sizeKb = file.length / 1024
        statusLabel.text = "Showing ${filteredEntries.size} of ${allEntries.size} entries | " +
            "Errors: $errors | Warnings: $warnings | File size: ${sizeKb} KB"
    }

    private inner class LogTableModel : AbstractTableModel() {
        private val columns = arrayOf("Line", "Timestamp", "Level", "Message")

        override fun getRowCount(): Int = filteredEntries.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val entry = filteredEntries[rowIndex]
            return when (columnIndex) {
                0 -> entry.lineNumber
                1 -> entry.timestamp?.toString() ?: ""
                2 -> entry.level.name
                3 -> entry.rawLine
                else -> ""
            }
        }
    }

    private inner class LogLevelCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (!isSelected && row < filteredEntries.size) {
                val entry = filteredEntries[row]
                component.foreground = when (entry.level) {
                    LogLevel.ERROR, LogLevel.FATAL -> Color(192, 57, 43)
                    LogLevel.WARN -> Color(180, 120, 0)
                    LogLevel.DEBUG, LogLevel.TRACE -> Color(127, 140, 141)
                    else -> table.foreground
                }
            }
            return component
        }
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = table
    override fun getName(): String = "LogParser"
    @Suppress("EmptyFunctionBlock")
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    @Suppress("EmptyFunctionBlock")
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    @Suppress("EmptyFunctionBlock")
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? = null
    @Suppress("EmptyFunctionBlock")
    override fun dispose() {}
    override fun getFile(): VirtualFile = file
    override fun <T : Any?> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) { userDataHolder.putUserData(key, value) }
}
