/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.diff

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogDiffEngineTest {

    private val engine = LogDiffEngine()

    private fun entry(
        message: String,
        level: LogLevel = LogLevel.INFO,
        logger: String? = null
    ): LogEntry = LogEntry(
        lineNumber = 1,
        timestamp = null,
        level = level,
        message = message,
        threadName = null,
        loggerName = logger,
        correlationId = null,
        stackTrace = null,
        rawLine = message
    )

    @Test
    fun `identical lists produce empty added and removed`() {
        val list = listOf(entry("hello"))
        val result = engine.diff(list, list)
        assertTrue(result.added.isEmpty())
        assertTrue(result.removed.isEmpty())
        assertEquals(1, result.common.size)
    }

    @Test
    fun `added entries appear only in right`() {
        val left = listOf(entry("common"))
        val right = listOf(entry("common"), entry("new"))
        val result = engine.diff(left, right)
        assertEquals(1, result.added.size)
        assertEquals("new", result.added.first().sample.message)
    }

    @Test
    fun `removed entries appear only in left`() {
        val left = listOf(entry("a"), entry("gone"))
        val right = listOf(entry("a"))
        val result = engine.diff(left, right)
        assertEquals(1, result.removed.size)
        assertEquals("gone", result.removed.first().sample.message)
    }

    @Test
    fun `digits are normalized`() {
        val left = listOf(entry("request 42 succeeded"))
        val right = listOf(entry("request 99 succeeded"))
        val result = engine.diff(left, right)
        assertEquals(1, result.common.size)
        assertTrue(result.added.isEmpty())
    }

    @Test
    fun `uuids are normalized`() {
        val left = listOf(entry("id=123e4567-e89b-12d3-a456-426614174000 ok"))
        val right = listOf(entry("id=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee ok"))
        val result = engine.diff(left, right)
        assertEquals(1, result.common.size)
    }

    @Test
    fun `level difference prevents match`() {
        val left = listOf(entry("msg", LogLevel.INFO))
        val right = listOf(entry("msg", LogLevel.WARN))
        val result = engine.diff(left, right)
        assertEquals(1, result.added.size)
        assertEquals(1, result.removed.size)
    }

    @Test
    fun `logger difference prevents match`() {
        val left = listOf(entry("msg", logger = "A"))
        val right = listOf(entry("msg", logger = "B"))
        val result = engine.diff(left, right)
        assertEquals(0, result.common.size)
    }

    @Test
    fun `counts reflect occurrences on each side`() {
        val left = listOf(entry("x"), entry("x"), entry("y"))
        val right = listOf(entry("x"), entry("y"), entry("y"), entry("y"))
        val result = engine.diff(left, right)
        val x = result.common.first { it.signature.contains("x") }
        val y = result.common.first { it.signature.contains("y") }
        assertEquals(3, x.count) // 2 + 1
        assertEquals(4, y.count) // 1 + 3
    }

    @Test
    fun `addedByLevel counts are emitted`() {
        val left = emptyList<LogEntry>()
        val right = listOf(entry("a", LogLevel.ERROR), entry("b", LogLevel.ERROR), entry("c", LogLevel.INFO))
        val result = engine.diff(left, right)
        assertEquals(2, result.addedByLevel[LogLevel.ERROR])
        assertEquals(1, result.addedByLevel[LogLevel.INFO])
    }

    @Test
    fun `timestamps and hex addresses are normalized`() {
        val left = listOf(entry("at 2026-04-22T10:00:00 got 0xabcdef failed"))
        val right = listOf(entry("at 2026-04-22T11:30:00 got 0x00ffff failed"))
        val result = engine.diff(left, right)
        assertEquals(1, result.common.size)
    }

    @Test
    fun `empty inputs yield empty outputs`() {
        val result = engine.diff(emptyList(), emptyList())
        assertTrue(result.added.isEmpty())
        assertTrue(result.removed.isEmpty())
        assertTrue(result.common.isEmpty())
    }
}
