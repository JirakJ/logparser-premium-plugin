/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.editor

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
import com.intellij.util.ui.JBUI
import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogFilter
import com.jakubjirak.logparser.pro.core.LogLevel
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.settings.LogParserSettings
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener
import java.io.File
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Custom file editor that displays parsed log entries in a filterable table.
 *
 * Provides colour-coded log levels (theme-aware), keyword search, level filtering,
 * stack trace folding, export, and a status bar with entry counts.
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
        table.columnModel.getColumn(0).preferredWidth = 60   // Line
        table.columnModel.getColumn(1).preferredWidth = 180  // Timestamp
        table.columnModel.getColumn(2).preferredWidth = 60   // Level
        table.columnModel.getColumn(3).preferredWidth = 120  // Thread
        table.columnModel.getColumn(4).preferredWidth = 180  // Logger
        table.columnModel.getColumn(5).preferredWidth = 600  // Message

        val settings = LogParserSettings.getInstance()
        table.font = table.font.deriveFont(settings.state.logViewerFontSize.toFloat())

        setupContextMenu()

        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        panel.add(createStatusBar(), BorderLayout.SOUTH)
    }

    private fun createToolbar(): JPanel {
        val toolbar = JPanel()
        toolbar.layout = BoxLayout(toolbar, BoxLayout.X_AXIS)
        toolbar.border = JBUI.Borders.empty(4, 8, 4, 8)

        toolbar.add(JBLabel("Search: "))
        val searchField = JBTextField(20)
        searchField.addActionListener {
            searchKeyword = searchField.text.trim()
            applyFilter()
        }
        toolbar.add(searchField)
        toolbar.add(Box.createHorizontalStrut(12))

        // Level toggle buttons
        toolbar.add(JBLabel("Levels: "))
        val levelTogglePanel = JPanel()
        levelTogglePanel.layout = BoxLayout(levelTogglePanel, BoxLayout.X_AXIS)
        for (level in listOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE)) {
            val toggle = JToggleButton(level.name)
            toggle.addActionListener {
                selectedLevels = if (toggle.isSelected) {
                    selectedLevels + level
                } else {
                    selectedLevels - level
                }
                applyFilter()
            }
            levelTogglePanel.add(toggle)
        }
        toolbar.add(levelTogglePanel)
        toolbar.add(Box.createHorizontalStrut(12))

        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { loadLogFile() }
        toolbar.add(refreshButton)

        toolbar.add(Box.createHorizontalStrut(8))
        val exportButton = JButton("Export")
        exportButton.addActionListener { exportFiltered() }
        toolbar.add(exportButton)

        return toolbar
    }

    private fun createStatusBar(): JPanel {
        val statusBar = JPanel(BorderLayout())
        statusBar.border = JBUI.Borders.empty(2, 8, 2, 8)
        statusBar.add(statusLabel, BorderLayout.WEST)
        return statusBar
    }

    private fun setupContextMenu() {
        val popup = JPopupMenu()

        val copyLine = JMenuItem("Copy Line")
        copyLine.addActionListener { copySelectedToClipboard(full = true) }
        popup.add(copyLine)

        val copyMessage = JMenuItem("Copy Message")
        copyMessage.addActionListener { copySelectedToClipboard(full = false) }
        popup.add(copyMessage)

        popup.addSeparator()

        val trackCorrelation = JMenuItem("Track Correlation ID")
        trackCorrelation.addActionListener { trackSelectedCorrelation() }
        popup.add(trackCorrelation)

        val goToLine = JMenuItem("Go to Line")
        goToLine.addActionListener { goToSelectedLine() }
        popup.add(goToLine)

        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { showPopup(e) }
            override fun mouseReleased(e: MouseEvent) { showPopup(e) }
            private fun showPopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val row = table.rowAtPoint(e.point)
                    if (row >= 0) table.setRowSelectionInterval(row, row)
                    popup.show(table, e.x, e.y)
                }
            }
        })
    }

    private fun copySelectedToClipboard(full: Boolean) {
        val row = table.selectedRow
        if (row < 0 || row >= filteredEntries.size) return
        val entry = filteredEntries[row]
        val text = if (full) entry.rawLine else entry.message
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    private fun trackSelectedCorrelation() {
        val row = table.selectedRow
        if (row < 0 || row >= filteredEntries.size) return
        val entry = filteredEntries[row]
        val corrId = entry.correlationId
        if (corrId == null) {
            JOptionPane.showMessageDialog(panel, "No correlation ID in this entry.", "LogParser Pro", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val matched = allEntries.filter { it.correlationId == corrId }
        JOptionPane.showMessageDialog(
            panel,
            "Found ${matched.size} entries for correlation ID: $corrId",
            "Correlation Tracking",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun goToSelectedLine() {
        val row = table.selectedRow
        if (row < 0 || row >= filteredEntries.size) return
        val lineNumber = filteredEntries[row].lineNumber
        val targetRow = filteredEntries.indexOfFirst { it.lineNumber == lineNumber }
        if (targetRow >= 0) {
            table.setRowSelectionInterval(targetRow, targetRow)
            table.scrollRectToVisible(table.getCellRect(targetRow, 0, true))
        }
    }

    private fun exportFiltered() {
        val settings = LogParserSettings.getInstance()
        val format = settings.state.exportFormat

        val chooser = JFileChooser()
        chooser.dialogTitle = "Export Filtered Entries"
        val ext = when (format) {
            "JSON" -> "json"
            "Plain Text" -> "txt"
            else -> "csv"
        }
        chooser.selectedFile = File("log-export.$ext")

        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) return

        val outputFile = chooser.selectedFile
        outputFile.bufferedWriter().use { writer ->
            when (format) {
                "JSON" -> exportJson(writer)
                "Plain Text" -> exportPlainText(writer)
                else -> exportCsv(writer)
            }
        }
    }

    private fun exportCsv(writer: java.io.BufferedWriter) {
        writer.write("Line,Timestamp,Level,Thread,Logger,Message")
        writer.newLine()
        for (entry in filteredEntries) {
            writer.write("${entry.lineNumber},${entry.timestamp ?: ""},${entry.level},${csvEscape(entry.threadName ?: "")},${csvEscape(entry.loggerName ?: "")},${csvEscape(entry.message)}")
            writer.newLine()
        }
    }

    private fun exportJson(writer: java.io.BufferedWriter) {
        writer.write("[\n")
        filteredEntries.forEachIndexed { index, entry ->
            writer.write("""  {"line":${entry.lineNumber},"timestamp":"${entry.timestamp ?: ""}","level":"${entry.level}","thread":"${jsonEscape(entry.threadName ?: "")}","logger":"${jsonEscape(entry.loggerName ?: "")}","message":"${jsonEscape(entry.message)}"}""")
            if (index < filteredEntries.size - 1) writer.write(",")
            writer.newLine()
        }
        writer.write("]\n")
    }

    private fun exportPlainText(writer: java.io.BufferedWriter) {
        for (entry in filteredEntries) {
            writer.write(entry.rawLine)
            writer.newLine()
        }
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun jsonEscape(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun loadLogFile() {
        val ioFile = File(file.path)
        if (!ioFile.exists()) return

        val service = LogParserService.getInstance(project)
        val parser = service.parseLogFile(ioFile)
        val settings = LogParserSettings.getInstance()
        allEntries = parser.parseLines(settings.state.maxLinesToParse)
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
        private val columns = arrayOf("Line", "Timestamp", "Level", "Thread", "Logger", "Message")

        override fun getRowCount(): Int = filteredEntries.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val entry = filteredEntries[rowIndex]
            return when (columnIndex) {
                0 -> entry.lineNumber
                1 -> entry.timestamp?.toString() ?: ""
                2 -> entry.level.name
                3 -> entry.threadName ?: ""
                4 -> entry.loggerName ?: ""
                5 -> entry.rawLine
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
                val settings = LogParserSettings.getInstance()
                component.foreground = if (settings.state.useThemeAwareColors) {
                    when (entry.level) {
                        LogLevel.ERROR, LogLevel.FATAL -> JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR
                        LogLevel.WARN -> JBUI.CurrentTheme.Banner.WARNING_BORDER_COLOR
                        LogLevel.DEBUG, LogLevel.TRACE -> JBUI.CurrentTheme.Banner.INFO_BORDER_COLOR
                        else -> table.foreground
                    }
                } else {
                    entry.level.color
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
