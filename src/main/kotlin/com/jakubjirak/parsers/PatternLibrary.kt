package com.jakubjirak.parsers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

class PatternLibrary {
    
    private val timestampPatterns = listOf(
        // ISO 8601: 2024-01-15T10:30:45.123Z
        DateTimeFormatter.ISO_DATE_TIME,
        
        // Spring Boot: 2024-01-15 10:30:45.123
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
            .optionalEnd()
            .toFormatter(),
        
        // Apache/Nginx: 15/Jan/2024:10:30:45 +0000
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z"),
        
        // Java: Jan 15, 2024 10:30:45 AM
        DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a"),
        
        // Syslog: Jan 15 10:30:45
        DateTimeFormatterBuilder()
            .appendPattern("MMM dd HH:mm:ss")
            .parseDefaulting(ChronoField.YEAR, LocalDateTime.now().year.toLong())
            .toFormatter()
    )
    
    private val levelPatterns = mapOf(
        "TRACE" to LogLevel.TRACE,
        "DEBUG" to LogLevel.DEBUG,
        "INFO" to LogLevel.INFO,
        "WARN" to LogLevel.WARN,
        "WARNING" to LogLevel.WARN,
        "ERROR" to LogLevel.ERROR,
        "FATAL" to LogLevel.FATAL,
        "SEVERE" to LogLevel.ERROR
    )
    
    // Common correlation ID patterns
    private val correlationIdPatterns = listOf(
        Regex("""X-Request-ID[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""X-Correlation-ID[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""traceId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""trace_id[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""spanId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""requestId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""correlationId[:\s=]+([a-f0-9\-]+)""", RegexOption.IGNORE_CASE),
        // UUID pattern
        Regex("""([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})""", RegexOption.IGNORE_CASE)
    )
    
    // Thread name patterns
    private val threadPattern = Regex("""\[([^\]]+)\]""")
    
    // Logger name patterns (e.g., com.example.MyClass)
    private val loggerPattern = Regex("""([a-z]+\.[a-z.]+[A-Z][a-zA-Z]+)""")
    
    fun parseTimestamp(line: String): LocalDateTime? {
        for (pattern in timestampPatterns) {
            try {
                // Try to find timestamp in the first 50 characters
                val prefix = line.take(50)
                val parsed = LocalDateTime.parse(prefix.trim(), pattern)
                return parsed
            } catch (e: Exception) {
                // Try next pattern
                continue
            }
        }
        return null
    }
    
    fun parseLevel(line: String): LogLevel {
        for ((keyword, level) in levelPatterns) {
            if (line.contains(keyword, ignoreCase = true)) {
                return level
            }
        }
        return LogLevel.UNKNOWN
    }
    
    fun extractCorrelationId(line: String): String? {
        for (pattern in correlationIdPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues.getOrNull(1) ?: match.value
            }
        }
        return null
    }
    
    fun extractThreadName(line: String): String? {
        val match = threadPattern.find(line)
        return match?.groupValues?.getOrNull(1)
    }
    
    fun extractLoggerName(line: String): String? {
        val match = loggerPattern.find(line)
        return match?.groupValues?.getOrNull(1)
    }
    
    fun isStackTraceLine(line: String): Boolean {
        return line.trimStart().startsWith("at ") ||
               line.trimStart().startsWith("Caused by:") ||
               line.trimStart().startsWith("... ") ||
               line.contains("Exception")
    }
}
