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

class LogClassifierTest {

    private lateinit var classifier: LogClassifier

    @BeforeEach
    fun setUp() {
        classifier = LogClassifier()
    }

    @Suppress("LongParameterList")
    private fun entry(
        message: String,
        level: LogLevel = LogLevel.INFO,
        timestamp: LocalDateTime? = LocalDateTime.of(2024, 6, 15, 12, 0, 0),
        stackTrace: String? = null,
        correlationId: String? = null,
        rawLine: String = message
    ) = LogEntry(
        lineNumber = 1,
        timestamp = timestamp,
        level = level,
        message = message,
        threadName = null,
        loggerName = null,
        correlationId = correlationId,
        stackTrace = stackTrace,
        rawLine = rawLine
    )

    // ── classifyLog ────────────────────────────────────────────────────

    @Nested
    inner class ClassifyLog {

        @Test
        fun `classifies database keywords`() {
            val result = classifier.classifyLog(entry("Failed to execute sql query on database"))
            val categories = result.map { it.name }
            assertTrue("database" in categories)
        }

        @Test
        fun `database category includes matched keywords`() {
            val result = classifier.classifyLog(entry("sql query failed on jdbc connection"))
            val db = result.first { it.name == "database" }
            assertTrue(db.keywords.containsAll(listOf("sql", "query", "jdbc", "connection")))
        }

        @Test
        fun `classifies network keywords`() {
            val result = classifier.classifyLog(entry("http socket timeout on tcp connection"))
            val categories = result.map { it.name }
            assertTrue("network" in categories)
        }

        @Test
        fun `classifies security keywords`() {
            val result = classifier.classifyLog(entry("authentication failed, invalid token"))
            val categories = result.map { it.name }
            assertTrue("security" in categories)
        }

        @Test
        fun `classifies performance keywords`() {
            val result = classifier.classifyLog(entry("slow query, high memory usage, gc pause"))
            val categories = result.map { it.name }
            assertTrue("performance" in categories)
        }

        @Test
        fun `classifies business logic keywords`() {
            val result = classifier.classifyLog(entry("processing order for customer payment"))
            val categories = result.map { it.name }
            assertTrue("business_logic" in categories)
        }

        @Test
        fun `classifies system keywords`() {
            val result = classifier.classifyLog(entry("application startup configuration initialized"))
            val categories = result.map { it.name }
            assertTrue("system" in categories)
        }

        @Test
        fun `returns empty list for no matching keywords`() {
            val result = classifier.classifyLog(entry("absolutely nothing relevant here xyz"))
            assertTrue(result.isEmpty())
        }

        @Test
        fun `results are sorted by confidence descending`() {
            val result = classifier.classifyLog(
                entry("sql database jdbc hibernate query connection - also http")
            )
            assertTrue(result.size >= 2)
            assertTrue(result[0].confidence >= result[1].confidence)
        }

        @Test
        fun `confidence is ratio of matched to total keywords`() {
            val result = classifier.classifyLog(entry("sql database"))
            val db = result.first { it.name == "database" }
            assertEquals(2.0 / 6.0, db.confidence, 0.001)
        }
    }

    // ── detectAnomalies ────────────────────────────────────────────────

