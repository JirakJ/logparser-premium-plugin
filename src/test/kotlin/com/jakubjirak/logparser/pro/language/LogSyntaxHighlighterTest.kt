/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogSyntaxHighlighterTest {

    private val highlighter = LogSyntaxHighlighter()

    @Test
    fun `every log token type has a distinct attribute key`() {
        val types = listOf(
            LogTokenTypes.LEVEL_ERROR, LogTokenTypes.LEVEL_WARN, LogTokenTypes.LEVEL_INFO,
            LogTokenTypes.LEVEL_DEBUG, LogTokenTypes.LEVEL_TRACE, LogTokenTypes.STACK_TRACE,
            LogTokenTypes.TIMESTAMP
        )
        val keys = types.map { highlighter.getTokenHighlights(it).single().externalName }
        assertEquals(
            listOf(
                "LOG_LEVEL_ERROR", "LOG_LEVEL_WARN", "LOG_LEVEL_INFO", "LOG_LEVEL_DEBUG",
                "LOG_LEVEL_TRACE", "LOG_STACK_TRACE", "LOG_TIMESTAMP"
            ),
            keys
        )
    }

    @Test
    fun `plain text and null tokens are not highlighted`() {
        assertTrue(highlighter.getTokenHighlights(LogTokenTypes.TEXT).isEmpty())
        assertTrue(highlighter.getTokenHighlights(null).isEmpty())
    }

    @Test
    fun `highlighting lexer is the log lexer`() {
        assertTrue(highlighter.highlightingLexer is LogLexer)
    }
}
