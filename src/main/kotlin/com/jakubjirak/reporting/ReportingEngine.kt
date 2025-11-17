package com.jakubjirak.reporting

import com.jakubjirak.analytics.LogStatistics
import com.jakubjirak.parsers.LogEntry
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Report(
    val id: String,
    val title: String,
    val type: ReportType,
    val createdAt: LocalDateTime,
    val timeRange: TimeRange,
    val sections: List<ReportSection>,
    val format: ReportFormat
)

enum class ReportType {
    DAILY_SUMMARY,
    WEEKLY_SUMMARY,
    MONTHLY_SUMMARY,
    INCIDENT_REPORT,
    PERFORMANCE_REPORT,
    SECURITY_REPORT,
    CUSTOM
}

enum class ReportFormat {
    PDF, HTML, MARKDOWN, JSON
}

data class TimeRange(
    val start: LocalDateTime,
    val end: LocalDateTime
)

sealed class ReportSection {
    abstract val title: String
    
    data class TextSection(
        override val title: String,
        val content: String
    ) : ReportSection()
    
    data class StatisticsSection(
        override val title: String,
        val statistics: LogStatistics
    ) : ReportSection()
    
    data class TableSection(
        override val title: String,
        val headers: List<String>,
        val rows: List<List<String>>
    ) : ReportSection()
    
    data class ChartSection(
        override val title: String,
        val chartType: ChartType,
        val data: Map<String, Double>
    ) : ReportSection()
    
    data class ErrorsSection(
        override val title: String,
        val errors: List<ErrorSummary>
    ) : ReportSection()
}

enum class ChartType {
    BAR, LINE, PIE, HEATMAP
}

data class ErrorSummary(
    val type: String,
    val count: Int,
    val firstOccurrence: LocalDateTime,
    val lastOccurrence: LocalDateTime,
    val examples: List<String>
)

class ReportingEngine {
    
    fun generateDailySummary(entries: List<LogEntry>, date: LocalDateTime): Report {
        val statistics = calculateStatistics(entries)
        val topErrors = findTopErrors(entries)
        
        return Report(
            id = java.util.UUID.randomUUID().toString(),
            title = "Daily Summary - ${date.format(DateTimeFormatter.ISO_DATE)}",
            type = ReportType.DAILY_SUMMARY,
            createdAt = LocalDateTime.now(),
            timeRange = TimeRange(
                date.withHour(0).withMinute(0),
                date.withHour(23).withMinute(59)
            ),
            sections = listOf(
                ReportSection.TextSection(
                    "Executive Summary",
                    generateExecutiveSummary(statistics)
                ),
                ReportSection.StatisticsSection(
                    "Statistics",
                    statistics
                ),
                ReportSection.ErrorsSection(
                    "Top Errors",
                    topErrors
                ),
                ReportSection.ChartSection(
                    "Log Level Distribution",
                    ChartType.PIE,
                    mapOf(
                        "ERROR" to statistics.errorCount.toDouble(),
                        "WARN" to statistics.warnCount.toDouble(),
                        "INFO" to statistics.infoCount.toDouble(),
                        "DEBUG" to statistics.debugCount.toDouble()
                    )
                )
            ),
            format = ReportFormat.HTML
        )
    }
    
    fun generateWeeklySummary(entries: List<LogEntry>, weekStart: LocalDateTime): Report {
        val statistics = calculateStatistics(entries)
        
        return Report(
            id = java.util.UUID.randomUUID().toString(),
            title = "Weekly Summary - Week of ${weekStart.format(DateTimeFormatter.ISO_DATE)}",
            type = ReportType.WEEKLY_SUMMARY,
            createdAt = LocalDateTime.now(),
            timeRange = TimeRange(weekStart, weekStart.plusDays(7)),
            sections = listOf(
                ReportSection.TextSection(
                    "Weekly Overview",
                    generateWeeklyOverview(statistics)
                ),
                ReportSection.StatisticsSection(
                    "Overall Statistics",
                    statistics
                ),
                createTrendSection(entries)
            ),
            format = ReportFormat.HTML
        )
    }
    