    @Nested
    inner class DetectAnomalies {

        @Test
        fun `returns empty for fewer than 10 entries`() {
            val entries = (1..9).map { entry("msg $it") }
            val result = classifier.detectAnomalies(entries)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty for exactly 9 entries`() {
            val entries = (1..9).map { entry("msg $it") }
            assertTrue(classifier.detectAnomalies(entries).isEmpty())
        }

        @Test
        fun `processes 10 or more entries without error`() {
            val entries = (1..15).map { entry("standard message number $it") }
            assertDoesNotThrow { classifier.detectAnomalies(entries) }
        }

        @Test
        fun `anomalies have score and reasons`() {
            val normal = (1..20).map { entry("normal short msg", level = LogLevel.INFO) }
            val outlier = entry(
                "A".repeat(500),
                level = LogLevel.ERROR,
                stackTrace = "at com.foo.Bar.baz(Bar.java:1)"
            )
            val all = normal + outlier
            val anomalies = classifier.detectAnomalies(all)

            anomalies.forEach { a ->
                assertTrue(a.score > 0.7)
                assertTrue(a.isAnomaly)
            }
        }
    }

    // ── discoverPatterns ───────────────────────────────────────────────

    @Nested
    inner class DiscoverPatterns {

        @Test
        fun `groups identical messages`() {
            val entries = (1..10).map { entry("Connection established to server") }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)

            assertEquals(1, patterns.size)
            assertEquals(10, patterns[0].frequency)
        }

        @Test
        fun `normalises numbers in patterns`() {
            val entries = (1..10).map { i -> entry("Processed $i items in ${i * 100}ms") }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)

            assertEquals(1, patterns.size)
            assertTrue(patterns[0].pattern.contains("<NUM>"))
        }

        @Test
        fun `normalises UUIDs via number replacement`() {
            val entries = (1..7).map {
                entry("Request a1b2c3d4-e5f6-7890-abcd-ef123456789$it handled")
            }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)

            assertEquals(1, patterns.size)
            assertTrue(patterns[0].pattern.contains("<NUM>"))
        }

        @Test
        fun `normalises IP addresses via number replacement`() {
            val entries = (1..6).map { entry("Connected to 192.168.1.$it on port 8080") }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)

            assertEquals(1, patterns.size)
            assertTrue(patterns[0].pattern.contains("<NUM>"))
        }

        @Test
        fun `respects minFrequency threshold`() {
            val frequent = (1..10).map { entry("repeated message") }
            val rare = (1..3).map { entry("rare message $it") }
            val patterns = classifier.discoverPatterns(frequent + rare, minFrequency = 5)

            assertEquals(1, patterns.size)
            assertEquals("repeated message", patterns[0].pattern)
        }

        @Test
        fun `returns empty when no pattern meets minFrequency`() {
            val entries = (1..4).map { entry("unique $it") }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)
            assertTrue(patterns.isEmpty())
        }

        @Test
        fun `patterns are sorted by frequency descending`() {
            val high = (1..20).map { entry("high frequency msg") }
            val medium = (1..10).map { entry("medium frequency msg") }
            val patterns = classifier.discoverPatterns(high + medium, minFrequency = 5)

            assertTrue(patterns.size >= 2)
            assertTrue(patterns[0].frequency >= patterns[1].frequency)
        }

        @Test
        fun `examples contain at most 3 raw lines`() {
            val entries = (1..10).map { entry("example line $it", rawLine = "RAW $it") }
            val patterns = classifier.discoverPatterns(entries, minFrequency = 5)

            assertTrue(patterns.isNotEmpty())
            assertTrue(patterns[0].examples.size <= 3)
        }
    }

    // ── predictLogLevel ────────────────────────────────────────────────

    @Nested
    inner class PredictLogLevel {

        @Test
        fun `predicts ERROR for error keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("critical error occurred"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `predicts ERROR for exception keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("unhandled exception in module"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `predicts ERROR for failed keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("task failed to complete"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `predicts ERROR for failure keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("connection failure detected"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `predicts ERROR for fatal keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("fatal shutdown initiated"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `predicts WARN for warn keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("warn: disk space low"))
            assertEquals(LogLevel.WARN, level)
        }

        @Test
        fun `predicts WARN for deprecated keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("method is deprecated"))
            assertEquals(LogLevel.WARN, level)
        }

        @Test
        fun `predicts WARN for slow keyword`() {
            val (level, _) = classifier.predictLogLevel(entry("slow response from API"))
            assertEquals(LogLevel.WARN, level)
        }

        @Test
        fun `predicts INFO as default for no keywords`() {
            val (level, _) = classifier.predictLogLevel(entry("something happened"))
            assertEquals(LogLevel.INFO, level)
        }

        @Test
        fun `error keywords take priority over warn keywords`() {
            val (level, _) = classifier.predictLogLevel(entry("error on slow path"))
            assertEquals(LogLevel.ERROR, level)
        }

        @Test
        fun `confidence is ratio of matched keywords`() {
            val (_, confidence) = classifier.predictLogLevel(entry("error and exception both"))
            assertEquals(2.0 / 5.0, confidence, 0.001)
        }
    }
}
