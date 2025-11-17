package com.jakubjirak.ui.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LogFileEditorProvider : FileEditorProvider {
    
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension in listOf("log", "txt", "out") || 
               file.name.contains("log", ignoreCase = true)
    }
    
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return LogFileEditor(project, file)
    }
    
    override fun getEditorTypeId(): String = "logparser-editor"
    
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
