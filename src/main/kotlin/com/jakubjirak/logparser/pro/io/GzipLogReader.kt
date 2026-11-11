/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.io

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

/**
 * Transparent reader for possibly-gzipped log files.
 *
 * A file is treated as gzip-compressed when its name matches
 * [GZIP_FILENAME_REGEX] *or* when its first two bytes are the gzip magic
 * number (`0x1F 0x8B`). The magic-number fallback protects users who drop
 * a gzipped file with a non-`.gz` suffix.
 *
 * This class does **not** register any file types — the plugin must
 * continue to claim only `.log` / `.out` for its editor integration.
 * Use this reader explicitly from parsers/exporters.
 */
object GzipLogReader {

    /** Gzip magic number bytes. */
    private const val MAGIC_BYTE_1 = 0x1F.toByte()
    private const val MAGIC_BYTE_2 = 0x8B.toByte()

    /** File names that should always be read through a gzip stream. */
    val GZIP_FILENAME_REGEX: Regex = Regex("""(?i).+\.(gz|gzip|log\.gz|out\.gz)$""")

    /**
     * Returns `true` when [file] should be decoded with gzip.
     *
     * Reads at most the first 2 bytes when the name suggests a plain file.
     */
    fun isGzipped(file: File): Boolean {
        if (!file.isFile) return false
        if (GZIP_FILENAME_REGEX.matches(file.name)) return true
        return try {
            file.inputStream().use { stream ->
                val b1 = stream.read()
                val b2 = stream.read()
                b1 == (MAGIC_BYTE_1.toInt() and 0xFF) && b2 == (MAGIC_BYTE_2.toInt() and 0xFF)
            }
        } catch (_: java.io.IOException) {
            // unreadable file: treat as not gzip, the caller falls back to plain-text reading
            false
        }
    }

    /** Returns `true` when [head] (first two bytes) is the gzip magic. */
    fun hasGzipMagic(head: ByteArray): Boolean {
        return head.size >= 2 && head[0] == MAGIC_BYTE_1 && head[1] == MAGIC_BYTE_2
    }

    /**
     * Opens a [BufferedReader] over [file], transparently decompressing
     * if [isGzipped] returns `true`.
     *
     * The caller is responsible for closing the returned reader.
     */
    fun open(file: File, charset: Charset = StandardCharsets.UTF_8): BufferedReader {
        val raw: InputStream = file.inputStream()
        val decoded: InputStream = if (isGzipped(file)) GZIPInputStream(raw) else raw
        return BufferedReader(InputStreamReader(decoded, charset))
    }

    /** Opens a [BufferedReader] over [input], detecting gzip by magic number. */
    fun open(input: InputStream, charset: Charset = StandardCharsets.UTF_8): BufferedReader {
        val buffered = input.buffered()
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        val decoded: InputStream = if (
            b1 == (MAGIC_BYTE_1.toInt() and 0xFF) && b2 == (MAGIC_BYTE_2.toInt() and 0xFF)
        ) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }
        return BufferedReader(InputStreamReader(decoded, charset))
    }
}
