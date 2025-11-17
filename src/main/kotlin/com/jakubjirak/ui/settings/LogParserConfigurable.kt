package com.jakubjirak.ui.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class LogParserConfigurable : Configurable {
    
    private var panel: JPanel? = null
    
    override fun getDisplayName(): String = "LogParser Pro"
    
    override fun createComponent(): JComponent {
        val p = JPanel()
        p.add(JLabel("LogParser Pro Settings - Coming Soon!"))
        panel = p
        return p
    }
    
    override fun isModified(): Boolean = false
    
    override fun apply() {}
    
    override fun reset() {}
    
    override fun disposeUIResources() {
        panel = null
    }
}
