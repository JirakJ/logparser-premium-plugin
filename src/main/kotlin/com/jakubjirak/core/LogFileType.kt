/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.core

import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

/**
 * Custom [FileType] registration for log files (.log, .txt, .out).
 *
 * Enables the IntelliJ platform to recognise log files and route them
 * to the dedicated [com.jakubjirak.ui.editor.LogFileEditorProvider].
 */
class LogFileType : FileType {

    companion object {
        @JvmStatic
        val INSTANCE = LogFileType()
    }

    override fun getName(): String = "Log File"

    override fun getDescription(): String = "Log file format for LogParser Pro"

    override fun getDefaultExtension(): String = "log"

    override fun getIcon(): Icon? = null

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = true
}
