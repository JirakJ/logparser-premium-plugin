/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.premium

import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * High-performance, multi-threaded log indexer for instant search across large files.
 *
 * Builds four concurrent indices (word, timestamp, log level, thread name) in parallel
 * using a fixed thread pool. Requires an active [PremiumFeature.LOG_INDEXING] license.
 */
class LogIndexer(private val licenseManager: LicenseManager) {

    private val indexes = ConcurrentHashMap<String, LogIndex>()
    private val executor = Executors.newFixedThreadPool(4)

    /** Immutable snapshot of all indices for a single file. */
    data class LogIndex(
        val fileId: String,
        val wordIndex: Map<String, List<Int>>,
        val timestampIndex: Map<Long, Int>,
        val levelIndex: Map<String, List<Int>>,
        val threadIndex: Map<String, List<Int>>,
        val totalLines: Int,
        val indexedAt: Long = System.currentTimeMillis()
    )

    /** Summary statistics for a [LogIndex]. */
    data class IndexStats(
        val totalWords: Int,
        val uniqueWords: Int,
        val totalLines: Int,
        val indexSizeMB: Double,
        val indexTimeMs: Long
    )

    /**
     * Indexes [logs] lines under the given [fileId].
     *
     * @throws IllegalStateException if the user does not hold a premium license
     */
    fun indexLogs(fileId: String, logs: List<String>, onProgress: (Int) -> Unit = {}): LogIndex {
        check(licenseManager.canUseFeature(PremiumFeature.LOG_INDEXING)) {
            "Log indexing requires a Premium license"
        }

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
                    tokenize(line).forEach { word ->
                        wordIndex.computeIfAbsent(word) { mutableListOf() }.add(lineNum)
                    }
                    extractTimestamp(line)?.let { ts -> timestampIndex[ts] = lineNum }
                    extractLogLevel(line)?.let { lvl ->
                        levelIndex.computeIfAbsent(lvl) { mutableListOf() }.add(lineNum)
                    }
                    extractThreadName(line)?.let { thread ->
                        threadIndex.computeIfAbsent(thread) { mutableListOf() }.add(lineNum)
                    }
                    if (lineNum % 1000 == 0) onProgress(lineNum)
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
        return index
    }

    /** Returns line numbers where all words of [query] appear (AND semantics). */
    fun searchIndexed(fileId: String, query: String): List<Int> {
        check(licenseManager.canUseFeature(PremiumFeature.LOG_INDEXING)) {
            "Indexed search requires a Premium license"
        }
        val index = checkNotNull(indexes[fileId]) { "File not indexed: $fileId" }
        val searchWords = tokenize(query.lowercase())
        if (searchWords.isEmpty()) return emptyList()

        val resultSets = searchWords.map { word -> index.wordIndex[word]?.toSet() ?: emptySet() }
        return resultSets.reduce { acc, set -> acc.intersect(set) }.sorted()
    }

    /** Returns line numbers matching the given log [level]. */
    fun searchByLevel(fileId: String, level: String): List<Int> {
        check(licenseManager.canUseFeature(PremiumFeature.LOG_INDEXING)) {
            "Level search requires a Premium license"
        }
        val index = checkNotNull(indexes[fileId]) { "File not indexed: $fileId" }
        return index.levelIndex[level.uppercase()] ?: emptyList()
    }

    /** Returns line numbers from the given [threadName]. */
    fun searchByThread(fileId: String, threadName: String): List<Int> {
        check(licenseManager.canUseFeature(PremiumFeature.LOG_INDEXING)) {
            "Thread search requires a Premium license"
        }
        val index = checkNotNull(indexes[fileId]) { "File not indexed: $fileId" }
        return index.threadIndex[threadName] ?: emptyList()
    }

    /** Returns line numbers whose timestamps fall within [startTime]..[endTime]. */
    fun searchByTimeRange(fileId: String, startTime: Long, endTime: Long): List<Int> {
        check(licenseManager.canUseFeature(PremiumFeature.LOG_INDEXING)) {
            "Time range search requires a Premium license"
        }
        val index = checkNotNull(indexes[fileId]) { "File not indexed: $fileId" }
        return index.timestampIndex.entries
            .filter { it.key in startTime..endTime }
            .map { it.value }
            .sorted()
    }

    /** Returns aggregate statistics for the index of [fileId], or `null` if not indexed. */
    fun getIndexStats(fileId: String): IndexStats? {
        val index = indexes[fileId] ?: return null
        val totalWords = index.wordIndex.values.sumOf { it.size }
        val uniqueWords = index.wordIndex.size
        val indexSizeMB = (totalWords * 8 + uniqueWords * 50) / (1024.0 * 1024.0)
        return IndexStats(totalWords, uniqueWords, index.totalLines, indexSizeMB, 0)
    }

    /** Removes the index for [fileId]. */
    fun clearIndex(fileId: String) { indexes.remove(fileId) }

    /** Removes all indexes. */
    fun clearAllIndexes() { indexes.clear() }

    /** Shuts down the indexing thread pool. */
    fun shutdown() { executor.shutdown() }

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
            if (Regex(pattern).containsMatchIn(line)) return System.currentTimeMillis()
        }
        return null
    }

    private fun extractLogLevel(line: String): String? {
        val levels = listOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE", "FATAL")
        return levels.find { line.contains(it, ignoreCase = true) }?.uppercase()
    }

    private fun extractThreadName(line: String): String? {
        val patterns = listOf("\\[([^\\]]+)\\]", "\\(([^)]+)\\)", "Thread-([\\w-]+)")
        for (pattern in patterns) {
            val match = Regex(pattern).find(line)
            if (match != null) return match.groupValues.getOrNull(1)
        }
        return null
    }
}
