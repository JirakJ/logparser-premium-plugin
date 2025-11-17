package com.jakubjirak.logparser.premium.themes

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "LogThemes", storages = [Storage("logparser-themes.xml")])
class CustomThemeManager(private val project: Project) : PersistentStateComponent<CustomThemeManager.State> {
    
    private var state = State()
    
    data class State(
        var themes: MutableList<LogTheme> = mutableListOf(),
        var activeThemeId: String = "cyber-industrial"
    )
    
    data class LogTheme(
        var id: String = "",
        var name: String = "",
        var errorColor: String = "#E74C3C",
        var warningColor: String = "#F39C12",
        var infoColor: String = "#5499C7",
        var backgroundColor: String = "#1C1F26",
        var textColor: String = "#ECF0F1"
    )
    
    init {
        if (state.themes.isEmpty()) {
            state.themes.add(LogTheme(id = "cyber-industrial", name = "Cyber Industrial"))
            state.themes.add(LogTheme(id = "dark-pro", name = "Dark Professional", 
                errorColor = "#FF6B6B", warningColor = "#FFD93D", backgroundColor = "#1E1E1E"))
        }
    }
    
    fun getActiveTheme() = state.themes.find { it.id == state.activeThemeId } ?: state.themes.first()
    override fun getState() = state
    override fun loadState(state: State) { this.state = state }
    
    companion object {
        fun getInstance(project: Project) = project.service<CustomThemeManager>()
    }
}
