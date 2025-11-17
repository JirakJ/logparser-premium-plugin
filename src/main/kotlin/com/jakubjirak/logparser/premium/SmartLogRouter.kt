package com.jakubjirak.logparser.premium

import com.intellij.openapi.project.Project
import com.jakubjirak.parsers.LogEntry
import kotlinx.coroutines.*
import java.time.Instant

/**
 * Premium Feature: Smart log routing based on content, patterns and ML
 */
class SmartLogRouter(private val project: Project) {
    
    data class RoutingRule(
        val name: String,
        val priority: Int = 0,
        val conditions: List<Condition>,
        val actions: List<Action>,
        val enabled: Boolean = true
    )
    
    data class Condition(
        val type: ConditionType,
        val field: String? = null,
        val pattern: String? = null
    )
    
    enum class ConditionType {
        LEVEL_EQUALS, MESSAGE_CONTAINS, MESSAGE_REGEX, FIELD_EXISTS, CORRELATION_ID_EXISTS
    }
    
    data class Action(
        val type: ActionType,
        val target: String
    )
    
    enum class ActionType {
        FORWARD_TO_STORAGE, TRIGGER_ALERT, SEND_WEBHOOK, CREATE_INCIDENT, INDEX_FOR_SEARCH
    }
    
    data class RoutingDecision(
        val entry: LogEntry,
        val matchedRules: List<RoutingRule>,
        val actions: List<Action>,
        val executedAt: Instant = Instant.now()
    )
    
    private val rules = mutableListOf<RoutingRule>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun addRule(rule: RoutingRule) {
        rules.add(rule)
        rules.sortByDescending { it.priority }
    }
    
    suspend fun route(entry: LogEntry): RoutingDecision {
        val matchedRules = rules.filter { it.enabled && matchesConditions(entry, it.conditions) }
        val actions = matchedRules.flatMap { it.actions }.distinctBy { it.type to it.target }
        
        return RoutingDecision(entry = entry, matchedRules = matchedRules, actions = actions)
    }
    
    private fun matchesConditions(entry: LogEntry, conditions: List<Condition>): Boolean {
        return conditions.all { condition ->
            when (condition.type) {
                ConditionType.LEVEL_EQUALS -> entry.level.name.equals(condition.pattern, true)
                ConditionType.MESSAGE_CONTAINS -> entry.message.contains(condition.pattern ?: "", true)
                ConditionType.CORRELATION_ID_EXISTS -> entry.correlationId != null
                else -> true
            }
        }
    }
    
    fun dispose() {
        scope.cancel()
    }
}
