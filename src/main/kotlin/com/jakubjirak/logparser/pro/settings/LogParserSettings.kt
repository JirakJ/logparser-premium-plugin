/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

/**
 * Persistent settings for LogParser Pro, accessible via
 * **Settings → Tools → LogParser Pro**.
 *
 * Stores the options the log editor actually honours: how many lines to load,
 * level colouring, table font size and the editor's export format.
 */
@Service(Service.Level.APP)
@State(name = "com.jakubjirak.logparser.pro.Settings", storages = [Storage("com-jakubjirak-logparser-pro.xml")])
class LogParserSettings : PersistentStateComponent<LogParserSettings.State> {

    /** Serialisable settings state. */
    data class State(
        var maxLinesToParse: Int = DEFAULT_MAX_LINES,
        var useThemeAwareColors: Boolean = true,
        var logViewerFontSize: Int = DEFAULT_FONT_SIZE,
        var exportFormat: String = "CSV"
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        const val DEFAULT_MAX_LINES: Int = 100_000
        const val DEFAULT_FONT_SIZE: Int = 12

        /** Returns the application-level settings instance. */
        fun getInstance(): LogParserSettings {
            return ApplicationManager.getApplication().getService(LogParserSettings::class.java)
        }
    }
}
