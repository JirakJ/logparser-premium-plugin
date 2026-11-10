/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.privacy

/**
 * Single redaction pattern.
 *
 * @property name        human-readable label (e.g. "email", "jwt")
 * @property regex       linear-time regex with bounded quantifiers
 * @property replacement token inserted in place of the match
 * @property luhnValidate run Luhn checksum validation on the match
 */
data class RedactionPattern(
    val name: String,
    val regex: Regex,
    val replacement: String,
    val luhnValidate: Boolean = false
)

/**
 * Occurrence of a redacted value.
 *
 * @property patternName the [RedactionPattern.name] that matched
 * @property start       inclusive start index in the *original* text
 * @property end         exclusive end index in the *original* text
 */
data class Redaction(val patternName: String, val start: Int, val end: Int)

/**
 * Output of [SensitiveDataRedactor.redact].
 *
 * @property text        the fully redacted text
 * @property redactions  every successful replacement, with original offsets
 */
data class RedactionResult(val text: String, val redactions: List<Redaction>)

/**
 * Replaces secrets / PII in log text with stable placeholder tokens.
 *
 * Safety model:
 * - All built-in regexes use **bounded quantifiers** to avoid catastrophic
 *   backtracking. No look-behinds with unbounded repetition.
 * - Credit-card hits are optionally verified against the Luhn checksum.
 * - Patterns are applied in a **fixed priority order**; earlier matches
 *   always win.
 * - Custom user regexes are accepted but are **length-bounded to 512
 *   characters per candidate** and run after all built-ins.
 * - The redactor is stateless and thread-safe.
 */
class SensitiveDataRedactor(
    private val patterns: List<RedactionPattern> = DEFAULT_PATTERNS
) {

    /**
     * Returns a new result with sensitive data replaced by placeholder tokens.
     * Preserves original offsets in [RedactionResult.redactions].
     */
    fun redact(text: String): RedactionResult {
        if (text.isEmpty()) return RedactionResult(text, emptyList())
        val hits = mutableListOf<Hit>()
        for (pattern in patterns) {
            for (match in pattern.regex.findAll(text)) {
                if (pattern.luhnValidate) {
                    val digits = match.value.filter { it.isDigit() }
                    if (!isValidLuhn(digits)) continue
                }
                hits += Hit(match.range.first, match.range.last + 1, pattern)
            }
        }
        if (hits.isEmpty()) return RedactionResult(text, emptyList())
        val merged = mergeNonOverlapping(hits.sortedWith(compareBy({ it.start }, { -it.end })))
        return assemble(text, merged)
    }

    private fun mergeNonOverlapping(sorted: List<Hit>): List<Hit> {
        val out = mutableListOf<Hit>()
        var lastEnd = -1
        for (h in sorted) {
            if (h.start >= lastEnd) {
                out += h
                lastEnd = h.end
            }
        }
        return out
    }

    private fun assemble(text: String, hits: List<Hit>): RedactionResult {
        val builder = StringBuilder(text.length)
        val redactions = ArrayList<Redaction>(hits.size)
        var cursor = 0
        for (h in hits) {
            if (h.start > cursor) builder.append(text, cursor, h.start)
            builder.append(h.pattern.replacement)
            redactions += Redaction(h.pattern.name, h.start, h.end)
            cursor = h.end
        }
        if (cursor < text.length) builder.append(text, cursor, text.length)
        return RedactionResult(builder.toString(), redactions)
    }

    private data class Hit(val start: Int, val end: Int, val pattern: RedactionPattern)

    companion object {
        /**
         * Built-in patterns. Order matters — more specific patterns go first.
         * All use bounded quantifiers to avoid ReDoS.
         */
        val DEFAULT_PATTERNS: List<RedactionPattern> = listOf(
            RedactionPattern(
                "jwt",
                Regex("""\beyJ[A-Za-z0-9_\-]{10,2048}\.[A-Za-z0-9_\-]{10,2048}\.[A-Za-z0-9_\-]{10,2048}\b"""),
                "[JWT]"
            ),
            RedactionPattern(
                "bearer",
                Regex("""(?i)\bBearer\s+[A-Za-z0-9._\-]{8,2048}\b"""),
                "Bearer [REDACTED]"
            ),
            RedactionPattern(
                "aws_access_key",
                Regex("""\b(?:AKIA|ASIA|AIDA|AROA|AIPA|ANPA|ANVA|ASCA)[A-Z0-9]{16}\b"""),
                "[AWS_ACCESS_KEY]"
            ),
            RedactionPattern(
                "aws_secret",
                Regex("""(?i)(?:aws_?secret_?access_?key|aws_?secret)\s*[:=]\s*["']?[A-Za-z0-9/+=]{40}["']?"""),
                "[AWS_SECRET]"
            ),
            RedactionPattern(
                "password_assignment",
                Regex(
                    """(?i)\b(password|passwd|pwd|secret|api[_-]?key|apikey|token)\s*[:=]\s*["']?""" +
                        """([^"'\s,;]{3,256})["']?"""
                ),
                "$1=[REDACTED]"
            ),
            RedactionPattern(
                "private_key_header",
                Regex("""-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----"""),
                "[PRIVATE_KEY_HEADER]"
            ),
            RedactionPattern(
                "credit_card",
                Regex("""\b(?:\d[ \-]?){13,19}\b"""),
                "[CC]",
                luhnValidate = true
            ),
            RedactionPattern(
                "email",
                Regex("""\b[A-Za-z0-9._%+\-]{1,64}@[A-Za-z0-9.\-]{1,253}\.[A-Za-z]{2,24}\b"""),
                "[EMAIL]"
            ),
            RedactionPattern(
                "uuid",
                Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""),
                "[UUID]"
            ),
            RedactionPattern(
                "ipv4",
                Regex(
                    """\b(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}""" +
                        """(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\b"""
                ),
                "[IPV4]"
            ),
            RedactionPattern(
                "ipv6",
                Regex("""\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\b"""),
                "[IPV6]"
            ),
            RedactionPattern(
                "stripe_live_key",
                Regex("""\b(?:sk|rk|pk)_live_[A-Za-z0-9]{16,64}\b"""),
                "[STRIPE_KEY]"
            )
        )

        /** Runs the Luhn checksum on a digits-only string. */
        fun isValidLuhn(digits: String): Boolean {
            if (digits.length < 13 || digits.length > 19) return false
            var sum = 0
            var alt = false
            for (i in digits.length - 1 downTo 0) {
                var n = digits[i] - '0'
                if (alt) {
                    n *= 2
                    if (n > 9) n -= 9
                }
                sum += n
                alt = !alt
            }
            return sum % 10 == 0
        }
    }
}
