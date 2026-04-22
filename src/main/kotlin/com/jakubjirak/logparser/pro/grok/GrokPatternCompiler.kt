/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.grok

/**
 * Thrown for malformed, cyclic, or over-large Grok patterns.
 */
class GrokCompileException(message: String) : RuntimeException(message)

/**
 * Compiled Grok pattern: a [Regex] plus the named field list.
 */
data class CompiledGrok(val regex: Regex, val fields: List<String>) {

    /** Applies the compiled pattern. `null` if no match. */
    fun match(input: CharSequence): Map<String, String>? {
        val m = regex.find(input) ?: return null
        val out = LinkedHashMap<String, String>(fields.size)
        for (name in fields) {
            val value = m.groups[name]?.value
            if (value != null) out[name] = value
        }
        return out
    }
}

/**
 * Compiles a subset of [Elastic Grok](https://www.elastic.co/guide/en/logstash/current/plugins-filters-grok.html)
 * syntax into a native [Regex].
 *
 * Supported forms:
 * - `%{PATTERN}`                — anonymous non-capturing expansion
 * - `%{PATTERN:fieldName}`      — named capture group
 * - `%{PATTERN:fieldName:type}` — type is ignored in the output but accepted
 *
 * Safety (per design review):
 * - Duplicate field names raise [GrokCompileException] to avoid invalid regex.
 * - Recursion uses a `visiting` set + a per-compile max expansion bound, so both
 *   cyclic references and expansion blow-ups are rejected.
 * - Anonymous expansions become non-capturing `(?:…)`.
 */
