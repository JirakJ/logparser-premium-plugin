package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.vfs.VirtualFile

class OpenLogFileAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val descriptor = FileChooserDescriptor(
            true, false, false, false, false, false
        ).apply {
            title = "Select Log File"
            description = "Select a log file to analyze"
            withFileFilter { file: VirtualFile ->
                file.extension in listOf("log", "txt", "out") || file.name.contains("log")
            }
        }
        
        FileChooserFactory.getInstance()
            .createFileChooser(descriptor, project, null)
            .choose(project) { files ->
                if (files.isNotEmpty()) {
                    val file = files[0]
                    // TODO: Open file in LogParser editor
                }
            }
    }
}
