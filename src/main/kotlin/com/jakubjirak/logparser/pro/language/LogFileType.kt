/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * File type registration for `.log` and `.out` files, bound to [LogLanguage].
 *
 * Uses the [LanguageFileType] pattern required by IntelliJ Platform for
 * proper language integration (syntax highlighting, parser definitions, etc.).
 */
class LogFileType private constructor() : LanguageFileType(LogLanguage) {

    override fun getName(): String = "Log File"

    override fun getDescription(): String = "Log file"

    override fun getDefaultExtension(): String = "log"

    override fun getIcon(): Icon? = null

    companion object {
        /** Singleton instance referenced from `plugin.xml` via `fieldName="INSTANCE"`. */
        @JvmField
        val INSTANCE = LogFileType()
    }
}
