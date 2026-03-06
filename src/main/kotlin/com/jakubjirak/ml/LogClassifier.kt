/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ml

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import kotlin.math.sqrt

/** Classification result for a single log entry. */
data class LogCategory(val name: String, val confidence: Double, val keywords: List<String>)

/** Anomaly detection result with score, reasons, and flag. */
data class AnomalyScore(
    val entry: LogEntry,
    val score: Double,
    val reasons: List<String>,
    val isAnomaly: Boolean
)

/** A discovered recurring pattern across multiple log entries. */
data class LogPattern(
    val pattern: String,
    val frequency: Int,
    val firstSeen: java.time.LocalDateTime,
    val lastSeen: java.time.LocalDateTime,
    val examples: List<String>
)

/**
 * Keyword-based log classifier with statistical anomaly detection
 * and automatic pattern discovery.
 *
 * Classification uses six predefined categories: database, network,
 * security, performance, business_logic, and system.
 * Anomaly detection implements a simplified Isolation-Forest-style scoring
 * based on Euclidean distance in the feature space.
 */
class LogClassifier {

    private val categories = mapOf(
        "database" to listOf("sql", "database", "jdbc", "hibernate", "query", "connection"),
        "network" to listOf("http", "tcp", "socket", "timeout", "connection refused", "ssl"),
        "security" to listOf("authentication", "authorization", "password", "token", "forbidden", "unauthorized"),
        "performance" to listOf("slow", "timeout", "memory", "cpu", "gc", "heap", "latency"),
        "business_logic" to listOf("order", "payment", "user", "customer", "transaction"),
        "system" to listOf("startup", "shutdown", "configuration", "initialization")
    )

    /** Classifies [entry] into zero or more categories, ordered by confidence. */
    fun classifyLog(entry: LogEntry): List<LogCategory> {
        val message = entry.message.lowercase()
        return categories.mapNotNull { (category, keywords) ->
            val matched = keywords.filter { message.contains(it) }
            if (matched.isNotEmpty()) {
                LogCategory(category, matched.size.toDouble() / keywords.size, matched)
            } else {
                null
            }
        }.sortedByDescending { it.confidence }
    }

    /**
     * Detects anomalous entries by computing feature-based distance scores.
     * Returns entries whose score exceeds the 0.7 threshold, sorted descending.
     * Requires at least 10 entries for meaningful analysis.
     */
    fun detectAnomalies(entries: List<LogEntry>): List<AnomalyScore> {
        if (entries.size < 10) return emptyList()

        val features = entries.map { extractFeatures(it) }
        val scores = features.map { calculateAnomalyScore(it, features) }
        val threshold = 0.7

        return entries.mapIndexedNotNull { index, entry ->
            val score = scores[index]
            if (score > threshold) {
                AnomalyScore(entry, score, analyzeAnomaly(entry, entries), true)
            } else {
                null
            }
        }.sortedByDescending { it.score }
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
        return if (stdDev > 0) (avgDistance - distances.average()) / stdDev else 0.0
    }

    private fun euclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        return sqrt(a.zip(b).sumOf { (x, y) -> (x - y) * (x - y) })
    }

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        return sqrt(values.map { (it - mean) * (it - mean) }.average())
    }

    private fun analyzeAnomaly(entry: LogEntry, context: List<LogEntry>): List<String> {
        val reasons = mutableListOf<String>()

        val errorRate = context.count { it.level == LogLevel.ERROR } / context.size.toDouble()
        if (entry.level == LogLevel.ERROR && errorRate < 0.05) {
            reasons.add("Unusual ERROR log (error rate: ${String.format(java.util.Locale.US, "%.2f%%", errorRate * 100)})")
        }

        val avgLength = context.map { it.message.length }.average()
        if (entry.message.length > avgLength * 2) {
            reasons.add("Unusually long message (${entry.message.length} vs avg ${avgLength.toInt()} chars)")
        }

        val stackTraceRate = context.count { it.stackTrace != null } / context.size.toDouble()
        if (entry.stackTrace != null && stackTraceRate < 0.1) {
            reasons.add("Rare stack trace presence")
        }

        return reasons
    }

    /**
     * Groups entries by normalised message pattern and returns patterns
     * that occur at least [minFrequency] times.
     */
    fun discoverPatterns(entries: List<LogEntry>, minFrequency: Int = 5): List<LogPattern> {
        val patterns = mutableMapOf<String, MutableList<LogEntry>>()
        entries.forEach { entry ->
            patterns.getOrPut(normalizeMessage(entry.message)) { mutableListOf() }.add(entry)
        }

        return patterns
            .filter { it.value.size >= minFrequency }
            .map { (pattern, matched) ->
                LogPattern(
                    pattern = pattern,
                    frequency = matched.size,
                    firstSeen = matched.first().timestamp ?: java.time.LocalDateTime.now(),
                    lastSeen = matched.last().timestamp ?: java.time.LocalDateTime.now(),
                    examples = matched.take(3).map { it.rawLine }
                )
            }
            .sortedByDescending { it.frequency }
    }

    private fun normalizeMessage(message: String): String {
        return message
            .replace(Regex("\\d+"), "<NUM>")
            .replace(
                Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"),
                "<UUID>"
            )
            .replace(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"), "<IP>")
            .replace(
                Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
                "<EMAIL>"
            )
    }

    /** Predicts the most likely log level based on keyword analysis. */
    fun predictLogLevel(entry: LogEntry): Pair<LogLevel, Double> {
        val message = entry.message.lowercase()

        val errorKeywords = listOf("error", "exception", "failed", "failure", "fatal")
        val warnKeywords = listOf("warn", "warning", "deprecated", "slow")
        val infoKeywords = listOf("info", "started", "completed", "success")

        val errorScore = errorKeywords.count { message.contains(it) }.toDouble()
        val warnScore = warnKeywords.count { message.contains(it) }.toDouble()
        val infoScore = infoKeywords.count { message.contains(it) }.toDouble()

        return when {
            errorScore > 0 -> LogLevel.ERROR to errorScore / errorKeywords.size
            warnScore > 0 -> LogLevel.WARN to warnScore / warnKeywords.size
            else -> LogLevel.INFO to infoScore / infoKeywords.size
        }
    }
}
