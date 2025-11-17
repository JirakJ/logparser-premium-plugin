package com.jakubjirak.integrations

import com.google.gson.GsonBuilder
import com.jakubjirak.analytics.LogStatistics
import com.jakubjirak.correlation.TransactionFlow
import com.jakubjirak.parsers.LogEntry
import java.io.File
import java.time.format.DateTimeFormatter

class ExportManager {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()
    
    fun exportToJson(entries: List<LogEntry>, outputFile: File): Boolean {
        return try {
            val json = gson.toJson(entries)
            outputFile.writeText(json)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun exportToCsv(entries: List<LogEntry>, outputFile: File): Boolean {
        return try {
            val csv = StringBuilder()
            csv.appendLine("LineNumber,Timestamp,Level,Message,ThreadName,CorrelationId")
            
            entries.forEach { entry ->
                val timestamp = entry.timestamp?.format(DateTimeFormatter.ISO_DATE_TIME) ?: ""
                val message = entry.message.replace("\"", "\"\"")
                csv.appendLine("${entry.lineNumber},\"$timestamp\",${entry.level},\"$message\",${entry.threadName ?: ""},${entry.correlationId ?: ""}")
            }
            
            outputFile.writeText(csv.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun exportStatistics(stats: LogStatistics, outputFile: File): Boolean {
        return try {
            val report = StringBuilder()
            report.appendLine("=== LogParser Pro - Statistics Report ===")
            report.appendLine()
            report.appendLine("Total Entries: ${stats.totalEntries}")
            report.appendLine("Errors: ${stats.errorCount}")
            report.appendLine("Warnings: ${stats.warnCount}")
            report.appendLine("Info: ${stats.infoCount}")
            report.appendLine("Debug: ${stats.debugCount}")
            report.appendLine("Error Rate: ${String.format("%.2f%%", stats.errorRate)}")
            report.appendLine("Correlation IDs Found: ${stats.correlationIdCount}")
            report.appendLine("Unique Threads: ${stats.uniqueThreads}")
            
            outputFile.writeText(report.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun exportTransactionTimeline(transaction: TransactionFlow, outputFile: File): Boolean {
        return try {
            val timeline = StringBuilder()
            timeline.appendLine("=== Transaction Timeline ===")
            timeline.appendLine("Correlation ID: ${transaction.correlationId}")
            timeline.appendLine("Duration: ${transaction.duration}ms")
            timeline.appendLine("Sources: ${transaction.sources.joinToString(", ")}")
            timeline.appendLine("Has Errors: ${transaction.hasErrors}")
            timeline.appendLine()
            timeline.appendLine("Timeline:")
            
            transaction.entries.forEach { entry ->
                val timestamp = entry.logEntry.timestamp?.format(DateTimeFormatter.ISO_DATE_TIME) ?: "N/A"
                timeline.appendLine("[$timestamp] [${entry.sourceName}] ${entry.logEntry.level}: ${entry.logEntry.message}")
            }
            
            outputFile.writeText(timeline.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
}
