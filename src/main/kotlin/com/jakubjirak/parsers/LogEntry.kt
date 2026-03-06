/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.parsers

import java.time.LocalDateTime

/** Log severity levels ordered from least to most severe. */
enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN
}

/**
 * Immutable representation of a single parsed log entry.
 *
 * @property lineNumber   1-based line number in the source file
 * @property timestamp    parsed timestamp, or `null` if not detected
 * @property level        detected log level
 * @property message      the log message body
 * @property threadName   extracted thread name, or `null`
 * @property loggerName   extracted logger/class name, or `null`
 * @property correlationId extracted correlation/trace ID, or `null`
 * @property stackTrace   appended stack trace lines, or `null`
 * @property rawLine      the original unmodified line from the file
 * @property metadata     additional key-value metadata
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
    val metadata: Map<String, Any> = emptyMap()
) {
    /** Returns `true` when this entry satisfies all criteria in [filter]. */
    fun matches(filter: LogFilter): Boolean {
        if (filter.levels.isNotEmpty() && level !in filter.levels) return false
        if (!matchesKeywords(filter.keywords)) return false
        if (!matchesTimeRange(filter.startTime, filter.endTime)) return false
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
}

/**
 * Composite filter for narrowing down [LogEntry] collections.
 *
 * All non-empty/non-null fields must match for an entry to pass.
 */
data class LogFilter(
    val levels: Set<LogLevel> = emptySet(),
    val keywords: List<String> = emptyList(),
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val correlationId: String? = null,
    val threadName: String? = null,
    val customPattern: Regex? = null
)
