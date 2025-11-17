package com.jakubjirak.logparser.premium

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Service(Service.Level.PROJECT)
class SmartSuggestionEngine(private val project: Project) {
    
    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions
    
    data class Suggestion(
        val type: SuggestionType,
        val title: String,
        val description: String,
        val action: SuggestionAction,
        val confidence: Double,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    enum class SuggestionType {
        FILTER,
        PATTERN,
        CORRELATION,
        PERFORMANCE,
        ERROR_ANALYSIS,
        EXPORT
    }
    
    data class SuggestionAction(
        val actionId: String,
        val parameters: Map<String, Any>
    )
    
    fun analyzeLogs(logContent: String): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        if (logContent.contains("ERROR") || logContent.contains("Exception")) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.ERROR_ANALYSIS,
                    title = "Error Pattern Detected",
                    description = "Found multiple error entries. Would you like to analyze error patterns?",
                    action = SuggestionAction("analyzeErrors", emptyMap()),
                    confidence = 0.9
                )
            )
        }
        
        if (logContent.contains(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.CORRELATION,
                    title = "Correlation IDs Found",
                    description = "UUID patterns detected. Track transaction flow?",
                    action = SuggestionAction("trackCorrelation", emptyMap()),
                    confidence = 0.85
                )
            )
        }
        
        if (logContent.lines().size > 10000) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.PERFORMANCE,
                    title = "Large Log File",
                    description = "Enable indexing for faster search?",
                    action = SuggestionAction("enableIndexing", emptyMap()),
                    confidence = 0.95
                )
            )
        }
        
        _suggestions.value = suggestions
        return suggestions
    }
    
    fun suggestFilters(logContent: String): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        val levels = Regex("(ERROR|WARN|INFO|DEBUG)").findAll(logContent)
            .map { it.value }.distinct().toList()
        
        if (levels.size > 1) {
            suggestions.add(
                Suggestion(
                    type = SuggestionType.FILTER,
                    title = "Multiple Log Levels",
                    description = "Filter by: ${levels.joinToString(", ")}",
                    action = SuggestionAction("applyLevelFilter", mapOf("levels" to levels)),
                    confidence = 0.8
                )
            )
        }
        
        return suggestions
    }
    
    fun suggestPatterns(logContent: String): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        val commonPatterns = listOf(
            Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}") to "ISO Timestamp",
            Regex("\\[.*?\\]") to "Thread Names",
            Regex("\\d+ms") to "Duration Metrics"
        )
        
        commonPatterns.forEach { (pattern, name) ->
            if (pattern.containsMatchIn(logContent)) {
                suggestions.add(
                    Suggestion(
                        type = SuggestionType.PATTERN,
                        title = "$name Detected",
                        description = "Extract $name for analysis?",
                        action = SuggestionAction("extractPattern", mapOf("pattern" to pattern.pattern)),
                        confidence = 0.75
                    )
                )
            }
        }
        
        return suggestions
    }
}
