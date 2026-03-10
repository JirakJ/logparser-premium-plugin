/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LogParserTest {

    private lateinit var patternLibrary: PatternLibrary

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        patternLibrary = PatternLibrary()
    }

    private fun logFile(name: String, content: String): File {
        val file = tempDir.resolve(name).toFile()
        file.writeText(content)
        return file
    }

    // ── parseLines ─────────────────────────────────────────────────────

    @Nested
    inner class ParseLines {

        @Test
        fun `parses standard log lines`() {
            val file = logFile(
                "app.log",
                """
                |2024-01-15T10:30:00 [main] INFO com.example.App - Application started
                |2024-01-15T10:30:01 [main] ERROR com.example.App - Something failed
                """.trimMargin()
            )
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(2, entries.size)
            assertEquals(LogLevel.INFO, entries[0].level)
            assertEquals(LogLevel.ERROR, entries[1].level)
            assertEquals(1, entries[0].lineNumber)
            assertEquals(2, entries[1].lineNumber)
        }

        @Test
        fun `parses empty file`() {
            val file = logFile("empty.log", "")
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertTrue(entries.isEmpty())
        }

        @Test
        fun `parses single line`() {
            val file = logFile("single.log", "2024-01-15T10:30:00 INFO startup")
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(1, entries.size)
            assertEquals(LogLevel.INFO, entries[0].level)
        }

        @Test
        fun `preserves rawLine`() {
            val rawLine = "2024-01-15T10:30:00 [main] ERROR failure occurred"
            val file = logFile("raw.log", rawLine)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(rawLine, entries[0].rawLine)
        }

        @Test
        fun `sets source to filename`() {
            val file = logFile("myapp.log", "2024-01-15T10:30:00 INFO test")
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals("myapp.log", entries[0].source)
        }
    }

    // ── Stack trace folding ────────────────────────────────────────────

    @Nested
    inner class StackTraceFolding {

        @Test
        fun `folds at lines into previous entry stackTrace`() {
            val content = """
                |2024-01-15T10:30:00 [main] ERROR NullPointerException
                |at com.foo.Bar.method(Bar.java:42)
                |at com.foo.Baz.call(Baz.java:10)
            """.trimMargin()
            val file = logFile("stack.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(1, entries.size)
            assertNotNull(entries[0].stackTrace)
            assertTrue(entries[0].stackTrace!!.contains("com.foo.Bar.method"))
            assertTrue(entries[0].stackTrace!!.contains("com.foo.Baz.call"))
        }

        @Test
        fun `folds Caused by into previous entry`() {
            val content = """
                |2024-01-15T10:30:00 [main] ERROR Exception occurred
                |Caused by: java.lang.IllegalArgumentException: bad input
            """.trimMargin()
            val file = logFile("caused.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(1, entries.size)
            assertNotNull(entries[0].stackTrace)
            assertTrue(entries[0].stackTrace!!.contains("Caused by:"))
        }

        @Test
        fun `folds ellipsis more into previous entry`() {
            val content = """
                |2024-01-15T10:30:00 [main] ERROR Exception
                |at com.foo.Bar.method(Bar.java:42)
                |... 15 more
            """.trimMargin()
            val file = logFile("more.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(1, entries.size)
            assertNotNull(entries[0].stackTrace)
            assertTrue(entries[0].stackTrace!!.contains("... 15 more"))
        }

        @Test
        fun `stack trace line without preceding entry becomes new entry`() {
            val content = "at com.foo.Bar.method(Bar.java:42)"
            val file = logFile("orphan.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(1, entries.size)
        }

        @Test
        fun `multiple log entries with interleaved stack traces`() {
            val content = """
                |2024-01-15T10:30:00 ERROR first error
                |at com.foo.First.run(First.java:1)
                |2024-01-15T10:30:01 ERROR second error
                |at com.foo.Second.run(Second.java:2)
            """.trimMargin()
            val file = logFile("multi.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines()

            assertEquals(2, entries.size)
            assertTrue(entries[0].stackTrace!!.contains("First.run"))
            assertTrue(entries[1].stackTrace!!.contains("Second.run"))
        }
    }

    // ── maxLines ───────────────────────────────────────────────────────

    @Nested
    inner class MaxLines {

        @Test
        fun `limits number of parsed lines`() {
            val content = (1..20).joinToString("\n") { "2024-01-15T10:30:00 INFO line $it" }
            val file = logFile("many.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines(maxLines = 5)

            assertEquals(5, entries.size)
        }

        @Test
        fun `maxLines greater than file lines returns all entries`() {
            val content = """
                |2024-01-15T10:30:00 INFO line 1
                |2024-01-15T10:30:01 INFO line 2
            """.trimMargin()
            val file = logFile("short.log", content)
            val parser = LogParser(file, patternLibrary)
            val entries = parser.parseLines(maxLines = 1000)

            assertEquals(2, entries.size)
        }
    }

    // ── searchCorrelationId ────────────────────────────────────────────

    @Nested
    inner class SearchCorrelationId {

        @Test
        fun `finds entries with matching correlation id`() {
            val uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            val content = """
                |2024-01-15T10:30:00 INFO request started $uuid
                |2024-01-15T10:30:01 INFO other request without id
                |2024-01-15T10:30:02 INFO request finished $uuid
            """.trimMargin()
            val file = logFile("corr.log", content)
            val parser = LogParser(file, patternLibrary)
            val results = parser.searchCorrelationId(uuid)

            assertEquals(2, results.size)
            assertTrue(results.all { it.correlationId == uuid })
        }

        @Test
        fun `returns empty list when no match`() {
            val content = "2024-01-15T10:30:00 INFO simple log"
            val file = logFile("no-corr.log", content)
            val parser = LogParser(file, patternLibrary)
            val results = parser.searchCorrelationId("non-existent-id")

            assertTrue(results.isEmpty())
        }
    }

    // ── getFileSize / getLineCount ─────────────────────────────────────

    @Nested
    inner class FileMetrics {

        @Test
        fun `getFileSize returns correct byte count`() {
            val content = "Hello, World!"
            val file = logFile("size.log", content)
            val parser = LogParser(file, patternLibrary)

            assertEquals(content.toByteArray().size.toLong(), parser.getFileSize())
        }

        @Test
        fun `getLineCount returns correct number of lines`() {
            val content = "line1\nline2\nline3"
            val file = logFile("lines.log", content)
            val parser = LogParser(file, patternLibrary)

            assertEquals(3, parser.getLineCount())
        }

        @Test
        fun `getLineCount for empty file returns zero`() {
            val file = logFile("empty.log", "")
            val parser = LogParser(file, patternLibrary)

            assertEquals(0, parser.getLineCount())
        }

        @Test
        fun `getFileSize for empty file returns zero`() {
            val file = logFile("empty2.log", "")
            val parser = LogParser(file, patternLibrary)

            assertEquals(0, parser.getFileSize())
        }

        @Test
        fun `getLineCount for single line without newline`() {
            val file = logFile("single.log", "only one line")
            val parser = LogParser(file, patternLibrary)

            assertEquals(1, parser.getLineCount())
        }
    }
}
