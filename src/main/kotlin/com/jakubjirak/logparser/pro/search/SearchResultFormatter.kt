/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.search

import com.jakubjirak.logparser.pro.grok.GrokCompileException
import java.util.regex.PatternSyntaxException

/**
 * Turns search results (or a pattern error) into the plain text shown in the Search tab.
 */
object SearchResultFormatter {

    /** Maximum number of hits listed; the rest are summarised. */
    const val MAX_LISTED = 200

    /** Formats [results] for [pattern]; Grok fields are listed under each hit as `name=value`. */
    fun format(pattern: String, results: List<SearchEngine.SearchResult>): String {
        if (results.isEmpty()) return "No matches found for: $pattern"
        return buildString {
            appendLine("Found ${results.size} matches:")
            appendLine()
            results.take(MAX_LISTED).forEach { r ->
                appendLine("Line ${r.lineNumber + 1}: ${r.content}")
                if (r.fields.isNotEmpty()) {
                    appendLine("    " + r.fields.entries.joinToString("  ") { (k, v) -> "$k=$v" })
                }
            }
            if (results.size > MAX_LISTED) {
                appendLine("... and ${results.size - MAX_LISTED} more matches")
            }
        }
    }

    /**
     * Runs [engine] and formats the outcome; an invalid regex or Grok pattern yields an error
     * message instead of an exception.
     */
    fun searchAndFormat(engine: SearchEngine, lines: List<String>, query: SearchEngine.SearchQuery): String =
        try {
            format(query.pattern, engine.search(lines, query))
        } catch (e: PatternSyntaxException) {
            "Invalid pattern: ${e.description}"
        } catch (e: GrokCompileException) {
            "Invalid Grok pattern: ${e.message}"
        }
}
