/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.parse

/**
 * Single-pass logfmt tokenizer: `key=value key="quoted \"value\""`.
 *
 * Bare tokens without `=` are skipped, backslash escapes inside quotes are
 * unescaped, and an unterminated quote consumes the rest of the line.
 */
internal object LogfmtScanner {

    /** Returns the key/value pairs of [line] in order of appearance (later duplicates win). */
    fun scan(line: String): LinkedHashMap<String, String> {
        val fields = LinkedHashMap<String, String>()
        var i = skipWhitespace(line, 0)
        while (i < line.length) {
            val keyEnd = scanKey(line, i)
            if (keyEnd >= line.length || line[keyEnd] != '=') {
                i = skipWhitespace(line, skipToken(line, keyEnd))
                continue
            }
            val key = line.substring(i, keyEnd)
            val value = StringBuilder()
            val valueStart = keyEnd + 1
            val next = if (valueStart < line.length && line[valueStart] == '"') {
                readQuoted(line, valueStart + 1, value)
            } else {
                readBare(line, valueStart, value)
            }
            if (key.isNotEmpty()) fields[key] = value.toString()
            i = skipWhitespace(line, next)
        }
        return fields
    }

    private fun skipWhitespace(line: String, from: Int): Int {
        var i = from
        while (i < line.length && line[i].isWhitespace()) i++
        return i
    }

    private fun skipToken(line: String, from: Int): Int {
        var i = from
        while (i < line.length && !line[i].isWhitespace()) i++
        return i
    }

    private fun scanKey(line: String, from: Int): Int {
        var i = from
        while (i < line.length && line[i] != '=' && !line[i].isWhitespace()) i++
        return i
    }

    /** Reads a quoted value starting just after the opening quote; returns the index after the closing quote. */
    private fun readQuoted(line: String, from: Int, sink: StringBuilder): Int {
        var i = from
        var closed = false
        while (i < line.length && !closed) {
            val c = line[i]
            when {
                c == '\\' && i + 1 < line.length -> { sink.append(line[i + 1]); i += 2 }
                c == '"' -> { closed = true; i++ }
                else -> { sink.append(c); i++ }
            }
        }
        return i
    }

    private fun readBare(line: String, from: Int, sink: StringBuilder): Int {
        val end = skipToken(line, from)
        sink.append(line, from, end)
        return end
    }
}
