package com.jakubjirak.correlation

import com.jakubjirak.parsers.LogEntry
import java.time.LocalDateTime

data class TransactionFlow(
    val correlationId: String,
    val entries: List<TransactionEntry>,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val duration: Long?, // in milliseconds
    val sources: Set<String>,
    val hasErrors: Boolean
)

data class TransactionEntry(
    val logEntry: LogEntry,
    val sourceName: String,
    val sourceType: SourceType
)

enum class SourceType {
    LOCAL_FILE,
    KUBERNETES_POD,
    DOCKER_CONTAINER,
    SSH_SERVER,
    CLOUD_STORAGE
}

class CorrelationTracker {
    
    private val transactions = mutableMapOf<String, MutableList<TransactionEntry>>()
    
    fun addEntry(correlationId: String, entry: LogEntry, sourceName: String, sourceType: SourceType) {
        val transactionEntry = TransactionEntry(entry, sourceName, sourceType)
        transactions.getOrPut(correlationId) { mutableListOf() }.add(transactionEntry)
    }
    
    fun getTransaction(correlationId: String): TransactionFlow? {
        val entries = transactions[correlationId] ?: return null
        
        if (entries.isEmpty()) return null
        
        val sortedEntries = entries.sortedBy { it.logEntry.timestamp }
        val startTime = sortedEntries.firstOrNull()?.logEntry?.timestamp
        val endTime = sortedEntries.lastOrNull()?.logEntry?.timestamp
        
        val duration = if (startTime != null && endTime != null) {
            java.time.Duration.between(startTime, endTime).toMillis()
        } else {
            null
        }
        
        val sources = entries.map { it.sourceName }.toSet()
        val hasErrors = entries.any { it.logEntry.level.name in setOf("ERROR", "FATAL") }
        
        return TransactionFlow(
            correlationId = correlationId,
            entries = sortedEntries,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            sources = sources,
            hasErrors = hasErrors
        )
    }
    
    fun getAllTransactions(): List<TransactionFlow> {
        return transactions.keys.mapNotNull { getTransaction(it) }
    }
    
    fun clear() {
        transactions.clear()
    }
    
    fun removeTransaction(correlationId: String) {
        transactions.remove(correlationId)
    }
    
    fun getTransactionCount(): Int = transactions.size
}
