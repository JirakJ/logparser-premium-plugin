/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory that creates the LogParser Pro tool window with four tabs:
 * Log Viewer, Search, Correlation, and Statistics.
 */
class LogParserToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val viewerContent = contentFactory.createContent(
            LogViewerPanel(project).getContent(), "Log Viewer", false
        )
        val searchContent = contentFactory.createContent(
            SearchPanel(project).getContent(), "Search", false
        )
        val correlationContent = contentFactory.createContent(
            CorrelationPanel(project).getContent(), "Correlation", false
        )
        val statsContent = contentFactory.createContent(
            StatisticsPanel(project).getContent(), "Statistics", false
        )

        toolWindow.contentManager.addContent(viewerContent)
        toolWindow.contentManager.addContent(searchContent)
        toolWindow.contentManager.addContent(correlationContent)
        toolWindow.contentManager.addContent(statsContent)
    }
}
