/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogLexerTest {

    @Test
    fun `stack trace lines win over level keywords`() {
        assertEquals(LogTokenTypes.STACK_TRACE, LogLexer.classifyLine("    at com.example.Error.run(Error.kt:1)"))
        assertEquals(LogTokenTypes.STACK_TRACE, LogLexer.classifyLine("Caused by: java.io.IOException: ERROR"))
        assertEquals(LogTokenTypes.STACK_TRACE, LogLexer.classifyLine("\t... 12 more"))
    }

    @Test
    fun `levels are classified by severity priority`() {
        assertEquals(LogTokenTypes.LEVEL_ERROR, LogLexer.classifyLine("2024-01-01 10:00:00 ERROR boom"))
        assertEquals(LogTokenTypes.LEVEL_ERROR, LogLexer.classifyLine("FATAL disk gone"))
        assertEquals(LogTokenTypes.LEVEL_ERROR, LogLexer.classifyLine("SEVERE: x"))
        assertEquals(LogTokenTypes.LEVEL_ERROR, LogLexer.classifyLine("WARN then error"))
        assertEquals(LogTokenTypes.LEVEL_WARN, LogLexer.classifyLine("warning: low memory"))
        assertEquals(LogTokenTypes.LEVEL_INFO, LogLexer.classifyLine("info started"))
        assertEquals(LogTokenTypes.LEVEL_DEBUG, LogLexer.classifyLine("DEBUG x=1"))
        assertEquals(LogTokenTypes.LEVEL_TRACE, LogLexer.classifyLine("TRACE enter"))
    }

    @Test
    fun `keywords must be whole words`() {
        assertEquals(LogTokenTypes.TEXT, LogLexer.classifyLine("ERRORS happen; INFOrmation; debugger"))
    }

    @Test
    fun `timestamp without level is TIMESTAMP and plain text is TEXT`() {
        assertEquals(LogTokenTypes.TIMESTAMP, LogLexer.classifyLine("2024-01-01T10:00:00 started"))
        assertEquals(LogTokenTypes.TEXT, LogLexer.classifyLine("hello world"))
        assertEquals(LogTokenTypes.TEXT, LogLexer.classifyLine(""))
    }

    @Test
    fun `lexer emits one token per line including the newline`() {
        val text = "INFO a\n    at x.y(Z.kt:1)\nplain"
        val tokens = tokenize(text)
        assertEquals(
            listOf(
                Triple(LogTokenTypes.LEVEL_INFO, 0, 7),
                Triple(LogTokenTypes.STACK_TRACE, 7, 26),
                Triple(LogTokenTypes.TEXT, 26, 31)
            ),
            tokens
        )
    }

    @Test
    fun `lexer covers the whole range without gaps`() {
        val text = "ERROR one\nWARN two\n\nlast line\n"
        val tokens = tokenize(text)
        assertEquals(0, tokens.first().second)
        assertEquals(text.length, tokens.last().third)
        tokens.zipWithNext().forEach { (a, b) -> assertEquals(a.third, b.second) }
    }

    @Test
    fun `lexer honours start and end offsets`() {
        val text = "xxINFO a\nDEBUG b\nyy"
        val lexer = LogLexer()
        lexer.start(text, 2, text.length - 2, 0)
        assertEquals(LogTokenTypes.LEVEL_INFO, lexer.tokenType)
        assertEquals(2, lexer.tokenStart)
        lexer.advance()
        assertEquals(LogTokenTypes.LEVEL_DEBUG, lexer.tokenType)
        lexer.advance()
        assertNull(lexer.tokenType)
        assertEquals(0, lexer.state)
        assertEquals(text.length - 2, lexer.bufferEnd)
        assertTrue(lexer.bufferSequence === text)
    }

    @Test
    fun `empty buffer yields no tokens`() {
        assertTrue(tokenize("").isEmpty())
    }

    private fun tokenize(text: String): List<Triple<IElementType, Int, Int>> {
        val lexer = LogLexer()
        lexer.start(text, 0, text.length, 0)
        val out = mutableListOf<Triple<IElementType, Int, Int>>()
        while (true) {
            val type = lexer.tokenType ?: break
            out += Triple(type, lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
        }
        return out
    }
}
