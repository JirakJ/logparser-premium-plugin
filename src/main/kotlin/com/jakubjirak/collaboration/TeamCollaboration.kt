package com.jakubjirak.collaboration

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogFilter
import java.time.LocalDateTime
import java.util.UUID

data class SharedQuery(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val filter: LogFilter,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val sharedWith: List<String> = emptyList()
)

data class Annotation(
    val id: String = UUID.randomUUID().toString(),
    val logEntry: LogEntry,
    val author: String,
    val comment: String,
    val createdAt: LocalDateTime,
    val tags: List<String> = emptyList(),
    val severity: AnnotationSeverity = AnnotationSeverity.INFO
)

enum class AnnotationSeverity {
    INFO, IMPORTANT, CRITICAL
}

data class IncidentReport(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val severity: IncidentSeverity,
    val affectedSystems: List<String>,
    val logEntries: List<LogEntry>,
    val annotations: List<Annotation>,
    val timeline: List<TimelineEvent>,
    val resolution: String? = null,
    val resolvedAt: LocalDateTime? = null
)

enum class IncidentSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class TimelineEvent(
    val timestamp: LocalDateTime,
    val event: String,
    val author: String,
    val details: Map<String, String> = emptyMap()
)

data class QueryTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val description: String,
    val filter: LogFilter,
    val usageCount: Int = 0,
    val rating: Double = 0.0
)

class TeamCollaboration {
    
    private val sharedQueries = mutableMapOf<String, SharedQuery>()
    private val annotations = mutableMapOf<String, MutableList<Annotation>>()
    private val incidentReports = mutableMapOf<String, IncidentReport>()
    private val queryTemplates = mutableMapOf<String, QueryTemplate>()
    
    // Shared Queries
    fun saveQuery(query: SharedQuery): Boolean {
        sharedQueries[query.id] = query
        return true
    }
    
    fun getQuery(id: String): SharedQuery? = sharedQueries[id]
    
    fun getQueriesForUser(username: String): List<SharedQuery> {
        return sharedQueries.values.filter { query ->
            query.createdBy == username || 
            query.isPublic || 
            query.sharedWith.contains(username)
        }
    }
    
    fun getPublicQueries(): List<SharedQuery> {
        return sharedQueries.values.filter { it.isPublic }
    }
    
    fun shareQuery(queryId: String, usernames: List<String>): Boolean {
        val query = sharedQueries[queryId] ?: return false
        sharedQueries[queryId] = query.copy(
            sharedWith = (query.sharedWith + usernames).distinct()
        )
        return true
    }
    
    // Annotations
    fun addAnnotation(logEntry: LogEntry, annotation: Annotation): Boolean {
        val key = "${logEntry.lineNumber}-${logEntry.rawLine.hashCode()}"
        annotations.getOrPut(key) { mutableListOf() }.add(annotation)
        return true
    }
    
    fun getAnnotations(logEntry: LogEntry): List<Annotation> {
        val key = "${logEntry.lineNumber}-${logEntry.rawLine.hashCode()}"
        return annotations[key] ?: emptyList()
    }
    
    fun getAllAnnotations(): List<Annotation> {
        return annotations.values.flatten()
    }
    
    fun deleteAnnotation(annotationId: String): Boolean {
        annotations.values.forEach { list ->
            list.removeIf { it.id == annotationId }
        }
        return true
    }
    
    // Incident Reports
    fun createIncidentReport(report: IncidentReport): String {
        incidentReports[report.id] = report
        return report.id
    }
    
    fun getIncidentReport(id: String): IncidentReport? = incidentReports[id]
    
    fun getAllIncidentReports(): List<IncidentReport> {
        return incidentReports.values.toList()
    }
    
    fun updateIncidentReport(id: String, update: (IncidentReport) -> IncidentReport): Boolean {
        val report = incidentReports[id] ?: return false
        incidentReports[id] = update(report)
        return true
    }
    
    fun addTimelineEvent(incidentId: String, event: TimelineEvent): Boolean {
        return updateIncidentReport(incidentId) { report ->
            report.copy(timeline = report.timeline + event)
        }
    }
    