    fun generateIncidentReport(
        entries: List<LogEntry>,
        incidentTitle: String,
        incidentDescription: String
    ): Report {
        
        val rootCause = analyzeRootCause(entries)
        val timeline = buildTimeline(entries)
        
        return Report(
            id = java.util.UUID.randomUUID().toString(),
            title = "Incident Report: $incidentTitle",
            type = ReportType.INCIDENT_REPORT,
            createdAt = LocalDateTime.now(),
            timeRange = TimeRange(
                entries.firstOrNull()?.timestamp ?: LocalDateTime.now(),
                entries.lastOrNull()?.timestamp ?: LocalDateTime.now()
            ),
            sections = listOf(
                ReportSection.TextSection("Description", incidentDescription),
                ReportSection.TextSection("Root Cause Analysis", rootCause),
                timeline
            ),
            format = ReportFormat.PDF
        )
    }
    
    private fun calculateStatistics(entries: List<LogEntry>): LogStatistics {
        return com.jakubjirak.analytics.LogAnalytics().calculateStatistics(entries)
    }
    
    private fun findTopErrors(entries: List<LogEntry>): List<ErrorSummary> {
        val errors = entries.filter { it.level.name == "ERROR" }
        val grouped = errors.groupBy { extractErrorType(it.message) }
        
        return grouped.map { (type, entries) ->
            ErrorSummary(
                type = type,
                count = entries.size,
                firstOccurrence = entries.first().timestamp ?: LocalDateTime.now(),
                lastOccurrence = entries.last().timestamp ?: LocalDateTime.now(),
                examples = entries.take(3).map { it.message }
            )
        }.sortedByDescending { it.count }.take(10)
    }
    
    private fun extractErrorType(message: String): String {
        val match = Regex("""(\w+Exception)""").find(message)
        return match?.value ?: "Unknown Error"
    }
    
    private fun generateExecutiveSummary(stats: LogStatistics): String {
        return """
            Total log entries processed: ${stats.totalEntries}
            
            Error Rate: ${String.format("%.2f%%", stats.errorRate)}
            Errors: ${stats.errorCount}
            Warnings: ${stats.warnCount}
            
            ${if (stats.errorRate > 5.0) "⚠️ High error rate detected!" else "✅ System operating normally"}
            
            Correlation tracking: ${stats.correlationIdCount} entries with tracking IDs
            Active threads: ${stats.uniqueThreads}
        """.trimIndent()
    }
    
    private fun generateWeeklyOverview(stats: LogStatistics): String {
        return """
            This week's highlights:
            
            • Processed ${stats.totalEntries} log entries
            • Error rate: ${String.format("%.2f%%", stats.errorRate)}
            • ${stats.uniqueThreads} unique threads active
            • ${stats.correlationIdCount} tracked transactions
        """.trimIndent()
    }
    
    private fun createTrendSection(entries: List<LogEntry>): ReportSection.ChartSection {
        val dailyErrors = entries
            .filter { it.level.name == "ERROR" }
            .groupBy { it.timestamp?.dayOfMonth ?: 0 }
            .mapValues { it.value.size.toDouble() }
            .mapKeys { it.key.toString() }
        
        return ReportSection.ChartSection(
            title = "Error Trend",
            chartType = ChartType.LINE,
            data = dailyErrors
        )
    }
    
    private fun analyzeRootCause(entries: List<LogEntry>): String {
        val errors = entries.filter { it.level.name == "ERROR" }
        val firstError = errors.firstOrNull()
        
        return if (firstError != null) {
            """
            Root Cause Analysis:
            
            First error occurred at: ${firstError.timestamp}
            Error type: ${extractErrorType(firstError.message)}
            
            Analysis suggests the issue originated from: ${firstError.threadName ?: "unknown thread"}
            
            Recommendation: ${generateRecommendation(firstError)}
            """.trimIndent()
        } else {
            "No errors found in the analyzed logs."
        }
    }
    
