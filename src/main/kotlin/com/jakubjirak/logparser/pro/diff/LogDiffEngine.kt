/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.diff

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel

/**
 * A single diff occurrence.
 *
 * @property signature normalised (level, message-template, logger) signature
 * @property sample    representative entry
 * @property count     how many entries with this signature existed on one side
 */
data class DiffOccurrence(val signature: String, val sample: LogEntry, val count: Int)

/**
 * Result of diffing two log entry streams.
 */
data class DiffResult(
    val added: List<DiffOccurrence>,
    val removed: List<DiffOccurrence>,
    val common: List<DiffOccurrence>,
    val addedByLevel: Map<LogLevel, Int>,
    val removedByLevel: Map<LogLevel, Int>
)

/**
 * Compares two lists of [LogEntry] by their normalised signature.
 *
 * Signature: `level | logger | normalizedMessage`
 *
 * Normalisation:
 * - UUIDs → `?`
 * - ISO timestamps → `?`
 * - IPv4 → `?`
 * - hex addresses `0x...` → `?`
 * - any run of digits with length 2+ → `#`
 * - whitespace collapsed
 *
 * An entry appears in [DiffResult.added] when it exists in `right` but not in `left`,
 * in [DiffResult.removed] when it exists in `left` but not in `right`, and in
 * [DiffResult.common] when both sides share the signature. Counts in [DiffOccurrence]
 * reflect occurrences on the side being reported (added → right, removed → left,
 * common → sum of both sides).
 */
class LogDiffEngine {

    fun diff(left: List<LogEntry>, right: List<LogEntry>): DiffResult {
        val leftBuckets = bucket(left)
        val rightBuckets = bucket(right)
        val leftKeys = leftBuckets.keys
        val rightKeys = rightBuckets.keys
        val added = (rightKeys - leftKeys).map {
            val group = rightBuckets.getValue(it)
            DiffOccurrence(it, group.first(), group.size)
        }.sortedByDescending { it.count }
        val removed = (leftKeys - rightKeys).map {
            val group = leftBuckets.getValue(it)
            DiffOccurrence(it, group.first(), group.size)
        }.sortedByDescending { it.count }
        val common = (leftKeys.intersect(rightKeys)).map {
            val lg = leftBuckets.getValue(it)
            val rg = rightBuckets.getValue(it)
            DiffOccurrence(it, rg.first(), lg.size + rg.size)
        }.sortedByDescending { it.count }
        return DiffResult(
            added = added,
            removed = removed,
            common = common,
            addedByLevel = added.groupingBy { it.sample.level }.eachCount(),
            removedByLevel = removed.groupingBy { it.sample.level }.eachCount()
        )
    }

    /**
     * Public so callers (e.g. exporter) can report signatures consistently.
     */
    fun signatureOf(entry: LogEntry): String {
        val msg = normalize(entry.message)
        val logger = entry.loggerName.orEmpty()
        return "${entry.level.name} | $logger | $msg"
    }

    private fun bucket(entries: List<LogEntry>): Map<String, List<LogEntry>> =
        entries.groupBy { signatureOf(it) }

    private fun normalize(msg: String): String {
        var s = msg
        s = UUID_RE.replace(s, "?")
        s = ISO_TS_RE.replace(s, "?")
        s = IPV4_RE.replace(s, "?")
        s = HEX_ADDR_RE.replace(s, "?")
        s = DIGITS_RE.replace(s, "#")
        s = WS_RE.replace(s, " ").trim()
        return s
    }

    companion object {
        private val UUID_RE =
            Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")
        private val ISO_TS_RE =
            Regex("""\b\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+\-]\d{2}:?\d{2})?\b""")
        private val IPV4_RE = Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""")
        private val HEX_ADDR_RE = Regex("""\b0x[0-9a-fA-F]{2,}\b""")
        private val DIGITS_RE = Regex("""\b\d{2,}\b""")
        private val WS_RE = Regex("""\s+""")
    }
}
