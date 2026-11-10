/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * Library of regex and [DateTimeFormatter] patterns used to extract structured
 * fields (timestamps, log levels, correlation IDs, thread/logger names) from
 * raw log lines.
 *
 * Supports common formats: ISO 8601, Spring Boot, Apache/Nginx, Java logging,
 * and Syslog.
 */
class PatternLibrary {

    private val timestampPatterns = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
            .optionalEnd()
            .toFormatter(),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z"),
        DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a"),
        DateTimeFormatterBuilder()
            .appendPattern("MMM dd HH:mm:ss")
            .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().year.toLong())
            .toFormatter()
    )

    private val levelPatterns = mapOf(
        "TRACE" to LogLevel.TRACE,
        "DEBUG" to LogLevel.DEBUG,
        "INFO" to LogLevel.INFO,
        "WARN" to LogLevel.WARN,
        "WARNING" to LogLevel.WARN,
        "ERROR" to LogLevel.ERROR,
        "FATAL" to LogLevel.FATAL,
        "SEVERE" to LogLevel.ERROR
    )

    private val correlationIdPatterns = listOf(
        Regex("""X-Request-ID[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""X-Correlation-ID[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""traceId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""trace_id[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""spanId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""requestId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""correlationId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})""", RegexOption.IGNORE_CASE)
    )

    private val threadPattern = Regex("""\[([^\]]+)\]""")
    private val loggerPattern = Regex("""([a-z]+\.[a-z.]+[A-Z][a-zA-Z]+)""")

    /** Attempts to parse a [LocalDateTime] from the first 50 characters of [line]. */
    fun parseTimestamp(line: String): LocalDateTime? {
        val prefix = line.take(50)
        for (pattern in timestampPatterns) {
            try {
                return LocalDateTime.parse(prefix.trim(), pattern)
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /** Detects the [LogLevel] by scanning [line] for known level keywords. */
    fun parseLevel(line: String): LogLevel {
        for ((keyword, level) in levelPatterns) {
            if (line.contains(keyword, ignoreCase = true)) return level
        }
        return LogLevel.UNKNOWN
    }

    /** Extracts a correlation/trace ID from [line] using known header and UUID patterns. */
    fun extractCorrelationId(line: String): String? {
        for (pattern in correlationIdPatterns) {
            val match = pattern.find(line)
            if (match != null) return match.groupValues.getOrNull(1) ?: match.value
        }
        return null
    }

    /** Extracts a thread name enclosed in square brackets, e.g. `[main]`. */
    fun extractThreadName(line: String): String? {
        return threadPattern.find(line)?.groupValues?.getOrNull(1)
    }

    /** Extracts a fully-qualified logger/class name, e.g. `com.example.MyClass`. */
    fun extractLoggerName(line: String): String? {
        return loggerPattern.find(line)?.groupValues?.getOrNull(1)
    }

    /** Returns `true` when [line] looks like part of a Java/Kotlin stack trace. */
    fun isStackTraceLine(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("at ") ||
            trimmed.startsWith("Caused by:") ||
            trimmed.startsWith("... ")
    }
}
