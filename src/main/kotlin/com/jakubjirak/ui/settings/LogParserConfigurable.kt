package com.jakubjirak.ui.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.ui.dsl.builder.panel

class LogParserConfigurable : BoundConfigurable("LogParser Pro") {

    override fun createPanel() = panel {
        group("LogParser Pro Settings") {
            row {
                label("Settings coming soon!")
            }
        }
    }
}
