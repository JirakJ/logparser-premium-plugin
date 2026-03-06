/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.correlation

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import java.time.Duration
import java.time.LocalDateTime

/**
 * Represents a complete request journey identified by a single correlation ID.
 *
 * @property correlationId the correlation/trace identifier
 * @property entries       ordered list of log entries belonging to this transaction
 * @property startTime     timestamp of the first entry
 * @property endTime       timestamp of the last entry
 * @property duration      wall-clock duration in milliseconds, or `null` if timestamps are missing
 * @property sources       set of source names that contributed entries
 * @property hasErrors     `true` when at least one entry has ERROR or FATAL level
 */
data class TransactionFlow(
    val correlationId: String,
    val entries: List<TransactionEntry>,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val duration: Long?,
    val sources: Set<String>,
    val hasErrors: Boolean
)

/** A log entry together with its source metadata. */
data class TransactionEntry(
    val logEntry: LogEntry,
    val sourceName: String,
    val sourceType: SourceType
)

/** The type of log source a [TransactionEntry] originates from. */
enum class SourceType {
    LOCAL_FILE,
    KUBERNETES_POD,
    DOCKER_CONTAINER,
    SSH_SERVER,
    CLOUD_STORAGE
}

/**
 * Aggregates [LogEntry] instances by correlation ID and reconstructs
 * [TransactionFlow] objects that represent end-to-end request journeys
 * across multiple log sources.
 */
class CorrelationTracker {

    private val transactions = mutableMapOf<String, MutableList<TransactionEntry>>()

    /** Adds a log [entry] to the transaction identified by [correlationId]. */
    fun addEntry(
        correlationId: String,
        entry: LogEntry,
        sourceName: String,
        sourceType: SourceType
    ) {
        val transactionEntry = TransactionEntry(entry, sourceName, sourceType)
        transactions.getOrPut(correlationId) { mutableListOf() }.add(transactionEntry)
    }

    /** Builds a [TransactionFlow] for [correlationId], or `null` if no entries exist. */
    fun getTransaction(correlationId: String): TransactionFlow? {
        val entries = transactions[correlationId]?.takeIf { it.isNotEmpty() } ?: return null

        val sortedEntries = entries.sortedBy { it.logEntry.timestamp }
        val startTime = sortedEntries.firstOrNull()?.logEntry?.timestamp
        val endTime = sortedEntries.lastOrNull()?.logEntry?.timestamp

        val duration = if (startTime != null && endTime != null) {
            Duration.between(startTime, endTime).toMillis()
        } else {
            null
        }

        return TransactionFlow(
            correlationId = correlationId,
            entries = sortedEntries,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            sources = entries.map { it.sourceName }.toSet(),
            hasErrors = entries.any { it.logEntry.level in setOf(LogLevel.ERROR, LogLevel.FATAL) }
        )
    }

    /** Builds [TransactionFlow] objects for all tracked correlation IDs. */
    fun getAllTransactions(): List<TransactionFlow> {
        return transactions.keys.mapNotNull { getTransaction(it) }
    }

    /** Removes all tracked transactions. */
    fun clear() {
        transactions.clear()
    }

    /** Removes the transaction for [correlationId]. */
    fun removeTransaction(correlationId: String) {
        transactions.remove(correlationId)
    }

    /** Number of distinct correlation IDs currently tracked. */
    fun getTransactionCount(): Int = transactions.size
}
