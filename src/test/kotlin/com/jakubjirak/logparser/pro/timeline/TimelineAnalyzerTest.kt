/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.timeline

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class TimelineAnalyzerTest {

    private val analyzer = TimelineAnalyzer()

    private fun entry(offsetSeconds: Long, level: LogLevel = LogLevel.INFO): LogEntry {
        val base = LocalDateTime.of(2026, 4, 22, 10, 0, 0)
        return LogEntry(
            lineNumber = 1,
            timestamp = base.plusSeconds(offsetSeconds),
            level = level,
            message = "m",
            threadName = null,
            loggerName = null,
            correlationId = null,
            stackTrace = null,
            rawLine = "r"
        )
    }

    @Test
    fun `buckets entries into minute windows`() {
        val entries = (0L..119L step 30L).map { entry(it) } // minutes 0 and 1
        val report = analyzer.analyze(entries, Duration.ofMinutes(1))
        assertEquals(2, report.buckets.size)
        assertEquals(2, report.buckets[0].totalCount)
        assertEquals(2, report.buckets[1].totalCount)
    }

    @Test
    fun `counts by level are correct`() {
        val entries = listOf(
            entry(0, LogLevel.INFO),
            entry(1, LogLevel.ERROR),
            entry(2, LogLevel.FATAL),
            entry(3, LogLevel.WARN)
        )
        val report = analyzer.analyze(entries, Duration.ofMinutes(1))
        val bucket = report.buckets.first()
        assertEquals(1, bucket.countsByLevel[LogLevel.INFO])
        assertEquals(1, bucket.countsByLevel[LogLevel.ERROR])
        assertEquals(1, bucket.countsByLevel[LogLevel.FATAL])
        assertEquals(2, bucket.errorCount) // ERROR + FATAL
    }

    @Test
    fun `skipped equals entries without timestamp`() {
        val withTs = entry(0)
        val noTs = withTs.copy(timestamp = null)
        val report = analyzer.analyze(listOf(withTs, noTs, noTs), Duration.ofMinutes(1))
        assertEquals(2, report.skipped)
        assertEquals(1, report.totalEntries)
    }

    @Test
    fun `empty input yields empty report`() {
        val report = analyzer.analyze(emptyList(), Duration.ofMinutes(1))
        assertTrue(report.buckets.isEmpty())
        assertEquals(0, report.totalEntries)
    }

    @Test
    fun `invalid bucket size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(listOf(entry(0)), Duration.ZERO)
        }
    }

    @Test
    fun `detects error spike`() {
        // 19 quiet buckets, 1 spike bucket with many errors
        val entries = mutableListOf<LogEntry>()
        repeat(19) { i ->
            entries += entry(i.toLong() * 60, LogLevel.INFO)
        }
        repeat(30) {
            entries += entry(19L * 60L + it, LogLevel.ERROR)
        }
        val report = analyzer.analyze(entries, Duration.ofMinutes(1), zThreshold = 2.0)
        assertTrue(report.spikes.isNotEmpty())
        assertEquals(19, report.spikes.last())
    }

    @Test
    fun `no spikes when variance is zero`() {
        val entries = (0L until 600L step 60L).map { entry(it, LogLevel.ERROR) }
        val report = analyzer.analyze(entries, Duration.ofMinutes(1), zThreshold = 2.0)
        assertTrue(report.spikes.isEmpty())
    }

    @Test
    fun `no spikes with fewer than three buckets`() {
        val entries = listOf(entry(0, LogLevel.ERROR), entry(60, LogLevel.ERROR))
        val report = analyzer.analyze(entries, Duration.ofMinutes(1))
        assertTrue(report.spikes.isEmpty())
    }

    @Test
    fun `second granularity works`() {
        val entries = (0L..5L).map { entry(it) }
        val report = analyzer.analyze(entries, Duration.ofSeconds(1))
        assertEquals(6, report.buckets.size)
        assertTrue(report.buckets.all { it.totalCount == 1 })
    }

    @Test
    fun `bucket boundaries are aligned`() {
        val report = analyzer.analyze(listOf(entry(17)), Duration.ofMinutes(1))
        val start = report.buckets.first().start
        assertEquals(0, start.second)
    }
}
