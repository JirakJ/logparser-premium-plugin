package com.jakubjirak.ml

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import kotlin.math.exp
import kotlin.math.sqrt

data class LogCategory(
    val name: String,
    val confidence: Double,
    val keywords: List<String>
)

data class AnomalyScore(
    val entry: LogEntry,
    val score: Double,
    val reasons: List<String>,
    val isAnomaly: Boolean
)

data class LogPattern(
    val pattern: String,
    val frequency: Int,
    val firstSeen: java.time.LocalDateTime,
    val lastSeen: java.time.LocalDateTime,
    val examples: List<String>
)

class LogClassifier {
    
    private val categories = mapOf(
        "database" to listOf("sql", "database", "jdbc", "hibernate", "query", "connection"),
        "network" to listOf("http", "tcp", "socket", "timeout", "connection refused", "ssl"),
        "security" to listOf("authentication", "authorization", "password", "token", "forbidden", "unauthorized"),
        "performance" to listOf("slow", "timeout", "memory", "cpu", "gc", "heap", "latency"),
        "business_logic" to listOf("order", "payment", "user", "customer", "transaction"),
        "system" to listOf("startup", "shutdown", "configuration", "initialization")
    )
    
    fun classifyLog(entry: LogEntry): List<LogCategory> {
        val results = mutableListOf<LogCategory>()
        val message = entry.message.lowercase()
        
        categories.forEach { (category, keywords) ->
            val matchedKeywords = keywords.filter { message.contains(it) }
            if (matchedKeywords.isNotEmpty()) {
                val confidence = matchedKeywords.size.toDouble() / keywords.size
                results.add(LogCategory(category, confidence, matchedKeywords))
            }
        }
        
        return results.sortedByDescending { it.confidence }
    }
    
    fun detectAnomalies(entries: List<LogEntry>): List<AnomalyScore> {
        if (entries.size < 10) return emptyList()
        
        val anomalies = mutableListOf<AnomalyScore>()
        
        // Isolation Forest simplified
        val features = entries.map { extractFeatures(it) }
        val scores = features.map { calculateAnomalyScore(it, features) }
        
        entries.forEachIndexed { index, entry ->
            val score = scores[index]
            val threshold = 0.7
            
            if (score > threshold) {
                val reasons = analyzeAnomaly(entry, entries)
                anomalies.add(
                    AnomalyScore(
                        entry = entry,
                        score = score,
                        reasons = reasons,
                        isAnomaly = true
                    )
                )
            }
        }
        
        return anomalies.sortedByDescending { it.score }
    }
    
    private fun extractFeatures(entry: LogEntry): DoubleArray {
        return doubleArrayOf(
            entry.level.ordinal.toDouble(),
            entry.message.length.toDouble(),
            if (entry.stackTrace != null) 1.0 else 0.0,
            if (entry.correlationId != null) 1.0 else 0.0,
            entry.message.count { it.isUpperCase() }.toDouble()
        )
    }
    
    private fun calculateAnomalyScore(features: DoubleArray, allFeatures: List<DoubleArray>): Double {
        val distances = allFeatures.map { euclideanDistance(features, it) }
        val avgDistance = distances.average()
        val stdDev = calculateStdDev(distances, avgDistance)
        
        return if (stdDev > 0) {
            (avgDistance - distances.average()) / stdDev
        } else 0.0
    }
    