    private fun generateRecommendation(error: LogEntry): String {
        return when {
            error.message.contains("Connection") -> "Check network connectivity and database availability"
            error.message.contains("Memory") -> "Increase heap size or investigate memory leak"
            error.message.contains("Timeout") -> "Review timeout settings and service performance"
            else -> "Review logs and stack traces for more details"
        }
    }
    
    private fun buildTimeline(entries: List<LogEntry>): ReportSection.TableSection {
        val rows = entries.sortedBy { it.timestamp }.take(50).map { entry ->
            listOf(
                entry.timestamp?.format(DateTimeFormatter.ISO_DATE_TIME) ?: "N/A",
                entry.level.name,
                entry.message.take(100)
            )
        }
        
        return ReportSection.TableSection(
            title = "Timeline",
            headers = listOf("Timestamp", "Level", "Message"),
            rows = rows
        )
    }
    
    fun exportReport(report: Report, outputFile: File): Boolean {
        return try {
            when (report.format) {
                ReportFormat.HTML -> exportAsHtml(report, outputFile)
                ReportFormat.MARKDOWN -> exportAsMarkdown(report, outputFile)
                ReportFormat.JSON -> exportAsJson(report, outputFile)
                ReportFormat.PDF -> exportAsPdf(report, outputFile)
            }
            true
        } catch (e: Exception) {
            println("Export failed: ${e.message}")
            false
        }
    }
    
    private fun exportAsHtml(report: Report, file: File) {
        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html><head><title>${report.title}</title>")
            appendLine("<style>body{font-family:Arial;margin:40px}h1{color:#E74C3C}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:8px}</style>")
            appendLine("</head><body>")
            appendLine("<h1>${report.title}</h1>")
            appendLine("<p>Generated: ${report.createdAt}</p>")
            
            report.sections.forEach { section ->
                when (section) {
                    is ReportSection.TextSection -> {
                        appendLine("<h2>${section.title}</h2>")
                        appendLine("<pre>${section.content}</pre>")
                    }
                    is ReportSection.TableSection -> {
                        appendLine("<h2>${section.title}</h2>")
                        appendLine("<table>")
                        appendLine("<tr>${section.headers.joinToString("") { "<th>$it</th>" }}</tr>")
                        section.rows.forEach { row ->
                            appendLine("<tr>${row.joinToString("") { "<td>$it</td>" }}</tr>")
                        }
                        appendLine("</table>")
                    }
                    else -> {}
                }
            }
            
            appendLine("</body></html>")
        }
        
        file.writeText(html)
    }
    
    private fun exportAsMarkdown(report: Report, file: File) {
        val md = buildString {
            appendLine("# ${report.title}")
            appendLine()
            appendLine("Generated: ${report.createdAt}")
            appendLine()
            
            report.sections.forEach { section ->
                appendLine("## ${section.title}")
                appendLine()
                
                when (section) {
                    is ReportSection.TextSection -> appendLine(section.content)
                    is ReportSection.TableSection -> {
                        appendLine(section.headers.joinToString(" | ", "| ", " |"))
                        appendLine(section.headers.joinToString(" | ", "| ", " |") { "---" })
                        section.rows.forEach { row ->
                            appendLine(row.joinToString(" | ", "| ", " |"))
                        }
                    }
                    else -> {}
                }
                appendLine()
            }
        }
        
        file.writeText(md)
    }
    
    private fun exportAsJson(report: Report, file: File) {
        val json = com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(report)
        file.writeText(json)
    }
    
    private fun exportAsPdf(report: Report, file: File) {
        // Would use PDF library like iText
        println("PDF export not yet implemented")
    }
}
