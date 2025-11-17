package com.jakubjirak.sources

import com.intellij.openapi.project.Project
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogLevel
import com.jakubjirak.parsers.PatternLibrary
import io.reactivex.rxjava3.core.Observable
import java.time.LocalDateTime

class DockerLogSource(
    private val project: Project,
    private val containerId: String,
    private val containerName: String? = null
) : BaseLogSource() {
    
    private val licenseManager = LicenseManager.getInstance()
    private val patternLibrary = PatternLibrary()
    private var process: Process? = null
    
    override fun connect(): Boolean {
        if (!licenseManager.canUseFeature(PremiumFeature.MULTI_SOURCE)) {
            throw IllegalStateException("Docker integration requires Premium license")
        }
        
        try {
            val process = ProcessBuilder("docker", "version", "--format", "{{.Server.Version}}")
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
    
    override fun getName(): String = containerName ?: containerId.take(12)
    
    override fun getType(): String = "Docker Container"
    
    override fun streamLogs(): Observable<LogEntry> {
        return Observable.create { emitter ->
            if (!connected) {
                emitter.onError(IllegalStateException("Not connected"))
                return@create
            }
            
            try {
                val command = buildDockerCommand(follow = false)
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
        
        val command = buildDockerCommand(follow = false, tail = lines)
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
                val command = buildDockerCommand(follow = true)
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
    
    private fun buildDockerCommand(follow: Boolean = false, tail: Int? = null): List<String> {
        val command = mutableListOf("docker", "logs")
        
        if (follow) {
            command.add("-f")
        }
        
        tail?.let {
            command.addAll(listOf("--tail", it.toString()))
        }
        
        command.add("--timestamps")
        command.add(containerId)
        
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
        fun listContainers(): List<DockerContainer> {
            val command = listOf("docker", "ps", "--format", "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}")
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val containers = mutableListOf<DockerContainer>()
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("|")
                    if (parts.size >= 4) {
                        containers.add(
                            DockerContainer(
                                id = parts[0],
                                name = parts[1],
                                image = parts[2],
                                status = parts[3]
                            )
                        )
                    }
                }
            }
            
            return containers
        }
        
        fun listAllContainers(): List<DockerContainer> {
            val command = listOf("docker", "ps", "-a", "--format", "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}")
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val containers = mutableListOf<DockerContainer>()
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("|")
                    if (parts.size >= 4) {
                        containers.add(
                            DockerContainer(
                                id = parts[0],
                                name = parts[1],
                                image = parts[2],
                                status = parts[3]
                            )
                        )
                    }
                }
            }
            
            return containers
        }
    }
}

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val status: String
)
