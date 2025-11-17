package com.jakubjirak.logparser.premium.audit

import com.jakubjirak.logparser.licensing.LicenseManager
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Premium Feature: Audit Trail
 * Track all user actions for compliance and security
 */
class AuditTrail(private val licenseManager: LicenseManager) {
    
    private val auditLog = ConcurrentHashMap<String, MutableList<AuditEntry>>()
    private val maxEntriesPerUser = 10000
    
    fun logAction(userId: String, action: AuditAction, details: Map<String, Any> = emptyMap()) {
        if (!licenseManager.isPremium()) {
            return
        }
        
        val entry = AuditEntry(
            timestamp = Instant.now(),
            userId = userId,
            action = action,
            details = details,
            ipAddress = getClientIpAddress(),
            sessionId = getCurrentSessionId()
        )
        
        val userLog = auditLog.getOrPut(userId) { mutableListOf() }
        
        synchronized(userLog) {
            userLog.add(entry)
            if (userLog.size > maxEntriesPerUser) {
                userLog.removeAt(0)
            }
        }
    }
    
    fun getAuditLog(userId: String, fromTime: Instant? = null, toTime: Instant? = null): List<AuditEntry> {
        if (!licenseManager.isPremium()) {
            return emptyList()
        }
        
        val userLog = auditLog[userId] ?: return emptyList()
        
        return userLog.filter { entry ->
            (fromTime == null || entry.timestamp >= fromTime) &&
            (toTime == null || entry.timestamp <= toTime)
        }
    }
    
    fun exportAuditLog(userId: String, format: ExportFormat = ExportFormat.JSON): String {
        if (!licenseManager.isPremium()) {
            return ""
        }
        
        val entries = getAuditLog(userId)
        
        return when (format) {
            ExportFormat.JSON -> exportAsJson(entries)
            ExportFormat.CSV -> exportAsCsv(entries)
            ExportFormat.XML -> exportAsXml(entries)
        }
    }
    
    fun getComplianceReport(fromTime: Instant, toTime: Instant): ComplianceReport {
        if (!licenseManager.isPremium()) {
            return ComplianceReport.empty()
        }
        
        val allEntries = auditLog.values.flatten()
            .filter { it.timestamp >= fromTime && it.timestamp <= toTime }
        
        return ComplianceReport(
            totalActions = allEntries.size,
            uniqueUsers = allEntries.map { it.userId }.distinct().size,
            actionBreakdown = allEntries.groupBy { it.action }.mapValues { it.value.size },
            sensitiveActions = allEntries.filter { it.action.isSensitive },
            failedActions = allEntries.filter { it.details["success"] == false }
        )
    }
    
    private fun getClientIpAddress(): String {
        return "127.0.0.1"
    }
    
    private fun getCurrentSessionId(): String {
        return System.currentTimeMillis().toString()
    }
    
    private fun exportAsJson(entries: List<AuditEntry>): String {
        return entries.joinToString(",\n", "[\n", "\n]") { entry ->
            """  {
    "timestamp": "${entry.timestamp}",
    "userId": "${entry.userId}",
    "action": "${entry.action}",
    "details": ${entry.details},
    "ipAddress": "${entry.ipAddress}",
    "sessionId": "${entry.sessionId}"
  }"""
        }
    }
    
    private fun exportAsCsv(entries: List<AuditEntry>): String {
        val header = "Timestamp,User ID,Action,Details,IP Address,Session ID\n"
        val rows = entries.joinToString("\n") { entry ->
            "${entry.timestamp},${entry.userId},${entry.action},${entry.details},${entry.ipAddress},${entry.sessionId}"
        }
        return header + rows
    }
    
    private fun exportAsXml(entries: List<AuditEntry>): String {
        val items = entries.joinToString("\n") { entry ->
            """  <entry>
    <timestamp>${entry.timestamp}</timestamp>
    <userId>${entry.userId}</userId>
    <action>${entry.action}</action>
    <ipAddress>${entry.ipAddress}</ipAddress>
    <sessionId>${entry.sessionId}</sessionId>
  </entry>"""
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<auditLog>\n$items\n</auditLog>"
    }
}

data class AuditEntry(
    val timestamp: Instant,
    val userId: String,
    val action: AuditAction,
    val details: Map<String, Any>,
    val ipAddress: String,
    val sessionId: String
)

enum class AuditAction(val isSensitive: Boolean = false) {
    LOG_OPENED(false),
    LOG_EXPORTED(true),
    FILTER_APPLIED(false),
    SEARCH_PERFORMED(false),
    CORRELATION_TRACED(false),
    PATTERN_CREATED(true),
    ALERT_CONFIGURED(true),
    SETTINGS_CHANGED(true),
    DATA_DELETED(true),
    ENCRYPTION_ENABLED(true),
    ENCRYPTION_DISABLED(true),
    USER_LOGIN(false),
    USER_LOGOUT(false),
    LICENSE_ACTIVATED(true),
    LICENSE_DEACTIVATED(true)
}

enum class ExportFormat {
    JSON, CSV, XML
}

data class ComplianceReport(
    val totalActions: Int,
    val uniqueUsers: Int,
    val actionBreakdown: Map<AuditAction, Int>,
    val sensitiveActions: List<AuditEntry>,
    val failedActions: List<AuditEntry>
) {
    companion object {
        fun empty() = ComplianceReport(0, 0, emptyMap(), emptyList(), emptyList())
    }
}
