package com.jakubjirak.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LicenseType {
    FREE,
    PREMIUM_TRIAL,
    PREMIUM,
    TEAM,
    ENTERPRISE
}

data class LicenseState(
    var type: LicenseType = LicenseType.FREE,
    var key: String = "",
    var email: String = "",
    var activatedAt: String = "",
    var expiresAt: String = "",
    var trialStartedAt: String = ""
)

@State(
    name = "LogParserProLicense",
    storages = [Storage("logparser-pro-license.xml")]
)
class LicenseManager : PersistentStateComponent<LicenseState> {
    
    private var state = LicenseState()

    companion object {
        fun getInstance(): LicenseManager {
            return ApplicationManager.getApplication().getService(LicenseManager::class.java)
        }
        
        const val TRIAL_DAYS = 30
        const val PRODUCT_CODE = "PLOGPARSERPRO"
    }

    override fun getState(): LicenseState = state

    override fun loadState(state: LicenseState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun isPremium(): Boolean {
        // Check JetBrains Marketplace license first
        if (hasMarketplaceLicense()) {
            return true
        }
        
        // Fallback to manual license check
        return when (state.type) {
            LicenseType.PREMIUM, LicenseType.TEAM, LicenseType.ENTERPRISE -> {
                if (state.expiresAt.isNotEmpty()) {
                    val expiryDate = LocalDateTime.parse(state.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
                    LocalDateTime.now().isBefore(expiryDate)
                } else {
                    false
                }
            }
            LicenseType.PREMIUM_TRIAL -> isTrialActive()
            else -> false
        }
    }
    
    private fun hasMarketplaceLicense(): Boolean {
        // Check if user has active paid license from JetBrains Marketplace
        // This integrates with JetBrains' built-in licensing system
        try {
            // TODO: Integrate with com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginAdvertiserService
            // For now, return false - will be activated after Marketplace submission
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun startTrial(): Boolean {
        if (state.trialStartedAt.isNotEmpty()) {
            return false // Trial already used
        }
        
        state.type = LicenseType.PREMIUM_TRIAL
        state.trialStartedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        state.expiresAt = LocalDateTime.now().plusDays(TRIAL_DAYS.toLong())
            .format(DateTimeFormatter.ISO_DATE_TIME)
        return true
    }

    fun isTrialActive(): Boolean {
        if (state.trialStartedAt.isEmpty()) return false
        
        val expiryDate = LocalDateTime.parse(state.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
        return LocalDateTime.now().isBefore(expiryDate)
    }

    fun getTrialDaysRemaining(): Int {
        if (!isTrialActive()) return 0
        
        val expiryDate = LocalDateTime.parse(state.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
        val now = LocalDateTime.now()
        return java.time.Duration.between(now, expiryDate).toDays().toInt()
    }

    fun activateLicense(key: String, email: String): Boolean {
        // TODO: Implement real license validation with backend
        // For now, simple validation
        if (!validateLicenseKey(key)) {
            return false
        }
        
        state.type = LicenseType.PREMIUM
        state.key = key
        state.email = email
        state.activatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        state.expiresAt = LocalDateTime.now().plusYears(1)
            .format(DateTimeFormatter.ISO_DATE_TIME)
        return true
    }

    private fun validateLicenseKey(key: String): Boolean {
        // Simple format validation: XXXX-XXXX-XXXX-XXXX
        val pattern = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return pattern.matches(key)
    }

    fun deactivateLicense() {
        state.type = LicenseType.FREE
        state.key = ""
        state.email = ""
        state.activatedAt = ""
        state.expiresAt = ""
    }

    fun canUseFeature(feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.BASIC_VIEWING -> true // Free for everyone
            else -> isPremium()
        }
    }
}

enum class PremiumFeature {
    BASIC_VIEWING,
    CORRELATION_TRACKING,
    MULTI_SOURCE,
    LIVE_TAIL,
    AI_PATTERNS,
    ANALYTICS,
    UNLIMITED_FILES,
    TEAM_COLLABORATION,
    API_ACCESS
}
