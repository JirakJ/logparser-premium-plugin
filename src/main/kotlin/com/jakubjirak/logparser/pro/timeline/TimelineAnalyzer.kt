/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.timeline

import com.jakubjirak.logparser.pro.core.LogEntry
import com.jakubjirak.logparser.pro.core.LogLevel
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.sqrt

/**
 * A histogram bucket for a fixed time window.
 *
 * @property start           inclusive start of the bucket
 * @property end             exclusive end of the bucket
 * @property totalCount      total entries in this bucket
 * @property countsByLevel   count per [LogLevel]
 * @property errorCount      `ERROR + FATAL` count (convenience)
 */
data class TimeBucket(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val totalCount: Int,
    val countsByLevel: Map<LogLevel, Int>,
    val errorCount: Int
)

/**
 * Output of [TimelineAnalyzer.analyze].
 *
 * @property buckets      time-ordered buckets
 * @property bucketSize   the [Duration] used to bucket
 * @property spikes       indices into [buckets] where [TimeBucket.errorCount]
 *                        exceeds `mean + zThreshold*stddev`
 * @property totalEntries total entries considered (those with timestamps)
 * @property skipped      entries skipped because they had no timestamp
 */
data class TimelineReport(
    val buckets: List<TimeBucket>,
    val bucketSize: Duration,
    val spikes: List<Int>,
    val totalEntries: Int,
    val skipped: Int
)

/**
 * Time-bucket histogram with simple z-score spike detection.
 */
class TimelineAnalyzer {

    /**
     * Buckets [entries] into consecutive windows of [bucketSize] starting at the
     * first entry's timestamp truncated to [bucketSize] alignment.
     *
     * Entries without a timestamp are excluded from buckets and returned as
     * [TimelineReport.skipped].
     *
     * @param zThreshold number of standard deviations above the mean error
     *                   count required to flag a bucket as a spike
     */
    fun analyze(
        entries: List<LogEntry>,
        bucketSize: Duration = Duration.ofMinutes(1),
        zThreshold: Double = 3.0
    ): TimelineReport {
        require(!bucketSize.isNegative && !bucketSize.isZero) { "bucketSize must be positive" }
        val timed = entries.filter { it.timestamp != null }
        if (timed.isEmpty()) {
            return TimelineReport(emptyList(), bucketSize, emptyList(), 0, entries.size)
        }
        val stepSeconds = bucketSize.seconds.coerceAtLeast(1L)
        val minTs = timed.minOf { it.timestamp!! }
        val maxTs = timed.maxOf { it.timestamp!! }
        val start = alignDown(minTs, stepSeconds)
        val end = alignDown(maxTs, stepSeconds).plusSeconds(stepSeconds)
        val bucketCount = ((secondsBetween(start, end)) / stepSeconds).toInt().coerceAtLeast(1)
        val arrays = Array(bucketCount) { LongArray(LogLevel.entries.size) }
        for (entry in timed) {
            val idx = ((secondsBetween(start, entry.timestamp!!)) / stepSeconds).toInt()
            if (idx in 0 until bucketCount) {
                arrays[idx][entry.level.ordinal]++
            }
        }
        val buckets = (0 until bucketCount).map { idx ->
            val bStart = start.plusSeconds(idx * stepSeconds)
            val bEnd = bStart.plusSeconds(stepSeconds)
            val counts = LogLevel.entries.associateWith { arrays[idx][it.ordinal].toInt() }
            val total = counts.values.sum()
            val errors = (counts[LogLevel.ERROR] ?: 0) + (counts[LogLevel.FATAL] ?: 0)
            TimeBucket(bStart, bEnd, total, counts, errors)
        }
        val spikes = detectSpikes(buckets, zThreshold)
        return TimelineReport(
            buckets = buckets,
            bucketSize = bucketSize,
            spikes = spikes,
            totalEntries = timed.size,
            skipped = entries.size - timed.size
        )
    }

    /**
     * Flags bucket indices whose errorCount exceeds `mean + z*stddev`.
     * Requires at least 3 buckets and non-zero variance.
     */
    fun detectSpikes(buckets: List<TimeBucket>, zThreshold: Double): List<Int> {
        if (buckets.size < 3) return emptyList()
        val errors = buckets.map { it.errorCount.toDouble() }
        val mean = errors.average()
        val variance = errors.sumOf { (it - mean) * (it - mean) } / errors.size
        val stddev = sqrt(variance)
        if (stddev <= 0.0) return emptyList()
        val cutoff = mean + zThreshold * stddev
        return buckets.indices.filter { errors[it] > cutoff }
    }

    private fun alignDown(ts: LocalDateTime, stepSeconds: Long): LocalDateTime {
        val epoch = ts.toEpochSecond(ZoneOffset.UTC)
        val aligned = (epoch / stepSeconds) * stepSeconds
        return LocalDateTime.ofEpochSecond(aligned, 0, ZoneOffset.UTC)
    }

    private fun secondsBetween(a: LocalDateTime, b: LocalDateTime): Long {
        return b.toEpochSecond(ZoneOffset.UTC) - a.toEpochSecond(ZoneOffset.UTC)
    }
}
