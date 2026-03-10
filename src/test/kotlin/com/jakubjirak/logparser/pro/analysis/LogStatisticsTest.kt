/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.analysis

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LogStatisticsTest {

    private lateinit var statistics: LogStatistics

    @BeforeEach
    fun setUp() {
        statistics = LogStatistics()
    }

    private fun entry(
        level: LogLevel = LogLevel.INFO,
        message: String = "test",
        loggerName: String? = null,
        timestamp: LocalDateTime? = null,
        stackTrace: String? = null
    ) = LogEntry(
        lineNumber = 1,
        timestamp = timestamp,
        level = level,
        message = message,
        threadName = null,
        loggerName = loggerName,
        correlationId = null,
        stackTrace = stackTrace,
        rawLine = message
    )

    // ── Empty entries ──────────────────────────────────────────────────

    @Nested
    inner class EmptyEntries {

        @Test
        fun `returns zero stats for empty list`() {
            val stats = statistics.compute(emptyList())
            assertEquals(0, stats.totalEntries)
            assertEquals(0.0, stats.errorRate)
            assertEquals(0.0, stats.warningRate)
            assertEquals(0.0, stats.avgMessageLength)
            assertEquals(0, stats.stackTraceCount)
            assertTrue(stats.countByLevel.isEmpty())
            assertTrue(stats.topLoggers.isEmpty())
            assertTrue(stats.entriesPerHour.isEmpty())
        }
    }

    // ── Count by level ─────────────────────────────────────────────────

    @Nested
    inner class CountByLevel {

        @Test
        fun `counts entries per level`() {
            val entries = listOf(
                entry(level = LogLevel.INFO),
                entry(level = LogLevel.INFO),
                entry(level = LogLevel.ERROR),
                entry(level = LogLevel.WARN),
                entry(level = LogLevel.DEBUG)
            )
            val stats = statistics.compute(entries)
            assertEquals(5, stats.totalEntries)
            assertEquals(2, stats.countByLevel[LogLevel.INFO])
            assertEquals(1, stats.countByLevel[LogLevel.ERROR])
            assertEquals(1, stats.countByLevel[LogLevel.WARN])
            assertEquals(1, stats.countByLevel[LogLevel.DEBUG])
        }
    }

    // ── Error and warning rate ─────────────────────────────────────────

    @Nested
    inner class Rates {

        @Test
        fun `computes error rate`() {
            val entries = listOf(
                entry(level = LogLevel.INFO),
                entry(level = LogLevel.ERROR),
                entry(level = LogLevel.FATAL),
                entry(level = LogLevel.INFO)
            )
            val stats = statistics.compute(entries)
            assertEquals(0.5, stats.errorRate, 0.01) // 2 out of 4
        }

        @Test
        fun `computes warning rate`() {
            val entries = listOf(
                entry(level = LogLevel.WARN),
                entry(level = LogLevel.INFO),
                entry(level = LogLevel.WARN),
                entry(level = LogLevel.INFO)
            )
            val stats = statistics.compute(entries)
            assertEquals(0.5, stats.warningRate, 0.01)
        }

        @Test
        fun `zero rates when no errors or warnings`() {
            val entries = listOf(
                entry(level = LogLevel.INFO),
                entry(level = LogLevel.DEBUG)
            )
            val stats = statistics.compute(entries)
            assertEquals(0.0, stats.errorRate)
            assertEquals(0.0, stats.warningRate)
        }
    }

    // ── Top loggers ────────────────────────────────────────────────────

    @Nested
    inner class TopLoggers {

        @Test
        fun `identifies top loggers`() {
            val entries = listOf(
                entry(loggerName = "com.example.AppA"),
                entry(loggerName = "com.example.AppA"),
                entry(loggerName = "com.example.AppB"),
                entry(loggerName = "com.example.AppA"),
                entry(loggerName = "com.example.AppB")
            )
            val stats = statistics.compute(entries)
            assertEquals(2, stats.topLoggers.size)
            assertEquals("com.example.AppA", stats.topLoggers[0].first)
            assertEquals(3, stats.topLoggers[0].second)
        }

        @Test
        fun `limits to top 10 loggers`() {
            val entries = (1..20).map { entry(loggerName = "logger$it") }
            val stats = statistics.compute(entries)
            assertTrue(stats.topLoggers.size <= 10)
        }

        @Test
        fun `handles entries without logger names`() {
            val entries = listOf(
                entry(loggerName = null),
                entry(loggerName = null)
            )
            val stats = statistics.compute(entries)
            assertTrue(stats.topLoggers.isEmpty())
        }
    }

    // ── Entries per hour ───────────────────────────────────────────────

    @Nested
    inner class EntriesPerHour {

        @Test
        fun `groups entries by hour`() {
            val entries = listOf(
                entry(timestamp = LocalDateTime.of(2024, 1, 1, 10, 0, 0)),
                entry(timestamp = LocalDateTime.of(2024, 1, 1, 10, 30, 0)),
                entry(timestamp = LocalDateTime.of(2024, 1, 1, 14, 0, 0))
            )
            val stats = statistics.compute(entries)
            assertEquals(2, stats.entriesPerHour[10])
            assertEquals(1, stats.entriesPerHour[14])
        }

        @Test
        fun `handles entries without timestamps`() {
            val entries = listOf(
                entry(timestamp = null),
                entry(timestamp = null)
            )
            val stats = statistics.compute(entries)
            assertTrue(stats.entriesPerHour.isEmpty())
        }
    }

    // ── Message length and stack traces ────────────────────────────────

    @Nested
    inner class MessageAndStackTrace {

        @Test
        fun `computes average message length`() {
            val entries = listOf(
                entry(message = "short"),      // 5
                entry(message = "longer msg")  // 10
            )
            val stats = statistics.compute(entries)
            assertEquals(7.5, stats.avgMessageLength, 0.01)
        }

        @Test
        fun `counts stack traces`() {
            val entries = listOf(
                entry(stackTrace = "at com.foo.Bar.method(Bar.java:1)"),
                entry(stackTrace = null),
                entry(stackTrace = "at com.baz.Qux.run(Qux.java:2)")
            )
            val stats = statistics.compute(entries)
            assertEquals(2, stats.stackTraceCount)
        }
    }
}
