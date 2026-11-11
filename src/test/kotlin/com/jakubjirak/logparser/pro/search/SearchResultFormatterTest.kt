/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchResultFormatterTest {

    private val engine = SearchEngine()

    private fun hit(line: Int, content: String, fields: Map<String, String> = emptyMap()) =
        SearchEngine.SearchResult(line, content, emptyList(), emptyList(), 1.0, emptyList(), fields)

    @Test
    fun `no results message names the pattern`() {
        assertEquals("No matches found for: foo", SearchResultFormatter.format("foo", emptyList()))
    }

    @Test
    fun `lists hits with one-based line numbers`() {
        val text = SearchResultFormatter.format("x", listOf(hit(0, "a x"), hit(4, "b x")))
        assertTrue(text.startsWith("Found 2 matches:"))
        assertTrue(text.contains("Line 1: a x"))
        assertTrue(text.contains("Line 5: b x"))
        assertFalse(text.contains("="))
    }

    @Test
    fun `grok fields are listed under the hit in order`() {
        val fields = linkedMapOf("level" to "WARN", "msg" to "slow")
        val text = SearchResultFormatter.format("p", listOf(hit(2, "WARN slow", fields)))
        assertTrue(text.contains("Line 3: WARN slow\n    level=WARN  msg=slow\n"))
    }

    @Test
    fun `caps the listing and summarises the remainder`() {
        val many = (0 until SearchResultFormatter.MAX_LISTED + 5).map { hit(it, "line $it") }
        val text = SearchResultFormatter.format("line", many)
        assertTrue(text.contains("Line ${SearchResultFormatter.MAX_LISTED}: "))
        assertFalse(text.contains("Line ${SearchResultFormatter.MAX_LISTED + 1}: "))
        assertTrue(text.contains("... and 5 more matches"))
    }

    @Test
    fun `invalid regex yields a message instead of an exception`() {
        val query = SearchEngine.SearchQuery(pattern = "([unclosed", isRegex = true)
        val text = SearchResultFormatter.searchAndFormat(engine, listOf("x"), query)
        assertTrue(text.startsWith("Invalid pattern: "), text)
    }

    @Test
    fun `invalid grok yields a message instead of an exception`() {
        val query = SearchEngine.SearchQuery(pattern = "%{UNKNOWN:x}", isGrok = true)
        val text = SearchResultFormatter.searchAndFormat(engine, listOf("x"), query)
        assertEquals("Invalid Grok pattern: Unknown grok pattern 'UNKNOWN'", text)
    }

    @Test
    fun `grok search end to end shows extracted fields`() {
        val query = SearchEngine.SearchQuery(pattern = "%{LOGLEVEL:level} %{GREEDYDATA:msg}", isGrok = true)
        val text = SearchResultFormatter.searchAndFormat(engine, listOf("10:00 ERROR boom"), query)
        assertTrue(text.contains("Line 1: 10:00 ERROR boom"))
        assertTrue(text.contains("level=ERROR  msg=boom"))
    }
}
