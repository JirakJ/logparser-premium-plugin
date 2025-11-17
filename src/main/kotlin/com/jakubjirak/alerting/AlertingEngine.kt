package com.jakubjirak.alerting

import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import com.jakubjirak.patterns.PatternMatch
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class AlertRule(
    val id: String,
    val name: String,
    val condition: AlertCondition,
    val actions: List<AlertAction>,
    val cooldownMinutes: Int = 5,
    val enabled: Boolean = true
)

sealed class AlertCondition {
    data class ErrorRateThreshold(val percentage: Double, val timeWindowMinutes: Int) : AlertCondition()
    data class ErrorCountThreshold(val count: Int, val timeWindowMinutes: Int) : AlertCondition()
    data class PatternMatchCount(val patternId: String, val count: Int, val timeWindowMinutes: Int) : AlertCondition()
    data class NoLogsReceived(val timeWindowMinutes: Int) : AlertCondition()
    data class SpecificLogLevel(val level: LogLevel, val count: Int, val timeWindowMinutes: Int) : AlertCondition()
}

sealed class AlertAction {
    data class Notification(val title: String, val message: String) : AlertAction()
    data class Slack(val webhookUrl: String, val channel: String, val message: String) : AlertAction()
    data class Email(val recipients: List<String>, val subject: String, val body: String) : AlertAction()
    data class Webhook(val url: String, val payload: Map<String, Any>) : AlertAction()
    data class JiraIssue(val projectKey: String, val issueType: String, val summary: String) : AlertAction()
}

data class Alert(
    val id: String,
    val rule: AlertRule,
    val triggeredAt: LocalDateTime,
    val message: String,
    val severity: AlertSeverity,
    val metadata: Map<String, Any> = emptyMap()
)

enum class AlertSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

class AlertingEngine {
    
    private val rules = ConcurrentHashMap<String, AlertRule>()
    private val recentAlerts = ConcurrentHashMap<String, LocalDateTime>()
    private val logHistory = mutableListOf<TimestampedEntry>()
    private val maxHistorySize = 10000
    
    data class TimestampedEntry(
        val entry: LogEntry,
        val timestamp: LocalDateTime
    )
    
    fun addRule(rule: AlertRule) {
        rules[rule.id] = rule
    }
    
    fun removeRule(ruleId: String) {
        rules.remove(ruleId)
    }
    
    fun processLogEntry(entry: LogEntry): List<Alert> {
        addToHistory(entry)
        
        val alerts = mutableListOf<Alert>()
        
        rules.values.filter { it.enabled }.forEach { rule ->
            if (shouldTriggerAlert(rule)) {
                val alert = createAlert(rule, entry)
                alerts.add(alert)
                recordAlertTrigger(rule.id)
            }
        }
        
        return alerts
    }
    
    private fun addToHistory(entry: LogEntry) {
        logHistory.add(TimestampedEntry(entry, LocalDateTime.now()))
        if (logHistory.size > maxHistorySize) {
            logHistory.removeAt(0)
        }
    }
    
    private fun shouldTriggerAlert(rule: AlertRule): Boolean {
        if (isInCooldown(rule.id, rule.cooldownMinutes)) {
            return false
        }
        
        return when (val condition = rule.condition) {
            is AlertCondition.ErrorRateThreshold -> checkErrorRate(condition)
            is AlertCondition.ErrorCountThreshold -> checkErrorCount(condition)
            is AlertCondition.NoLogsReceived -> checkNoLogs(condition)
            is AlertCondition.SpecificLogLevel -> checkLogLevel(condition)
            is AlertCondition.PatternMatchCount -> false // Requires pattern matches
        }
    }
    
    private fun checkErrorRate(condition: AlertCondition.ErrorRateThreshold): Boolean {
        val cutoffTime = LocalDateTime.now().minusMinutes(condition.timeWindowMinutes.toLong())
        val recentLogs = logHistory.filter { it.timestamp.isAfter(cutoffTime) }
        
        if (recentLogs.isEmpty()) return false
        
        val errorCount = recentLogs.count { 
            it.entry.level == LogLevel.ERROR || it.entry.level == LogLevel.FATAL 
        }
        val errorRate = (errorCount.toDouble() / recentLogs.size) * 100
        
        return errorRate > condition.percentage
    }
    
    private fun checkErrorCount(condition: AlertCondition.ErrorCountThreshold): Boolean {
        val cutoffTime = LocalDateTime.now().minusMinutes(condition.timeWindowMinutes.toLong())
        val errorCount = logHistory.count { 
            it.timestamp.isAfter(cutoffTime) && 
            (it.entry.level == LogLevel.ERROR || it.entry.level == LogLevel.FATAL)
        }
        
        return errorCount >= condition.count
    }
    
