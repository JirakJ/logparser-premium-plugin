package com.jakubjirak.sources

import com.intellij.openapi.project.Project
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import com.jakubjirak.parsers.PatternLibrary
import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.time.LocalDateTime

class SSHLogSource(
    private val project: Project,
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val remoteLogPath: String,
    private val privateKeyPath: String? = null,
    private val password: String? = null
) : BaseLogSource() {
    
    private val licenseManager = LicenseManager.getInstance()
    private val patternLibrary = PatternLibrary()
    private var process: Process? = null
    
    override fun connect(): Boolean {
        if (!licenseManager.canUseFeature(PremiumFeature.MULTI_SOURCE)) {
            throw IllegalStateException("SSH integration requires Premium license")
        }
        
        try {
            val testCommand = buildSSHCommand(listOf("echo", "test"))
            val process = ProcessBuilder(testCommand)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            connected = exitCode == 0
            return connected
        } catch (e: Exception) {
            connected = false
            return false
        }
    }
    
    override fun getName(): String = "$username@$host:$remoteLogPath"
    
    override fun getType(): String = "SSH Server"
    
    override fun streamLogs(): Observable<LogEntry> {
        return Observable.create { emitter ->
            if (!connected) {
                emitter.onError(IllegalStateException("Not connected"))
                return@create
            }
            
            try {
                val command = buildSSHCommand(listOf("cat", remoteLogPath))
                process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    var lineNumber = 0
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        if (!emitter.isDisposed) {
                            val entry = parseLine(line!!, lineNumber)
                            emitter.onNext(entry)
                        }
                    }
                }
                
                emitter.onComplete()
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }
    
    override fun tailLogs(lines: Int): List<LogEntry> {
        if (!connected) {
            throw IllegalStateException("Not connected")
        }
        
        val command = buildSSHCommand(listOf("tail", "-n", lines.toString(), remoteLogPath))
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        
        val entries = mutableListOf<LogEntry>()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            var lineNumber = 0
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                entries.add(parseLine(line!!, lineNumber))
            }
        }
        
        return entries
    }
    
    fun followLogs(): Observable<LogEntry> {
        return Observable.create { emitter ->
            if (!connected) {
                emitter.onError(IllegalStateException("Not connected"))
                return@create
            }
            
            try {
                val command = buildSSHCommand(listOf("tail", "-f", remoteLogPath))
                process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                process?.inputStream?.bufferedReader()?.use { reader ->
                    var line: String?
                    var lineNumber = 0
                    while (reader.readLine().also { line = it } != null && !emitter.isDisposed) {
                        lineNumber++
                        val entry = parseLine(line!!, lineNumber)
                        emitter.onNext(entry)
                    }
                }
                
                emitter.onComplete()
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }
    
    fun executeCommand(command: String): List<String> {
        if (!connected) {
            throw IllegalStateException("Not connected")
        }
        
        val sshCommand = buildSSHCommand(command.split(" "))
        val process = ProcessBuilder(sshCommand)
            .redirectErrorStream(true)
            .start()
        
        val output = mutableListOf<String>()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.add(line!!)
            }
        }
        
        return output
    }
    
    fun listLogFiles(directory: String = "/var/log"): List<String> {
        if (!connected) {
            throw IllegalStateException("Not connected")
        }
        
        val command = buildSSHCommand(listOf("find", directory, "-name", "*.log", "-type", "f"))
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        
        val logFiles = mutableListOf<String>()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logFiles.add(line!!)
            }
        }
        
        return logFiles
    }
    
    private fun buildSSHCommand(remoteCommand: List<String>): List<String> {
        val command = mutableListOf("ssh")
        
        command.addAll(listOf("-p", port.toString()))
        
        command.addAll(listOf("-o", "StrictHostKeyChecking=no"))
        command.addAll(listOf("-o", "UserKnownHostsFile=/dev/null"))
        command.addAll(listOf("-o", "LogLevel=ERROR"))
        
        if (privateKeyPath != null && File(privateKeyPath).exists()) {
            command.addAll(listOf("-i", privateKeyPath))
        }
        
        command.add("$username@$host")
        
        command.addAll(remoteCommand)
        
        return command
    }
    
    private fun parseLine(line: String, lineNumber: Int): LogEntry {
        val timestamp = patternLibrary.parseTimestamp(line)
        val level = patternLibrary.parseLevel(line)
        val correlationId = patternLibrary.extractCorrelationId(line)
        val threadName = patternLibrary.extractThreadName(line)
        
        return LogEntry(
            lineNumber = lineNumber,
            timestamp = timestamp ?: LocalDateTime.now(),
            level = level,
            message = line,
            threadName = threadName,
            loggerName = null,
            correlationId = correlationId,
            stackTrace = null,
            rawLine = line
        )
    }
    
    override fun disconnect() {
        process?.destroy()
        process = null
        super.disconnect()
    }
    
    companion object {
        fun testConnection(host: String, port: Int, username: String, privateKeyPath: String?): Boolean {
            try {
                val command = mutableListOf("ssh")
                command.addAll(listOf("-p", port.toString()))
                command.addAll(listOf("-o", "StrictHostKeyChecking=no"))
                command.addAll(listOf("-o", "ConnectTimeout=5"))
                
                if (privateKeyPath != null && File(privateKeyPath).exists()) {
                    command.addAll(listOf("-i", privateKeyPath))
                }
                
                command.add("$username@$host")
                command.add("echo")
                command.add("connected")
                
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                return process.waitFor() == 0
            } catch (e: Exception) {
                return false
            }
        }
    }
}

data class SSHConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val privateKeyPath: String? = null,
    val password: String? = null
)
