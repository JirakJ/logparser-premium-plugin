package com.jakubjirak.logparser.premium

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.time.Instant

@Service(Service.Level.PROJECT)
class ContextAwareSearch(private val project: Project) {
    
    data class SearchContext(
        val query: String,
        val timeRange: Pair<Instant, Instant>?,
        val logLevels: List<String>,
        val sources: List<String>,
        val correlationId: String?,
        val proximitySearch: Boolean = false,
        val fuzzyMatch: Boolean = false
    )
    
    data class SearchResult(
        val matches: List<Match>,
        val totalHits: Int,
        val executionTimeMs: Long,
        val suggestions: List<String>
    )
    
    data class Match(
        val lineNumber: Int,
        val content: String,
        val context: MatchContext,
        val relevanceScore: Double,
        val highlights: List<Highlight>
    )
    
    data class MatchContext(
        val before: List<String>,
        val after: List<String>,
        val relatedMatches: List<Int>
    )
    
    data class Highlight(
        val start: Int,
        val end: Int,
        val type: HighlightType
    )
    
    enum class HighlightType {
        EXACT_MATCH,
        FUZZY_MATCH,
        CONTEXT_MATCH,
        CORRELATION_MATCH
    }
    
    fun search(logContent: String, context: SearchContext): SearchResult {
        val startTime = System.currentTimeMillis()
        val lines = logContent.lines()
        val matches = mutableListOf<Match>()
        
        lines.forEachIndexed { index, line ->
            if (matchesContext(line, context)) {
                val contextBefore = lines.subList(
                    maxOf(0, index - 3),
                    index
                )
                val contextAfter = lines.subList(
                    minOf(index + 1, lines.size),
                    minOf(index + 4, lines.size)
                )
                
                matches.add(
                    Match(
                        lineNumber = index + 1,
                        content = line,
                        context = MatchContext(contextBefore, contextAfter, emptyList()),
                        relevanceScore = calculateRelevance(line, context),
                        highlights = findHighlights(line, context)
                    )
                )
            }
        }
        
        val executionTime = System.currentTimeMillis() - startTime
        
        return SearchResult(
            matches = matches.sortedByDescending { it.relevanceScore },
            totalHits = matches.size,
            executionTimeMs = executionTime,
            suggestions = generateSuggestions(context, matches)
        )
    }
    
    private fun matchesContext(line: String, context: SearchContext): Boolean {
        var matches = context.query.isEmpty() || line.contains(context.query, ignoreCase = true)
        
        if (context.logLevels.isNotEmpty()) {
            matches = matches && context.logLevels.any { line.contains(it) }
        }
        
        if (context.sources.isNotEmpty()) {
            matches = matches && context.sources.any { line.contains(it) }
        }
        
        if (context.correlationId != null) {
            matches = matches && line.contains(context.correlationId)
        }
        
        return matches
    }
    
    private fun calculateRelevance(line: String, context: SearchContext): Double {
        var score = 0.0
        
        if (line.contains(context.query, ignoreCase = true)) {
            score += 1.0
        }
        
        context.logLevels.forEach { level ->
            if (line.contains(level)) {
                score += 0.5
            }
        }
        
        if (context.correlationId != null && line.contains(context.correlationId)) {
            score += 2.0
        }
        
        return score
    }
    
    private fun findHighlights(line: String, context: SearchContext): List<Highlight> {
        val highlights = mutableListOf<Highlight>()
        
        val index = line.indexOf(context.query, ignoreCase = true)
        if (index >= 0) {
            highlights.add(
                Highlight(
                    start = index,
                    end = index + context.query.length,
                    type = HighlightType.EXACT_MATCH
                )
            )
        }
        
        return highlights
    }
    
    private fun generateSuggestions(context: SearchContext, matches: List<Match>): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (matches.isEmpty()) {
            suggestions.add("Try broadening your search criteria")
            if (context.fuzzyMatch.not()) {
                suggestions.add("Enable fuzzy matching for similar results")
            }
        }
        
        if (matches.size > 1000) {
            suggestions.add("Too many results. Consider narrowing time range")
        }
        
        return suggestions
    }
}
