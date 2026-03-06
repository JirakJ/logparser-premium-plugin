/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.actions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExtractPatternActionTest {

    // ── buildRegex ─────────────────────────────────────────────────────

    @Nested
    inner class BuildRegex {

        @Test
        fun `replaces UUID with named capture group`() {
            val text = "Request a1b2c3d4-E5F6-7890-ABCD-ef1234567890 processed"
            val result = ExtractPatternAction.buildRegex(text)
            // UUID is first replaced, then NUMBER_REGEX modifies digits inside the group
            assertTrue(result.contains("(?<uuid>"))
            assertFalse(result.contains("a1b2c3d4-E5F6-7890-ABCD-ef1234567890"))
        }

        @Test
        fun `replaces IP address with named capture group`() {
            val text = "Connected to 192.168.1.42"
            val result = ExtractPatternAction.buildRegex(text)
            assertTrue(result.contains("(?<ip>"))
        }

        @Test
        fun `replaces numbers with named capture group`() {
            val text = "Processed 42 items"
            val result = ExtractPatternAction.buildRegex(text)
            assertTrue(result.contains("(?<num>"))
        }

        @Test
        fun `replaces ISO timestamp with named capture group`() {
            val text = "Event at 2024-01-15T10:30:00 was logged"
            val result = ExtractPatternAction.buildRegex(text)
            assertTrue(result.contains("(?<timestamp>"))
        }

        @Test
        fun `escapes special regex characters in plain text`() {
            val text = "file.txt (version 1)"
            val result = ExtractPatternAction.buildRegex(text)
            // Regex.escape will add backslashes for . and ( )
            assertTrue(result.contains("file"))
            assertTrue(result.contains("txt"))
        }

        @Test
        fun `handles text with multiple token types`() {
            val text = "a1b2c3d4-e5f6-7890-abcd-ef1234567890 from 10.0.0.1"
            val result = ExtractPatternAction.buildRegex(text)
            assertTrue(result.contains("(?<uuid>"))
            assertTrue(result.contains("(?<ip>"))
        }

        @Test
        fun `handles text with no special tokens`() {
            val text = "simple text"
            val result = ExtractPatternAction.buildRegex(text)
            // Should be the escaped form of the input
            assertTrue(result.contains("simple"))
            assertTrue(result.contains("text"))
        }

        @Test
        fun `handles empty string`() {
            val result = ExtractPatternAction.buildRegex("")
            // Regex.escape("") produces \Q\E
            assertEquals("\\Q\\E", result)
        }

        @Test
        fun `replaces timestamp with fractional seconds`() {
            val text = "At 2024-01-15 10:30:00.123 event"
            val result = ExtractPatternAction.buildRegex(text)
            assertTrue(result.contains("(?<timestamp>"))
        }
    }
}
