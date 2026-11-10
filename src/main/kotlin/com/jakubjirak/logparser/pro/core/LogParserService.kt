/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

/**
 * Project-scoped service that manages [LogParser] instances and the shared [PatternLibrary].
 *
 * Parsers are cached by absolute file path so that repeated access to the same log file
 * reuses the existing parser rather than re-reading the file from disk.
 */
class LogParserService(private val project: Project) : Disposable {

    private val patternLibrary = PatternLibrary()
    private val parsers = mutableMapOf<String, LogParser>()

    /** Returns a cached [LogParser] for [file], creating one if it does not already exist. */
    fun parseLogFile(file: java.io.File): LogParser {
        return parsers.getOrPut(file.absolutePath) {
            LogParser(file, patternLibrary)
        }
    }

    /** Closes and removes the parser associated with [file]. */
    fun closeLogFile(file: java.io.File) {
        parsers.remove(file.absolutePath)?.dispose()
    }

    /** Returns the shared [PatternLibrary] used for timestamp/level/correlation detection. */
    fun getPatternLibrary(): PatternLibrary = patternLibrary

    override fun dispose() {
        parsers.values.forEach { it.dispose() }
        parsers.clear()
    }

    companion object {
        /** Convenience accessor for the project-scoped service instance. */
        fun getInstance(project: Project): LogParserService {
            return project.getService(LogParserService::class.java)
        }
    }
}
