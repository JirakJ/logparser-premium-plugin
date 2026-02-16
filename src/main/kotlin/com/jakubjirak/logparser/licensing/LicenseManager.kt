package com.jakubjirak.logparser.licensing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * License manager for handling freemium vs premium features
 */
@Service
class LicenseManager {
    
    companion object {
        fun getInstance(): LicenseManager = ApplicationManager.getApplication().getService(LicenseManager::class.java)
        
        private const val PRODUCT_CODE = "PLOGPARSERPRO"
    }
    
    private var cachedLicenseStatus: LicenseStatus? = null
    private var lastCheckTime: Long = 0
    private val checkIntervalMs = 60_000L
    
    enum class LicenseType {
        FREE,
        PREMIUM,
        TRIAL
    }
    
    data class LicenseStatus(
        val type: LicenseType,
        val isActive: Boolean,
        val expirationDate: Long? = null,
        val userName: String? = null,
        val email: String? = null
    )
    
    fun isPremium(): Boolean {
        val status = getLicenseStatus()
        return status.isActive && (status.type == LicenseType.PREMIUM || status.type == LicenseType.TRIAL)
    }
    
    fun isTrial(): Boolean {
        val status = getLicenseStatus()
        return status.isActive && status.type == LicenseType.TRIAL
    }
    
    fun isFree(): Boolean {
        return !isPremium()
    }
    
    fun getLicenseStatus(): LicenseStatus {
        val currentTime = System.currentTimeMillis()
        
        if (cachedLicenseStatus != null && (currentTime - lastCheckTime) < checkIntervalMs) {
            return cachedLicenseStatus!!
        }
        
        val status = try {
            checkMarketplaceLicense()
        } catch (e: Exception) {
            LicenseStatus(LicenseType.FREE, true)
        }
        
        cachedLicenseStatus = status
        lastCheckTime = currentTime
        
        return status
    }
    
    fun refreshLicenseStatus() {
        cachedLicenseStatus = null
        lastCheckTime = 0
    }
    
    fun getDaysRemaining(): Int? {
        val status = getLicenseStatus()
        if (status.expirationDate == null) return null
        
        val daysRemaining = ((status.expirationDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
        return maxOf(0, daysRemaining)
    }
    
    private fun checkMarketplaceLicense(): LicenseStatus {
        return try {
            LicenseStatus(
                type = LicenseType.FREE,
                isActive = true,
                userName = null,
                email = null
            )
        } catch (e: Exception) {
            LicenseStatus(LicenseType.FREE, true)
        }
    }
    
    fun hasFeature(feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.ADVANCED_SEARCH -> isPremium()
            PremiumFeature.CORRELATION_TRACKING -> isPremium()
            PremiumFeature.MULTI_SOURCE -> isPremium()
            PremiumFeature.DOCKER_INTEGRATION -> isPremium()
            PremiumFeature.SSH_INTEGRATION -> isPremium()
            PremiumFeature.KUBERNETES_INTEGRATION -> isPremium()
            PremiumFeature.CLOUD_STORAGE -> isPremium()
            PremiumFeature.ANALYTICS -> isPremium()
            PremiumFeature.PATTERN_DETECTION -> isPremium()
            PremiumFeature.ALERTING -> isPremium()
            PremiumFeature.TEAM_COLLABORATION -> isPremium()
            PremiumFeature.BOOKMARKS -> isPremium()
            PremiumFeature.LOG_INDEXING -> isPremium()
            PremiumFeature.EXPORT_PREMIUM -> isPremium()
            PremiumFeature.JIRA_INTEGRATION -> isPremium()
            PremiumFeature.SLACK_INTEGRATION -> isPremium()
            PremiumFeature.API_ACCESS -> isPremium()
        }
    }
    
    enum class PremiumFeature {
        ADVANCED_SEARCH,
        CORRELATION_TRACKING,
        MULTI_SOURCE,
        DOCKER_INTEGRATION,
        SSH_INTEGRATION,
        KUBERNETES_INTEGRATION,
        CLOUD_STORAGE,
        ANALYTICS,
        PATTERN_DETECTION,
        ALERTING,
        TEAM_COLLABORATION,
        BOOKMARKS,
        LOG_INDEXING,
        EXPORT_PREMIUM,
        JIRA_INTEGRATION,
        SLACK_INTEGRATION,
        API_ACCESS
    }
}
