package com.jakubjirak.parsers

import java.time.LocalDateTime

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN
}

data class LogEntry(
    val lineNumber: Int,
    val timestamp: LocalDateTime?,
    val level: LogLevel,
    val message: String,
    val threadName: String?,
    val loggerName: String?,
    val correlationId: String?,
    val stackTrace: String?,
    val rawLine: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    fun matches(filter: LogFilter): Boolean {
        // Level filter
        if (filter.levels.isNotEmpty() && level !in filter.levels) {
            return false
        }
        
        // Keyword filter
        if (filter.keywords.isNotEmpty()) {
            val searchText = (message + (stackTrace ?: "")).lowercase()
            val matchesAny = filter.keywords.any { keyword ->
                searchText.contains(keyword.lowercase())
            }
            if (!matchesAny) return false
        }
        
        // Time range filter
        if (timestamp != null) {
            if (filter.startTime != null && timestamp.isBefore(filter.startTime)) {
                return false
            }
            if (filter.endTime != null && timestamp.isAfter(filter.endTime)) {
                return false
            }
        }
        
        // Correlation ID filter
        if (filter.correlationId != null && correlationId != filter.correlationId) {
            return false
        }
        
        // Thread name filter
        if (filter.threadName != null && threadName != filter.threadName) {
            return false
        }
        
        return true
    }
}

data class LogFilter(
    val levels: Set<LogLevel> = emptySet(),
    val keywords: List<String> = emptyList(),
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val correlationId: String? = null,
    val threadName: String? = null,
    val customPattern: Regex? = null
)
