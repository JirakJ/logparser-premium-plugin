package com.jakubjirak.sources

import com.jakubjirak.parsers.LogEntry
import io.reactivex.rxjava3.core.Observable

interface LogSource {
    fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun getName(): String
    fun getType(): String
    fun streamLogs(): Observable<LogEntry>
    fun tailLogs(lines: Int = 100): List<LogEntry>
}

abstract class BaseLogSource : LogSource {
    protected var connected = false
    
    override fun isConnected(): Boolean = connected
    
    override fun disconnect() {
        connected = false
    }
}
