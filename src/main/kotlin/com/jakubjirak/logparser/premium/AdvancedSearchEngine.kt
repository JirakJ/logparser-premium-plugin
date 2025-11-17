package com.jakubjirak.logparser.premium

import com.jakubjirak.logparser.licensing.LicenseManager
import java.util.regex.Pattern

/**
 * Advanced search engine with regex, fuzzy search, and query building
 * PREMIUM FEATURE
 */
class AdvancedSearchEngine(private val licenseManager: LicenseManager) {
    
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
    
    data class TimeRange(val start: Long, val end: Long)
    
    data class SearchResult(
        val lineNumber: Int,
        val content: String,
        val contextBefore: List<String>,
        val contextAfter: List<String>,
        val matchScore: Double = 1.0,
        val highlights: List<Pair<Int, Int>>
    )
    
    fun search(logs: List<String>, query: SearchQuery): List<SearchResult> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Advanced search requires Premium license")
        }
        
        val results = mutableListOf<SearchResult>()
        
        for (i in logs.indices) {
            val line = logs[i]
            val match = when {
                query.isRegex -> matchRegex(line, query.pattern, query.isCaseSensitive)
                query.isFuzzy -> matchFuzzy(line, query.pattern, query.fuzzyThreshold)
                else -> matchSimple(line, query.pattern, query.isCaseSensitive)
            }
            
            if (match != null) {
                if (query.logLevels.isNotEmpty() && !matchesLogLevel(line, query.logLevels)) continue
                if (query.threadFilter != null && !matchesThread(line, query.threadFilter)) continue
                
                val contextBefore = getContext(logs, i - query.contextLines, i)
                val contextAfter = getContext(logs, i + 1, i + query.contextLines + 1)
                
                results.add(SearchResult(
                    lineNumber = i,
                    content = line,
                    contextBefore = contextBefore,
                    contextAfter = contextAfter,
                    matchScore = match.score,
                    highlights = match.highlights
                ))
            }
        }
        
        return results
    }
    
    private data class MatchResult(val score: Double, val highlights: List<Pair<Int, Int>>)
    
    private fun matchRegex(line: String, pattern: String, caseSensitive: Boolean): MatchResult? {
        val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
        val regex = Pattern.compile(pattern, flags)
        val matcher = regex.matcher(line)
        
        if (matcher.find()) {
            val highlights = mutableListOf<Pair<Int, Int>>()
            do {
                highlights.add(Pair(matcher.start(), matcher.end()))
            } while (matcher.find())
            return MatchResult(1.0, highlights)
        }
        return null
    }
    
    private fun matchSimple(line: String, pattern: String, caseSensitive: Boolean): MatchResult? {
        val searchLine = if (caseSensitive) line else line.lowercase()
        val searchPattern = if (caseSensitive) pattern else pattern.lowercase()
        
        val index = searchLine.indexOf(searchPattern)
        if (index >= 0) {
            return MatchResult(1.0, listOf(Pair(index, index + pattern.length)))
        }
        return null
    }
    
    private fun matchFuzzy(line: String, pattern: String, threshold: Double): MatchResult? {
        val words = line.split("\\s+".toRegex())
        var bestMatch: Pair<Int, Double>? = null
        
        for ((index, word) in words.withIndex()) {
            val similarity = calculateLevenshteinSimilarity(word.lowercase(), pattern.lowercase())
            if (similarity >= threshold) {
                if (bestMatch == null || similarity > bestMatch.second) {
                    bestMatch = Pair(index, similarity)
                }
            }
        }
        
        if (bestMatch != null) {
            val wordIndex = words.take(bestMatch.first).sumOf { it.length + 1 }
            val wordLength = words[bestMatch.first].length
            return MatchResult(bestMatch.second, listOf(Pair(wordIndex, wordIndex + wordLength)))
        }
        return null
    }
    
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    private fun matchesLogLevel(line: String, levels: Set<String>): Boolean {
        return levels.any { level ->
            line.contains(level, ignoreCase = true) ||
            line.contains("[$level]", ignoreCase = true) ||
            line.contains("$level:", ignoreCase = true)
        }
    }
    
    private fun matchesThread(line: String, threadFilter: String): Boolean {
        return line.contains(threadFilter)
    }
    
    private fun getContext(logs: List<String>, start: Int, end: Int): List<String> {
        val validStart = maxOf(0, start)
        val validEnd = minOf(logs.size, end)
        return if (validStart < validEnd) logs.subList(validStart, validEnd) else emptyList()
    }
    
    fun buildQuery(builder: SearchQueryBuilder.() -> Unit): SearchQuery {
        return SearchQueryBuilder().apply(builder).build()
    }
    
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
        fun fuzzy(threshold: Double = 0.8) { 
            isFuzzy = true
            fuzzyThreshold = threshold
        }
        fun context(lines: Int) { contextLines = lines }
        fun levels(vararg levels: String) { logLevels = levels.toSet() }
        fun timeRange(start: Long, end: Long) { timeRange = TimeRange(start, end) }
        fun thread(filter: String) { threadFilter = filter }
        
        fun build() = SearchQuery(
            pattern, isRegex, isCaseSensitive, isFuzzy, fuzzyThreshold,
            contextLines, logLevels, timeRange, threadFilter
        )
    }
}
