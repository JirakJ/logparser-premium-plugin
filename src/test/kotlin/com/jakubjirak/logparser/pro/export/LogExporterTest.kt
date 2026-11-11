/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.export

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LogExporterTest {

    private val exporter = LogExporter()

    private fun sample(): LogEntry = LogEntry(
        lineNumber = 1,
        timestamp = LocalDateTime.of(2026, 4, 22, 10, 0, 0),
        level = LogLevel.ERROR,
        message = "boom, with \"quotes\" and <script>",
        threadName = "main",
        loggerName = "com.foo.Bar",
        correlationId = "abc-1",
        stackTrace = "java.lang.Error: e",
        rawLine = "raw"
    )

    // ---- CSV ---------------------------------------------------------------

    @Test
    fun `csv contains header row`() {
        val csv = exporter.toCsv(listOf(sample()))
        val firstLine = csv.lineSequence().first()
        assertTrue(firstLine.contains("line"))
        assertTrue(firstLine.contains("timestamp"))
        assertTrue(firstLine.contains("message"))
    }

    @Test
    fun `csv quotes fields containing delimiter or quote`() {
        assertEquals(""""a,b"""", exporter.csvField("a,b"))
        assertEquals("""" ""q"" """", exporter.csvField(""" "q" """))
    }

    @Test
    fun `csv mitigates formula injection`() {
        assertEquals("'=SUM(A1)", exporter.csvField("=SUM(A1)").trim('"'))
        assertEquals("'+cmd", exporter.csvField("+cmd").trim('"'))
        assertEquals("'-cmd", exporter.csvField("-cmd").trim('"'))
        assertEquals("'@cmd", exporter.csvField("@cmd").trim('"'))
    }

    @Test
    fun `csv plain value is returned unchanged`() {
        assertEquals("ok", exporter.csvField("ok"))
    }

    @Test
    fun `csv rows end with CRLF`() {
        val csv = exporter.toCsv(listOf(sample()))
        assertTrue(csv.contains("\r\n"))
    }

    // ---- JSON --------------------------------------------------------------

    @Test
    fun `json is valid array`() {
        val json = exporter.toJson(listOf(sample()))
        assertTrue(json.trim().startsWith("["))
        assertTrue(json.trim().endsWith("]"))
        assertTrue(json.contains("\"level\""))
        assertTrue(json.contains("\"ERROR\""))
    }

    @Test
    fun `json escapes quotes inside message`() {
        val json = exporter.toJson(listOf(sample()))
        assertTrue(json.contains("\\\"quotes\\\""))
    }

    @Test
    fun `json empty list`() {
        val json = exporter.toJson(emptyList())
        assertEquals("[]", json.trim())
    }

    // ---- HTML --------------------------------------------------------------

    @Test
    fun `html escapes tags in message`() {
        val html = exporter.toHtml(listOf(sample()))
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test
    fun `html escapes ampersands and quotes`() {
        assertEquals("a &amp; b", exporter.htmlEscape("a & b"))
        assertEquals("&quot;x&quot;", exporter.htmlEscape("\"x\""))
        assertEquals("&#39;x&#39;", exporter.htmlEscape("'x'"))
    }

    @Test
    fun `html is standalone document`() {
        val html = exporter.toHtml(listOf(sample()))
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<table"))
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun `html includes stack trace in trace row`() {
        val html = exporter.toHtml(listOf(sample()))
        assertTrue(html.contains("java.lang.Error"))
        assertTrue(html.contains("class=\"trace\""))
    }

    @Test
    fun `html level class is emitted`() {
        val html = exporter.toHtml(listOf(sample()))
        assertTrue(html.contains("lvl-error"))
    }

    // ---- dispatcher --------------------------------------------------------

    @Test
    fun `export dispatches to correct format`() {
        assertTrue(exporter.export(listOf(sample()), ExportFormat.CSV).contains("line,"))
        assertTrue(exporter.export(listOf(sample()), ExportFormat.JSON).trim().startsWith("["))
        assertTrue(exporter.export(listOf(sample()), ExportFormat.HTML).startsWith("<!DOCTYPE"))
    }

    @Test
    fun `csvField neutralises every formula trigger`() {
        val exporter = LogExporter()
        listOf("=1+1", "+1", "-1", "@SUM(A1)").forEach {
            assertEquals("'$it", exporter.csvField(it), it)
        }
        assertEquals("'\tx", exporter.csvField("\tx"))
        assertEquals("safe", exporter.csvField("safe"))
        assertEquals("", exporter.csvField(""))
    }
}
