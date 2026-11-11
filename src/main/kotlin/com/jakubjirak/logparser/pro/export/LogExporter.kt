/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.export

import com.google.gson.GsonBuilder
import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import java.time.format.DateTimeFormatter

/** Target export format. */
enum class ExportFormat { CSV, JSON, HTML }

/**
 * Writes [LogEntry] collections to CSV / JSON / HTML.
 *
 * Security hardening (per design review):
 * - **CSV formula injection** is mitigated: cells starting with `=`, `+`, `-`, `@`
 *   or a TAB/CR are prefixed with a single quote (`'`) before quoting. This is the
 *   OWASP-recommended mitigation for spreadsheet CSV consumers.
 * - **HTML injection** is mitigated: every value passes through [htmlEscape]
 *   before being inserted into the template.
 * - **JSON** uses Gson, which escapes control and non-ASCII characters.
 *
 * None of these exporters perform any I/O. Callers pick the target stream and
 * are responsible for ensuring they run off the UI thread.
 */
class LogExporter {

    /** Returns the configured rendering of [entries] in [format]. */
    fun export(entries: List<LogEntry>, format: ExportFormat): String = when (format) {
        ExportFormat.CSV -> toCsv(entries)
        ExportFormat.JSON -> toJson(entries)
        ExportFormat.HTML -> toHtml(entries)
    }

    // ---- CSV ---------------------------------------------------------------

    fun toCsv(entries: List<LogEntry>): String {
        val header = listOf("line", "timestamp", "level", "logger", "thread", "correlationId", "message", "source")
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append("\r\n")
        for (e in entries) {
            sb.append(csvField(e.lineNumber.toString())).append(',')
            sb.append(csvField(e.timestamp?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).orEmpty())).append(',')
            sb.append(csvField(e.level.name)).append(',')
            sb.append(csvField(e.loggerName.orEmpty())).append(',')
            sb.append(csvField(e.threadName.orEmpty())).append(',')
            sb.append(csvField(e.correlationId.orEmpty())).append(',')
            sb.append(csvField(e.message)).append(',')
            sb.append(csvField(e.source))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    // ---- JSON --------------------------------------------------------------

    fun toJson(entries: List<LogEntry>): String {
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val dto = entries.map { e ->
            linkedMapOf<String, Any?>(
                "line" to e.lineNumber,
                "timestamp" to e.timestamp?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "level" to e.level.name,
                "logger" to e.loggerName,
                "thread" to e.threadName,
                "correlationId" to e.correlationId,
                "message" to e.message,
                "stackTrace" to e.stackTrace,
                "source" to e.source.ifEmpty { null },
                "metadata" to e.metadata.takeIf { it.isNotEmpty() }
            )
        }
        return gson.toJson(dto)
    }

    // ---- HTML --------------------------------------------------------------

    fun toHtml(entries: List<LogEntry>, title: String = "LogParser Pro Export"): String {
        val sb = StringBuilder(1024 + entries.size * 128)
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
        sb.append("<title>").append(htmlEscape(title)).append("</title>")
        sb.append("<style>")
        sb.append(DEFAULT_CSS)
        sb.append("</style></head><body>")
        sb.append("<h1>").append(htmlEscape(title)).append("</h1>")
        sb.append("<p class=\"meta\">Entries: ").append(entries.size).append("</p>")
        sb.append("<table><thead><tr>")
        listOf("#", "Timestamp", "Level", "Logger", "Thread", "Correlation ID", "Message").forEach {
            sb.append("<th>").append(htmlEscape(it)).append("</th>")
        }
        sb.append("</tr></thead><tbody>")
        for (e in entries) {
            sb.append("<tr class=\"lvl-").append(cssLevelClass(e.level)).append("\">")
            sb.append(td(e.lineNumber.toString()))
            sb.append(td(e.timestamp?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).orEmpty()))
            sb.append(td(e.level.name))
            sb.append(td(e.loggerName.orEmpty()))
            sb.append(td(e.threadName.orEmpty()))
            sb.append(td(e.correlationId.orEmpty()))
            sb.append(td(e.message))
            sb.append("</tr>")
            if (!e.stackTrace.isNullOrBlank()) {
                sb.append("<tr class=\"trace\"><td colspan=\"7\"><pre>")
                sb.append(htmlEscape(e.stackTrace))
                sb.append("</pre></td></tr>")
            }
        }
        sb.append("</tbody></table></body></html>")
        return sb.toString()
    }

    // ---- helpers -----------------------------------------------------------

    /**
     * RFC 4180 CSV field quoting **plus** OWASP CSV-injection mitigation.
     * Exposed for tests.
     */
    fun csvField(value: String): String {
        val safe = csvInjectionEscape(value)
        val needsQuote = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return safe
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun csvInjectionEscape(value: String): String {
        if (value.isEmpty()) return value
        return if (value[0] in FORMULA_TRIGGERS) "'$value" else value
    }

    /** Escapes `&`, `<`, `>`, `"`, `'` for safe HTML insertion. Exposed for tests. */
    fun htmlEscape(value: String): String {
        if (value.isEmpty()) return value
        val sb = StringBuilder(value.length + 16)
        for (c in value) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun td(value: String): String = "<td>${htmlEscape(value)}</td>"

    private fun cssLevelClass(level: LogLevel): String = level.name.lowercase()

    companion object {
        private val DEFAULT_CSS = """
            body{font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;margin:1.5rem;background:#fafafa;color:#222;}
            h1{margin:0 0 .5rem 0;font-size:1.25rem;}
            .meta{color:#666;margin:0 0 1rem 0;}
            table{border-collapse:collapse;width:100%;font-size:12px;background:#fff;}
            th,td{border:1px solid #ddd;padding:4px 8px;vertical-align:top;text-align:left;}
            th{background:#f0f0f0;position:sticky;top:0;}
            tr.lvl-error td, tr.lvl-fatal td{background:#ffecec;}
            tr.lvl-warn td{background:#fff8d6;}
            tr.lvl-debug td{color:#666;}
            tr.lvl-trace td{color:#999;}
            tr.trace td{background:#fbfbfb;color:#555;}
            pre{margin:0;white-space:pre-wrap;word-break:break-word;font-family:SFMono-Regular,Consolas,Menlo,monospace;}
        """.trimIndent()

        /** Leading characters that make spreadsheet apps evaluate a cell as a formula (OWASP CSV injection). */
        private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@', '\t', '\r')
    }
}
