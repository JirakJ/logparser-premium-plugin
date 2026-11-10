/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.language

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Parser definition for [LogLanguage].
 *
 * Log files are displayed via the custom [LogFileEditor][com.jakubjirak.logparser.pro.editor.LogFileEditor]
 * rather than a PSI tree, so this definition intentionally provides only a minimal
 * lexer-based parse and a simple [LogFile] PSI root.
 */
class LogParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = LogLexer()

    override fun createParser(project: Project?): PsiParser = PsiParser { _, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(FILE)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode?): PsiElement {
        throw UnsupportedOperationException("Log language does not define custom PSI elements")
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = LogFile(viewProvider)

    companion object {
        /** The single file-level element type for log files. */
        @JvmField
        val FILE = IFileElementType(LogLanguage)
    }
}

/**
 * PSI file node for log files, used by the IntelliJ Platform infrastructure.
 */
class LogFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LogLanguage) {

    override fun getFileType(): FileType = LogFileType.INSTANCE

    override fun toString(): String = "Log File"
}
