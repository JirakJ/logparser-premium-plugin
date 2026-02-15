package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.LicenseType
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridLayout

class ManageLicenseAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = LicenseDialog(e.project)
        dialog.show()
    }
}

class LicenseDialog(project: com.intellij.openapi.project.Project?) : DialogWrapper(project) {
    
    private val licenseManager = LicenseManager.getInstance()
    private val licenseKeyField = JTextField(20)
    private val emailField = JTextField(20)
    
    init {
        title = "Manage License"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        val statusPanel = JPanel()
        statusPanel.layout = BoxLayout(statusPanel, BoxLayout.Y_AXIS)
        
        val state = licenseManager.state
        statusPanel.add(JLabel("Current License: ${state.type}"))
        
        when (state.type) {
            LicenseType.FREE -> {
                statusPanel.add(JLabel("You are using the free version"))
                statusPanel.add(JLabel(" "))
                statusPanel.add(JLabel("Start your 30-day FREE trial to unlock all Premium features!"))
            }
            LicenseType.PREMIUM_TRIAL -> {
                val daysRemaining = licenseManager.getTrialDaysRemaining()
                statusPanel.add(JLabel("Trial days remaining: $daysRemaining"))
            }
            LicenseType.PREMIUM, LicenseType.TEAM, LicenseType.ENTERPRISE -> {
                statusPanel.add(JLabel("Email: ${state.email}"))
                statusPanel.add(JLabel("Activated: ${state.activatedAt}"))
                statusPanel.add(JLabel("Expires: ${state.expiresAt}"))
            }
        }
        
        panel.add(statusPanel, BorderLayout.NORTH)
        
        if (state.type == LicenseType.FREE || state.type == LicenseType.PREMIUM_TRIAL) {
            val formPanel = JPanel(GridLayout(3, 2, 5, 5))
            formPanel.add(JLabel("License Key:"))
            formPanel.add(licenseKeyField)
            formPanel.add(JLabel("Email:"))
            formPanel.add(emailField)
            
            val activateButton = JButton("Activate License")
            activateButton.addActionListener { activateLicense() }
            formPanel.add(JLabel(""))
            formPanel.add(activateButton)
            
            panel.add(formPanel, BorderLayout.CENTER)
            
            if (state.type == LicenseType.FREE && state.trialStartedAt.isEmpty()) {
                val trialButton = JButton("Start 30-Day FREE Trial")
                trialButton.addActionListener { startTrial() }
                panel.add(trialButton, BorderLayout.SOUTH)
            }
        } else {
            val deactivateButton = JButton("Deactivate License")
            deactivateButton.addActionListener { deactivateLicense() }
            panel.add(deactivateButton, BorderLayout.SOUTH)
        }
        
        return panel
    }
    
    private fun startTrial() {
        val success = licenseManager.startTrial()
        if (success) {
            Messages.showInfoMessage(
                "Your 30-day FREE trial has started!\n\nAll Premium features are now unlocked.",
                "Trial Started"
            )
            close(OK_EXIT_CODE)
        } else {
            Messages.showErrorDialog(
                "Trial has already been used for this installation.",
                "Trial Not Available"
            )
        }
    }
    
    private fun activateLicense() {
        val key = licenseKeyField.text.trim()
        val email = emailField.text.trim()
        
        if (key.isEmpty() || email.isEmpty()) {
            Messages.showErrorDialog("Please enter both license key and email", "Validation Error")
            return
        }
        
        val success = licenseManager.activateLicense(key, email)
        if (success) {
            Messages.showInfoMessage(
                "License activated successfully!\n\nThank you for supporting LogParser Pro!",
                "License Activated"
            )
            close(OK_EXIT_CODE)
        } else {
            Messages.showErrorDialog(
                "Invalid license key format.\n\nExpected format: XXXX-XXXX-XXXX-XXXX",
                "Activation Failed"
            )
        }
    }
    
    private fun deactivateLicense() {
        val result = Messages.showYesNoDialog(
            "Are you sure you want to deactivate your license?\n\nYou will lose access to Premium features.",
            "Confirm Deactivation",
            Messages.getWarningIcon()
        )
        
        if (result == Messages.YES) {
            licenseManager.deactivateLicense()
            Messages.showInfoMessage("License deactivated", "Success")
            close(OK_EXIT_CODE)
        }
    }
}
