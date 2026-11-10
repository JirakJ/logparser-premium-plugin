/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.search

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SearchEngineTest {

    private lateinit var engine: SearchEngine

    @BeforeEach
    fun setUp() {
        engine = SearchEngine()
    }

    private val sampleLogs = listOf(
        "2024-01-15 10:30:00 [main] INFO Application started",
        "2024-01-15 10:30:01 [main] ERROR NullPointerException occurred",
        "2024-01-15 10:30:02 [worker-1] DEBUG Processing batch",
        "2024-01-15 10:30:03 [worker-2] WARN Slow query detected",
        "2024-01-15 10:30:04 [main] INFO Request completed successfully"
    )

    // ── Simple search ──────────────────────────────────────────────────

    @Nested
    inner class SimpleSearch {

        @Test
        fun `finds matching lines`() {
            val query = SearchEngine.SearchQuery(pattern = "ERROR")
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
            assertEquals(1, results[0].lineNumber)
        }

        @Test
        fun `case insensitive by default`() {
            val query = SearchEngine.SearchQuery(pattern = "error")
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
        }

        @Test
        fun `case sensitive when enabled`() {
            val query = SearchEngine.SearchQuery(pattern = "error", isCaseSensitive = true)
            val results = engine.search(sampleLogs, query)
            assertTrue(results.isEmpty())
        }

        @Test
        fun `returns empty for no match`() {
            val query = SearchEngine.SearchQuery(pattern = "FATAL")
            val results = engine.search(sampleLogs, query)
            assertTrue(results.isEmpty())
        }

        @Test
        fun `returns context lines`() {
            val query = SearchEngine.SearchQuery(pattern = "ERROR", contextLines = 1)
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
            assertEquals(1, results[0].contextBefore.size)
            assertEquals(1, results[0].contextAfter.size)
        }
    }

    // ── Regex search ───────────────────────────────────────────────────

    @Nested
    inner class RegexSearch {

        @Test
        fun `matches regex pattern`() {
            val query = SearchEngine.SearchQuery(pattern = "Null.*Exception", isRegex = true)
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
        }

        @Test
        fun `regex highlights match positions`() {
            val query = SearchEngine.SearchQuery(pattern = "ERROR", isRegex = true)
            val results = engine.search(sampleLogs, query)
            assertTrue(results[0].highlights.isNotEmpty())
        }

        @Test
        fun `regex returns empty for no match`() {
            val query = SearchEngine.SearchQuery(pattern = "^FATAL$", isRegex = true)
            val results = engine.search(sampleLogs, query)
            assertTrue(results.isEmpty())
        }
    }

    // ── Fuzzy search ───────────────────────────────────────────────────

    @Nested
    inner class FuzzySearch {

        @Test
        fun `matches similar words`() {
            val query = SearchEngine.SearchQuery(
                pattern = "Applicaton",  // typo: Application
                isFuzzy = true,
                fuzzyThreshold = 0.7
            )
            val results = engine.search(sampleLogs, query)
            assertTrue(results.isNotEmpty())
        }

        @Test
        fun `rejects when threshold is too high`() {
            val query = SearchEngine.SearchQuery(
                pattern = "xyz",
                isFuzzy = true,
                fuzzyThreshold = 0.99
            )
            val results = engine.search(sampleLogs, query)
            assertTrue(results.isEmpty())
        }

        @Test
        fun `fuzzy match has score less than 1`() {
            val query = SearchEngine.SearchQuery(
                pattern = "Applicaton",
                isFuzzy = true,
                fuzzyThreshold = 0.7
            )
            val results = engine.search(sampleLogs, query)
            if (results.isNotEmpty()) {
                assertTrue(results[0].matchScore < 1.0)
            }
        }
    }

    // ── Level filter ───────────────────────────────────────────────────

    @Nested
    inner class LevelFilter {

        @Test
        fun `filters by log level`() {
            val query = SearchEngine.SearchQuery(
                pattern = ".",
                isRegex = true,
                logLevels = setOf("ERROR")
            )
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
            assertTrue(results[0].content.contains("ERROR"))
        }

        @Test
        fun `filters by multiple levels`() {
            val query = SearchEngine.SearchQuery(
                pattern = ".",
                isRegex = true,
                logLevels = setOf("ERROR", "WARN")
            )
            val results = engine.search(sampleLogs, query)
            assertEquals(2, results.size)
        }
    }

    // ── Thread filter ──────────────────────────────────────────────────

    @Nested
    inner class ThreadFilter {

        @Test
        fun `filters by thread name`() {
            val query = SearchEngine.SearchQuery(
                pattern = ".",
                isRegex = true,
                threadFilter = "worker-1"
            )
            val results = engine.search(sampleLogs, query)
            assertEquals(1, results.size)
            assertTrue(results[0].content.contains("worker-1"))
        }
    }

    // ── Levenshtein distance ───────────────────────────────────────────

    @Nested
    inner class LevenshteinDistance {

        @Test
        fun `distance between identical strings is zero`() {
            assertEquals(0, engine.levenshteinDistance("hello", "hello"))
        }

        @Test
        fun `distance between empty strings is zero`() {
            assertEquals(0, engine.levenshteinDistance("", ""))
        }

        @Test
        fun `distance from empty string is length of other`() {
            assertEquals(5, engine.levenshteinDistance("", "hello"))
            assertEquals(5, engine.levenshteinDistance("hello", ""))
        }

        @Test
        fun `single substitution has distance 1`() {
            assertEquals(1, engine.levenshteinDistance("cat", "car"))
        }

        @Test
        fun `single insertion has distance 1`() {
            assertEquals(1, engine.levenshteinDistance("cat", "cats"))
        }

        @Test
        fun `single deletion has distance 1`() {
            assertEquals(1, engine.levenshteinDistance("cats", "cat"))
        }
    }

    // ── Query builder ──────────────────────────────────────────────────

    @Nested
    inner class QueryBuilder {

        @Test
        fun `builds query with DSL`() {
            val query = engine.buildQuery {
                pattern("ERROR")
                regex()
                caseSensitive()
                context(3)
                levels("ERROR", "FATAL")
                thread("main")
            }
            assertEquals("ERROR", query.pattern)
            assertTrue(query.isRegex)
            assertTrue(query.isCaseSensitive)
            assertEquals(3, query.contextLines)
            assertEquals(setOf("ERROR", "FATAL"), query.logLevels)
            assertEquals("main", query.threadFilter)
        }

        @Test
        fun `builds fuzzy query`() {
            val query = engine.buildQuery {
                pattern("test")
                fuzzy(0.6)
            }
            assertTrue(query.isFuzzy)
            assertEquals(0.6, query.fuzzyThreshold)
        }
    }
}
