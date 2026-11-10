/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.parse

import com.jakubjirak.logparser.pro.core.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructuredLogParserTest {

    private val parser = StructuredLogParser()

    @Test
    fun `detects JSON format`() {
        assertEquals(
            StructuredFormat.JSON,
            parser.detectFormat("""{"msg":"hi","level":"INFO"}""")
        )
    }

    @Test
    fun `detects logfmt format`() {
        assertEquals(
            StructuredFormat.LOGFMT,
            parser.detectFormat("""level=info msg="hello world" logger=Main""")
        )
    }

    @Test
    fun `detects key value loose format when only one pair is present`() {
        assertEquals(
            StructuredFormat.KEY_VALUE,
            parser.detectFormat("user=alice logged in")
        )
    }

    @Test
    fun `unstructured fallback for plain text`() {
        val result = parser.parse("just a plain log line, nothing to see")
        assertEquals(StructuredFormat.UNSTRUCTURED, result.format)
        assertEquals("just a plain log line, nothing to see", result.message)
        assertEquals(LogLevel.UNKNOWN, result.level)
    }

    @Test
    fun `parses JSON with standard fields`() {
        val line = """{"timestamp":"2026-04-22T10:00:00Z","level":"WARN","message":"db slow","logger":"db.Pool","duration_ms":742}"""
        val result = parser.parse(line)
        assertEquals(StructuredFormat.JSON, result.format)
        assertEquals(LogLevel.WARN, result.level)
        assertEquals("db slow", result.message)
        assertEquals("db.Pool", result.logger)
        assertNotNull(result.timestamp)
        assertEquals("742", result.fields["duration_ms"])
    }

    @Test
    fun `JSON level alias severity is recognized`() {
        val result = parser.parse("""{"severity":"error","msg":"boom"}""")
        assertEquals(LogLevel.ERROR, result.level)
        assertEquals("boom", result.message)
    }

    @Test
    fun `JSON flattens nested objects`() {
        val result = parser.parse("""{"user":{"id":42,"name":"alice"},"message":"login"}""")
        assertEquals("42", result.fields["user.id"])
        assertEquals("alice", result.fields["user.name"])
    }

    @Test
    fun `JSON epoch seconds timestamp parses`() {
        val result = parser.parse("""{"ts":1714003200,"message":"x"}""")
        assertNotNull(result.timestamp)
    }

    @Test
    fun `JSON epoch millis timestamp parses`() {
        val result = parser.parse("""{"ts":1714003200123,"message":"x"}""")
        assertNotNull(result.timestamp)
    }

    @Test
    fun `parses logfmt with quoted value containing spaces`() {
        val line = """level=warn msg="user signed in from new device" user=alice"""
        val result = parser.parse(line)
        assertEquals(StructuredFormat.LOGFMT, result.format)
        assertEquals(LogLevel.WARN, result.level)
        assertEquals("user signed in from new device", result.message)
        assertEquals("alice", result.fields["user"])
    }

    @Test
    fun `logfmt handles escaped quotes`() {
        val line = """level=info msg="say \"hi\" back""""
        val result = parser.parse(line)
        assertEquals("""say "hi" back""", result.message)
    }

    @Test
    fun `loose key value extracts inline fields`() {
        val result = parser.parse("something went wrong user=alice request=req-7")
        assertEquals(StructuredFormat.KEY_VALUE, result.format)
        assertEquals("alice", result.fields["user"])
        assertEquals("req-7", result.fields["request"])
    }

    @Test
    fun `malformed JSON falls back to unstructured`() {
        val result = parser.parse("""{"broken":""")
        assertEquals(StructuredFormat.UNSTRUCTURED, result.format)
    }

    @Test
    fun `empty input is handled`() {
        val result = parser.parse("")
        assertEquals(StructuredFormat.UNSTRUCTURED, result.format)
        assertEquals("", result.message)
    }

    @Test
    fun `level mapping covers all aliases`() {
        listOf("trace" to LogLevel.TRACE, "debug" to LogLevel.DEBUG, "info" to LogLevel.INFO,
               "warning" to LogLevel.WARN, "error" to LogLevel.ERROR, "critical" to LogLevel.FATAL)
            .forEach { (alias, expected) ->
                val result = parser.parse("""{"level":"$alias","message":"m"}""")
                assertEquals(expected, result.level, "alias=$alias")
            }
    }

    @Test
    fun `residue fields exclude consumed canonical keys`() {
        val result = parser.parse("""{"level":"info","message":"m","extra":"keep"}""")
        assertTrue(result.fields.containsKey("extra"))
        assertNull(result.fields["level"])
        assertNull(result.fields["message"])
    }
}
