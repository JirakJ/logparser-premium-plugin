/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.grouping

import com.jakubjirak.logparser.pro.core.LogEntry
import java.time.LocalDateTime

/**
 * Aggregated occurrences of a single exception signature.
 *
 * @property signature      canonicalised trace signature (see [ExceptionGrouper.signatureOf])
 * @property exceptionClass detected exception class name (best effort)
 * @property rootCause      normalised root-cause class, or `null`
 * @property count          number of [LogEntry] instances grouped
 * @property firstSeen      earliest [LogEntry.timestamp] in the group
 * @property lastSeen       latest [LogEntry.timestamp] in the group
 * @property sampleLineNumbers up to [ExceptionGrouper.MAX_SAMPLES] line numbers
 * @property exemplar       one representative [LogEntry]
 */
data class ExceptionGroup(
    val signature: String,
    val exceptionClass: String?,
    val rootCause: String?,
    val count: Int,
    val firstSeen: LocalDateTime?,
    val lastSeen: LocalDateTime?,
    val sampleLineNumbers: List<Int>,
    val exemplar: LogEntry
)

/**
 * Groups log entries that contain stack traces by a normalised signature.
 *
 * Improved signature (per design review):
 * `{exceptionClass} | {normalizedMessage} | {firstAppFrame} | cause:{rootCauseClass}`
 *
 * Normalisation rules:
 * - Line numbers in `(File.kt:123)` are replaced by `(File.kt:#)`.
 * - Hex addresses `0x[0-9a-fA-F]+` become `0x#`.
 * - Lambda / synthetic class suffixes (`$$Lambda$42/0x0000...`) are stripped.
 * - CGLIB/ByteBuddy enhancer suffixes (`$$EnhancerBy...`) are stripped.
 * - Numeric IDs inside quoted strings are replaced with `#`.
 * - Paths are trimmed to the file name only.
 */
class ExceptionGrouper {

    /**
     * Groups [entries] that contain a non-blank [LogEntry.stackTrace].
     * Entries without a stack trace are ignored.
     */
    fun group(entries: List<LogEntry>): List<ExceptionGroup> {
        val buckets = linkedMapOf<String, MutableList<LogEntry>>()
        entries
            .filter { !it.stackTrace.isNullOrBlank() }
            .forEach { entry -> buckets.getOrPut(signatureOf(entry.stackTrace!!)) { mutableListOf() }.add(entry) }
        return buckets.map { (sig, group) ->
            val times = group.mapNotNull { it.timestamp }
            val exemplar = group.first()
            ExceptionGroup(
                signature = sig,
                exceptionClass = extractExceptionClass(exemplar.stackTrace),
                rootCause = extractRootCause(exemplar.stackTrace),
                count = group.size,
                firstSeen = times.minOrNull(),
                lastSeen = times.maxOrNull(),
                sampleLineNumbers = group.take(MAX_SAMPLES).map { it.lineNumber },
                exemplar = exemplar
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Computes a stable signature for a stack trace string.
     * Public for testability.
     */
    fun signatureOf(stackTrace: String): String {
        val cleaned = normalize(stackTrace)
        val lines = cleaned.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "unknown"
        val exceptionClass = extractExceptionClass(cleaned) ?: "unknown"
        val message = extractExceptionMessage(cleaned)?.take(MAX_MESSAGE_LEN) ?: ""
        val firstFrame = lines.firstOrNull { it.trimStart().startsWith("at ") }?.trim().orEmpty()
        val rootCause = extractRootCause(cleaned) ?: ""
        return "$exceptionClass | $message | $firstFrame | cause:$rootCause"
    }

    private fun normalize(trace: String): String {
        var s = trace
        s = LAMBDA_SUFFIX.replace(s, "")
        s = ENHANCER_SUFFIX.replace(s, "")
        s = HEX_ADDRESS.replace(s, "0x#")
        s = LINE_NUMBER_IN_PAREN.replace(s, "($1:#)")
        s = NUMERIC_ID_IN_QUOTE.replace(s, "'#'")
        return s
    }

    private fun extractExceptionClass(trace: String?): String? {
        if (trace == null) return null
        // first line usually "org.pkg.FooException: message"
        val head = trace.lines().firstOrNull { it.isNotBlank() }?.trim() ?: return null
        val match = EXCEPTION_HEAD.find(head) ?: return null
        return match.groupValues[1]
    }

    private fun extractExceptionMessage(trace: String): String? {
        val head = trace.lines().firstOrNull { it.isNotBlank() }?.trim() ?: return null
        val colon = head.indexOf(':')
        if (colon < 0 || colon == head.length - 1) return null
        return head.substring(colon + 1).trim().ifEmpty { null }
    }

    private fun extractRootCause(trace: String?): String? {
        if (trace == null) return null
        val causes = trace.lines()
            .mapNotNull { CAUSED_BY.find(it.trim())?.groupValues?.get(1) }
        return causes.lastOrNull()
    }

    companion object {
        const val MAX_SAMPLES: Int = 5
        const val MAX_MESSAGE_LEN: Int = 160

        private val HEX_ADDRESS = Regex("""0x[0-9a-fA-F]{4,}""")
        private val LINE_NUMBER_IN_PAREN =
            Regex("""\(([A-Za-z_][A-Za-z0-9_]{0,127}\.(?:kt|java|scala|groovy|py|js|ts)):\d+\)""")
        private val LAMBDA_SUFFIX =
            Regex("""\${'$'}\${'$'}Lambda\${'$'}\d+(?:/0x[0-9a-fA-F]+)?""")
        private val ENHANCER_SUFFIX =
            Regex("""\${'$'}\${'$'}(?:EnhancerBy|EnhancerByCGLIB|FastClassBy)[A-Za-z0-9_${'$'}]{0,64}""")
        private val NUMERIC_ID_IN_QUOTE = Regex("""'\d{2,}'""")
        private val EXCEPTION_HEAD =
            Regex("""([A-Za-z_][A-Za-z0-9_.]{0,255}(?:Exception|Error|Throwable|Fault))\b""")
        private val CAUSED_BY =
            Regex("""^Caused by:\s*([A-Za-z_][A-Za-z0-9_.]{0,255}(?:Exception|Error|Throwable|Fault))""")
    }
}
