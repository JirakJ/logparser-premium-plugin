/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.parse

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.jakubjirak.logparser.pro.core.LogLevel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Supported structured formats.
 */
enum class StructuredFormat { JSON, LOGFMT, KEY_VALUE, UNSTRUCTURED }

/**
 * Canonical structured view of a log line.
 *
 * @property format         detected format
 * @property timestamp      extracted timestamp, or `null`
 * @property level          extracted [LogLevel], or [LogLevel.UNKNOWN]
 * @property message        best-effort message, or the trimmed raw line
 * @property logger         extracted logger/class/service name, or `null`
 * @property fields         all remaining fields as primitive strings
 * @property raw            original input line
 */
data class StructuredFields(
    val format: StructuredFormat,
    val timestamp: LocalDateTime?,
    val level: LogLevel,
    val message: String,
    val logger: String?,
    val fields: Map<String, String>,
    val raw: String
)

/**
 * Detects and parses structured log lines.
 *
 * Supports JSON object lines (one per line), logfmt
 * (`key=value key="quoted value"`), and loose `key=value` extraction.
 *
 * Design notes from review feedback:
 * - Nested objects/arrays are flattened into dotted-key strings.
 * - `message`, `msg`, `event`, `text` are accepted as message aliases.
 * - `level`, `severity`, `lvl`, `log.level` are accepted as level aliases.
 * - Epoch millis/seconds timestamps are supported.
 * - Duplicate keys: last occurrence wins.
 * - On partial failure the raw line is preserved verbatim.
 */
class StructuredLogParser {

    private val messageKeys = listOf("message", "msg", "event", "text", "log")
    private val levelKeys = listOf("level", "severity", "lvl", "log.level", "loglevel")
    private val loggerKeys = listOf("logger", "logger_name", "class", "service", "component", "source")
    private val timestampKeys = listOf(
        "timestamp", "time", "ts", "@timestamp", "eventTime", "datetime", "date"
    )

    private val isoFormatters = listOf(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT
    )

    /**
     * Detects the most likely format for [line]. Does not parse.
     */
    fun detectFormat(line: String): StructuredFormat {
        val trimmed = line.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return StructuredFormat.JSON
        if (looksLikeLogfmt(trimmed)) return StructuredFormat.LOGFMT
        if (KV_LOOSE.containsMatchIn(trimmed)) return StructuredFormat.KEY_VALUE
        return StructuredFormat.UNSTRUCTURED
    }

    /**
     * Parses [line] into [StructuredFields]. Always returns a value;
     * unparseable input yields [StructuredFormat.UNSTRUCTURED] with
     * the raw line as message.
     */
    fun parse(line: String): StructuredFields {
        return when (detectFormat(line)) {
            StructuredFormat.JSON -> parseJson(line) ?: fallback(line)
            StructuredFormat.LOGFMT -> parseLogfmt(line)
            StructuredFormat.KEY_VALUE -> parseLooseKv(line)
            StructuredFormat.UNSTRUCTURED -> fallback(line)
        }
    }

    private fun fallback(line: String) = StructuredFields(
        format = StructuredFormat.UNSTRUCTURED,
        timestamp = null,
        level = LogLevel.UNKNOWN,
        message = line.trim(),
        logger = null,
        fields = emptyMap(),
        raw = line
    )

    // ---- JSON --------------------------------------------------------------

    private fun parseJson(line: String): StructuredFields? {
        val element: JsonElement = try {
            JsonParser.parseString(line)
        } catch (ex: JsonSyntaxException) {
            return null
        }
        if (!element.isJsonObject) return null
        val flat = mutableMapOf<String, String>()
        flatten(prefix = "", element = element, sink = flat)
        return fromFields(flat, line, StructuredFormat.JSON)
    }

    private fun flatten(prefix: String, element: JsonElement, sink: MutableMap<String, String>) {
        when {
            element.isJsonNull -> sink[prefix] = ""
            element.isJsonPrimitive -> sink[prefix] = element.asJsonPrimitive.asString
            element.isJsonArray -> element.asJsonArray.forEachIndexed { index, child ->
                flatten(if (prefix.isEmpty()) index.toString() else "$prefix.$index", child, sink)
            }
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { (k, v) ->
                flatten(if (prefix.isEmpty()) k else "$prefix.$k", v, sink)
            }
        }
    }

    // ---- logfmt ------------------------------------------------------------