    fun resolveIncident(incidentId: String, resolution: String): Boolean {
        return updateIncidentReport(incidentId) { report ->
            report.copy(
                resolution = resolution,
                resolvedAt = LocalDateTime.now()
            )
        }
    }
    
    // Query Templates
    fun addTemplate(template: QueryTemplate): Boolean {
        queryTemplates[template.id] = template
        return true
    }
    
    fun getTemplate(id: String): QueryTemplate? = queryTemplates[id]
    
    fun getTemplatesByCategory(category: String): List<QueryTemplate> {
        return queryTemplates.values.filter { it.category == category }
    }
    
    fun getAllTemplates(): List<QueryTemplate> {
        return queryTemplates.values.toList()
    }
    
    fun incrementTemplateUsage(templateId: String) {
        queryTemplates[templateId]?.let { template ->
            queryTemplates[templateId] = template.copy(usageCount = template.usageCount + 1)
        }
    }
    
    fun rateTemplate(templateId: String, rating: Double) {
        queryTemplates[templateId]?.let { template ->
            val newRating = (template.rating * template.usageCount + rating) / (template.usageCount + 1)
            queryTemplates[templateId] = template.copy(rating = newRating)
        }
    }
    
    // Export incident report
    fun exportIncidentReport(incidentId: String): String {
        val report = incidentReports[incidentId] ?: return ""
        
        val sb = StringBuilder()
        sb.appendLine("=== INCIDENT REPORT ===")
        sb.appendLine()
        sb.appendLine("Title: ${report.title}")
        sb.appendLine("Severity: ${report.severity}")
        sb.appendLine("Created by: ${report.createdBy}")
        sb.appendLine("Created at: ${report.createdAt}")
        sb.appendLine("Affected Systems: ${report.affectedSystems.joinToString(", ")}")
        sb.appendLine()
        sb.appendLine("Description:")
        sb.appendLine(report.description)
        sb.appendLine()
        
        sb.appendLine("Timeline:")
        report.timeline.forEach { event ->
            sb.appendLine("[${event.timestamp}] ${event.author}: ${event.event}")
        }
        sb.appendLine()
        
        sb.appendLine("Log Entries (${report.logEntries.size}):")
        report.logEntries.take(50).forEach { entry ->
            sb.appendLine("${entry.timestamp} [${entry.level}] ${entry.message}")
        }
        
        if (report.annotations.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Annotations:")
            report.annotations.forEach { annotation ->
                sb.appendLine("[${annotation.author}] ${annotation.comment}")
            }
        }
        
        if (report.resolution != null) {
            sb.appendLine()
            sb.appendLine("Resolution:")
            sb.appendLine(report.resolution)
            sb.appendLine("Resolved at: ${report.resolvedAt}")
        }
        
        return sb.toString()
    }
}

// Predefined query templates
object PredefinedTemplates {
    
    fun getDefaultTemplates(): List<QueryTemplate> = listOf(
        QueryTemplate(
            name = "All Errors in Last Hour",
            category = "Error Analysis",
            description = "Show all ERROR and FATAL level logs from the last hour",
            filter = LogFilter(
                levels = setOf(com.jakubjirak.parsers.LogLevel.ERROR, com.jakubjirak.parsers.LogLevel.FATAL)
            )
        ),
        QueryTemplate(
            name = "SQL Exceptions",
            category = "Database",
            description = "Find all SQL-related exceptions",
            filter = LogFilter(
                keywords = listOf("SQLException", "SQLIntegrityConstraintViolationException")
            )
        ),
        QueryTemplate(
            name = "Slow Queries",
            category = "Performance",
            description = "Find database queries taking more than 1 second",
            filter = LogFilter(
                customPattern = Regex("query.*took.*[1-9]\\d{3,}ms")
            )
        ),
        QueryTemplate(
            name = "Memory Issues",
            category = "System",
            description = "Find memory-related errors",
            filter = LogFilter(
                keywords = listOf("OutOfMemoryError", "GC overhead", "heap space")
            )
        ),
        QueryTemplate(
            name = "Failed Requests by Correlation ID",
            category = "Distributed Tracing",
            description = "Find failed requests with correlation tracking",
            filter = LogFilter(
                levels = setOf(com.jakubjirak.parsers.LogLevel.ERROR)
            )
        )
    )
}
