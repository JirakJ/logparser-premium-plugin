/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.privacy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SensitiveDataRedactorTest {

    private val redactor = SensitiveDataRedactor()

    @Test
    fun `redacts email`() {
        val result = redactor.redact("Contact alice@example.com for details")
        assertEquals("Contact [EMAIL] for details", result.text)
        assertEquals(1, result.redactions.size)
        assertEquals("email", result.redactions.first().patternName)
    }

    @Test
    fun `redacts JWT token`() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val result = redactor.redact("Authorization: Bearer $jwt")
        assertTrue(result.text.contains("[JWT]") || result.text.contains("Bearer [REDACTED]"))
        assertFalse(result.text.contains(jwt))
    }

    @Test
    fun `redacts AWS access key`() {
        val result = redactor.redact("key=AKIAIOSFODNN7EXAMPLE in the file")
        assertTrue(result.text.contains("[AWS_ACCESS_KEY]"))
    }

    @Test
    fun `redacts password assignment`() {
        val result = redactor.redact("db.password=hunter2secret running")
        assertTrue(result.text.contains("[REDACTED]"))
        assertFalse(result.text.contains("hunter2secret"))
    }

    @Test
    fun `redacts ipv4`() {
        val result = redactor.redact("client 10.0.0.1 connected")
        assertEquals("client [IPV4] connected", result.text)
    }

    @Test
    fun `redacts uuid`() {
        val result = redactor.redact("trace-id 123e4567-e89b-12d3-a456-426614174000 end")
        assertEquals("trace-id [UUID] end", result.text)
    }

    @Test
    fun `redacts credit card only when Luhn valid`() {
        val valid = "4111 1111 1111 1111"
        val invalid = "4111 1111 1111 1112"
        val r1 = redactor.redact("card=$valid")
        val r2 = redactor.redact("card=$invalid")
        assertTrue(r1.text.contains("[CC]"))
        assertFalse(r2.text.contains("[CC]"))
    }

    @Test
    fun `luhn helper validates known good numbers`() {
        assertTrue(SensitiveDataRedactor.isValidLuhn("4111111111111111"))
        assertTrue(SensitiveDataRedactor.isValidLuhn("5500000000000004"))
        assertFalse(SensitiveDataRedactor.isValidLuhn("1234567890123456"))
    }

    @Test
    fun `preserves original text without sensitive data`() {
        val input = "This message has nothing sensitive at all."
        val result = redactor.redact(input)
        assertEquals(input, result.text)
        assertTrue(result.redactions.isEmpty())
    }

    @Test
    fun `overlapping matches resolve deterministically by priority`() {
        // JWT regex will match before email
        val token = "eyJabcdefghijk.eyJabcdefghij.sig_abcdefghij"
        val result = redactor.redact("tok=$token, mail=a@b.co")
        assertTrue(result.text.contains("mail=[EMAIL]"))
    }

    @Test
    fun `offsets in redactions point to original text`() {
        val input = "hello alice@example.com world"
        val result = redactor.redact(input)
        val hit = result.redactions.first()
        assertEquals("alice@example.com", input.substring(hit.start, hit.end))
    }

    @Test
    fun `empty input returns empty result`() {
        val result = redactor.redact("")
        assertEquals("", result.text)
        assertTrue(result.redactions.isEmpty())
    }

    @Test
    fun `stripe live key is redacted`() {
        val result = redactor.redact("token=sk_live_abc123DEF456ghi789XYZ0")
        assertFalse(result.text.contains("sk_live_abc123DEF456ghi789XYZ0"))
        assertTrue(result.redactions.isNotEmpty())
    }

    @Test
    fun `custom patterns override defaults when provided`() {
        val custom = SensitiveDataRedactor(
            listOf(RedactionPattern("order", Regex("""ORDER-\d{3,6}"""), "[ORDER]"))
        )
        val result = custom.redact("created ORDER-12345 today")
        assertEquals("created [ORDER] today", result.text)
    }

    @Test
    fun `multiple occurrences all redacted`() {
        val result = redactor.redact("from 10.0.0.1 to 10.0.0.2 then 10.0.0.3")
        assertEquals(3, result.redactions.size)
    }
}
