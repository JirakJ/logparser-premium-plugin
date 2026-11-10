/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

/**
 * Generates a regex pattern from the currently selected text in the editor.
 *
 * Common tokens (timestamps, numbers, UUIDs, IP addresses) are replaced
 * with named capture groups; the rest is quoted literally.
 */
class ExtractPatternAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText

        if (selectedText.isNullOrEmpty()) {
            Messages.showWarningDialog(
                e.project,
                "Please select text to extract a pattern from.",
                "No Selection"
            )
            return
        }

        val pattern = buildRegex(selectedText)
        Messages.showInfoMessage(
            e.project,
            "Generated regex:\n\n$pattern",
            "Extracted Pattern"
        )
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && !editor.selectionModel.selectedText.isNullOrEmpty()
    }

    internal companion object {
        private val UUID_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )
        private val ISO_TIMESTAMP_REGEX = Regex(
            "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d*"
        )
        private val IP_REGEX = Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
        private val NUMBER_REGEX = Regex("\\b\\d+\\b")

        /** Replaces common tokens with named capture groups. */
        internal fun buildRegex(text: String): String {
            var result = Regex.escape(text)
            result = UUID_REGEX.replace(result) { "(?<uuid>[0-9a-fA-F-]{36})" }
            result = ISO_TIMESTAMP_REGEX.replace(result) { "(?<timestamp>\\\\S+)" }
            result = IP_REGEX.replace(result) { "(?<ip>\\\\d{1,3}(?:\\\\.\\\\d{1,3}){3})" }
            result = NUMBER_REGEX.replace(result) { "(?<num>\\\\d+)" }
            return result
        }
    }
}
