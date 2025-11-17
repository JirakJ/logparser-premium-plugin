package com.jakubjirak.comparison

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import java.time.LocalDateTime

data class ComparisonResult(
    val leftSource: String,
    val rightSource: String,
    val summary: ComparisonSummary,
    val differences: List<Difference>,
    val newEntries: List<LogEntry>,
    val removedEntries: List<LogEntry>,
    val changedEntries: List<ChangedEntry>
)

data class ComparisonSummary(
    val totalLeft: Int,
    val totalRight: Int,
    val identical: Int,
    val modified: Int,
    val added: Int,
    val removed: Int,
    val errorRateChangePercent: Double,
    val performanceImpact: PerformanceImpact
)

data class Difference(
    val type: DifferenceType,
    val category: String,
    val description: String,
    val severity: DifferenceSeverity,
    val leftValue: Any?,
    val rightValue: Any?
)

enum class DifferenceType {
    ERROR_RATE_CHANGE,
    NEW_ERROR_TYPE,
    REMOVED_ERROR,
    PERFORMANCE_DEGRADATION,
    PERFORMANCE_IMPROVEMENT,
    PATTERN_CHANGE,
    VOLUME_CHANGE
}

enum class DifferenceSeverity {
    INFO, WARNING, CRITICAL
}

data class ChangedEntry(
    val original: LogEntry,
    val modified: LogEntry,
    val changes: List<String>
)

data class PerformanceImpact(
    val throughputChange: Double,
    val avgResponseTimeChange: Double,
    val impact: ImpactLevel
)

enum class ImpactLevel {
    POSITIVE, NEUTRAL, NEGATIVE, CRITICAL
}

class LogComparisonEngine {
    
    fun compare(
        leftEntries: List<LogEntry>,
        rightEntries: List<LogEntry>,
        leftSource: String = "Before",
        rightSource: String = "After"
    ): ComparisonResult {
        
        val summary = calculateSummary(leftEntries, rightEntries)
        val differences = findDifferences(leftEntries, rightEntries)
        val (new, removed, changed) = findChanges(leftEntries, rightEntries)
        
        return ComparisonResult(
            leftSource = leftSource,
            rightSource = rightSource,
            summary = summary,
            differences = differences,
            newEntries = new,
            removedEntries = removed,
            changedEntries = changed
        )
    }
    
    private fun calculateSummary(
        left: List<LogEntry>,
        right: List<LogEntry>
    ): ComparisonSummary {
        
        val leftErrors = left.count { it.level == LogLevel.ERROR }
        val rightErrors = right.count { it.level == LogLevel.ERROR }
        
        val leftErrorRate = if (left.isNotEmpty()) leftErrors.toDouble() / left.size else 0.0
        val rightErrorRate = if (right.isNotEmpty()) rightErrors.toDouble() / right.size else 0.0
        val errorRateChange = ((rightErrorRate - leftErrorRate) / (leftErrorRate + 0.001)) * 100
        
        val performanceImpact = calculatePerformanceImpact(left, right)
        
        return ComparisonSummary(
            totalLeft = left.size,
            totalRight = right.size,
            identical = 0, // Would implement deep comparison
            modified = 0,
            added = right.size - left.size,
            removed = left.size - right.size,
            errorRateChangePercent = errorRateChange,
            performanceImpact = performanceImpact
        )
    }
    
    private fun calculatePerformanceImpact(
        left: List<LogEntry>,
        right: List<LogEntry>
    ): PerformanceImpact {
        
        val volumeChange = right.size.toDouble() / (left.size + 1) - 1.0
        
        val impact = when {
            volumeChange < -0.3 -> ImpactLevel.POSITIVE
            volumeChange > 0.5 -> ImpactLevel.NEGATIVE
            volumeChange > 1.0 -> ImpactLevel.CRITICAL
            else -> ImpactLevel.NEUTRAL
        }
        
        return PerformanceImpact(
            throughputChange = volumeChange * 100,
            avgResponseTimeChange = 0.0,
            impact = impact
        )
    }
    
