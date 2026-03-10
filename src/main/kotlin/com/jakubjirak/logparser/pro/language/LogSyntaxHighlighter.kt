/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

/**
 * Provides colour-coded syntax highlighting for log files.
 *
 * Maps [LogTokenTypes] to [TextAttributesKey] entries so the editor
 * renders log levels, timestamps, and stack traces with distinct colours.
 */
class LogSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = LogLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            LogTokenTypes.LEVEL_ERROR -> ERROR_KEYS
            LogTokenTypes.LEVEL_WARN -> WARN_KEYS
            LogTokenTypes.LEVEL_INFO -> INFO_KEYS
            LogTokenTypes.LEVEL_DEBUG -> DEBUG_KEYS
            LogTokenTypes.LEVEL_TRACE -> TRACE_KEYS
            LogTokenTypes.STACK_TRACE -> STACK_TRACE_KEYS
            LogTokenTypes.TIMESTAMP -> TIMESTAMP_KEYS
            else -> EMPTY_KEYS
        }
    }

    companion object {
        private val ERROR_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_LEVEL_ERROR", DefaultLanguageHighlighterColors.CONSTANT
        )
        private val WARN_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_LEVEL_WARN", DefaultLanguageHighlighterColors.NUMBER
        )
        private val INFO_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_LEVEL_INFO", DefaultLanguageHighlighterColors.STRING
        )
        private val DEBUG_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_LEVEL_DEBUG", DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        private val TRACE_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_LEVEL_TRACE", DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        private val STACK_TRACE_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_STACK_TRACE", DefaultLanguageHighlighterColors.BLOCK_COMMENT
        )
        private val TIMESTAMP_KEY = TextAttributesKey.createTextAttributesKey(
            "LOG_TIMESTAMP", DefaultLanguageHighlighterColors.KEYWORD
        )

        private val ERROR_KEYS = arrayOf(ERROR_KEY)
        private val WARN_KEYS = arrayOf(WARN_KEY)
        private val INFO_KEYS = arrayOf(INFO_KEY)
        private val DEBUG_KEYS = arrayOf(DEBUG_KEY)
        private val TRACE_KEYS = arrayOf(TRACE_KEY)
        private val STACK_TRACE_KEYS = arrayOf(STACK_TRACE_KEY)
        private val TIMESTAMP_KEYS = arrayOf(TIMESTAMP_KEY)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}

/**
 * Factory that provides [LogSyntaxHighlighter] for files with [LogLanguage].
 */
class LogSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return LogSyntaxHighlighter()
    }
}
