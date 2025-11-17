package com.jakubjirak.patterns

import com.jakubjirak.parsers.LogEntry
import java.util.regex.Pattern

data class CustomPattern(
    val id: String,
    val name: String,
    val description: String,
    val pattern: String,
    val patternType: PatternType,
    val severity: PatternSeverity,
    val enabled: Boolean = true,
    val caseSensitive: Boolean = false,
    val actions: List<PatternAction> = emptyList()
)

enum class PatternType {
    REGEX,
    GROK,
    SIMPLE_STRING,
    EXCEPTION_TYPE
}

enum class PatternSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class PatternMatch(
    val pattern: CustomPattern,
    val logEntry: LogEntry,
    val matchedGroups: Map<String, String>,
    val timestamp: java.time.LocalDateTime
)

sealed class PatternAction {
    data class Notify(val message: String) : PatternAction()
    data class Alert(val channel: String, val message: String) : PatternAction()
    data class CreateIssue(val project: String, val summary: String) : PatternAction()
    data class ExecuteScript(val script: String) : PatternAction()
}

class CustomPatternMatcher {
    
    private val patterns = mutableMapOf<String, CustomPattern>()
    private val compiledPatterns = mutableMapOf<String, Pattern>()
    
    fun addPattern(pattern: CustomPattern) {
        patterns[pattern.id] = pattern
        if (pattern.patternType == PatternType.REGEX) {
            val flags = if (pattern.caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            compiledPatterns[pattern.id] = Pattern.compile(pattern.pattern, flags)
        }
    }
    
    fun removePattern(patternId: String) {
        patterns.remove(patternId)
        compiledPatterns.remove(patternId)
    }
    
    fun matchEntry(entry: LogEntry): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        
        patterns.values.filter { it.enabled }.forEach { pattern ->
            val match = when (pattern.patternType) {
                PatternType.REGEX -> matchRegex(entry, pattern)
                PatternType.SIMPLE_STRING -> matchSimpleString(entry, pattern)
                PatternType.EXCEPTION_TYPE -> matchException(entry, pattern)
                PatternType.GROK -> matchGrok(entry, pattern)
            }
            
            if (match != null) {
                matches.add(match)
            }
        }
        
        return matches
    }
    
    private fun matchRegex(entry: LogEntry, pattern: CustomPattern): PatternMatch? {
        val compiled = compiledPatterns[pattern.id] ?: return null
        val matcher = compiled.matcher(entry.rawLine)
        
        if (matcher.find()) {
            val groups = mutableMapOf<String, String>()
            for (i in 0..matcher.groupCount()) {
                groups["group$i"] = matcher.group(i) ?: ""
            }
            
            return PatternMatch(
                pattern = pattern,
                logEntry = entry,
                matchedGroups = groups,
                timestamp = java.time.LocalDateTime.now()
            )
        }
        
        return null
    }
    
    private fun matchSimpleString(entry: LogEntry, pattern: CustomPattern): PatternMatch? {
        val text = entry.rawLine
        val searchText = if (pattern.caseSensitive) text else text.lowercase()
        val patternText = if (pattern.caseSensitive) pattern.pattern else pattern.pattern.lowercase()
        
        return if (searchText.contains(patternText)) {
            PatternMatch(
                pattern = pattern,
                logEntry = entry,
                matchedGroups = mapOf("matched" to pattern.pattern),
                timestamp = java.time.LocalDateTime.now()
            )
        } else null
    }
    
    private fun matchException(entry: LogEntry, pattern: CustomPattern): PatternMatch? {
        val exceptionPattern = Regex("""${pattern.pattern}""")
        val match = exceptionPattern.find(entry.message)
        
        return if (match != null) {
            PatternMatch(
                pattern = pattern,
                logEntry = entry,
                matchedGroups = mapOf("exception" to match.value),
                timestamp = java.time.LocalDateTime.now()
            )
        } else null
    }
    
    private fun matchGrok(entry: LogEntry, pattern: CustomPattern): PatternMatch? {
        // Simplified Grok pattern matching
        // Full implementation would use grok-java library
        val grokPatterns = mapOf(
            "%{TIMESTAMP_ISO8601}" to """(\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d{3})?)""",
            "%{LOGLEVEL}" to """(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)""",
            "%{IP}" to """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""",
            "%{UUID}" to """([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})"""
        )
        
        var regexPattern = pattern.pattern
        grokPatterns.forEach { (grokToken, regex) ->
            regexPattern = regexPattern.replace(grokToken, regex)
        }
        
        val compiled = Pattern.compile(regexPattern)
        val matcher = compiled.matcher(entry.rawLine)
        
        return if (matcher.find()) {
            val groups = mutableMapOf<String, String>()
            for (i in 0..matcher.groupCount()) {
                groups["group$i"] = matcher.group(i) ?: ""
            }
            PatternMatch(pattern, entry, groups, java.time.LocalDateTime.now())
        } else null
    }
    
    fun getAllPatterns(): List<CustomPattern> = patterns.values.toList()
    
    fun getPattern(id: String): CustomPattern? = patterns[id]
    
    fun enablePattern(id: String) {
        patterns[id]?.let { pattern ->
            patterns[id] = pattern.copy(enabled = true)
        }
    }
    
    fun disablePattern(id: String) {
        patterns[id]?.let { pattern ->
            patterns[id] = pattern.copy(enabled = false)
        }
    }
}

// Predefined patterns library
object PredefinedPatterns {
    
    fun getDefaultPatterns(): List<CustomPattern> = listOf(
        CustomPattern(
            id = "sql-exception",
            name = "SQL Exception",
            description = "Detects SQL exceptions in logs",
            pattern = """SQLException|SQLIntegrityConstraintViolationException""",
            patternType = PatternType.REGEX,
            severity = PatternSeverity.ERROR
        ),
        CustomPattern(
            id = "connection-timeout",
            name = "Connection Timeout",
            description = "Detects connection timeout errors",
            pattern = """Connection.*timeout|SocketTimeoutException""",
            patternType = PatternType.REGEX,
            severity = PatternSeverity.ERROR
        ),
        CustomPattern(
            id = "out-of-memory",
            name = "Out of Memory",
            description = "Detects OOM errors",
            pattern = """OutOfMemoryError|java.lang.OutOfMemoryError""",
            patternType = PatternType.REGEX,
            severity = PatternSeverity.CRITICAL
        ),
        CustomPattern(
            id = "null-pointer",
            name = "Null Pointer Exception",
            description = "Detects NPE errors",
            pattern = """NullPointerException""",
            patternType = PatternType.SIMPLE_STRING,
            severity = PatternSeverity.ERROR
        ),
        CustomPattern(
            id = "http-5xx",
            name = "HTTP 5xx Errors",
            description = "Detects HTTP 500-level errors",
            pattern = """HTTP/\d\.\d" 5\d{2}""",
            patternType = PatternType.REGEX,
            severity = PatternSeverity.ERROR
        ),
        CustomPattern(
            id = "slow-query",
            name = "Slow Database Query",
            description = "Detects slow queries (>1000ms)",
            pattern = """query.*took.*[1-9]\d{3,}ms""",
            patternType = PatternType.REGEX,
            severity = PatternSeverity.WARNING
        )
    )
}