    private fun findDifferences(
        left: List<LogEntry>,
        right: List<LogEntry>
    ): List<Difference> {
        
        val differences = mutableListOf<Difference>()
        
        // Error rate difference
        val leftErrorRate = left.count { it.level == LogLevel.ERROR }.toDouble() / left.size
        val rightErrorRate = right.count { it.level == LogLevel.ERROR }.toDouble() / right.size
        
        if (kotlin.math.abs(rightErrorRate - leftErrorRate) > 0.05) {
            differences.add(
                Difference(
                    type = DifferenceType.ERROR_RATE_CHANGE,
                    category = "Error Rate",
                    description = "Error rate changed from ${String.format("%.2f%%", leftErrorRate * 100)} to ${String.format("%.2f%%", rightErrorRate * 100)}",
                    severity = if (rightErrorRate > leftErrorRate) DifferenceSeverity.CRITICAL else DifferenceSeverity.INFO,
                    leftValue = leftErrorRate,
                    rightValue = rightErrorRate
                )
            )
        }
        
        // New error types
        val leftExceptions = extractExceptions(left)
        val rightExceptions = extractExceptions(right)
        val newExceptions = rightExceptions - leftExceptions
        
        newExceptions.forEach { exception ->
            differences.add(
                Difference(
                    type = DifferenceType.NEW_ERROR_TYPE,
                    category = "Exceptions",
                    description = "New exception appeared: $exception",
                    severity = DifferenceSeverity.WARNING,
                    leftValue = null,
                    rightValue = exception
                )
            )
        }
        
        // Volume change
        val volumeChange = ((right.size - left.size).toDouble() / left.size) * 100
        if (kotlin.math.abs(volumeChange) > 20) {
            differences.add(
                Difference(
                    type = DifferenceType.VOLUME_CHANGE,
                    category = "Volume",
                    description = "Log volume changed by ${String.format("%.1f%%", volumeChange)}",
                    severity = if (kotlin.math.abs(volumeChange) > 50) DifferenceSeverity.WARNING else DifferenceSeverity.INFO,
                    leftValue = left.size,
                    rightValue = right.size
                )
            )
        }
        
        return differences
    }
    
    private fun extractExceptions(entries: List<LogEntry>): Set<String> {
        return entries
            .map { Regex("""(\w+Exception)""").find(it.message)?.value }
            .filterNotNull()
            .toSet()
    }
    
    private fun findChanges(
        left: List<LogEntry>,
        right: List<LogEntry>
    ): Triple<List<LogEntry>, List<LogEntry>, List<ChangedEntry>> {
        
        // Simplified - would use better matching algorithm
        val new = if (right.size > left.size) right.takeLast(right.size - left.size) else emptyList()
        val removed = if (left.size > right.size) left.takeLast(left.size - right.size) else emptyList()
        val changed = emptyList<ChangedEntry>()
        
        return Triple(new, removed, changed)
    }
    
    fun compareDeployments(
        beforeDeploy: List<LogEntry>,
        afterDeploy: List<LogEntry>
    ): DeploymentComparison {
        
        val result = compare(beforeDeploy, afterDeploy, "Before Deploy", "After Deploy")
        
        val recommendation = when {
            result.summary.errorRateChangePercent > 50 -> DeploymentRecommendation.ROLLBACK
            result.summary.errorRateChangePercent > 20 -> DeploymentRecommendation.MONITOR
            else -> DeploymentRecommendation.PROCEED
        }
        
        return DeploymentComparison(
            result = result,
            recommendation = recommendation,
            criticalIssues = result.differences.filter { it.severity == DifferenceSeverity.CRITICAL }
        )
    }
}

data class DeploymentComparison(
    val result: ComparisonResult,
    val recommendation: DeploymentRecommendation,
    val criticalIssues: List<Difference>
)

enum class DeploymentRecommendation {
    PROCEED, MONITOR, INVESTIGATE, ROLLBACK
}

class TimeBasedComparison {
    
    fun compareTimeRanges(
        entries: List<LogEntry>,
        range1Start: LocalDateTime,
        range1End: LocalDateTime,
        range2Start: LocalDateTime,
        range2End: LocalDateTime
    ): ComparisonResult {
        
        val range1 = entries.filter { entry ->
            entry.timestamp?.let {
                it.isAfter(range1Start) && it.isBefore(range1End)
            } ?: false
        }
        
        val range2 = entries.filter { entry ->
            entry.timestamp?.let {
                it.isAfter(range2Start) && it.isBefore(range2End)
            } ?: false
        }
        
        val engine = LogComparisonEngine()
        return engine.compare(range1, range2, "Time Range 1", "Time Range 2")
    }
    
    fun compareHourly(entries: List<LogEntry>, date: LocalDateTime): List<HourlyComparison> {
        val comparisons = mutableListOf<HourlyComparison>()
        
        for (hour in 0..22) {
            val current = entries.filter { 
                it.timestamp?.hour == hour
            }
            val next = entries.filter {
                it.timestamp?.hour == hour + 1
            }
            
            if (current.isNotEmpty() && next.isNotEmpty()) {
                val engine = LogComparisonEngine()
                val result = engine.compare(current, next, "Hour $hour", "Hour ${hour + 1}")
                
                comparisons.add(
                    HourlyComparison(
                        hour = hour,
                        result = result
                    )
                )
            }
        }
        
        return comparisons
    }
}

data class HourlyComparison(
    val hour: Int,
    val result: ComparisonResult
)
