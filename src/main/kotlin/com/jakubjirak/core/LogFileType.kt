package com.jakubjirak.core

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class LogFileType : FileType {
    companion object {
        val INSTANCE = LogFileType()
    }

    override fun getName(): String = "Log File"

    override fun getDescription(): String = "Log file format"

    override fun getDefaultExtension(): String = "log"

    override fun getIcon(): Icon? = null // TODO: Add icon

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = true
}
