/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.feedback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class FeedbackNotifierTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val start = 1_700_000_000_000L

    @Test
    fun `first run records activation and does not prompt`() {
        val state = FeedbackState.State()
        assertFalse(FeedbackNotifier.shouldShowFeedback(state, start))
        assertEquals(start, state.firstActivation)
    }

    @Test
    fun `prompt waits a full week`() {
        val state = FeedbackState.State(firstActivation = start)
        assertFalse(FeedbackNotifier.shouldShowFeedback(state, start + 7 * day - 1))
        assertTrue(FeedbackNotifier.shouldShowFeedback(state, start + 7 * day))
        assertTrue(FeedbackNotifier.shouldShowFeedback(state, start + 30 * day))
    }

    @Test
    fun `never prompts again once shown or dismissed`() {
        assertFalse(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start, feedbackShown = true), start + 30 * day))
        assertFalse(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start, dismissed = true), start + 30 * day))
    }

    @Test
    fun `state component round-trips`() {
        val component = FeedbackState()
        val loaded = FeedbackState.State(firstActivation = 1L, feedbackShown = true, dismissed = false)
        component.loadState(loaded)
        assertSame(loaded, component.state)
    }
}