    private fun euclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        return sqrt(a.zip(b).sumOf { (x, y) -> (x - y) * (x - y) })
    }
    
    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    private fun analyzeAnomaly(entry: LogEntry, context: List<LogEntry>): List<String> {
        val reasons = mutableListOf<String>()
        
        // Check if error level is unusual
        val errorRate = context.count { it.level == LogLevel.ERROR } / context.size.toDouble()
        if (entry.level == LogLevel.ERROR && errorRate < 0.05) {
            reasons.add("Unusual ERROR log (error rate: ${String.format("%.2f%%", errorRate * 100)})")
        }
        
        // Check message length
        val avgLength = context.map { it.message.length }.average()
        if (entry.message.length > avgLength * 2) {
            reasons.add("Unusually long message (${entry.message.length} vs avg ${avgLength.toInt()} chars)")
        }
        
        // Check for stack trace when uncommon
        val stackTraceRate = context.count { it.stackTrace != null } / context.size.toDouble()
        if (entry.stackTrace != null && stackTraceRate < 0.1) {
            reasons.add("Rare stack trace presence")
        }
        
        return reasons
    }
    
    fun discoverPatterns(entries: List<LogEntry>, minFrequency: Int = 5): List<LogPattern> {
        val patterns = mutableMapOf<String, MutableList<LogEntry>>()
        
        entries.forEach { entry ->
            val normalized = normalizeMessage(entry.message)
            patterns.getOrPut(normalized) { mutableListOf() }.add(entry)
        }
        
        return patterns
            .filter { it.value.size >= minFrequency }
            .map { (pattern, entries) ->
                LogPattern(
                    pattern = pattern,
                    frequency = entries.size,
                    firstSeen = entries.first().timestamp ?: java.time.LocalDateTime.now(),
                    lastSeen = entries.last().timestamp ?: java.time.LocalDateTime.now(),
                    examples = entries.take(3).map { it.rawLine }
                )
            }
            .sortedByDescending { it.frequency }
    }
    
    private fun normalizeMessage(message: String): String {
        return message
            .replace(Regex("\\d+"), "<NUM>")
            .replace(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"), "<UUID>")
            .replace(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"), "<IP>")
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "<EMAIL>")
    }
    
    fun predictLogLevel(entry: LogEntry): Pair<LogLevel, Double> {
        val message = entry.message.lowercase()
        
        val errorKeywords = listOf("error", "exception", "failed", "failure", "fatal")
        val warnKeywords = listOf("warn", "warning", "deprecated", "slow")
        val infoKeywords = listOf("info", "started", "completed", "success")
        
        val errorScore = errorKeywords.count { message.contains(it) }.toDouble()
        val warnScore = warnKeywords.count { message.contains(it) }.toDouble()
        val infoScore = infoKeywords.count { message.contains(it) }.toDouble()
        
        return when {
            errorScore > 0 -> Pair(LogLevel.ERROR, errorScore / errorKeywords.size)
            warnScore > 0 -> Pair(LogLevel.WARN, warnScore / warnKeywords.size)
            else -> Pair(LogLevel.INFO, infoScore / infoKeywords.size)
        }
    }
}

class LogRecommendationEngine {
    
    fun suggestFilters(entries: List<LogEntry>): List<FilterSuggestion> {
        val suggestions = mutableListOf<FilterSuggestion>()
        
        val errorRate = entries.count { it.level == LogLevel.ERROR } / entries.size.toDouble()
        if (errorRate > 0.1) {
            suggestions.add(
                FilterSuggestion(
                    name = "Filter High Error Rate",
                    description = "High error rate detected (${String.format("%.1f%%", errorRate * 100)})",
                    filter = "level:ERROR",
                    priority = 9
                )
            )
        }
        
        val commonExceptions = entries
            .filter { it.message.contains("Exception") }
            .groupBy { 
                Regex("""(\w+Exception)""").find(it.message)?.value ?: "Unknown"
            }
            .filter { it.value.size > 5 }
        
        commonExceptions.forEach { (exception, occurrences) ->
            suggestions.add(
                FilterSuggestion(
                    name = "Filter $exception",
                    description = "$exception occurred ${occurrences.size} times",
                    filter = "message:$exception",
                    priority = 7
                )
            )
        }
        
        return suggestions.sortedByDescending { it.priority }
    }
    
    fun suggestQueries(entries: List<LogEntry>): List<QuerySuggestion> {
        val suggestions = mutableListOf<QuerySuggestion>()
        
        val hasCorrelationIds = entries.any { it.correlationId != null }
        if (hasCorrelationIds) {
            suggestions.add(
                QuerySuggestion(
                    name = "Track Transaction Flow",
                    query = "Track correlation IDs across sources",
                    benefit = "See complete request journey"
                )
            )
        }
        
        return suggestions
    }
}

data class FilterSuggestion(
    val name: String,
    val description: String,
    val filter: String,
    val priority: Int
)

data class QuerySuggestion(
    val name: String,
    val query: String,
    val benefit: String
)
