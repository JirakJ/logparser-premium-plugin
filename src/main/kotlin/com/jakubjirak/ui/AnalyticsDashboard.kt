package com.jakubjirak.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.jakubjirak.analytics.LogAnalytics
import com.jakubjirak.analytics.LogStatistics
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.LogEntry
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.*

class AnalyticsDashboard(
    private val project: Project,
    private val entries: List<LogEntry>
) : JPanel(BorderLayout()) {
    
    private val licenseManager = LicenseManager.getInstance()
    private val analytics = LogAnalytics()
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        if (!licenseManager.canUseFeature(PremiumFeature.ANALYTICS)) {
            showPremiumPrompt()
            return
        }
        
        val stats = analytics.calculateStatistics(entries)
        
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        
        // Statistics cards
        mainPanel.add(createStatisticsPanel(stats))
        
        // Charts
        mainPanel.add(Box.createVerticalStrut(20))
        mainPanel.add(createChartsPanel(stats))
        
        val scrollPane = JBScrollPane(mainPanel)
        add(scrollPane, BorderLayout.CENTER)
    }
    
    private fun createStatisticsPanel(stats: LogStatistics): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // Cyber-industrial color palette
        panel.add(createStatCard("Total Entries", stats.totalEntries.toString(), Color(52, 73, 94)))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(createStatCard("Errors", stats.errorCount.toString(), Color(192, 57, 43)))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(createStatCard("Warnings", stats.warnCount.toString(), Color(214, 137, 16)))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(createStatCard("Info", stats.infoCount.toString(), Color(22, 160, 133)))
        panel.add(Box.createHorizontalStrut(10))
        panel.add(createStatCard("Error Rate", String.format("%.2f%%", stats.errorRate), Color(231, 76, 60)))
        
        return panel
    }
    
    private fun createStatCard(label: String, value: String, color: Color): JPanel {
        val card = JPanel()
        card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
        card.background = color
        card.border = BorderFactory.createEmptyBorder(15, 20, 15, 20)
        card.preferredSize = Dimension(150, 100)
        card.maximumSize = Dimension(150, 100)
        
        val labelComponent = JLabel(label)
        labelComponent.foreground = Color.WHITE
        labelComponent.font = labelComponent.font.deriveFont(12f)
        labelComponent.alignmentX = CENTER_ALIGNMENT
        
        val valueComponent = JLabel(value)
        valueComponent.foreground = Color.WHITE
        valueComponent.font = valueComponent.font.deriveFont(24f).deriveFont(java.awt.Font.BOLD)
        valueComponent.alignmentX = CENTER_ALIGNMENT
        
        card.add(labelComponent)
        card.add(Box.createVerticalStrut(10))
        card.add(valueComponent)
        
        return card
    }
    
    private fun createChartsPanel(stats: LogStatistics): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // Log level distribution chart
        val chartPanel = createLogLevelChart(stats)
        chartPanel.preferredSize = Dimension(600, 300)
        panel.add(chartPanel)
        
        return panel
    }
    
    private fun createLogLevelChart(stats: LogStatistics): JPanel {
        return object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                
                val total = stats.totalEntries
                if (total == 0) return
                
                val colors = arrayOf(
                    Color(192, 57, 43),   // Error - Red
                    Color(214, 137, 16),  // Warn - Orange
                    Color(22, 160, 133),  // Info - Green
                    Color(52, 73, 94)     // Debug - Gray
                )
                
                val values = arrayOf(
                    stats.errorCount,
                    stats.warnCount,
                    stats.infoCount,
                    stats.debugCount
                )
                
                val labels = arrayOf("Errors", "Warnings", "Info", "Debug")
                
                val barWidth = 80
                val barSpacing = 20
                val startX = 50
                val maxBarHeight = height - 100
                
                val maxValue = values.maxOrNull() ?: 1
                
                // Draw bars
                for (i in values.indices) {
                    val x = startX + i * (barWidth + barSpacing)
                    val barHeight = if (maxValue > 0) (values[i].toDouble() / maxValue * maxBarHeight).toInt() else 0
                    val y = height - 50 - barHeight
                    
                    g.color = colors[i]
                    g.fillRect(x, y, barWidth, barHeight)
                    
                    // Draw value on top
                    g.color = Color.BLACK
                    g.drawString(values[i].toString(), x + barWidth / 2 - 10, y - 5)
                    
                    // Draw label at bottom
                    g.drawString(labels[i], x + barWidth / 2 - 20, height - 30)
                }
                
                // Draw title
                g.color = Color.BLACK
                g.font = g.font.deriveFont(16f).deriveFont(java.awt.Font.BOLD)
                g.drawString("Log Level Distribution", width / 2 - 80, 30)
            }
        }
    }
    
    private fun showPremiumPrompt() {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(50, 50, 50, 50)
        
        val icon = JLabel("🔒")
        icon.font = icon.font.deriveFont(48f)
        icon.alignmentX = CENTER_ALIGNMENT
        
        val title = JLabel("Advanced Analytics - Premium Feature")
        title.font = title.font.deriveFont(18f).deriveFont(java.awt.Font.BOLD)
        title.alignmentX = CENTER_ALIGNMENT
        
        val description = JLabel("<html><center>Unlock powerful analytics including:<br>" +
                "• Log level distribution charts<br>" +
                "• Error rate trends<br>" +
                "• Performance heatmaps<br>" +
                "• Anomaly detection<br>" +
                "• Comparative analysis</center></html>")
        description.alignmentX = CENTER_ALIGNMENT
        
        val upgradeButton = JButton("Start 30-Day FREE Trial")
        upgradeButton.alignmentX = CENTER_ALIGNMENT
        upgradeButton.addActionListener {
            val started = licenseManager.startTrial()
            if (started) {
                JOptionPane.showMessageDialog(
                    this,
                    "Trial activated! Restart the dashboard to see analytics.",
                    "Trial Started",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Trial already used. Please purchase a license.",
                    "Trial Not Available",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        
        panel.add(icon)
        panel.add(Box.createVerticalStrut(20))
        panel.add(title)
        panel.add(Box.createVerticalStrut(10))
        panel.add(description)
        panel.add(Box.createVerticalStrut(30))
        panel.add(upgradeButton)
        
        add(panel, BorderLayout.CENTER)
    }
}
