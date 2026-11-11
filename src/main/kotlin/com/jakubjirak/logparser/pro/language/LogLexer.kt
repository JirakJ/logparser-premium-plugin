/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Line-oriented lexer for log files.
 *
 * Processes one line at a time. Each line is classified as a single token
 * based on its dominant content: timestamp, log level, stack trace, or plain text.
 * This keeps the lexer simple and fast while providing enough granularity
 * for meaningful syntax highlighting.
 */
class LogLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var currentOffset = 0
    private var tokenEnd = 0
    private var currentTokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentOffset = startOffset
        this.tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentTokenType

    override fun getTokenStart(): Int = currentOffset

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        currentOffset = tokenEnd
        if (currentOffset >= endOffset) {
            currentTokenType = null
            return
        }

        val lineEnd = findLineEnd(currentOffset)
        val line = buffer.subSequence(currentOffset, lineEnd).toString()

        currentTokenType = classifyLine(line)
        tokenEnd = lineEnd
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun findLineEnd(from: Int): Int {
        var pos = from
        while (pos < endOffset && buffer[pos] != '\n') pos++
        if (pos < endOffset) pos++ // include newline
        return pos
    }

    companion object {

        private val TIMESTAMP_PATTERN = Regex(
            """^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}"""
        )
        private val ERROR_PATTERN = Regex("""\b(ERROR|FATAL|SEVERE)\b""", RegexOption.IGNORE_CASE)
        private val WARN_PATTERN = Regex("""\b(WARN|WARNING)\b""", RegexOption.IGNORE_CASE)
        private val INFO_PATTERN = Regex("""\bINFO\b""", RegexOption.IGNORE_CASE)
        private val DEBUG_PATTERN = Regex("""\bDEBUG\b""", RegexOption.IGNORE_CASE)
        private val STACK_PREFIXES = listOf("at ", "Caused by:", "... ")
        private val TRACE_PATTERN = Regex("""\bTRACE\b""", RegexOption.IGNORE_CASE)

        /** Classifies a single line into the most significant [IElementType]. */
        internal fun classifyLine(line: String): IElementType {
            val trimmed = line.trimStart()
            return when {
                STACK_PREFIXES.any { trimmed.startsWith(it) } -> LogTokenTypes.STACK_TRACE
                ERROR_PATTERN.containsMatchIn(line) -> LogTokenTypes.LEVEL_ERROR
                WARN_PATTERN.containsMatchIn(line) -> LogTokenTypes.LEVEL_WARN
                INFO_PATTERN.containsMatchIn(line) -> LogTokenTypes.LEVEL_INFO
                DEBUG_PATTERN.containsMatchIn(line) -> LogTokenTypes.LEVEL_DEBUG
                TRACE_PATTERN.containsMatchIn(line) -> LogTokenTypes.LEVEL_TRACE
                TIMESTAMP_PATTERN.containsMatchIn(line) -> LogTokenTypes.TIMESTAMP
                else -> LogTokenTypes.TEXT
            }
        }
    }
}
