/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.jakubjirak.logparser.pro.analysis.LogStatistics
import com.jakubjirak.logparser.pro.core.LogParserService
import java.awt.BorderLayout
import java.io.File
import javax.swing.*

/**
 * Statistics tab in the tool window.
 *
 * Displays aggregate statistics for the currently loaded log file including
 * level distribution (text-based bar chart), top loggers, entries over time,
 * and error rate.
 */
class StatisticsPanel(private val project: Project) {

    private val panel = JPanel(BorderLayout())
    private val statsArea = JBTextArea(20, 60)

    init {
        setupUI()
    }

    private fun setupUI() {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.border = JBUI.Borders.empty(12)

        topPanel.add(JBLabel("<html><h3>Statistics</h3></html>"))
        topPanel.add(Box.createVerticalStrut(8))

        val refreshButton = JButton("Refresh Statistics")
        refreshButton.addActionListener { computeStatistics() }
        topPanel.add(refreshButton)

        panel.add(topPanel, BorderLayout.NORTH)

        statsArea.isEditable = false
        statsArea.font = statsArea.font.deriveFont(12f)
        panel.add(JBScrollPane(statsArea), BorderLayout.CENTER)
    }

    @Suppress("MagicNumber")
    private fun computeStatistics() {
        val editorManager = FileEditorManager.getInstance(project)
        val currentFile = editorManager.selectedFiles.firstOrNull()

        if (currentFile == null) {
            statsArea.text = "No file currently open. Open a log file first."
            return
        }

        val ioFile = File(currentFile.path)
        if (!ioFile.exists()) {
            statsArea.text = "File not found: ${ioFile.path}"
            return
        }

        val service = LogParserService.getInstance(project)
        val parser = service.parseLogFile(ioFile)
        val entries = parser.parseLines()

        if (entries.isEmpty()) {
            statsArea.text = "No log entries found in: ${ioFile.name}"
            return
        }

        val statistics = LogStatistics()
        val stats = statistics.compute(entries)

        statsArea.text = buildString {
            appendLine("=== Statistics for ${ioFile.name} ===")
            appendLine()
            appendLine("Total entries: ${stats.totalEntries}")
            appendLine("Error rate: ${String.format(java.util.Locale.US, "%.1f%%", stats.errorRate * 100)}")
            appendLine("Warning rate: ${String.format(java.util.Locale.US, "%.1f%%", stats.warningRate * 100)}")
            appendLine("Stack traces: ${stats.stackTraceCount}")
            appendLine("Avg message length: ${stats.avgMessageLength.toInt()} chars")

            appendLine()
            appendLine("Level Distribution:")
            val maxCount = stats.countByLevel.values.maxOrNull() ?: 1
            stats.countByLevel.entries.sortedByDescending { it.value }.forEach { (level, count) ->
                val barLength = (count.toDouble() / maxCount * 30).toInt()
                val bar = "█".repeat(barLength)
                appendLine("  ${level.name.padEnd(7)} $bar $count")
            }

            if (stats.topLoggers.isNotEmpty()) {
                appendLine()
                appendLine("Top 10 Loggers:")
                stats.topLoggers.forEachIndexed { i, (logger, count) ->
                    appendLine("  ${i + 1}. $logger ($count)")
                }
            }

            if (stats.entriesPerHour.isNotEmpty()) {
                appendLine()
                appendLine("Entries per Hour:")
                val maxHourCount = stats.entriesPerHour.values.maxOrNull() ?: 1
                for (hour in 0..23) {
                    val count = stats.entriesPerHour[hour] ?: 0
                    val barLength = (count.toDouble() / maxHourCount * 20).toInt()
                    val bar = "▓".repeat(barLength)
                    appendLine("  ${hour.toString().padStart(2, '0')}:00 $bar $count")
                }
            }
        }
        statsArea.caretPosition = 0
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
