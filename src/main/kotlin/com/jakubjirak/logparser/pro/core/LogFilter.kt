/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.core

import java.time.LocalDateTime

/**
 * Composite filter for narrowing down [LogEntry] collections.
 *
 * All non-empty/non-null fields must match for an entry to pass.
 *
 * @property levels         only entries at these levels pass (empty = all levels)
 * @property keywords       entry must contain at least one keyword (empty = no keyword filter)
 * @property startTime      inclusive lower bound on timestamp
 * @property endTime        inclusive upper bound on timestamp
 * @property correlationId  exact correlation ID match
 * @property threadName     exact thread name match
 * @property customPattern  regex that must match the raw line
 */
data class LogFilter(
    val levels: Set<LogLevel> = emptySet(),
    val keywords: List<String> = emptyList(),
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val correlationId: String? = null,
    val threadName: String? = null,
    val customPattern: Regex? = null
)
