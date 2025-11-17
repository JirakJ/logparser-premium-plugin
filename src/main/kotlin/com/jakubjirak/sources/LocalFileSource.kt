package com.jakubjirak.sources

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogParser
import com.jakubjirak.parsers.PatternLibrary
import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.io.RandomAccessFile

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
                    if (!emitter.isDisposed) {
                        emitter.onNext(entry)
                    }
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }
    
    override fun tailLogs(lines: Int): List<LogEntry> {
        val allEntries = parser.parseLines()
        return allEntries.takeLast(lines)
    }
    
    fun tailFollow(): Observable<LogEntry> {
        return Observable.create { emitter ->
            val raf = RandomAccessFile(file, "r")
            var lastPosition = raf.length()
            raf.seek(lastPosition)
            
            try {
                while (!emitter.isDisposed) {
                    val currentLength = raf.length()
                    if (currentLength > lastPosition) {
                        raf.seek(lastPosition)
                        var line: String?
                        while (raf.readLine().also { line = it } != null) {
                            // Parse line and emit
                            // Simplified for now
                            if (!emitter.isDisposed) {
                                // TODO: Parse line to LogEntry
                            }
                        }
                        lastPosition = raf.filePointer
                    }
                    Thread.sleep(100) // Poll every 100ms
                }
            } catch (e: Exception) {
                emitter.onError(e)
            } finally {
                raf.close()
            }
        }
    }
}
