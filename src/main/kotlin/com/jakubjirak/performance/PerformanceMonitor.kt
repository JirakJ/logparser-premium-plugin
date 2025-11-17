package com.jakubjirak.performance

import java.time.LocalDateTime

data class PerformanceMetrics(
    val avgParsingTime: Double,
    val totalEntriesProcessed: Long,
    val memoryUsageMB: Double
)

class PerformanceMonitor {
    private var totalEntriesProcessed = 0L
    
    fun recordOperation(durationMs: Long, operation: String = "parse") {
        totalEntriesProcessed++
    }
    
    fun getMetrics(): PerformanceMetrics {
        val runtime = Runtime.getRuntime()
        val memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0
        
        return PerformanceMetrics(
            avgParsingTime = 0.0,
            totalEntriesProcessed = totalEntriesProcessed,
            memoryUsageMB = memoryUsed
        )
    }
}
