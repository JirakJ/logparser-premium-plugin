package com.jakubjirak.logparser.premium

import com.intellij.openapi.project.Project
import com.jakubjirak.parsers.LogEntry
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Premium Feature: Performance heatmap visualization
 * Shows when and where performance issues occur across time and services
 */
class PerformanceHeatmap(private val project: Project) {
    
    data class HeatmapData(
        val timeRange: TimeRange,
        val services: List<String>,
        val cells: List<List<HeatmapCell>>,
        val metric: MetricType
    )
    
    data class TimeRange(val start: LocalDateTime, val end: LocalDateTime)
    
    data class HeatmapCell(
        val timestamp: LocalDateTime,
        val service: String,
        val value: Double,
        val count: Int
    )
    
    enum class MetricType { RESPONSE_TIME, ERROR_RATE, REQUEST_RATE, CPU_USAGE, MEMORY_USAGE }
    
    fun generateHeatmap(
        logs: List<LogEntry>,
        timeRange: TimeRange,
        metric: MetricType = MetricType.RESPONSE_TIME,
        granularity: Duration = Duration.ofMinutes(5)
    ): HeatmapData {
        val services = logs.mapNotNull { it.loggerName }.distinct().sorted()
        val timeSlots = generateTimeSlots(timeRange, granularity)
        
        val cells = timeSlots.map { slot ->
            services.map { service ->
                val logsInCell = logs.filter { log ->
                    log.loggerName == service &&
                    log.timestamp != null &&
                    !log.timestamp!!.isBefore(slot) &&
                    log.timestamp!!.isBefore(slot.plus(granularity))
                }
                
                HeatmapCell(
                    timestamp = slot,
                    service = service,
                    value = calculateMetricValue(logsInCell, metric),
                    count = logsInCell.size
                )
            }
        }
        
        return HeatmapData(timeRange, services, cells, metric)
    }
    
    private fun generateTimeSlots(range: TimeRange, granularity: Duration): List<LocalDateTime> {
        val slots = mutableListOf<LocalDateTime>()
        var current = range.start
        while (current.isBefore(range.end)) {
            slots.add(current)
            current = current.plus(granularity)
        }
        return slots
    }
    
    private fun calculateMetricValue(logs: List<LogEntry>, metric: MetricType): Double {
        if (logs.isEmpty()) return 0.0
        return when (metric) {
            MetricType.ERROR_RATE -> logs.count { it.level.name in listOf("ERROR", "FATAL") }.toDouble() / logs.size
            MetricType.REQUEST_RATE -> logs.size.toDouble()
            else -> 0.0
        }
    }
    
    fun exportAsCSV(heatmap: HeatmapData): String {
        val sb = StringBuilder()
        sb.append("Timestamp")
        heatmap.services.forEach { sb.append(",").append(it) }
        sb.appendLine()
        
        heatmap.cells.forEach { row ->
            sb.append(row.first().timestamp)
            row.forEach { cell -> sb.append(",").append(String.format("%.2f", cell.value)) }
            sb.appendLine()
        }
        return sb.toString()
    }
}
