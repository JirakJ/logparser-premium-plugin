package com.jakubjirak.logparser.premium

import com.jakubjirak.logparser.licensing.LicenseManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * High-performance log indexer for instant search in large files
 * PREMIUM FEATURE
 */
class LogIndexer(private val licenseManager: LicenseManager) {
    
    private val indexes = ConcurrentHashMap<String, LogIndex>()
    private val executor = Executors.newFixedThreadPool(4)
    
    data class LogIndex(
        val fileId: String,
        val wordIndex: Map<String, List<Int>>,
        val timestampIndex: Map<Long, Int>,
        val levelIndex: Map<String, List<Int>>,
        val threadIndex: Map<String, List<Int>>,
        val totalLines: Int,
        val indexedAt: Long = System.currentTimeMillis()
    )
    
    data class IndexStats(
        val totalWords: Int,
        val uniqueWords: Int,
        val totalLines: Int,
        val indexSizeMB: Double,
        val indexTimeMs: Long
    )
    
    fun indexLogs(fileId: String, logs: List<String>, onProgress: (Int) -> Unit = {}): LogIndex {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Log indexing requires Premium license")
        }
        
        val startTime = System.currentTimeMillis()
        
        val wordIndex = ConcurrentHashMap<String, MutableList<Int>>()
        val timestampIndex = ConcurrentHashMap<Long, Int>()
        val levelIndex = ConcurrentHashMap<String, MutableList<Int>>()
        val threadIndex = ConcurrentHashMap<String, MutableList<Int>>()
        
        val chunkSize = maxOf(1000, logs.size / 4)
        val futures = logs.chunked(chunkSize).mapIndexed { chunkIdx, chunk ->
            executor.submit {
                val baseLineNum = chunkIdx * chunkSize
                chunk.forEachIndexed { idx, line ->
                    val lineNum = baseLineNum + idx
                    
                    val words = tokenize(line)
                    words.forEach { word ->
                        wordIndex.computeIfAbsent(word) { mutableListOf() }.add(lineNum)
                    }
                    
                    extractTimestamp(line)?.let { timestamp ->
                        timestampIndex[timestamp] = lineNum
                    }
                    
                    extractLogLevel(line)?.let { level ->
                        levelIndex.computeIfAbsent(level) { mutableListOf() }.add(lineNum)
                    }
                    
                    extractThreadName(line)?.let { thread ->
                        threadIndex.computeIfAbsent(thread) { mutableListOf() }.add(lineNum)
                    }
                    
                    if (lineNum % 1000 == 0) {
                        onProgress(lineNum)
                    }
                }
            }
        }
        
        futures.forEach { it.get() }
        
        val index = LogIndex(
            fileId = fileId,
            wordIndex = wordIndex.mapValues { it.value.toList() },
            timestampIndex = timestampIndex.toMap(),
            levelIndex = levelIndex.mapValues { it.value.toList() },
            threadIndex = threadIndex.mapValues { it.value.toList() },
            totalLines = logs.size
        )
        
        indexes[fileId] = index
        
        val indexTime = System.currentTimeMillis() - startTime
        println("Indexed ${logs.size} lines in ${indexTime}ms")
        
        return index
    }
    
    fun searchIndexed(fileId: String, query: String): List<Int> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Indexed search requires Premium license")
        }
        
        val index = indexes[fileId] ?: throw IllegalStateException("File not indexed: $fileId")
        
        val searchWords = tokenize(query.lowercase())
        if (searchWords.isEmpty()) return emptyList()
        
        val resultSets = searchWords.map { word ->
            index.wordIndex[word]?.toSet() ?: emptySet()
        }
        
        return if (resultSets.isNotEmpty()) {
            resultSets.reduce { acc, set -> acc.intersect(set) }.sorted()
        } else {
            emptyList()
        }
    }
    
    fun searchByLevel(fileId: String, level: String): List<Int> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Level search requires Premium license")
        }
        
        val index = indexes[fileId] ?: throw IllegalStateException("File not indexed: $fileId")
        return index.levelIndex[level.uppercase()] ?: emptyList()
    }
    
    fun searchByThread(fileId: String, threadName: String): List<Int> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Thread search requires Premium license")
        }
        
        val index = indexes[fileId] ?: throw IllegalStateException("File not indexed: $fileId")
        return index.threadIndex[threadName] ?: emptyList()
    }
    
    fun searchByTimeRange(fileId: String, startTime: Long, endTime: Long): List<Int> {
        if (!licenseManager.isPremium()) {
            throw IllegalStateException("Time range search requires Premium license")
        }
        
        val index = indexes[fileId] ?: throw IllegalStateException("File not indexed: $fileId")
        return index.timestampIndex.entries
            .filter { it.key in startTime..endTime }
            .map { it.value }
            .sorted()
    }
    
    fun getIndexStats(fileId: String): IndexStats? {
        val index = indexes[fileId] ?: return null
        
        val totalWords = index.wordIndex.values.sumOf { it.size }
        val uniqueWords = index.wordIndex.size
        val indexSizeMB = (totalWords * 8 + uniqueWords * 50) / (1024.0 * 1024.0)
        
        return IndexStats(
            totalWords = totalWords,
            uniqueWords = uniqueWords,
            totalLines = index.totalLines,
            indexSizeMB = indexSizeMB,
            indexTimeMs = 0
        )
    }
    
    fun clearIndex(fileId: String) {
        indexes.remove(fileId)
    }
    
    fun clearAllIndexes() {
        indexes.clear()
    }
    
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split("\\s+".toRegex())
            .map { it.replace("[^a-z0-9]".toRegex(), "") }
            .filter { it.length > 2 }
    }
    
    private fun extractTimestamp(line: String): Long? {
        val patterns = listOf(
            "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}",
            "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}",
            "\\d{13}"
        )
        
        for (pattern in patterns) {
            val match = Regex(pattern).find(line)
            if (match != null) {
                return try {
                    System.currentTimeMillis()
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }
    
    private fun extractLogLevel(line: String): String? {
        val levels = listOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE", "FATAL")
        return levels.find { level ->
            line.contains(level, ignoreCase = true) ||
            line.contains("[$level]", ignoreCase = true) ||
            line.contains("$level:", ignoreCase = true)
        }?.uppercase()
    }
    
    private fun extractThreadName(line: String): String? {
        val patterns = listOf(
            "\\[([^\\]]+)\\]",
            "\\(([^)]+)\\)",
            "Thread-([\\w-]+)"
        )
        
        for (pattern in patterns) {
            val match = Regex(pattern).find(line)
            if (match != null) {
                return match.groupValues.getOrNull(1)
            }
        }
        return null
    }
    
    fun shutdown() {
        executor.shutdown()
    }
}
