/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.core

import java.time.LocalDateTime

/**
 * Immutable representation of a single parsed log entry.
 *
 * @property lineNumber    1-based line number in the source file
 * @property timestamp     parsed timestamp, or `null` if not detected
 * @property level         detected log level
 * @property message       the log message body
 * @property threadName    extracted thread name, or `null`
 * @property loggerName    extracted logger/class name, or `null`
 * @property correlationId extracted correlation/trace ID, or `null`
 * @property stackTrace    appended stack trace lines, or `null`
 * @property rawLine       the original unmodified line from the file
 * @property source        name of the log source (file name, pod name, etc.)
 * @property metadata      additional key-value metadata
 */
data class LogEntry(
    val lineNumber: Int,
    val timestamp: LocalDateTime?,
    val level: LogLevel,
    val message: String,
    val threadName: String?,
    val loggerName: String?,
    val correlationId: String?,
    val stackTrace: String?,
    val rawLine: String,
    val source: String = "",
    val metadata: Map<String, Any> = emptyMap()
) {
    /** Returns `true` when this entry satisfies all criteria in [filter]. */
    fun matches(filter: LogFilter): Boolean {
        if (filter.levels.isNotEmpty() && level !in filter.levels) return false
        if (!matchesKeywords(filter.keywords)) return false
        if (!matchesTimeRange(filter.startTime, filter.endTime)) return false
        if (!matchesRegex(filter.customPattern)) return false
        return matchesIdentifiers(filter)
    }

    private fun matchesIdentifiers(filter: LogFilter): Boolean {
        if (filter.correlationId != null && correlationId != filter.correlationId) return false
        if (filter.threadName != null && threadName != filter.threadName) return false
        return true
    }

    private fun matchesKeywords(keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        val searchText = (message + (stackTrace ?: "")).lowercase()
        return keywords.any { keyword -> searchText.contains(keyword.lowercase()) }
    }

    private fun matchesTimeRange(start: LocalDateTime?, end: LocalDateTime?): Boolean {
        if (timestamp == null) return true
        if (start != null && timestamp.isBefore(start)) return false
        if (end != null && timestamp.isAfter(end)) return false
        return true
    }

    private fun matchesRegex(pattern: Regex?): Boolean {
        if (pattern == null) return true
        return pattern.containsMatchIn(rawLine)
    }
}
