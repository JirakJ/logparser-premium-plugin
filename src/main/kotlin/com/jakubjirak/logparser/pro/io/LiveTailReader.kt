/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.io

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Result of a single [LiveTailReader.pullNew] invocation.
 *
 * @property newLines  complete lines emitted since the last pull
 * @property truncated `true` when the underlying file was rotated/truncated
 */
data class TailChunk(val newLines: List<String>, val truncated: Boolean)

/**
 * Incrementally reads new content appended to a file since the previous pull.
 *
 * Design notes (per review feedback):
 * - **Byte buffering.** The reader buffers *undecoded* bytes between calls so
 *   that a multibyte UTF-8 codepoint split across two appends is assembled
 *   correctly.
 * - **Line buffering.** A trailing partial line (no `\n`) is retained and
 *   prepended to the next read.
 * - **Rotation detection.** If the file length drops below the previous
 *   offset, the reader resets to offset 0 and reports `truncated = true`.
 * - **Single consumer.** [pullNew] is guarded by an [AtomicBoolean] so
 *   concurrent callers receive a clear error instead of lost lines.
 * - **Pure IO.** No IDE APIs — safe for background pool or plain JVM use.
 */
class LiveTailReader(
    private val file: File,
    startAtEnd: Boolean = true,
    charset: Charset = StandardCharsets.UTF_8
) {

    private val decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private val pending = ByteArrayOutputStream()
    private val lineBuffer = StringBuilder()
    private var offset: Long = 0
    private val reading = AtomicBoolean(false)

    init {
        offset = if (startAtEnd && file.isFile) file.length() else 0L
    }

    /** Current read offset in bytes. */
    fun currentOffset(): Long = offset

    /**
     * Reads any content appended since the last call and returns complete
     * lines. A trailing partial line is retained for the next call.
     *
     * @throws IllegalStateException if invoked concurrently on the same instance
     */
    fun pullNew(): TailChunk {
        check(reading.compareAndSet(false, true)) { "LiveTailReader must have a single consumer" }
        try {
            if (!file.isFile) {
                // File disappeared — wait for it to reappear.
                return TailChunk(emptyList(), truncated = false)
            }
            val length = file.length()
            var truncated = false
            if (length < offset) {
                // rotation / truncation
                offset = 0L
                lineBuffer.setLength(0)
                pending.reset()
                truncated = true
            }
            if (length == offset) {
                return TailChunk(emptyList(), truncated)
            }
            val bytes = readBytesFrom(offset, length - offset)
            offset = length
            val newLines = appendBytesAndExtractLines(bytes)
            return TailChunk(newLines, truncated)
        } finally {
            reading.set(false)
        }
    }

    /** Resets the reader to the end of the current file. */
    fun resetToEnd() {
        lineBuffer.setLength(0)
        pending.reset()
        offset = if (file.isFile) file.length() else 0L
    }

    /** Resets the reader to the beginning of the file. */
    fun resetToStart() {
        lineBuffer.setLength(0)
        pending.reset()
        offset = 0L
    }

    private fun readBytesFrom(from: Long, count: Long): ByteArray {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(from)
            val safe = count.coerceAtMost(Int.MAX_VALUE.toLong() - 1024).toInt()
            val buf = ByteArray(safe)
            var read = 0
            while (read < safe) {
                val n = raf.read(buf, read, safe - read)
                if (n < 0) break
                read += n
            }
            return if (read == safe) buf else buf.copyOf(read)
        }
    }

    private fun appendBytesAndExtractLines(newBytes: ByteArray): List<String> {
        pending.write(newBytes)
        val combined = pending.toByteArray()
        val buffer = java.nio.ByteBuffer.wrap(combined)
        val charBuffer = java.nio.CharBuffer.allocate(combined.size + 8)
        decoder.reset()
        decoder.decode(buffer, charBuffer, false)
        charBuffer.flip()
        lineBuffer.append(charBuffer)
        // Keep undecoded trailing bytes (incomplete multibyte)
        val remaining = buffer.remaining()
        pending.reset()
        if (remaining > 0) {
            pending.write(combined, buffer.position(), remaining)
        }
        return extractCompleteLines()
    }

    private fun extractCompleteLines(): List<String> {
        if (lineBuffer.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < lineBuffer.length) {
            val c = lineBuffer[i]
            if (c == '\n') {
                var endExclusive = i
                if (endExclusive > start && lineBuffer[endExclusive - 1] == '\r') endExclusive--
                out += lineBuffer.substring(start, endExclusive)
                start = i + 1
            }
            i++
        }
        if (start > 0) lineBuffer.delete(0, start)
        return out
    }
}
