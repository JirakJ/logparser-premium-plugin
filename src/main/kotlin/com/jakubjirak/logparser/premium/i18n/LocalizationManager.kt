package com.jakubjirak.logparser.premium.i18n

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jakubjirak.logparser.licensing.LicenseManager
import java.util.*

@Service(Service.Level.PROJECT)
class LocalizationManager(private val project: Project) {
    
    private val licenseManager = LicenseManager.getInstance()
    private var currentLocale: Locale = Locale.ENGLISH
    
    private val translations = mapOf(
        "en" to mapOf("error" to "Error", "warning" to "Warning", "info" to "Information"),
        "cs" to mapOf("error" to "Chyba", "warning" to "Varování", "info" to "Informace"),
        "de" to mapOf("error" to "Fehler", "warning" to "Warnung", "info" to "Information")
    )
    
    fun setLocale(locale: Locale): Result<Unit> {
        if (!licenseManager.isPremium()) return Result.failure(Exception("Premium feature required"))
        currentLocale = locale
        return Result.success(Unit)
    }
    
    fun getString(key: String): String {
        val lang = if (licenseManager.isPremium()) currentLocale.language else "en"
        return translations[lang]?.get(key) ?: key
    }
    
    companion object {
        fun getInstance(project: Project): LocalizationManager = project.getService(LocalizationManager::class.java)
    }
}
