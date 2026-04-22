/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class GzipLogReaderTest {

    @TempDir lateinit var tempDir: Path

    private fun gzippedBytes(content: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { it.write(content.toByteArray()) }
        return bytes.toByteArray()
    }

    @Test
    fun `detects gzip by filename`() {
        val f = File(tempDir.toFile(), "app.log.gz").apply { writeBytes(gzippedBytes("hello")) }
        assertTrue(GzipLogReader.isGzipped(f))
    }

    @Test
    fun `detects gzip by magic when filename does not indicate it`() {
        val f = File(tempDir.toFile(), "mystery.bin").apply { writeBytes(gzippedBytes("hello")) }
        assertTrue(GzipLogReader.isGzipped(f))
    }

    @Test
    fun `returns false for plain text file`() {
        val f = File(tempDir.toFile(), "plain.log").apply { writeText("hello\nworld") }
        assertFalse(GzipLogReader.isGzipped(f))
    }

    @Test
    fun `reads gzipped content transparently`() {
        val f = File(tempDir.toFile(), "a.log.gz").apply { writeBytes(gzippedBytes("one\ntwo\nthree\n")) }
        val lines = GzipLogReader.open(f).useLines { it.toList() }
        assertEquals(listOf("one", "two", "three"), lines)
    }

    @Test
    fun `reads plain file transparently`() {
        val f = File(tempDir.toFile(), "a.log").apply { writeText("alpha\nbeta\n") }
        val lines = GzipLogReader.open(f).useLines { it.toList() }
        assertEquals(listOf("alpha", "beta"), lines)
    }

    @Test
    fun `stream overload decompresses gzip input`() {
        val bytes = gzippedBytes("stream-one\nstream-two\n")
        val lines = GzipLogReader.open(ByteArrayInputStream(bytes)).useLines { it.toList() }
        assertEquals(listOf("stream-one", "stream-two"), lines)
    }

    @Test
    fun `stream overload reads plain input when no magic`() {
        val lines = GzipLogReader.open(ByteArrayInputStream("x\ny".toByteArray())).useLines { it.toList() }
        assertEquals(listOf("x", "y"), lines)
    }

    @Test
    fun `returns false for missing file`() {
        val f = File(tempDir.toFile(), "missing.gz")
        assertFalse(GzipLogReader.isGzipped(f))
    }

    @Test
    fun `returns false for empty file`() {
        val f = File(tempDir.toFile(), "empty.log").apply { createNewFile() }
        assertFalse(GzipLogReader.isGzipped(f))
    }

    @Test
    fun `hasGzipMagic helper works`() {
        assertTrue(GzipLogReader.hasGzipMagic(byteArrayOf(0x1F.toByte(), 0x8B.toByte(), 0x08)))
        assertFalse(GzipLogReader.hasGzipMagic(byteArrayOf(0x00, 0x00)))
        assertFalse(GzipLogReader.hasGzipMagic(byteArrayOf()))
    }
}
