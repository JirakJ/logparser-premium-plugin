package com.jakubjirak.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jakubjirak.parsers.LogParser
import com.jakubjirak.parsers.PatternLibrary
import java.io.File

class LogParserService(private val project: Project) : Disposable {
    
    private val patternLibrary = PatternLibrary()
    private val parsers = mutableMapOf<String, LogParser>()
    
    fun parseLogFile(file: File): LogParser {
        val parser = parsers.getOrPut(file.absolutePath) {
            LogParser(file, patternLibrary)
        }
        return parser
    }
    
    fun closeLogFile(file: File) {
        parsers.remove(file.absolutePath)?.dispose()
    }
    
    fun getPatternLibrary(): PatternLibrary = patternLibrary
    
    override fun dispose() {
        parsers.values.forEach { it.dispose() }
        parsers.clear()
    }
    
    companion object {
        fun getInstance(project: Project): LogParserService {
            return project.getService(LogParserService::class.java)
        }
    }
}
