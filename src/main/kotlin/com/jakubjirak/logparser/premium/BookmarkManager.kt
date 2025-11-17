package com.jakubjirak.logparser.premium

import com.jakubjirak.logparser.licensing.LicenseManager
import com.intellij.openapi.project.Project
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

/**
 * Bookmark manager for saving interesting log entries and transactions
 * PREMIUM FEATURE
 */
class BookmarkManager(
    private val project: Project,
    private val licenseManager: LicenseManager
) {
    
    @Serializable
    data class Bookmark(
        val id: String,
        val name: String,
        val description: String = "",
        val fileId: String,
        val lineNumber: Int,
        val content: String,
        val tags: List<String> = emptyList(),
        val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
        val context: BookmarkContext? = null
    )
    
    @Serializable
    data class BookmarkContext(
        val correlationId: String? = null,
        val threadName: String? = null,
        val logLevel: String? = null,
        val timestamp: Long? = null,
        val contextBefore: List<String> = emptyList(),
        val contextAfter: List<String> = emptyList()
    )
    
    @Serializable
    data class BookmarkCollection(
        val bookmarks: MutableList<Bookmark> = mutableListOf()
    )
    
    private val json = Json { prettyPrint = true }
    private val bookmarksFile = File(project.basePath, ".logparser/bookmarks.json")
    
    private var collection: BookmarkCollection = loadBookmarks()
    
    fun addBookmark(
        name: String,
        description: String = "",
        fileId: String,
        lineNumber: Int,
        content: String,
        tags: List<String> = emptyList(),
        context: BookmarkContext? = null
    ): Bookmark {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        
        val bookmark = Bookmark(
            id = generateId(),
            name = name,
            description = description,
            fileId = fileId,
            lineNumber = lineNumber,
            content = content,
            tags = tags,
            context = context
        )
        
        collection.bookmarks.add(bookmark)
        saveBookmarks()
        
        return bookmark
    }
    
    fun removeBookmark(id: String): Boolean {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        
        val removed = collection.bookmarks.removeIf { it.id == id }
        if (removed) {
            saveBookmarks()
        }
        return removed
    }
    
    fun getBookmark(id: String): Bookmark? {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        return collection.bookmarks.find { it.id == id }
    }
    
    fun getAllBookmarks(): List<Bookmark> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        return collection.bookmarks.toList()
    }
    
    fun searchBookmarks(query: String): List<Bookmark> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        
        val lowerQuery = query.lowercase()
        return collection.bookmarks.filter { bookmark ->
            bookmark.name.lowercase().contains(lowerQuery) ||
            bookmark.description.lowercase().contains(lowerQuery) ||
            bookmark.content.lowercase().contains(lowerQuery) ||
            bookmark.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }
    
    fun getBookmarksByTag(tag: String): List<Bookmark> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        return collection.bookmarks.filter { tag in it.tags }
    }
    
    fun getBookmarksByFile(fileId: String): List<Bookmark> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        return collection.bookmarks.filter { it.fileId == fileId }
    }
    
    fun updateBookmark(id: String, updater: (Bookmark) -> Bookmark): Boolean {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        
        val index = collection.bookmarks.indexOfFirst { it.id == id }
        if (index >= 0) {
            collection.bookmarks[index] = updater(collection.bookmarks[index])
            saveBookmarks()
            return true
        }
        return false
    }
    
    fun exportBookmarks(file: File) {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        file.writeText(json.encodeToString(collection))
    }
    
    fun importBookmarks(file: File, merge: Boolean = false) {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        
        val imported = json.decodeFromString<BookmarkCollection>(file.readText())
        
        if (merge) {
            val existingIds = collection.bookmarks.map { it.id }.toSet()
            val newBookmarks = imported.bookmarks.filter { it.id !in existingIds }
            collection.bookmarks.addAll(newBookmarks)
        } else {
            collection = imported
        }
        
        saveBookmarks()
    }
    
    fun getAllTags(): Set<String> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Bookmarks require Premium license")
        }
        return collection.bookmarks.flatMap { it.tags }.toSet()
    }
    
    private fun loadBookmarks(): BookmarkCollection {
        return try {
            if (bookmarksFile.exists()) {
                json.decodeFromString(bookmarksFile.readText())
            } else {
                BookmarkCollection()
            }
        } catch (e: Exception) {
            println("Failed to load bookmarks: ${e.message}")
            BookmarkCollection()
        }
    }
    
    private fun saveBookmarks() {
        try {
            bookmarksFile.parentFile.mkdirs()
            bookmarksFile.writeText(json.encodeToString(collection))
        } catch (e: Exception) {
            println("Failed to save bookmarks: ${e.message}")
        }
    }
    
    private fun generateId(): String {
        return "bookmark_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}
