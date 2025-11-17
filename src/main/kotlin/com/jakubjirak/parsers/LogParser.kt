package com.jakubjirak.parsers

import com.intellij.openapi.Disposable
import java.io.File
import java.io.RandomAccessFile

class LogParser(
    private val file: File,
    private val patternLibrary: PatternLibrary
) : Disposable {
    
    private val maxLinesForFree = 10_000
    private var currentLine = 0
    private var currentStackTrace: StringBuilder? = null
    
    fun parseLines(maxLines: Int = Int.MAX_VALUE): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()
        
        file.bufferedReader().use { reader ->
            var line: String?
            var lineNumber = 0
            
            while (reader.readLine().also { line = it } != null && lineNumber < maxLines) {
                lineNumber++
                line?.let { currentLine ->
                    val entry = parseLine(currentLine, lineNumber)
                    
                    if (patternLibrary.isStackTraceLine(currentLine)) {
                        // Append to previous entry's stack trace
                        if (entries.isNotEmpty()) {
                            val prevEntry = entries.last()
                            val updatedEntry = prevEntry.copy(
                                stackTrace = (prevEntry.stackTrace ?: "") + "\n" + currentLine
                            )
                            entries[entries.lastIndex] = updatedEntry
                        }
                    } else {
                        entries.add(entry)
                    }
                }
            }
        }
        
        return entries
    }
    
    private fun parseLine(line: String, lineNumber: Int): LogEntry {
        val timestamp = patternLibrary.parseTimestamp(line)
        val level = patternLibrary.parseLevel(line)
        val correlationId = patternLibrary.extractCorrelationId(line)
        val threadName = patternLibrary.extractThreadName(line)
        val loggerName = patternLibrary.extractLoggerName(line)
        
        // Extract message (everything after timestamp and level)
        val message = extractMessage(line)
        
        return LogEntry(
            lineNumber = lineNumber,
            timestamp = timestamp,
            level = level,
            message = message,
            threadName = threadName,
            loggerName = loggerName,
            correlationId = correlationId,
            stackTrace = null,
            rawLine = line
        )
    }
    
    private fun extractMessage(line: String): String {
        // Simple heuristic: skip first 50 chars (usually timestamp + level)
        // and take the rest as message
        val potentialMessage = if (line.length > 50) {
            line.substring(50).trim()
        } else {
            line.trim()
        }
        
        return potentialMessage
    }
    
    fun searchCorrelationId(correlationId: String): List<LogEntry> {
        return parseLines().filter { it.correlationId == correlationId }
    }
    
    fun getFileSize(): Long = file.length()
    
    fun getLineCount(): Int {
        var count = 0
        file.bufferedReader().use { reader ->
            while (reader.readLine() != null) {
                count++
            }
        }
        return count
    }
    
    override fun dispose() {
        // Cleanup resources if needed
    }
}
