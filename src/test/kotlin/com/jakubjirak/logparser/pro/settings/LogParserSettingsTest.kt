/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogParserSettingsTest {

    @Test
    fun `defaults match documented values`() {
        val state = LogParserSettings().state
        assertEquals(100_000, state.maxLinesToParse)
        assertTrue(state.useThemeAwareColors)
        assertEquals(12, state.logViewerFontSize)
        assertEquals("CSV", state.exportFormat)
    }

    @Test
    fun `loadState replaces the state`() {
        val settings = LogParserSettings()
        val loaded = LogParserSettings.State(
            maxLinesToParse = 5,
            useThemeAwareColors = false,
            logViewerFontSize = 20,
            exportFormat = "JSON"
        )
        settings.loadState(loaded)
        assertSame(loaded, settings.state)
    }

    @Test
    fun `parseMaxLines accepts positive integers`() {
        assertEquals(250, LogParserConfigurable.parseMaxLines("250"))
        assertEquals(250, LogParserConfigurable.parseMaxLines("  250 "))
    }

    @Test
    fun `parseMaxLines falls back to default for invalid input`() {
        listOf("", "abc", "0", "-5", "99999999999").forEach {
            assertEquals(LogParserSettings.DEFAULT_MAX_LINES, LogParserConfigurable.parseMaxLines(it), it)
        }
    }
}
