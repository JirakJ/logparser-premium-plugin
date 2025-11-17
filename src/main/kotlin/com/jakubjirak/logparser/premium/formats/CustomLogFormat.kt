package com.jakubjirak.logparser.premium.formats

import com.jakubjirak.logparser.licensing.LicenseManager
import java.util.regex.Pattern

/**
 * Premium Feature: Custom Log Format Definition
 */
class CustomLogFormatManager(private val licenseManager: LicenseManager) {
    
    private val customFormats = mutableMapOf<String, CustomLogFormat>()
    
    fun createFormat(
        name: String,
        pattern: String,
        fieldMappings: Map<String, Int>,
        timestampFormat: String? = null
    ): Boolean {
        if (!licenseManager.isPremium()) {
            return false
        }
        
        try {
            val compiledPattern = Pattern.compile(pattern)
            
            val format = CustomLogFormat(
                name = name,
                pattern = pattern,
                compiledPattern = compiledPattern,
                fieldMappings = fieldMappings,
                timestampFormat = timestampFormat
            )
            
            customFormats[name] = format
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun getFormat(name: String): CustomLogFormat? {
        if (!licenseManager.isPremium()) {
            return null
        }
        return customFormats[name]
    }
    
    fun getAllFormats(): List<CustomLogFormat> {
        if (!licenseManager.isPremium()) {
            return emptyList()
        }
        return customFormats.values.toList()
    }
    
    fun deleteFormat(name: String): Boolean {
        if (!licenseManager.isPremium()) {
            return false
        }
        
        return customFormats.remove(name) != null
    }
    
    fun parseLogLine(formatName: String, line: String): ParsedLogEntry? {
        if (!licenseManager.isPremium()) {
            return null
        }
        
        val format = customFormats[formatName] ?: return null
        val matcher = format.compiledPattern.matcher(line)
        
        if (!matcher.matches()) {
            return null
        }
        
        val fields = mutableMapOf<String, String>()
        format.fieldMappings.forEach { (fieldName, groupIndex) ->
            if (groupIndex <= matcher.groupCount()) {
                fields[fieldName] = matcher.group(groupIndex) ?: ""
            }
        }
        
        return ParsedLogEntry(
            formatName = formatName,
            fields = fields,
            rawLine = line
        )
    }
    
    fun testFormat(pattern: String, sampleLog: String): FormatTestResult {
        if (!licenseManager.isPremium()) {
            return FormatTestResult(false, "Premium feature required")
        }
        
        try {
            val compiledPattern = Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(sampleLog)
            
            if (matcher.matches()) {
                val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                return FormatTestResult(true, "Pattern matches", groups)
            } else {
                return FormatTestResult(false, "Pattern does not match")
            }
        } catch (e: Exception) {
            return FormatTestResult(false, "Invalid pattern: ${e.message}")
        }
    }
}

data class CustomLogFormat(
    val name: String,
    val pattern: String,
    val compiledPattern: Pattern,
    val fieldMappings: Map<String, Int>,
    val timestampFormat: String?
)

data class ParsedLogEntry(
    val formatName: String,
    val fields: Map<String, String>,
    val rawLine: String
)

data class FormatTestResult(
    val success: Boolean,
    val message: String,
    val capturedGroups: List<String> = emptyList()
)
