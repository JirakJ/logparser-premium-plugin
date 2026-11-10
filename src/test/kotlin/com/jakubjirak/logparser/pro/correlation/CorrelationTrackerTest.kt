/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.correlation

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CorrelationTrackerTest {

    private lateinit var tracker: CorrelationTracker

    @BeforeEach
    fun setUp() {
        tracker = CorrelationTracker()
    }

    private fun entry(
        lineNumber: Int = 1,
        timestamp: LocalDateTime? = null,
        level: LogLevel = LogLevel.INFO,
        message: String = "test"
    ) = LogEntry(
        lineNumber = lineNumber,
        timestamp = timestamp,
        level = level,
        message = message,
        threadName = null,
        loggerName = null,
        correlationId = null,
        stackTrace = null,
        rawLine = message
    )

    // ── addEntry + getTransaction ──────────────────────────────────────

    @Nested
    inner class AddAndGet {

        @Test
        fun `builds transaction from added entries`() {
            val e1 = entry(lineNumber = 1, message = "request start")
            val e2 = entry(lineNumber = 2, message = "request end")

            tracker.addEntry("tx-1", e1, "app.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-1", e2, "app.log", SourceType.LOCAL_FILE)

            val flow = tracker.getTransaction("tx-1")
            assertNotNull(flow)
            assertEquals("tx-1", flow!!.correlationId)
            assertEquals(2, flow.entries.size)
            assertEquals("app.log", flow.entries[0].sourceName)
            assertEquals(SourceType.LOCAL_FILE, flow.entries[0].sourceType)
        }

        @Test
        fun `tracks entries from multiple sources`() {
            val e1 = entry(message = "gateway")
            val e2 = entry(message = "service")

            tracker.addEntry("tx-2", e1, "gateway.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-2", e2, "service.log", SourceType.KUBERNETES_POD)

            val flow = tracker.getTransaction("tx-2")
            assertNotNull(flow)
            assertEquals(setOf("gateway.log", "service.log"), flow!!.sources)
        }

        @Test
        fun `returns null for unknown correlation id`() {
            assertNull(tracker.getTransaction("non-existent"))
        }
    }

    // ── Duration calculation ───────────────────────────────────────────

    @Nested
    inner class DurationCalculation {

        @Test
        fun `calculates duration from timestamps`() {
            val start = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
            val end = LocalDateTime.of(2024, 6, 15, 12, 0, 5)

            tracker.addEntry("tx-d", entry(timestamp = start), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-d", entry(timestamp = end), "a.log", SourceType.LOCAL_FILE)

            val flow = tracker.getTransaction("tx-d")
            assertNotNull(flow)
            assertEquals(start, flow!!.startTime)
            assertEquals(end, flow.endTime)
            assertEquals(5000L, flow.duration)
        }

        @Test
        fun `duration is null when timestamps are missing`() {
            tracker.addEntry("tx-n", entry(timestamp = null), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-n", entry(timestamp = null), "a.log", SourceType.LOCAL_FILE)

            val flow = tracker.getTransaction("tx-n")
            assertNotNull(flow)
            assertNull(flow!!.duration)
        }

        @Test
        fun `entries are sorted by timestamp`() {
            val early = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val late = LocalDateTime.of(2024, 1, 1, 11, 0, 0)

            tracker.addEntry("tx-s", entry(timestamp = late, message = "late"), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-s", entry(timestamp = early, message = "early"), "a.log", SourceType.LOCAL_FILE)

            val flow = tracker.getTransaction("tx-s")
            assertEquals(early, flow!!.startTime)
            assertEquals(late, flow.endTime)
        }
    }

    // ── hasErrors ──────────────────────────────────────────────────────

    @Nested
    inner class HasErrors {

        @Test
        fun `hasErrors true when entry has ERROR level`() {
            tracker.addEntry("tx-e", entry(level = LogLevel.ERROR), "a.log", SourceType.LOCAL_FILE)

            assertTrue(tracker.getTransaction("tx-e")!!.hasErrors)
        }

        @Test
        fun `hasErrors true when entry has FATAL level`() {
            tracker.addEntry("tx-f", entry(level = LogLevel.FATAL), "a.log", SourceType.LOCAL_FILE)

            assertTrue(tracker.getTransaction("tx-f")!!.hasErrors)
        }

        @Test
        fun `hasErrors false when all entries are INFO`() {
            tracker.addEntry("tx-i", entry(level = LogLevel.INFO), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-i", entry(level = LogLevel.DEBUG), "a.log", SourceType.LOCAL_FILE)

            assertFalse(tracker.getTransaction("tx-i")!!.hasErrors)
        }

        @Test
        fun `hasErrors true when at least one entry is ERROR among many`() {
            tracker.addEntry("tx-m", entry(level = LogLevel.INFO), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-m", entry(level = LogLevel.ERROR), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-m", entry(level = LogLevel.INFO), "a.log", SourceType.LOCAL_FILE)

            assertTrue(tracker.getTransaction("tx-m")!!.hasErrors)
        }
    }

    // ── getAllTransactions ──────────────────────────────────────────────

    @Nested
    inner class GetAllTransactions {

        @Test
        fun `returns all tracked transactions`() {
            tracker.addEntry("tx-a", entry(message = "a"), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-b", entry(message = "b"), "b.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-c", entry(message = "c"), "c.log", SourceType.LOCAL_FILE)

            val all = tracker.getAllTransactions()
            assertEquals(3, all.size)
            val ids = all.map { it.correlationId }.toSet()
            assertEquals(setOf("tx-a", "tx-b", "tx-c"), ids)
        }

        @Test
        fun `returns empty list when no transactions`() {
            assertTrue(tracker.getAllTransactions().isEmpty())
        }
    }

    // ── clear ──────────────────────────────────────────────────────────

    @Nested
    inner class Clear {

        @Test
        fun `removes all transactions`() {
            tracker.addEntry("tx-1", entry(), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-2", entry(), "b.log", SourceType.LOCAL_FILE)

            tracker.clear()

            assertEquals(0, tracker.getTransactionCount())
            assertTrue(tracker.getAllTransactions().isEmpty())
        }
    }

    // ── removeTransaction ──────────────────────────────────────────────

    @Nested
    inner class RemoveTransaction {

        @Test
        fun `removes specific transaction`() {
            tracker.addEntry("tx-1", entry(), "a.log", SourceType.LOCAL_FILE)
            tracker.addEntry("tx-2", entry(), "b.log", SourceType.LOCAL_FILE)

            tracker.removeTransaction("tx-1")

            assertNull(tracker.getTransaction("tx-1"))
            assertNotNull(tracker.getTransaction("tx-2"))
            assertEquals(1, tracker.getTransactionCount())
        }

        @Test
        fun `removing non-existent transaction does not throw`() {
            assertDoesNotThrow { tracker.removeTransaction("ghost") }
        }
    }

    // ── getTransactionCount ────────────────────────────────────────────

    @Nested
    inner class GetTransactionCount {

        @Test
        fun `returns zero when empty`() {
            assertEquals(0, tracker.getTransactionCount())
        }

        @Test
        fun `returns correct count`() {
            tracker.addEntry("a", entry(), "x.log", SourceType.LOCAL_FILE)
            tracker.addEntry("b", entry(), "y.log", SourceType.LOCAL_FILE)
            tracker.addEntry("a", entry(), "x.log", SourceType.LOCAL_FILE)

            assertEquals(2, tracker.getTransactionCount())
        }
    }
}