    private fun parseLogfmt(line: String): StructuredFields {
        val fields = LinkedHashMap<String, String>()
        var i = 0
        val n = line.length
        while (i < n) {
            while (i < n && line[i].isWhitespace()) i++
            if (i >= n) break
            val keyStart = i
            while (i < n && line[i] != '=' && !line[i].isWhitespace()) i++
            if (i >= n || line[i] != '=') {
                // bare token — ignore
                while (i < n && !line[i].isWhitespace()) i++
                continue
            }
            val key = line.substring(keyStart, i)
            i++ // consume '='
            val valueBuilder = StringBuilder()
            if (i < n && line[i] == '"') {
                i++
                while (i < n) {
                    val c = line[i]
                    if (c == '\\' && i + 1 < n) {
                        valueBuilder.append(line[i + 1])
                        i += 2
                        continue
                    }
                    if (c == '"') { i++; break }
                    valueBuilder.append(c)
                    i++
                }
            } else {
                while (i < n && !line[i].isWhitespace()) {
                    valueBuilder.append(line[i])
                    i++
                }
            }
            if (key.isNotEmpty()) fields[key] = valueBuilder.toString()
        }
        return fromFields(fields, line, StructuredFormat.LOGFMT)
    }

    private fun looksLikeLogfmt(s: String): Boolean {
        if (s.isEmpty() || s.startsWith("{")) return false
        // Require the line to START with a key=value token so prose like
        // "something went user=alice" falls into the looser KEY_VALUE mode.
        if (!LOGFMT_LEADING.containsMatchIn(s)) return false
        val m = LOGFMT_PAIR.findAll(s).count()
        return m >= 2
    }

    // ---- loose key=value ---------------------------------------------------

    private fun parseLooseKv(line: String): StructuredFields {
        val fields = LinkedHashMap<String, String>()
        KV_LOOSE.findAll(line).forEach { match ->
            val key = match.groupValues[1]
            val quoted = match.groupValues[2]
            val bare = match.groupValues[3]
            fields[key] = if (quoted.isNotEmpty()) quoted else bare
        }
        return fromFields(fields, line, StructuredFormat.KEY_VALUE)
    }

    // ---- shared extraction -------------------------------------------------

    private fun fromFields(
        fields: Map<String, String>,
        raw: String,
        format: StructuredFormat
    ): StructuredFields {
        val lower = fields.mapKeys { it.key.lowercase() }
        val message = firstKey(lower, messageKeys) ?: raw.trim()
        val level = parseLevel(firstKey(lower, levelKeys))
        val logger = firstKey(lower, loggerKeys)
        val timestamp = firstKey(lower, timestampKeys)?.let { parseTimestamp(it) }
        val consumed = buildSet {
            addAll(messageKeys); addAll(levelKeys); addAll(loggerKeys); addAll(timestampKeys)
        }
        val residue = fields.filterKeys { it.lowercase() !in consumed }
        return StructuredFields(format, timestamp, level, message, logger, residue, raw)
    }

    private fun firstKey(lower: Map<String, String>, keys: List<String>): String? =
        keys.asSequence().mapNotNull { lower[it] }.firstOrNull { it.isNotEmpty() }

    private fun parseLevel(s: String?): LogLevel {
        if (s.isNullOrBlank()) return LogLevel.UNKNOWN
        return when (s.trim().uppercase()) {
            "TRACE", "TRC" -> LogLevel.TRACE
            "DEBUG", "DBG" -> LogLevel.DEBUG
            "INFO", "INFORMATION", "NOTICE" -> LogLevel.INFO
            "WARN", "WARNING" -> LogLevel.WARN
            "ERROR", "ERR", "SEVERE" -> LogLevel.ERROR
            "FATAL", "CRITICAL", "CRIT", "EMERG", "EMERGENCY", "ALERT" -> LogLevel.FATAL
            else -> LogLevel.UNKNOWN
        }
    }

    private fun parseTimestamp(value: String): LocalDateTime? {
        val trimmed = value.trim().trim('"')
        if (trimmed.isEmpty()) return null
        // epoch variants
        trimmed.toLongOrNull()?.let { epoch ->
            val instant = when {
                trimmed.length <= 10 -> Instant.ofEpochSecond(epoch)
                trimmed.length <= 13 -> Instant.ofEpochMilli(epoch)
                else -> Instant.ofEpochSecond(epoch / 1_000_000_000L, (epoch % 1_000_000_000L))
            }
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        }
        for (fmt in isoFormatters) {
            try {
                return when (fmt) {
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME -> LocalDateTime.parse(trimmed, fmt)
                    DateTimeFormatter.ISO_INSTANT -> LocalDateTime.ofInstant(
                        Instant.parse(trimmed), ZoneId.systemDefault()
                    )
                    else -> ZonedDateTime.parse(trimmed, fmt).toLocalDateTime()
                }
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return null
    }

    companion object {
        // key=quoted OR key=bare; avoids digits-only keys; limits value length for safety.
        private val KV_LOOSE = Regex(
            """\b([A-Za-z_][A-Za-z0-9_.\-]{0,63})=(?:"((?:[^"\\]|\\.){0,512})"|([^\s,;]{1,512}))"""
        )
        private val LOGFMT_PAIR = Regex(
            """\b[A-Za-z_][A-Za-z0-9_.\-]{0,63}=(?:"(?:[^"\\]|\\.){0,512}"|[^\s]{1,512})"""
        )
        private val LOGFMT_LEADING = Regex(
            """^\s*[A-Za-z_][A-Za-z0-9_.\-]{0,63}="""
        )
    }
}