class GrokPatternCompiler(
    extraPatterns: Map<String, String> = emptyMap(),
    private val maxExpansions: Int = 10_000
) {

    private val patterns: Map<String, String> = BUILTIN_PATTERNS + extraPatterns

    /**
     * Compile [pattern] into a [CompiledGrok].
     *
     * @throws GrokCompileException on syntax / cycles / excessive expansion
     */
    fun compile(pattern: String): CompiledGrok {
        val state = CompileState()
        val regex = expand(pattern, state, emptySet())
        return CompiledGrok(Regex(regex), state.fields.toList())
    }

    private fun expand(pattern: String, state: CompileState, visiting: Set<String>): String {
        val sb = StringBuilder(pattern.length + 64)
        var i = 0
        val n = pattern.length
        while (i < n) {
            val c = pattern[i]
            if (c == '%' && i + 1 < n && pattern[i + 1] == '{') {
                val close = pattern.indexOf('}', i + 2)
                if (close < 0) throw GrokCompileException("Unterminated %{ at index $i in: $pattern")
                val inner = pattern.substring(i + 2, close)
                sb.append(expandReference(inner, state, visiting))
                i = close + 1
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun expandReference(inner: String, state: CompileState, visiting: Set<String>): String {
        if (state.incrementAndCheck() > maxExpansions) {
            throw GrokCompileException("Grok expansion exceeded $maxExpansions tokens")
        }
        val parts = inner.split(':')
        val name = parts[0].trim()
        val field = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        if (!VALID_NAME.matches(name)) {
            throw GrokCompileException("Invalid pattern name '$name'")
        }
        if (name in visiting) {
            throw GrokCompileException("Cyclic grok reference detected at '$name'")
        }
        val body = patterns[name] ?: throw GrokCompileException("Unknown grok pattern '$name'")
        val expandedBody = expand(body, state, visiting + name)
        return if (field == null) {
            "(?:$expandedBody)"
        } else {
            if (!VALID_NAME.matches(field)) {
                throw GrokCompileException("Invalid field name '$field'")
            }
            if (!state.fields.add(field)) {
                throw GrokCompileException("Duplicate field name '$field'")
            }
            "(?<$field>$expandedBody)"
        }
    }

    private class CompileState {
        val fields: LinkedHashSet<String> = LinkedHashSet()
        private var expansions = 0
        fun incrementAndCheck(): Int = ++expansions
    }

    companion object {
        private val VALID_NAME = Regex("""[A-Za-z_][A-Za-z0-9_]{0,63}""")

        /**
         * Minimum viable Grok library. Values are regex bodies to be interpolated —
         * none capture groups on their own (capture happens at the outer `%{…:name}`).
         */
        val BUILTIN_PATTERNS: Map<String, String> = linkedMapOf(
            "WORD" to """\b\w+\b""",
            "NOTSPACE" to """\S+""",
            "SPACE" to """\s*""",
            "DATA" to """.*?""",
            "GREEDYDATA" to """.*""",
            "INT" to """(?:[+-]?(?:\d+))""",
            "NUMBER" to """(?:[+-]?(?:\d+(?:\.\d+)?|\.\d+)(?:[eE][+-]?\d+)?)""",
            "BASE10NUM" to """(?:[+-]?(?:\d+(?:\.\d+)?|\.\d+))""",
            "BASE16NUM" to """(?:0[xX]?[0-9a-fA-F]+)""",
            "POSINT" to """\d+""",
            "NONNEGINT" to """\d+""",
            "QUOTEDSTRING" to """"(?:[^"\\]|\\.)*"""",
            "UUID" to """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""",
            "MAC" to """(?:[0-9a-fA-F]{2}[:\-]){5}[0-9a-fA-F]{2}""",
            "IPV4" to """(?:(?:25[0-5]|2[0-4]\d|[0-1]?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|[0-1]?\d?\d)""",
            "IPV6" to """(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}""",
            "IP" to """(?:%\{IPV6\}|%\{IPV4\})""",
            "HOSTNAME" to """[A-Za-z0-9][A-Za-z0-9.\-]{0,253}""",
            "HOST" to """%{HOSTNAME}""",
            "HOSTPORT" to """%{HOSTNAME}:%{POSINT}""",
            "PATH" to """(?:/[^\s]*)+""",
            "WINPATH" to """(?:[A-Za-z]:\\[^\s]*)+""",
            "URIPROTO" to """[A-Za-z]+(?:\+[A-Za-z+]+)?""",
            "URIHOST" to """%{IPORHOST}(?::%{POSINT:port})?""",
            "IPORHOST" to """(?:%{HOSTNAME}|%{IP})""",
            "MONTH" to """\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\b""",
            "MONTHNUM" to """(?:0?[1-9]|1[0-2])""",
            "MONTHDAY" to """(?:(?:0[1-9])|(?:[12][0-9])|(?:3[01])|[1-9])""",
            "DAY" to """(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)""",
            "YEAR" to """(?:\d{4})""",
            "HOUR" to """(?:2[0-3]|[01]?\d)""",
            "MINUTE" to """(?:[0-5][0-9])""",
            "SECOND" to """(?:[0-5][0-9](?:\.\d+)?)""",
            "TIMESTAMP_ISO8601" to """%{YEAR}-%{MONTHNUM}-%{MONTHDAY}[T ]%{HOUR}:%{MINUTE}:%{SECOND}(?:Z|[+\-]\d{2}:?\d{2})?""",
            "TIME" to """%{HOUR}:%{MINUTE}:%{SECOND}""",
            "LOGLEVEL" to """(?:TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|SEVERE|CRITICAL)""",
            "JAVACLASS" to """(?:[a-zA-Z_$][a-zA-Z0-9_$]*\.)+[a-zA-Z_$][a-zA-Z0-9_$]*""",
            "JAVAFILE" to """(?:[A-Za-z0-9_.\-]+)""",
            "JAVAMETHOD" to """(?:<(?:cl)?init>|[a-zA-Z_$][a-zA-Z0-9_$]*)""",
            "JAVASTACKTRACEPART" to """\s+at %{JAVACLASS:class}\.%{JAVAMETHOD:method}\((?:%{JAVAFILE:file}:?%{NUMBER:line}?|[^)]+)\)"""
        )
    }
}
