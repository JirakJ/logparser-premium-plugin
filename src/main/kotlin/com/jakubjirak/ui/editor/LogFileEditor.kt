package com.jakubjirak.ui.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class LogFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {
    
    private val panel = JPanel()
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        // TODO: Implement full log viewer UI
        panel.add(JLabel("LogParser Pro - Coming Soon!"))
        panel.add(JLabel("File: ${file.name}"))
    }
    
    override fun getComponent(): JComponent = panel
    
    override fun getPreferredFocusedComponent(): JComponent = panel
    
    override fun getName(): String = "LogParser"
    
    override fun setState(state: FileEditorState) {}
    
    override fun isModified(): Boolean = false
    
    override fun isValid(): Boolean = file.isValid
    
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    
    override fun getCurrentLocation(): FileEditorLocation? = null
    
    override fun dispose() {}
    
    override fun <T : Any?> getUserData(key: Key<T>): T? = null
    
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {}
    
    override fun getFile(): VirtualFile = file
}
