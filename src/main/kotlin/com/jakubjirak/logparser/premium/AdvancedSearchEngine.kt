/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.premium

import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import java.util.regex.Pattern

/**
 * Advanced search engine supporting regex, simple substring, and fuzzy (Levenshtein) matching.
 *
 * All search methods require [PremiumFeature.ADVANCED_SEARCH].
 */
class AdvancedSearchEngine(private val licenseManager: LicenseManager) {

    /** Describes a search query with all filtering/matching options. */
    data class SearchQuery(
        val pattern: String,
        val isRegex: Boolean = false,
        val isCaseSensitive: Boolean = false,
        val isFuzzy: Boolean = false,
        val fuzzyThreshold: Double = 0.8,
        val contextLines: Int = 2,
        val logLevels: Set<String> = emptySet(),
        val timeRange: TimeRange? = null,
        val threadFilter: String? = null
    )

    /** Inclusive time range filter. */
    data class TimeRange(val start: Long, val end: Long)

    /** A single search hit with surrounding context and highlight positions. */
    data class SearchResult(
        val lineNumber: Int,
        val content: String,
        val contextBefore: List<String>,
        val contextAfter: List<String>,
        val matchScore: Double = 1.0,
        val highlights: List<Pair<Int, Int>>
    )

    /**
     * Searches [logs] according to [query] and returns matching lines with context.
     *
     * @throws IllegalStateException if the user does not hold a premium license
     */
    fun search(logs: List<String>, query: SearchQuery): List<SearchResult> {
        check(licenseManager.canUseFeature(PremiumFeature.ADVANCED_SEARCH)) {
            "Advanced search requires a Premium license"
        }

        return logs.indices
            .mapNotNull { i -> matchLine(logs, i, query) }
    }

    private fun matchLine(logs: List<String>, index: Int, query: SearchQuery): SearchResult? {
        val line = logs[index]
        val match = when {
            query.isRegex -> matchRegex(line, query.pattern, query.isCaseSensitive)
            query.isFuzzy -> matchFuzzy(line, query.pattern, query.fuzzyThreshold)
            else -> matchSimple(line, query.pattern, query.isCaseSensitive)
        } ?: return null

        if (query.logLevels.isNotEmpty() && !matchesLogLevel(line, query.logLevels)) return null
        if (query.threadFilter != null && !line.contains(query.threadFilter)) return null

        return SearchResult(
            lineNumber = index,
            content = line,
            contextBefore = getContext(logs, index - query.contextLines, index),
            contextAfter = getContext(logs, index + 1, index + query.contextLines + 1),
            matchScore = match.score,
            highlights = match.highlights
        )
    }

    private data class MatchResult(val score: Double, val highlights: List<Pair<Int, Int>>)

    private fun matchRegex(line: String, pattern: String, caseSensitive: Boolean): MatchResult? {
        val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
        val matcher = Pattern.compile(pattern, flags).matcher(line)
        if (!matcher.find()) return null

        val highlights = mutableListOf<Pair<Int, Int>>()
        do {
            highlights.add(matcher.start() to matcher.end())
        } while (matcher.find())
        return MatchResult(1.0, highlights)
    }

    private fun matchSimple(line: String, pattern: String, caseSensitive: Boolean): MatchResult? {
        val searchLine = if (caseSensitive) line else line.lowercase()
        val searchPattern = if (caseSensitive) pattern else pattern.lowercase()
        val index = searchLine.indexOf(searchPattern)
        if (index < 0) return null
        return MatchResult(1.0, listOf(index to index + pattern.length))
    }

    private fun matchFuzzy(line: String, pattern: String, threshold: Double): MatchResult? {
        val words = line.split("\\s+".toRegex())
        var bestMatch: Pair<Int, Double>? = null

        for ((index, word) in words.withIndex()) {
            val similarity = calculateLevenshteinSimilarity(word.lowercase(), pattern.lowercase())
            if (similarity >= threshold && (bestMatch == null || similarity > bestMatch.second)) {
                bestMatch = index to similarity
            }
        }

        if (bestMatch != null) {
            val wordIndex = words.take(bestMatch.first).sumOf { it.length + 1 }
            val wordLength = words[bestMatch.first].length
            return MatchResult(bestMatch.second, listOf(wordIndex to wordIndex + wordLength))
        }
        return null
    }

    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }

    internal fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun matchesLogLevel(line: String, levels: Set<String>): Boolean {
        return levels.any { level -> line.contains(level, ignoreCase = true) }
    }

    private fun getContext(logs: List<String>, start: Int, end: Int): List<String> {
        val validStart = maxOf(0, start)
        val validEnd = minOf(logs.size, end)
        return if (validStart < validEnd) logs.subList(validStart, validEnd) else emptyList()
    }

    /** DSL-style query builder. */
    fun buildQuery(builder: SearchQueryBuilder.() -> Unit): SearchQuery {
        return SearchQueryBuilder().apply(builder).build()
    }

    /** Builder for constructing [SearchQuery] instances. */
    class SearchQueryBuilder {
        private var pattern: String = ""
        private var isRegex: Boolean = false
        private var isCaseSensitive: Boolean = false
        private var isFuzzy: Boolean = false
        private var fuzzyThreshold: Double = 0.8
        private var contextLines: Int = 2
        private var logLevels: Set<String> = emptySet()
        private var timeRange: TimeRange? = null
        private var threadFilter: String? = null

        fun pattern(value: String) { pattern = value }
        fun regex() { isRegex = true }
        fun caseSensitive() { isCaseSensitive = true }
        fun fuzzy(threshold: Double = 0.8) { isFuzzy = true; fuzzyThreshold = threshold }
        fun context(lines: Int) { contextLines = lines }
        fun levels(vararg levels: String) { logLevels = levels.toSet() }
        fun timeRange(start: Long, end: Long) { timeRange = TimeRange(start, end) }
        fun thread(filter: String) { threadFilter = filter }

        internal fun build() = SearchQuery(
            pattern, isRegex, isCaseSensitive, isFuzzy, fuzzyThreshold,
            contextLines, logLevels, timeRange, threadFilter
        )
    }
}
