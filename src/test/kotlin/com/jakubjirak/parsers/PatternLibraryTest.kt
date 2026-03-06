/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.parsers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PatternLibraryTest {

    private lateinit var library: PatternLibrary

    @BeforeEach
    fun setUp() {
        library = PatternLibrary()
    }

    // ── parseTimestamp ──────────────────────────────────────────────────

    @Nested
    inner class ParseTimestamp {

        @Test
        fun `parses ISO 8601 format`() {
            val result = library.parseTimestamp("2024-01-15T10:30:00")
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), result)
        }

        @Test
        fun `parses Spring Boot format with millis`() {
            val result = library.parseTimestamp("2024-01-15 10:30:00.123")
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0, 123_000_000), result)
        }

        @Test
        fun `parses Spring Boot format without millis`() {
            val result = library.parseTimestamp("2024-01-15 10:30:00")
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), result)
        }

        @Test
        fun `returns null for unparseable string`() {
            assertNull(library.parseTimestamp("not a timestamp at all"))
        }

        @Test
        fun `returns null for empty string`() {
            assertNull(library.parseTimestamp(""))
        }

        @Test
        fun `returns null for random numbers`() {
            assertNull(library.parseTimestamp("12345 67890"))
        }

        @Test
        fun `uses only first 50 chars for matching`() {
            // Pad timestamp to 50 chars with trailing spaces so trim() yields the timestamp
            val timestamp = "2024-01-15T10:30:00"
            val padding = " ".repeat(50 - timestamp.length)
            val line = timestamp + padding + "extra text beyond 50 chars"
            val result = library.parseTimestamp(line)
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), result)
        }

        @Test
        fun `returns null when first 50 chars contain non-timestamp text`() {
            val line = "2024-01-15T10:30:00 " + "x".repeat(100)
            assertNull(library.parseTimestamp(line))
        }
    }

    // ── parseLevel ─────────────────────────────────────────────────────

    @Nested
    inner class ParseLevel {

        @Test
        fun `detects TRACE level`() {
            assertEquals(LogLevel.TRACE, library.parseLevel("2024-01-15 TRACE some message"))
        }

        @Test
        fun `detects DEBUG level`() {
            assertEquals(LogLevel.DEBUG, library.parseLevel("2024-01-15 DEBUG some message"))
        }

        @Test
        fun `detects INFO level`() {
            assertEquals(LogLevel.INFO, library.parseLevel("2024-01-15 INFO some message"))
        }

        @Test
        fun `detects WARN level`() {
            assertEquals(LogLevel.WARN, library.parseLevel("2024-01-15 WARN some message"))
        }

        @Test
        fun `maps WARNING to WARN`() {
            assertEquals(LogLevel.WARN, library.parseLevel("2024-01-15 WARNING some message"))
        }

        @Test
        fun `detects ERROR level`() {
            assertEquals(LogLevel.ERROR, library.parseLevel("2024-01-15 ERROR some message"))
        }

        @Test
        fun `maps SEVERE to ERROR`() {
            assertEquals(LogLevel.ERROR, library.parseLevel("2024-01-15 SEVERE some message"))
        }

        @Test
        fun `detects FATAL level`() {
            assertEquals(LogLevel.FATAL, library.parseLevel("2024-01-15 FATAL some message"))
        }

        @Test
        fun `returns UNKNOWN for unrecognised level`() {
            assertEquals(LogLevel.UNKNOWN, library.parseLevel("just a plain line without a level"))
        }

        @Test
        fun `level detection is case insensitive`() {
            assertEquals(LogLevel.ERROR, library.parseLevel("2024-01-15 error some message"))
        }

        @Test
        fun `returns UNKNOWN for empty string`() {
            assertEquals(LogLevel.UNKNOWN, library.parseLevel(""))
        }
    }

    // ── extractCorrelationId ───────────────────────────────────────────

    @Nested
    inner class ExtractCorrelationId {

        @Test
        fun `extracts X-Request-ID`() {
            val line = "X-Request-ID: abc12345-def6-7890-abcd-ef1234567890"
            val result = library.extractCorrelationId(line)
            assertEquals("abc12345-def6-7890-abcd-ef1234567890", result)
        }

        @Test
        fun `extracts traceId`() {
            val line = "traceId=abc12345"
            val result = library.extractCorrelationId(line)
            assertEquals("abc12345", result)
        }

        @Test
        fun `extracts UUID pattern`() {
            val line = "Processing request a1b2c3d4-e5f6-7890-abcd-ef1234567890 started"
            val result = library.extractCorrelationId(line)
            assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", result)
        }

        @Test
        fun `extracts X-Correlation-ID`() {
            val line = "X-Correlation-ID: deadbeef-1234-5678-9abc-def012345678"
            val result = library.extractCorrelationId(line)
            assertEquals("deadbeef-1234-5678-9abc-def012345678", result)
        }

        @Test
        fun `extracts correlationId`() {
            val line = "correlationId=abcdef12"
            val result = library.extractCorrelationId(line)
            assertEquals("abcdef12", result)
        }

        @Test
        fun `extracts requestId`() {
            val line = "requestId: abcdef12"
            val result = library.extractCorrelationId(line)
            assertEquals("abcdef12", result)
        }

        @Test
        fun `returns null for line without correlation id`() {
            assertNull(library.extractCorrelationId("Plain log line with no id"))
        }

        @Test
        fun `returns null for empty string`() {
            assertNull(library.extractCorrelationId(""))
        }
    }

    // ── extractThreadName ──────────────────────────────────────────────

    @Nested
    inner class ExtractThreadName {

        @Test
        fun `extracts main thread`() {
            val line = "2024-01-15 [main] INFO some message"
            assertEquals("main", library.extractThreadName(line))
        }

        @Test
        fun `extracts http nio thread`() {
            val line = "2024-01-15 [http-nio-8080] INFO handling request"
            assertEquals("http-nio-8080", library.extractThreadName(line))
        }

        @Test
        fun `extracts thread with numbers`() {
            val line = "[pool-3-thread-7] Processing item"
            assertEquals("pool-3-thread-7", library.extractThreadName(line))
        }

        @Test
        fun `returns null when no brackets present`() {
            assertNull(library.extractThreadName("No thread info here"))
        }

        @Test
        fun `returns null for empty string`() {
            assertNull(library.extractThreadName(""))
        }
    }

    // ── extractLoggerName ──────────────────────────────────────────────

    @Nested
    inner class ExtractLoggerName {

        @Test
        fun `extracts fully qualified class name`() {
            val line = "2024-01-15 INFO com.example.MyClass - Starting up"
            val result = library.extractLoggerName(line)
            assertEquals("com.example.MyClass", result)
        }

        @Test
        fun `extracts nested package name`() {
            val line = "org.springframework.boot.SpringApplication started"
            val result = library.extractLoggerName(line)
            assertEquals("org.springframework.boot.SpringApplication", result)
        }

        @Test
        fun `returns null for line without logger name`() {
            assertNull(library.extractLoggerName("WARN simple message"))
        }

        @Test
        fun `returns null for empty string`() {
            assertNull(library.extractLoggerName(""))
        }
    }

    // ── isStackTraceLine ───────────────────────────────────────────────

    @Nested
    inner class IsStackTraceLine {

        @Test
        fun `identifies at line`() {
            assertTrue(library.isStackTraceLine("at com.foo.Bar.method(Bar.java:42)"))
        }

        @Test
        fun `identifies indented at line`() {
            assertTrue(library.isStackTraceLine("    at com.foo.Bar.method(Bar.java:42)"))
        }

        @Test
        fun `identifies Caused by line`() {
            assertTrue(library.isStackTraceLine("Caused by: java.lang.NullPointerException"))
        }

        @Test
        fun `identifies ellipsis more line`() {
            assertTrue(library.isStackTraceLine("... 15 more"))
        }

        @Test
        fun `identifies indented ellipsis more line`() {
            assertTrue(library.isStackTraceLine("    ... 15 more"))
        }

        @Test
        fun `returns false for normal log line`() {
            assertFalse(library.isStackTraceLine("2024-01-15 INFO application started"))
        }

        @Test
        fun `returns false for empty string`() {
            assertFalse(library.isStackTraceLine(""))
        }

        @Test
        fun `returns false for line starting with a word at`() {
            // "at " only matches when trimStart produces a line starting with "at "
            // The word "atmosphere" starts with "at" but not "at " so it should not match
            assertFalse(library.isStackTraceLine("atmosphere is clear today"))
        }
    }
}
