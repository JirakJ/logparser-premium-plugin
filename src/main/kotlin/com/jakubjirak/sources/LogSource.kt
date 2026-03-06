/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.sources

import com.jakubjirak.parsers.LogEntry
import io.reactivex.rxjava3.core.Observable

/** Contract for all log sources (local files, remote servers, containers, etc.). */
interface LogSource {
    /** Establishes a connection. Returns `true` on success. */
    fun connect(): Boolean

    /** Closes the connection and releases resources. */
    fun disconnect()

    /** Whether the source is currently connected. */
    fun isConnected(): Boolean

    /** Human-readable name of this source. */
    fun getName(): String

    /** Type label (e.g. "Local File", "Kubernetes Pod"). */
    fun getType(): String

    /** Returns an [Observable] that emits parsed [LogEntry] items as they are read. */
    fun streamLogs(): Observable<LogEntry>

    /** Returns the last [lines] entries from this source. */
    fun tailLogs(lines: Int = 100): List<LogEntry>
}

/** Base implementation with shared connected-state bookkeeping. */
abstract class BaseLogSource : LogSource {
    protected var connected = false

    override fun isConnected(): Boolean = connected

    override fun disconnect() {
        connected = false
    }
}
