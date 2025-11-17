package com.jakubjirak.logparser.premium

import com.intellij.openapi.project.Project
import com.jakubjirak.parsers.LogEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Duration
import java.time.Instant

/**
 * Premium Feature: Advanced log aggregation pipeline
 * Processes logs from multiple sources with transformations and aggregations
 */
class LogAggregationPipeline(private val project: Project) {
    
    data class AggregationConfig(
        val windowSize: Duration = Duration.ofMinutes(1),
        val groupByFields: List<String> = emptyList(),
        val aggregations: List<AggregationType> = emptyList()
    )
    
    enum class AggregationType {
        COUNT, SUM, AVG, MIN, MAX, PERCENTILE_95, PERCENTILE_99, RATE_PER_SECOND, ERROR_RATE
    }
    
    data class AggregationResult(
        val timestamp: Instant,
        val groupKey: Map<String, String>,
        val metrics: Map<String, Double>,
        val count: Long,
        val samples: List<LogEntry> = emptyList()
    )
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun createPipeline(config: AggregationConfig): Flow<AggregationResult> {
        return flow {
            emitAll(aggregateLogsWithWindowing(config))
        }
    }
    
    private fun aggregateLogsWithWindowing(config: AggregationConfig): Flow<AggregationResult> = flow {
        val windowBuffer = mutableMapOf<String, MutableList<LogEntry>>()
        var currentWindowStart = Instant.now()
        
        while (true) {
            delay(1000)
            
            val now = Instant.now()
            if (Duration.between(currentWindowStart, now) >= config.windowSize) {
                windowBuffer.forEach { (groupKey, logs) ->
                    val metrics = calculateMetrics(logs, config.aggregations)
                    emit(AggregationResult(
                        timestamp = currentWindowStart,
                        groupKey = parseGroupKey(groupKey),
                        metrics = metrics,
                        count = logs.size.toLong(),
                        samples = logs.take(5)
                    ))
                }
                
                windowBuffer.clear()
                currentWindowStart = now
            }
        }
    }
    
    private fun calculateMetrics(logs: List<LogEntry>, types: List<AggregationType>): Map<String, Double> {
        val metrics = mutableMapOf<String, Double>()
        
        types.forEach { type ->
            when (type) {
                AggregationType.COUNT -> metrics["count"] = logs.size.toDouble()
                AggregationType.ERROR_RATE -> {
                    val errors = logs.count { it.level.contains("ERROR", true) }
                    metrics["error_rate"] = if (logs.isNotEmpty()) errors.toDouble() / logs.size else 0.0
                }
                AggregationType.RATE_PER_SECOND -> {
                    val duration = Duration.between(
                        logs.minOfOrNull { it.timestamp } ?: Instant.now(),
                        logs.maxOfOrNull { it.timestamp } ?: Instant.now()
                    ).seconds
                    metrics["rate_per_second"] = if (duration > 0) logs.size.toDouble() / duration else 0.0
                }
                else -> {}
            }
        }
        
        return metrics
    }
    
    private fun parseGroupKey(key: String): Map<String, String> {
        return key.split("|").associate {
            val parts = it.split("=")
            parts[0] to (parts.getOrNull(1) ?: "")
        }
    }
    
    fun dispose() {
        scope.cancel()
    }
}
