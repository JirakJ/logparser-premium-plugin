/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

/**
 * Persistent settings for LogParser Pro, accessible via
 * **Settings → Tools → LogParser Pro**.
 *
 * Stores user preferences including display options, search defaults,
 * and export format preferences.
 */
@Service(Service.Level.APP)
@State(name = "com.jakubjirak.logparser.pro.Settings", storages = [Storage("com-jakubjirak-logparser-pro.xml")])
class LogParserSettings : PersistentStateComponent<LogParserSettings.State> {

    /** Serialisable settings state. */
    data class State(
        var maxLinesToParse: Int = 100_000,
        var defaultFilterLevel: String = "ALL",
        var showLineNumbers: Boolean = true,
        var autoRefresh: Boolean = false,
        var dateFormatPattern: String = "yyyy-MM-dd HH:mm:ss.SSS",
        var customCorrelationPatterns: String = "",
        var useThemeAwareColors: Boolean = true,
        var wrapLongLines: Boolean = false,
        var logViewerFontSize: Int = 12,
        var exportFormat: String = "CSV"
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        /** Returns the application-level settings instance. */
        fun getInstance(): LogParserSettings {
            return ApplicationManager.getApplication().getService(LogParserSettings::class.java)
        }
    }
}
