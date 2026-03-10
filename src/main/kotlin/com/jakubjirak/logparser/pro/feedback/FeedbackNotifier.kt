/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.feedback

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Persistent state tracking first activation and feedback display status.
 */
@Service(Service.Level.APP)
@State(name = "com.jakubjirak.logparser.pro.FeedbackState", storages = [Storage("com-jakubjirak-logparser-pro-feedback.xml")])
class FeedbackState : PersistentStateComponent<FeedbackState.State> {
    /** Serialisable feedback state. */
    data class State(
        var firstActivation: Long = 0L,
        var feedbackShown: Boolean = false,
        var dismissed: Boolean = false
    )

    private var myState = State()
    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        /** Returns the application-level feedback state instance. */
        fun getInstance(): FeedbackState = service()
    }
}

/**
 * Non-intrusive feedback prompt shown once after 7 days of plugin usage.
 *
 * Tracks first activation timestamp and shows a dismissible balloon notification
 * with options to send feedback, rate on Marketplace, or dismiss.
 */
class FeedbackNotifier : ProjectActivity {

    @Suppress("MagicNumber")
    override suspend fun execute(project: Project) {
        val state = FeedbackState.getInstance().state
        val now = System.currentTimeMillis()

        if (state.firstActivation == 0L) {
            state.firstActivation = now
            return
        }

        if (state.feedbackShown || state.dismissed) return

        val daysSinceFirst = (now - state.firstActivation) / (1000 * 60 * 60 * 24)
        if (daysSinceFirst < 7) return

        state.feedbackShown = true

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("LogParser Pro Feedback")
            .createNotification(
                "LogParser Pro",
                "You've been using this plugin for over a week — thank you! \uD83C\uDF89\nWe'd love your feedback to help us improve.",
                NotificationType.INFORMATION
            )
            .addAction(NotificationAction.createSimpleExpiring("\uD83D\uDCE7 Send Feedback") {
                BrowserUtil.browse("mailto:feedback@jakubjirak.com?subject=Feedback: LogParser%20Pro")
            })
            .addAction(NotificationAction.createSimpleExpiring("⭐ Rate on Marketplace") {
                BrowserUtil.browse("https://plugins.jetbrains.com/plugin/29050/reviews")
            })
            .addAction(NotificationAction.createSimpleExpiring("Later") {
                state.feedbackShown = false
            })
            .addAction(NotificationAction.createSimpleExpiring("Don't show again") {
                state.dismissed = true
            })

        notification.notify(project)
    }
}
