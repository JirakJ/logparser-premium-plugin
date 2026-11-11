/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LogParserServiceTest {

    @TempDir
    lateinit var dir: File

    private fun log(name: String) = File(dir, name).apply { writeText("2024-01-01 10:00:00 INFO [main] hello\n") }

    @Test
    fun `parsers are cached per absolute path`() {
        val service = LogParserService()
        val file = log("a.log")
        val first = service.parseLogFile(file)
        assertSame(first, service.parseLogFile(File(file.absolutePath)))
        assertNotSame(first, service.parseLogFile(log("b.log")))
        assertEquals(1, first.parseLines().size)
    }

    @Test
    fun `closing a file evicts its parser`() {
        val service = LogParserService()
        val file = log("a.log")
        val first = service.parseLogFile(file)
        service.closeLogFile(file)
        assertNotSame(first, service.parseLogFile(file))
        service.closeLogFile(File(dir, "never-opened.log"))
    }

    @Test
    fun `dispose clears the cache and pattern library is shared`() {
        val service = LogParserService()
        val file = log("a.log")
        val first = service.parseLogFile(file)
        val library = service.getPatternLibrary()
        service.dispose()
        assertNotSame(first, service.parseLogFile(file))
        assertSame(library, service.getPatternLibrary())
    }
}
