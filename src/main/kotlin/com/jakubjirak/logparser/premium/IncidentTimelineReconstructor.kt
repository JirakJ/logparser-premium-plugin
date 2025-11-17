package com.jakubjirak.logparser.premium

import com.intellij.openapi.project.Project
import com.jakubjirak.parsers.LogEntry
import java.time.Duration
import java.time.Instant

/**
 * Premium Feature: Reconstruct incident timeline from distributed logs
 * Creates visual timeline showing cascading failures and root cause
 */
class IncidentTimelineReconstructor(private val project: Project) {
    
    data class Incident(
        val id: String,
        val startTime: Instant,
        val endTime: Instant?,
        val severity: Severity,
        val rootCause: RootCause?,
        val timeline: List<TimelineEvent>,
        val impactedServices: Set<String>,
        val correlationIds: Set<String>
    )
    
    enum class Severity { CRITICAL, HIGH, MEDIUM, LOW }
    
    data class RootCause(
        val service: String,
        val timestamp: Instant,
        val error: String,
        val confidence: Double
    )
    
    data class TimelineEvent(
        val timestamp: Instant,
        val type: EventType,
        val service: String,
        val message: String,
        val logEntry: LogEntry
    )
    
    enum class EventType {
        ERROR_OCCURRED, SERVICE_DEGRADED, TIMEOUT, CONNECTION_FAILED, SERVICE_RECOVERED
    }
    
    fun reconstructIncident(logs: List<LogEntry>, incidentStartTime: Instant): Incident {
        val relevantLogs = logs.filter { 
            it.timestamp >= incidentStartTime && it.level in listOf("ERROR", "WARN", "FATAL")
        }
        val events = relevantLogs.map { log ->
            TimelineEvent(
                timestamp = log.timestamp,
                type = classifyEventType(log),
                service = log.source,
                message = log.message,
                logEntry = log
            )
        }
        
        val firstError = events.minByOrNull { it.timestamp }
        val rootCause = firstError?.let {
            RootCause(it.service, it.timestamp, it.message, 0.8)
        }
        
        return Incident(
            id = "INC-${incidentStartTime.toEpochMilli()}",
            startTime = incidentStartTime,
            endTime = events.lastOrNull { it.type == EventType.SERVICE_RECOVERED }?.timestamp,
            severity = calculateSeverity(events),
            rootCause = rootCause,
            timeline = events.sortedBy { it.timestamp },
            impactedServices = events.map { it.service }.toSet(),
            correlationIds = relevantLogs.mapNotNull { it.correlationId }.toSet()
        )
    }
    
    private fun classifyEventType(log: LogEntry): EventType {
        val msg = log.message.lowercase()
        return when {
            msg.contains("timeout") -> EventType.TIMEOUT
            msg.contains("connection") && msg.contains("failed") -> EventType.CONNECTION_FAILED
            msg.contains("recovered") -> EventType.SERVICE_RECOVERED
            else -> EventType.ERROR_OCCURRED
        }
    }
    
    private fun calculateSeverity(events: List<TimelineEvent>): Severity {
        val errorCount = events.size
        val services = events.map { it.service }.toSet().size
        return when {
            services >= 5 -> Severity.CRITICAL
            errorCount >= 100 || services >= 3 -> Severity.HIGH
            errorCount >= 10 -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }
}
