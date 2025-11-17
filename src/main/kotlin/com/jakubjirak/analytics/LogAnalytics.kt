package com.jakubjirak.analytics

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class LogStatistics(
    val totalEntries: Int,
    val errorCount: Int,
    val warnCount: Int,
    val infoCount: Int,
    val debugCount: Int,
    val errorRate: Double,
    val averageEntriesPerMinute: Double,
    val topErrors: List<ErrorPattern>,
    val correlationIdCount: Int,
    val uniqueThreads: Int
)

data class ErrorPattern(
    val message: String,
    val count: Int,
    val percentage: Double
)

data class TimeSeriesData(
    val timestamp: LocalDateTime,
    val errorCount: Int,
    val warnCount: Int,
    val infoCount: Int
)

class LogAnalytics {
    fun calculateStatistics(entries: List<LogEntry>): LogStatistics {
        if (entries.isEmpty()) {
            return LogStatistics(0, 0, 0, 0, 0, 0.0, 0.0, emptyList(), 0, 0)
        }
        
        val errorCount = entries.count { it.level == LogLevel.ERROR || it.level == LogLevel.FATAL }
        val warnCount = entries.count { it.level == LogLevel.WARN }
        val infoCount = entries.count { it.level == LogLevel.INFO }
        val debugCount = entries.count { it.level == LogLevel.DEBUG || it.level == LogLevel.TRACE }
        
        return LogStatistics(
            totalEntries = entries.size,
            errorCount = errorCount,
            warnCount = warnCount,
            infoCount = infoCount,
            debugCount = debugCount,
            errorRate = if (entries.isNotEmpty()) errorCount.toDouble() / entries.size * 100 else 0.0,
            averageEntriesPerMinute = 0.0,
            topErrors = emptyList(),
            correlationIdCount = entries.count { it.correlationId != null },
            uniqueThreads = entries.mapNotNull { it.threadName }.toSet().size
        )
    }
}
