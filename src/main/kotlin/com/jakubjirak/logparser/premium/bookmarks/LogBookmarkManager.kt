package com.jakubjirak.logparser.premium.bookmarks

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jakubjirak.logparser.licensing.LicenseManager
import java.time.Instant

@Service(Service.Level.PROJECT)
@State(name = "LogBookmarks", storages = [Storage("logparser-bookmarks.xml")])
class LogBookmarkManager(private val project: Project) : PersistentStateComponent<LogBookmarkManager.State> {
    
    private var state = State()
    private val licenseManager = LicenseManager.getInstance(project)
    
    data class State(var bookmarks: MutableList<Bookmark> = mutableListOf())
    
    data class Bookmark(
        var id: String = "",
        var name: String = "",
        var description: String = "",
        var filePath: String = "",
        var lineNumber: Int = 0,
        var logContent: String = "",
        var timestamp: Long = 0,
        var tags: List<String> = emptyList(),
        var category: String = "General"
    )
    
    fun addBookmark(name: String, filePath: String, lineNumber: Int, content: String): Result<Bookmark> {
        if (!licenseManager.isPremium()) {
            return Result.failure(Exception("Premium feature required"))
        }
        
        val bookmark = Bookmark(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = "",
            filePath = filePath,
            lineNumber = lineNumber,
            logContent = content,
            timestamp = Instant.now().toEpochMilli()
        )
        state.bookmarks.add(bookmark)
        return Result.success(bookmark)
    }
    
    fun getBookmarks() = state.bookmarks.toList()
    override fun getState() = state
    override fun loadState(state: State) { this.state = state }
    
    companion object {
        fun getInstance(project: Project) = project.service<LogBookmarkManager>()
    }
}