    private fun checkNoLogs(condition: AlertCondition.NoLogsReceived): Boolean {
        if (logHistory.isEmpty()) return false
        
        val lastLogTime = logHistory.lastOrNull()?.timestamp ?: return false
        val cutoffTime = LocalDateTime.now().minusMinutes(condition.timeWindowMinutes.toLong())
        
        return lastLogTime.isBefore(cutoffTime)
    }
    
    private fun checkLogLevel(condition: AlertCondition.SpecificLogLevel): Boolean {
        val cutoffTime = LocalDateTime.now().minusMinutes(condition.timeWindowMinutes.toLong())
        val count = logHistory.count {
            it.timestamp.isAfter(cutoffTime) && it.entry.level == condition.level
        }
        
        return count >= condition.count
    }
    
    private fun isInCooldown(ruleId: String, cooldownMinutes: Int): Boolean {
        val lastTrigger = recentAlerts[ruleId] ?: return false
        val cooldownEnd = lastTrigger.plusMinutes(cooldownMinutes.toLong())
        return LocalDateTime.now().isBefore(cooldownEnd)
    }
    
    private fun recordAlertTrigger(ruleId: String) {
        recentAlerts[ruleId] = LocalDateTime.now()
    }
    
    private fun createAlert(rule: AlertRule, entry: LogEntry): Alert {
        val severity = when (rule.condition) {
            is AlertCondition.ErrorRateThreshold -> AlertSeverity.ERROR
            is AlertCondition.ErrorCountThreshold -> AlertSeverity.WARNING
            is AlertCondition.NoLogsReceived -> AlertSeverity.CRITICAL
            is AlertCondition.SpecificLogLevel -> AlertSeverity.WARNING
            is AlertCondition.PatternMatchCount -> AlertSeverity.INFO
        }
        
        return Alert(
            id = "${rule.id}-${System.currentTimeMillis()}",
            rule = rule,
            triggeredAt = LocalDateTime.now(),
            message = "Alert triggered: ${rule.name}",
            severity = severity,
            metadata = mapOf(
                "logEntry" to entry.rawLine,
                "level" to entry.level.name
            )
        )
    }
    
    fun executeAlertActions(alert: Alert) {
        alert.rule.actions.forEach { action ->
            when (action) {
                is AlertAction.Notification -> sendNotification(action, alert)
                is AlertAction.Slack -> sendSlackMessage(action, alert)
                is AlertAction.Email -> sendEmail(action, alert)
                is AlertAction.Webhook -> callWebhook(action, alert)
                is AlertAction.JiraIssue -> createJiraIssue(action, alert)
            }
        }
    }
    
    private fun sendNotification(action: AlertAction.Notification, alert: Alert) {
        // Implementation would use IntelliJ notification system
        println("📢 Notification: ${action.title} - ${action.message}")
    }
    
    private fun sendSlackMessage(action: AlertAction.Slack, alert: Alert) {
        // Implementation would use Slack webhook
        println("💬 Slack to ${action.channel}: ${action.message}")
    }
    
    private fun sendEmail(action: AlertAction.Email, alert: Alert) {
        // Implementation would use email service
        println("📧 Email to ${action.recipients}: ${action.subject}")
    }
    
    private fun callWebhook(action: AlertAction.Webhook, alert: Alert) {
        // Implementation would make HTTP POST
        println("🔗 Webhook to ${action.url}")
    }
    
    private fun createJiraIssue(action: AlertAction.JiraIssue, alert: Alert) {
        // Implementation would use JIRA API
        println("🎫 JIRA issue in ${action.projectKey}: ${action.summary}")
    }
    
    fun getRecentAlerts(minutes: Int = 60): List<Alert> {
        val cutoff = LocalDateTime.now().minusMinutes(minutes.toLong())
        // In real implementation, store alerts in persistent storage
        return emptyList()
    }
}

// Predefined alert rules
object PredefinedAlerts {
    
    fun getDefaultRules(): List<AlertRule> = listOf(
        AlertRule(
            id = "high-error-rate",
            name = "High Error Rate",
            condition = AlertCondition.ErrorRateThreshold(10.0, 5),
            actions = listOf(
                AlertAction.Notification("High Error Rate", "Error rate exceeded 10% in last 5 minutes")
            ),
            cooldownMinutes = 15
        ),
        AlertRule(
            id = "critical-errors",
            name = "Critical Errors",
            condition = AlertCondition.ErrorCountThreshold(10, 1),
            actions = listOf(
                AlertAction.Notification("Critical Errors", "10+ errors in last minute")
            ),
            cooldownMinutes = 5
        ),
        AlertRule(
            id = "no-logs",
            name = "No Logs Received",
            condition = AlertCondition.NoLogsReceived(10),
            actions = listOf(
                AlertAction.Notification("No Logs", "No logs received in last 10 minutes")
            ),
            cooldownMinutes = 30
        )
    )
}
