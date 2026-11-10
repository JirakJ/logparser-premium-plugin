/*
 * Copyright 2026 Jakub Jirák
 * SPDX-License-Identifier: Apache-2.0
 */
package com.jakubjirak.logparser.pro.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class LiveTailReaderTest {

    @TempDir lateinit var tempDir: Path

    private fun file(name: String = "tail.log") = File(tempDir.toFile(), name)

    @Test
    fun `startAtEnd skips existing content`() {
        val f = file().apply { writeText("pre\nexisting\n") }
        val reader = LiveTailReader(f, startAtEnd = true)
        val chunk = reader.pullNew()
        assertTrue(chunk.newLines.isEmpty())
    }

    @Test
    fun `reads newly appended lines`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        f.appendText("one\ntwo\n")
        val chunk = reader.pullNew()
        assertEquals(listOf("one", "two"), chunk.newLines)
    }

    @Test
    fun `holds back partial last line`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        f.appendText("complete\npartial")
        val first = reader.pullNew()
        assertEquals(listOf("complete"), first.newLines)
        f.appendText("-finished\n")
        val second = reader.pullNew()
        assertEquals(listOf("partial-finished"), second.newLines)
    }

    @Test
    fun `handles CRLF line endings`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        f.appendText("alpha\r\nbeta\r\n")
        val chunk = reader.pullNew()
        assertEquals(listOf("alpha", "beta"), chunk.newLines)
    }

    @Test
    fun `detects truncation when length decreases`() {
        val f = file().apply { writeText("one\ntwo\nthree\n") }
        val reader = LiveTailReader(f, startAtEnd = true)
        f.writeText("short\n")
        val chunk = reader.pullNew()
        assertTrue(chunk.truncated)
        assertEquals(listOf("short"), chunk.newLines)
    }

    @Test
    fun `no new content returns empty chunk`() {
        val f = file().apply { writeText("x\n") }
        val reader = LiveTailReader(f, startAtEnd = true)
        val chunk = reader.pullNew()
        assertTrue(chunk.newLines.isEmpty())
        assertFalse(chunk.truncated)
    }

    @Test
    fun `handles multibyte utf8 split across reads`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        // "ř" in UTF-8 is 0xC5 0x99
        val bytes1 = "prefix ".toByteArray(Charsets.UTF_8) + byteArrayOf(0xC5.toByte())
        val bytes2 = byteArrayOf(0x99.toByte()) + " suffix\n".toByteArray(Charsets.UTF_8)
        f.writeBytes(bytes1)
        val c1 = reader.pullNew()
        assertTrue(c1.newLines.isEmpty())
        f.appendBytes(bytes2)
        val c2 = reader.pullNew()
        assertEquals(listOf("prefix ř suffix"), c2.newLines)
    }

    @Test
    fun `resetToStart rereads from beginning`() {
        val f = file().apply { writeText("one\ntwo\n") }
        val reader = LiveTailReader(f, startAtEnd = true)
        assertTrue(reader.pullNew().newLines.isEmpty())
        reader.resetToStart()
        assertEquals(listOf("one", "two"), reader.pullNew().newLines)
    }

    @Test
    fun `resetToEnd skips existing content`() {
        val f = file().apply { writeText("one\ntwo\n") }
        val reader = LiveTailReader(f, startAtEnd = false)
        reader.resetToEnd()
        assertTrue(reader.pullNew().newLines.isEmpty())
    }

    @Test
    fun `missing file returns empty non-truncated chunk`() {
        val f = file("missing.log")
        val reader = LiveTailReader(f, startAtEnd = false)
        val chunk = reader.pullNew()
        assertTrue(chunk.newLines.isEmpty())
        assertFalse(chunk.truncated)
    }

    @Test
    fun `concurrent pull raises explicit error`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        val running = AtomicBoolean(false)
        // simulate re-entrancy by grabbing the internal flag via a second thread
        // starting a pull while we block. Since this is hard to coordinate we
        // instead verify the safeguard directly by invoking reflectively.
        val field = LiveTailReader::class.java.getDeclaredField("reading")
        field.isAccessible = true
        val ab = field.get(reader) as AtomicBoolean
        ab.set(true)
        assertThrows(IllegalStateException::class.java) { reader.pullNew() }
        ab.set(false)
        running.set(true)
    }

    @Test
    fun `currentOffset advances after pull`() {
        val f = file().apply { writeText("") }
        val reader = LiveTailReader(f, startAtEnd = false)
        f.appendText("hello\n")
        reader.pullNew()
        assertEquals(f.length(), reader.currentOffset())
    }
}
