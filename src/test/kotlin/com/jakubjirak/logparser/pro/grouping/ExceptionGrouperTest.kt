/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.grouping

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ExceptionGrouperTest {

    private val grouper = ExceptionGrouper()

    private fun entry(
        line: Int,
        trace: String?,
        ts: LocalDateTime? = null,
        message: String = "error"
    ): LogEntry = LogEntry(
        lineNumber = line,
        timestamp = ts,
        level = LogLevel.ERROR,
        message = message,
        threadName = null,
        loggerName = null,
        correlationId = null,
        stackTrace = trace,
        rawLine = "raw-$line"
    )

    private val trace1 = """
        java.lang.NullPointerException: boom
        	at com.foo.Bar.run(Bar.kt:142)
        	at com.foo.Main.main(Main.kt:27)
    """.trimIndent()

    private val trace1AtDifferentLine = """
        java.lang.NullPointerException: boom
        	at com.foo.Bar.run(Bar.kt:198)
        	at com.foo.Main.main(Main.kt:27)
    """.trimIndent()

    private val trace2 = """
        java.lang.IllegalArgumentException: bad input
        	at com.foo.Validator.check(Validator.kt:8)
    """.trimIndent()

    @Test
    fun `groups identical traces`() {
        val groups = grouper.group(listOf(entry(1, trace1), entry(2, trace1)))
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().count)
    }

    @Test
    fun `line numbers are ignored in signature`() {
        val groups = grouper.group(listOf(entry(1, trace1), entry(2, trace1AtDifferentLine)))
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().count)
    }

    @Test
    fun `different exception classes produce different groups`() {
        val groups = grouper.group(listOf(entry(1, trace1), entry(2, trace2)))
        assertEquals(2, groups.size)
    }

    @Test
    fun `entries without stack trace are ignored`() {
        val groups = grouper.group(listOf(entry(1, null), entry(2, "  "), entry(3, trace1)))
        assertEquals(1, groups.size)
        assertEquals(1, groups.first().count)
    }

    @Test
    fun `extracts exception class`() {
        val groups = grouper.group(listOf(entry(1, trace1)))
        assertEquals("java.lang.NullPointerException", groups.first().exceptionClass)
    }

    @Test
    fun `extracts root cause from caused by chain`() {
        val trace = """
            java.lang.RuntimeException: wrapper
            	at com.foo.Bar.run(Bar.kt:10)
            Caused by: java.io.IOException: disk full
            	at com.foo.Disk.write(Disk.kt:88)
        """.trimIndent()
        val groups = grouper.group(listOf(entry(1, trace)))
        assertEquals("java.io.IOException", groups.first().rootCause)
    }

    @Test
    fun `first and last seen are populated`() {
        val t1 = LocalDateTime.of(2026, 4, 22, 10, 0)
        val t2 = LocalDateTime.of(2026, 4, 22, 12, 0)
        val groups = grouper.group(listOf(entry(1, trace1, t1), entry(2, trace1, t2)))
        assertEquals(t1, groups.first().firstSeen)
        assertEquals(t2, groups.first().lastSeen)
    }

    @Test
    fun `sample line numbers are capped`() {
        val many = (1..20).map { entry(it, trace1) }
        val groups = grouper.group(many)
        assertTrue(groups.first().sampleLineNumbers.size <= ExceptionGrouper.MAX_SAMPLES)
    }

    @Test
    fun `groups sorted by count descending`() {
        val entries = listOf(entry(1, trace2)) + (1..3).map { entry(it + 1, trace1) }
        val groups = grouper.group(entries)
        assertEquals(3, groups[0].count)
        assertEquals(1, groups[1].count)
    }

    @Test
    fun `signatureOf normalizes lambda suffix`() {
        val a = grouper.signatureOf("""
            java.lang.NullPointerException: x
            	at com.foo.${'$'}${'$'}Lambda${'$'}42/0x00000008008e7000.apply(Bar.kt:10)
        """.trimIndent())
        val b = grouper.signatureOf("""
            java.lang.NullPointerException: x
            	at com.foo.${'$'}${'$'}Lambda${'$'}77/0x0000000800aaa100.apply(Bar.kt:10)
        """.trimIndent())
        assertEquals(a, b)
    }

    @Test
    fun `signatureOf normalizes hex addresses`() {
        val a = grouper.signatureOf("java.lang.RuntimeException: at 0xabcdef")
        val b = grouper.signatureOf("java.lang.RuntimeException: at 0x123456")
        assertEquals(a, b)
    }

    @Test
    fun `exemplar is preserved`() {
        val e = entry(7, trace1, message = "specific message")
        val groups = grouper.group(listOf(e))
        assertNotNull(groups.first().exemplar)
        assertEquals(7, groups.first().exemplar.lineNumber)
    }
}
