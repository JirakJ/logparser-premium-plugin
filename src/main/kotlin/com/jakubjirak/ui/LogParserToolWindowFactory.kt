package com.jakubjirak.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*

class LogParserToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = LogParserToolWindowContent(project)
        val content = ContentFactory.getInstance()
            .createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class LogParserToolWindowContent(private val project: Project) {
    
    private val panel = JPanel()
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // Welcome message
        val welcomeLabel = JLabel("<html><h2>LogParser Pro</h2></html>")
        panel.add(welcomeLabel)
        
        // Free features info
        val freeLabel = JLabel("<html>" +
                "<h3>✅ Free Features:</h3>" +
                "<ul>" +
                "<li>Basic log viewing & syntax highlighting</li>" +
                "<li>Log level filtering</li>" +
                "<li>Keyword search</li>" +
                "<li>Stack trace folding</li>" +
                "</ul>" +
                "</html>")
        panel.add(freeLabel)
        
        // Premium features teaser
        val premiumLabel = JLabel("<html>" +
                "<h3>💎 Premium Features:</h3>" +
                "<ul>" +
                "<li>🔍 Transaction/Correlation ID Tracking</li>" +
                "<li>☸️ Kubernetes & Docker Integration</li>" +
                "<li>🔴 Real-time Multi-source Live Tail</li>" +
                "<li>🤖 AI-powered Pattern Detection</li>" +
                "<li>📊 Advanced Analytics & Visualization</li>" +
                "<li>👥 Team Collaboration</li>" +
                "</ul>" +
                "</html>")
        panel.add(premiumLabel)
        
        // Action buttons
        val buttonPanel = JPanel()
        
        val openFileButton = JButton("Open Log File")
        openFileButton.addActionListener {
            // TODO: Implement file picker
        }
        buttonPanel.add(openFileButton)
        
        val startTrialButton = JButton("Start 30-Day FREE Trial")
        startTrialButton.addActionListener {
            // TODO: Implement trial activation
        }
        buttonPanel.add(startTrialButton)
        
        val upgradePremiumButton = JButton("Upgrade to Premium ($79/year)")
        upgradePremiumButton.addActionListener {
            // TODO: Implement upgrade flow
        }
        buttonPanel.add(upgradePremiumButton)
        
        panel.add(buttonPanel)
    }
    
    fun getContent(): JComponent = panel
}
