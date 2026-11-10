/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.grok

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GrokPatternCompilerTest {

    private val compiler = GrokPatternCompiler()

    @Test
    fun `named capture is emitted`() {
        val g = compiler.compile("%{WORD:verb}")
        val m = g.match("GET /foo")
        assertEquals(mapOf("verb" to "GET"), m)
    }

    @Test
    fun `anonymous pattern does not capture`() {
        val g = compiler.compile("%{WORD} %{WORD:verb}")
        val m = g.match("foo BAR")
        assertEquals(mapOf("verb" to "BAR"), m)
    }

    @Test
    fun `ipv4 pattern compiles`() {
        val g = compiler.compile("from %{IPV4:client}")
        val m = g.match("from 10.0.0.1")
        assertEquals("10.0.0.1", m?.get("client"))
    }

    @Test
    fun `timestamp iso8601 pattern compiles`() {
        val g = compiler.compile("%{TIMESTAMP_ISO8601:ts}")
        val m = g.match("2026-04-22T10:00:00Z")
        assertEquals("2026-04-22T10:00:00Z", m?.get("ts"))
    }

    @Test
    fun `combined pattern extracts multiple fields`() {
        val g = compiler.compile("%{TIMESTAMP_ISO8601:ts} %{LOGLEVEL:level} %{GREEDYDATA:msg}")
        val m = g.match("2026-04-22T10:00:00Z ERROR something broke")
        assertEquals("ERROR", m?.get("level"))
        assertEquals("something broke", m?.get("msg"))
    }

    @Test
    fun `unknown pattern raises exception`() {
        assertThrows(GrokCompileException::class.java) {
            compiler.compile("%{NOPE}")
        }
    }

    @Test
    fun `unterminated expression raises exception`() {
        assertThrows(GrokCompileException::class.java) {
            compiler.compile("%{WORD")
        }
    }

    @Test
    fun `invalid field name raises exception`() {
        assertThrows(GrokCompileException::class.java) {
            compiler.compile("%{WORD:1bad}")
        }
    }

    @Test
    fun `duplicate field name raises exception`() {
        assertThrows(GrokCompileException::class.java) {
            compiler.compile("%{WORD:x} %{WORD:x}")
        }
    }

    @Test
    fun `cyclic pattern raises exception`() {
        val custom = GrokPatternCompiler(mapOf("LOOP" to "%{LOOP}"))
        assertThrows(GrokCompileException::class.java) { custom.compile("%{LOOP}") }
    }

    @Test
    fun `custom patterns override builtins`() {
        val custom = GrokPatternCompiler(mapOf("WORD" to "X+"))
        val g = custom.compile("%{WORD:w}")
        assertEquals("XXX", g.match("XXX")?.get("w"))
    }

    @Test
    fun `no match returns null`() {
        val g = compiler.compile("%{IPV4:ip}")
        assertNull(g.match("no ip here"))
    }

    @Test
    fun `field list is preserved in declaration order`() {
        val g = compiler.compile("%{WORD:a} %{WORD:b} %{WORD:c}")
        assertEquals(listOf("a", "b", "c"), g.fields)
    }

    @Test
    fun `expansion limit guards against blow up`() {
        val explosive = GrokPatternCompiler(
            mapOf("A" to "%{B}%{B}", "B" to "%{A}%{A}"),
            maxExpansions = 50
        )
        assertThrows(GrokCompileException::class.java) { explosive.compile("%{A}") }
    }

    @Test
    fun `literal text around grok works`() {
        val g = compiler.compile("user=%{WORD:user} ok")
        val m = g.match("user=alice ok")
        assertNotNull(m)
        assertEquals("alice", m?.get("user"))
    }
}
