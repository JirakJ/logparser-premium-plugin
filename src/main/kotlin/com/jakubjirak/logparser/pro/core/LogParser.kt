/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import com.intellij.openapi.Disposable
import com.jakubjirak.logparser.pro.io.GzipLogReader
import java.io.File

/**
 * Parses a log file into a list of [LogEntry] objects.
 *
 * Uses a [PatternLibrary] to detect timestamps, log levels, correlation IDs,
 * thread names, and logger names. Consecutive stack-trace lines are folded
 * into the preceding entry's [LogEntry.stackTrace] field.
 */
class LogParser(
    private val file: File,
    private val patternLibrary: PatternLibrary
) : Disposable {

    /**
     * Reads and parses up to [maxLines] lines from the underlying file.
     *
     * Stack trace lines (lines starting with `at `, `Caused by:`, or `...`)
     * are appended to the previous entry rather than creating new entries.
     */
    fun parseLines(maxLines: Int = Int.MAX_VALUE): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()
        openReader().use { reader ->
            var lineNumber = 0
            reader.forEachLine { currentLine ->
                if (lineNumber >= maxLines) return@forEachLine
                lineNumber++
                if (patternLibrary.isStackTraceLine(currentLine) && entries.isNotEmpty()) {
                    val prevEntry = entries.last()
                    entries[entries.lastIndex] = prevEntry.copy(
                        stackTrace = (prevEntry.stackTrace ?: "") + "\n" + currentLine
                    )
                } else {
                    entries.add(parseLine(currentLine, lineNumber))
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

        return LogEntry(
            lineNumber = lineNumber,
            timestamp = timestamp,
            level = level,
            message = extractMessage(line),
            threadName = threadName,
            loggerName = loggerName,
            correlationId = correlationId,
            stackTrace = null,
            rawLine = line,
            source = file.name
        )
    }

    /**
     * Heuristic message extraction: strips timestamp and metadata prefix
     * (typically the first ~50 characters) from the raw line.
     */
    private fun extractMessage(line: String): String {
        return if (line.length > 50) line.substring(50).trim() else line.trim()
    }

    /** Returns all entries whose [LogEntry.correlationId] equals [correlationId]. */
    fun searchCorrelationId(correlationId: String): List<LogEntry> {
        return parseLines().filter { it.correlationId == correlationId }
    }

    /** File size in bytes. */
    fun getFileSize(): Long = file.length()

    /** Total number of lines in the file. */
    fun getLineCount(): Int {
        var count = 0
        openReader().use { reader ->
            while (reader.readLine() != null) count++
        }
        return count
    }

    /**
     * Opens a reader over the underlying file, transparently decompressing
     * the stream when the file is gzip-encoded.
     */
    private fun openReader() = GzipLogReader.open(file)

    override fun dispose() {
        // No persistent resources to release
    }
}
