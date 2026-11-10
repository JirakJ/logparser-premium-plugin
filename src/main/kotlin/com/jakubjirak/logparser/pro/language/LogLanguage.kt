/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.lang.Language

/**
 * Singleton language definition for log files.
 *
 * Registers with the IntelliJ Platform so that file types, parsers,
 * and syntax highlighters can bind to the `"Log"` language ID.
 */
object LogLanguage : Language("Log") {

    /** Prevents serialisation warnings. */
    private fun readResolve(): Any = LogLanguage

    override fun getDisplayName(): String = "Log"
}
