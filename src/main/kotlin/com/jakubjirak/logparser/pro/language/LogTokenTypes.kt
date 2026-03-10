/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.psi.tree.IElementType

/**
 * Token types used by the [LogLexer] for syntax highlighting.
 *
 * Each token is bound to [LogLanguage] so the platform can associate
 * highlighting attributes with the correct language.
 */
object LogTokenTypes {

    /** Timestamp portion of a log line. */
    @JvmField
    val TIMESTAMP = IElementType("LOG_TIMESTAMP", LogLanguage)

    /** ERROR / FATAL level keyword. */
    @JvmField
    val LEVEL_ERROR = IElementType("LOG_LEVEL_ERROR", LogLanguage)

    /** WARN / WARNING level keyword. */
    @JvmField
    val LEVEL_WARN = IElementType("LOG_LEVEL_WARN", LogLanguage)

    /** INFO level keyword. */
    @JvmField
    val LEVEL_INFO = IElementType("LOG_LEVEL_INFO", LogLanguage)

    /** DEBUG level keyword. */
    @JvmField
    val LEVEL_DEBUG = IElementType("LOG_LEVEL_DEBUG", LogLanguage)

    /** TRACE level keyword. */
    @JvmField
    val LEVEL_TRACE = IElementType("LOG_LEVEL_TRACE", LogLanguage)

    /** Stack trace line (lines beginning with `at `, `Caused by:`, etc.). */
    @JvmField
    val STACK_TRACE = IElementType("LOG_STACK_TRACE", LogLanguage)

    /** Default plain-text token for unrecognised content. */
    @JvmField
    val TEXT = IElementType("LOG_TEXT", LogLanguage)
}
