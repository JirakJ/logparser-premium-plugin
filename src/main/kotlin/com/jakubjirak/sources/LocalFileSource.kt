/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.sources

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogParser
import com.jakubjirak.parsers.PatternLibrary
import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.io.RandomAccessFile

/**
 * [LogSource] backed by a local file on disk.
 *
 * Supports one-shot parsing via [streamLogs]/[tailLogs] and continuous
 * file-following via [tailFollow].
 */
class LocalFileSource(
    private val file: File,
    private val patternLibrary: PatternLibrary
) : BaseLogSource() {

    private val parser = LogParser(file, patternLibrary)

    override fun connect(): Boolean {
        connected = file.exists() && file.canRead()
        return connected
    }

    override fun getName(): String = file.name

    override fun getType(): String = "Local File"

    override fun streamLogs(): Observable<LogEntry> {
        return Observable.create { emitter ->
            try {
                val entries = parser.parseLines()
                entries.forEach { entry ->
                    if (!emitter.isDisposed) emitter.onNext(entry)
                }
                emitter.onComplete()
            } catch (e: Exception) {
                if (!emitter.isDisposed) emitter.onError(e)
            }
        }
    }

    override fun tailLogs(lines: Int): List<LogEntry> {
        return parser.parseLines().takeLast(lines)
    }

    /**
     * Follows the file for new lines (similar to `tail -f`).
     *
     * The returned [Observable] emits new [LogEntry] items whenever the
     * file grows. Polling happens every 100 ms. Dispose the subscription to stop.
     */
    fun tailFollow(): Observable<LogEntry> {
        return Observable.create { emitter ->
            val raf = RandomAccessFile(file, "r")
            var lastPosition = raf.length()
            raf.seek(lastPosition)

            try {
                while (!emitter.isDisposed) {
                    val currentLength = file.length()
                    if (currentLength > lastPosition) {
                        raf.seek(lastPosition)
                        var rawLine: String?
                        var lineNumber = parser.getLineCount()
                        while (raf.readLine().also { rawLine = it } != null) {
                            lineNumber++
                            rawLine?.let { line ->
                                val timestamp = patternLibrary.parseTimestamp(line)
                                val level = patternLibrary.parseLevel(line)
                                val entry = LogEntry(
                                    lineNumber = lineNumber,
                                    timestamp = timestamp,
                                    level = level,
                                    message = line,
                                    threadName = patternLibrary.extractThreadName(line),
                                    loggerName = patternLibrary.extractLoggerName(line),
                                    correlationId = patternLibrary.extractCorrelationId(line),
                                    stackTrace = null,
                                    rawLine = line
                                )
                                if (!emitter.isDisposed) emitter.onNext(entry)
                            }
                        }
                        lastPosition = raf.filePointer
                    }
                    Thread.sleep(100)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                if (!emitter.isDisposed) emitter.onError(e)
            } finally {
                raf.close()
            }
        }
    }
}
