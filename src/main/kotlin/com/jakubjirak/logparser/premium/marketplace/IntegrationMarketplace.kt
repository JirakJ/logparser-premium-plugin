package com.jakubjirak.logparser.premium.marketplace

import com.jakubjirak.logparser.licensing.LicenseManager

/**
 * Premium Feature: Integration Marketplace
 */
class IntegrationMarketplace(private val licenseManager: LicenseManager) {
    
    private val installedIntegrations = mutableSetOf<String>()
    
    fun browseIntegrations(category: IntegrationCategory? = null): List<Integration> {
        if (!licenseManager.isPremium()) {
            return emptyList()
        }
        
        val allIntegrations = getAvailableIntegrations()
        
        return if (category != null) {
            allIntegrations.filter { it.category == category }
        } else {
            allIntegrations
        }
    }
    
    fun installIntegration(integrationId: String): Boolean {
        if (!licenseManager.isPremium()) {
            return false
        }
        
        installedIntegrations.add(integrationId)
        return true
    }
    
    fun uninstallIntegration(integrationId: String): Boolean {
        if (!licenseManager.isPremium()) {
            return false
        }
        
        return installedIntegrations.remove(integrationId)
    }
    
    fun getInstalledIntegrations(): List<Integration> {
        if (!licenseManager.isPremium()) {
            return emptyList()
        }
        
        return getAvailableIntegrations().filter { it.id in installedIntegrations }
    }
    
    fun searchIntegrations(query: String): List<Integration> {
        if (!licenseManager.isPremium()) {
            return emptyList()
        }
        
        return getAvailableIntegrations().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
    
    private fun getAvailableIntegrations(): List<Integration> {
        return listOf(
            Integration(
                id = "grafana-loki",
                name = "Grafana Loki",
                description = "Connect to Grafana Loki for distributed log aggregation",
                category = IntegrationCategory.LOG_AGGREGATOR,
                author = "LogParser Team",
                version = "1.0.0",
                downloads = 1250,
                rating = 4.5,
                tags = listOf("grafana", "loki", "kubernetes")
            ),
            Integration(
                id = "elasticsearch",
                name = "Elasticsearch",
                description = "Query logs from Elasticsearch clusters",
                category = IntegrationCategory.LOG_AGGREGATOR,
                author = "Community",
                version = "2.1.0",
                downloads = 3420,
                rating = 4.8,
                tags = listOf("elasticsearch", "elk")
            ),
            Integration(
                id = "prometheus",
                name = "Prometheus Alerts",
                description = "Convert log patterns to Prometheus alert rules",
                category = IntegrationCategory.MONITORING,
                author = "LogParser Team",
                version = "1.2.0",
                downloads = 890,
                rating = 4.3,
                tags = listOf("prometheus", "alerting")
            )
        )
    }
}

data class Integration(
    val id: String,
    val name: String,
    val description: String,
    val category: IntegrationCategory,
    val author: String,
    val version: String,
    val downloads: Int,
    val rating: Double,
    val tags: List<String>
)

enum class IntegrationCategory {
    LOG_AGGREGATOR,
    MONITORING,
    INCIDENT_MANAGEMENT,
    NOTIFICATION,
    APM,
    CLOUD_PROVIDER,
    CI_CD
}
