/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LogEntryTest {

    @Suppress("LongParameterList")
    private fun entry(
        lineNumber: Int = 1,
        timestamp: LocalDateTime? = LocalDateTime.of(2024, 6, 15, 12, 0, 0),
        level: LogLevel = LogLevel.INFO,
        message: String = "default message",
        threadName: String? = "main",
        loggerName: String? = "com.example.App",
        correlationId: String? = null,
        stackTrace: String? = null,
        rawLine: String = "raw",
        source: String = "",
        metadata: Map<String, Any> = emptyMap()
    ) = LogEntry(
        lineNumber = lineNumber,
        timestamp = timestamp,
        level = level,
        message = message,
        threadName = threadName,
        loggerName = loggerName,
        correlationId = correlationId,
        stackTrace = stackTrace,
        rawLine = rawLine,
        source = source,
        metadata = metadata
    )

    // ── Level filter ───────────────────────────────────────────────────

    @Nested
    inner class LevelFilter {

        @Test
        fun `matches when level is in filter set`() {
            val e = entry(level = LogLevel.ERROR)
            val f = LogFilter(levels = setOf(LogLevel.ERROR, LogLevel.FATAL))
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when level is not in filter set`() {
            val e = entry(level = LogLevel.DEBUG)
            val f = LogFilter(levels = setOf(LogLevel.ERROR))
            assertFalse(e.matches(f))
        }

        @Test
        fun `matches when level filter is empty`() {
            val e = entry(level = LogLevel.DEBUG)
            val f = LogFilter(levels = emptySet())
            assertTrue(e.matches(f))
        }
    }

    // ── Keyword filter ─────────────────────────────────────────────────

    @Nested
    inner class KeywordFilter {

        @Test
        fun `matches when keyword is found in message`() {
            val e = entry(message = "Database connection established")
            val f = LogFilter(keywords = listOf("connection"))
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when keyword is not found`() {
            val e = entry(message = "Application started")
            val f = LogFilter(keywords = listOf("database"))
            assertFalse(e.matches(f))
        }

        @Test
        fun `keyword match is case insensitive`() {
            val e = entry(message = "NullPointerException occurred")
            val f = LogFilter(keywords = listOf("nullpointerexception"))
            assertTrue(e.matches(f))
        }

        @Test
        fun `matches any keyword`() {
            val e = entry(message = "Timeout on request")
            val f = LogFilter(keywords = listOf("database", "timeout"))
            assertTrue(e.matches(f))
        }

        @Test
        fun `matches when keyword appears in stackTrace`() {
            val e = entry(
                message = "Something failed",
                stackTrace = "at com.foo.Bar.process(Bar.java:42)\nCaused by: java.sql.SQLException"
            )
            val f = LogFilter(keywords = listOf("sqlexception"))
            assertTrue(e.matches(f))
        }

        @Test
        fun `matches when keywords list is empty`() {
            val e = entry(message = "anything")
            val f = LogFilter(keywords = emptyList())
            assertTrue(e.matches(f))
        }
    }

    // ── Time range filter ──────────────────────────────────────────────

    @Nested
    inner class TimeRangeFilter {

        private val ts = LocalDateTime.of(2024, 6, 15, 12, 0, 0)

        @Test
        fun `matches when timestamp is within range`() {
            val e = entry(timestamp = ts)
            val f = LogFilter(
                startTime = ts.minusHours(1),
                endTime = ts.plusHours(1)
            )
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when timestamp is before start`() {
            val e = entry(timestamp = ts.minusHours(2))
            val f = LogFilter(startTime = ts)
            assertFalse(e.matches(f))
        }

        @Test
        fun `rejects when timestamp is after end`() {
            val e = entry(timestamp = ts.plusHours(2))
            val f = LogFilter(endTime = ts)
            assertFalse(e.matches(f))
        }

        @Test
        fun `matches when entry has no timestamp`() {
            val e = entry(timestamp = null)
            val f = LogFilter(startTime = ts, endTime = ts.plusHours(1))
            assertTrue(e.matches(f))
        }

        @Test
        fun `matches when filter has no time range`() {
            val e = entry(timestamp = ts)
            val f = LogFilter()
            assertTrue(e.matches(f))
        }
    }

    // ── Correlation ID filter ──────────────────────────────────────────

    @Nested
    inner class CorrelationIdFilter {

        @Test
        fun `matches when correlation id equals filter`() {
            val e = entry(correlationId = "abc-123")
            val f = LogFilter(correlationId = "abc-123")
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when correlation id differs`() {
            val e = entry(correlationId = "abc-123")
            val f = LogFilter(correlationId = "xyz-789")
            assertFalse(e.matches(f))
        }

        @Test
        fun `rejects when entry has no correlation id but filter does`() {
            val e = entry(correlationId = null)
            val f = LogFilter(correlationId = "abc-123")
            assertFalse(e.matches(f))
        }

        @Test
        fun `matches when filter has no correlation id`() {
            val e = entry(correlationId = "abc-123")
            val f = LogFilter(correlationId = null)
            assertTrue(e.matches(f))
        }
    }

    // ── Thread name filter ─────────────────────────────────────────────

    @Nested
    inner class ThreadNameFilter {

        @Test
        fun `matches when thread name equals filter`() {
            val e = entry(threadName = "main")
            val f = LogFilter(threadName = "main")
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when thread name differs`() {
            val e = entry(threadName = "main")
            val f = LogFilter(threadName = "worker-1")
            assertFalse(e.matches(f))
        }

        @Test
        fun `matches when filter thread name is null`() {
            val e = entry(threadName = "main")
            val f = LogFilter(threadName = null)
            assertTrue(e.matches(f))
        }
    }

    // ── Regex filter ───────────────────────────────────────────────────

    @Nested
    inner class RegexFilter {

        @Test
        fun `matches when regex matches raw line`() {
            val e = entry(rawLine = "2024-01-15 ERROR NullPointerException")
            val f = LogFilter(customPattern = Regex("ERROR.*Exception"))
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when regex does not match`() {
            val e = entry(rawLine = "2024-01-15 INFO started")
            val f = LogFilter(customPattern = Regex("ERROR"))
            assertFalse(e.matches(f))
        }

        @Test
        fun `matches when regex is null`() {
            val e = entry(rawLine = "anything")
            val f = LogFilter(customPattern = null)
            assertTrue(e.matches(f))
        }
    }

    // ── Empty filter ───────────────────────────────────────────────────

    @Nested
    inner class EmptyFilter {

        @Test
        fun `empty filter matches any entry`() {
            val e = entry(
                level = LogLevel.FATAL,
                message = "critical failure",
                correlationId = "id-1",
                threadName = "worker"
            )
            assertTrue(e.matches(LogFilter()))
        }

        @Test
        fun `empty filter matches entry with nulls`() {
            val e = entry(
                timestamp = null,
                threadName = null,
                loggerName = null,
                correlationId = null
            )
            assertTrue(e.matches(LogFilter()))
        }
    }

    // ── Combined filters ───────────────────────────────────────────────

    @Nested
    inner class CombinedFilters {

        @Test
        fun `matches when all filter criteria are satisfied`() {
            val ts = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
            val e = entry(
                timestamp = ts,
                level = LogLevel.ERROR,
                message = "Database connection failed",
                correlationId = "req-42",
                threadName = "pool-1"
            )
            val f = LogFilter(
                levels = setOf(LogLevel.ERROR),
                keywords = listOf("database"),
                startTime = ts.minusMinutes(5),
                endTime = ts.plusMinutes(5),
                correlationId = "req-42",
                threadName = "pool-1"
            )
            assertTrue(e.matches(f))
        }

        @Test
        fun `rejects when one criterion fails in combined filter`() {
            val e = entry(
                level = LogLevel.INFO,
                message = "Database connection OK",
                correlationId = "req-42",
                threadName = "pool-1"
            )
            val f = LogFilter(
                levels = setOf(LogLevel.ERROR),
                keywords = listOf("database"),
                correlationId = "req-42",
                threadName = "pool-1"
            )
            assertFalse(e.matches(f))
        }
    }

    // ── Source field ───────────────────────────────────────────────────

    @Nested
    inner class SourceField {

        @Test
        fun `source defaults to empty string`() {
            val e = entry()
            assertEquals("", e.source)
        }

        @Test
        fun `source is preserved when specified`() {
            val e = entry(source = "app.log")
            assertEquals("app.log", e.source)
        }
    }
}
