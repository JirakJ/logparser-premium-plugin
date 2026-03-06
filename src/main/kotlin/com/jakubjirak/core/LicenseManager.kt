/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Represents the tier of a user's license. */
enum class LicenseType {
    FREE,
    PREMIUM_TRIAL,
    PREMIUM,
    TEAM,
    ENTERPRISE
}

/** Serialisable state object persisted across IDE sessions. */
data class LicenseState(
    var type: LicenseType = LicenseType.FREE,
    var key: String = "",
    var email: String = "",
    var activatedAt: String = "",
    var expiresAt: String = "",
    var trialStartedAt: String = ""
)

/**
 * Application-level service responsible for license validation and feature gating.
 *
 * Supports a 30-day free trial, manual license-key activation (XXXX-XXXX-XXXX-XXXX),
 * and integrates with the JetBrains Marketplace licensing system when available.
 */
@State(
    name = "LogParserProLicense",
    storages = [Storage("logparser-pro-license.xml")]
)
class LicenseManager : PersistentStateComponent<LicenseState> {

    private var myState = LicenseState()

    companion object {
        /** Retrieves the application-scoped singleton. */
        fun getInstance(): LicenseManager {
            return ApplicationManager.getApplication().getService(LicenseManager::class.java)
        }

        const val TRIAL_DAYS = 30
        const val PRODUCT_CODE = "PLOGPARSERPRO"
    }

    override fun getState(): LicenseState = myState

    override fun loadState(state: LicenseState) {
        XmlSerializerUtil.copyBean(state, this.myState)
    }

    /** Returns `true` when the user holds an active premium-tier license or trial. */
    fun isPremium(): Boolean {
        if (hasMarketplaceLicense()) return true

        return when (myState.type) {
            LicenseType.PREMIUM, LicenseType.TEAM, LicenseType.ENTERPRISE -> isLicenseNotExpired()
            LicenseType.PREMIUM_TRIAL -> isTrialActive()
            else -> false
        }
    }

    private fun isLicenseNotExpired(): Boolean {
        if (myState.expiresAt.isEmpty()) return false
        val expiryDate = LocalDateTime.parse(myState.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
        return LocalDateTime.now().isBefore(expiryDate)
    }

    /**
     * Checks for an active paid license from the JetBrains Marketplace.
     * Returns `false` until the plugin is submitted and marketplace integration is configured.
     */
    @Suppress("FunctionOnlyReturningConstant")
    private fun hasMarketplaceLicense(): Boolean = false

    /** Starts a 30-day premium trial. Returns `false` if a trial was already started. */
    fun startTrial(): Boolean {
        if (myState.trialStartedAt.isNotEmpty()) return false

        myState.type = LicenseType.PREMIUM_TRIAL
        myState.trialStartedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        myState.expiresAt = LocalDateTime.now()
            .plusDays(TRIAL_DAYS.toLong())
            .format(DateTimeFormatter.ISO_DATE_TIME)
        return true
    }

    /** Returns `true` when the trial period has not yet expired. */
    fun isTrialActive(): Boolean {
        if (myState.trialStartedAt.isEmpty()) return false
        val expiryDate = LocalDateTime.parse(myState.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
        return LocalDateTime.now().isBefore(expiryDate)
    }

    /** Number of calendar days remaining in the trial, or `0` if the trial is inactive. */
    fun getTrialDaysRemaining(): Int {
        if (!isTrialActive()) return 0
        val expiryDate = LocalDateTime.parse(myState.expiresAt, DateTimeFormatter.ISO_DATE_TIME)
        return Duration.between(LocalDateTime.now(), expiryDate).toDays().toInt()
    }

    /**
     * Activates a license with format `XXXX-XXXX-XXXX-XXXX`.
     * Returns `true` on success, `false` if the key format is invalid.
     */
    fun activateLicense(key: String, email: String): Boolean {
        if (!validateLicenseKey(key)) return false

        myState.type = LicenseType.PREMIUM
        myState.key = key
        myState.email = email
        myState.activatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        myState.expiresAt = LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ISO_DATE_TIME)
        return true
    }

    private fun validateLicenseKey(key: String): Boolean {
        val pattern = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return pattern.matches(key)
    }

    /** Reverts the license state to [LicenseType.FREE]. */
    fun deactivateLicense() {
        myState.type = LicenseType.FREE
        myState.key = ""
        myState.email = ""
        myState.activatedAt = ""
        myState.expiresAt = ""
    }

    /** Checks whether the given [feature] is available under the current license. */
    fun canUseFeature(feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.BASIC_VIEWING -> true
            else -> isPremium()
        }
    }
}

/** Enumeration of features with their respective licensing tier. */
enum class PremiumFeature {
    BASIC_VIEWING,
    CORRELATION_TRACKING,
    MULTI_SOURCE,
    LIVE_TAIL,
    AI_PATTERNS,
    ANALYTICS,
    UNLIMITED_FILES,
    ADVANCED_SEARCH,
    LOG_INDEXING
}
