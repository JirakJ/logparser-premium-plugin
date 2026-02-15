package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

class ExtractPatternAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText
        
        if (selectedText.isNullOrEmpty()) {
            Messages.showWarningDialog(
                e.project,
                "Please select text to extract a pattern from",
                "No Selection"
            )
            return
        }
        
        Messages.showInfoMessage(
            e.project,
            """Pattern extraction (coming soon!)
            
            Selected text:
            $selectedText
            
            This will help you create custom filters and regex patterns.""".trimMargin(),
            "Extract Pattern"
        )
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && !editor.selectionModel.selectedText.isNullOrEmpty()
    }
}
