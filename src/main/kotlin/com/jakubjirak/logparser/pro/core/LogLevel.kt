/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.core

import java.awt.Color

/**
 * Log severity levels ordered from least to most severe.
 *
 * Each level carries a default display [color] for use in UI components.
 * The colours are intentionally muted to work on both light and dark themes;
 * use [com.jakubjirak.logparser.pro.settings.LogParserSettings] theme-aware
 * toggle for full JBUI colour adaptation.
 */
enum class LogLevel(val color: Color) {
    TRACE(Color(127, 140, 141)),
    DEBUG(Color(127, 140, 141)),
    INFO(Color(52, 152, 219)),
    WARN(Color(180, 120, 0)),
    ERROR(Color(192, 57, 43)),
    FATAL(Color(192, 57, 43)),
    UNKNOWN(Color(149, 165, 166))
}
