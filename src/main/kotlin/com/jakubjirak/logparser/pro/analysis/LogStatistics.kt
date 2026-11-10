/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.analysis

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel

/**
 * Computes aggregate statistics over a collection of [LogEntry] objects.
 *
 * Provides counts by log level, top loggers, entries over time windows,
 * and error/warning rates.
 */
class LogStatistics {

    /**
     * Full statistics snapshot for a set of log entries.
     *
     * @property totalEntries    total number of entries
     * @property countByLevel    count per [LogLevel]
     * @property topLoggers      top-10 logger names by frequency
     * @property entriesPerHour  entries grouped by hour of day (0..23)
     * @property errorRate       fraction of entries at ERROR or FATAL
     * @property warningRate     fraction of entries at WARN
     * @property avgMessageLength average message length in characters
     * @property stackTraceCount number of entries with a stack trace
     */
    data class Stats(
        val totalEntries: Int,
        val countByLevel: Map<LogLevel, Int>,
        val topLoggers: List<Pair<String, Int>>,
        val entriesPerHour: Map<Int, Int>,
        val errorRate: Double,
        val warningRate: Double,
        val avgMessageLength: Double,
        val stackTraceCount: Int
    )

    /** Computes [Stats] from the given [entries]. */
    fun compute(entries: List<LogEntry>): Stats {
        if (entries.isEmpty()) {
            return Stats(
                totalEntries = 0,
                countByLevel = emptyMap(),
                topLoggers = emptyList(),
                entriesPerHour = emptyMap(),
                errorRate = 0.0,
                warningRate = 0.0,
                avgMessageLength = 0.0,
                stackTraceCount = 0
            )
        }

        val countByLevel = entries.groupingBy { it.level }.eachCount()

        val topLoggers = entries
            .mapNotNull { it.loggerName }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }

        val entriesPerHour = entries
            .mapNotNull { it.timestamp?.hour }
            .groupingBy { it }
            .eachCount()

        val totalEntries = entries.size
        val errorCount = entries.count { it.level == LogLevel.ERROR || it.level == LogLevel.FATAL }
        val warnCount = entries.count { it.level == LogLevel.WARN }

        return Stats(
            totalEntries = totalEntries,
            countByLevel = countByLevel,
            topLoggers = topLoggers,
            entriesPerHour = entriesPerHour,
            errorRate = errorCount.toDouble() / totalEntries,
            warningRate = warnCount.toDouble() / totalEntries,
            avgMessageLength = entries.map { it.message.length }.average(),
            stackTraceCount = entries.count { it.stackTrace != null }
        )
    }
}
