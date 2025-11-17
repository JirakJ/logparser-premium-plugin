package com.jakubjirak.filters

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel

data class CompositeFilter(
    val id: String,
    val name: String,
    val operator: LogicalOperator = LogicalOperator.AND
)

enum class LogicalOperator { AND, OR, NOT }

class AdvancedFilterEngine {
    fun applyFilter(entries: List<LogEntry>, filter: CompositeFilter): List<LogEntry> {
        return entries
    }
}
