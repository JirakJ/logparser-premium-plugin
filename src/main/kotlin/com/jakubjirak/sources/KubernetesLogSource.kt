package com.jakubjirak.sources

import com.intellij.openapi.project.Project
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.LogEntry
import io.reactivex.rxjava3.core.Observable

class KubernetesLogSource(
    private val project: Project,
    private val namespace: String,
    private val podName: String,
    private val containerName: String? = null
) : BaseLogSource() {
    
    private val licenseManager = LicenseManager.getInstance()
    
    override fun connect(): Boolean {
        if (!licenseManager.canUseFeature(PremiumFeature.MULTI_SOURCE)) {
            throw IllegalStateException("Kubernetes integration requires Premium license")
        }
        
        // TODO: Implement kubectl connection
        // For now, just check if kubectl is available
        try {
            val process = ProcessBuilder("kubectl", "version", "--client")
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
    
    override fun getName(): String = "$namespace/$podName${containerName?.let { "/$it" } ?: ""}"
    
    override fun getType(): String = "Kubernetes Pod"
    
    override fun streamLogs(): Observable<LogEntry> {
        return Observable.create { emitter ->
            if (!connected) {
                emitter.onError(IllegalStateException("Not connected"))
                return@create
            }
            
            try {
                val command = buildKubectlCommand(follow = false)
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    var lineNumber = 0
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        // TODO: Parse line to LogEntry
                        // For now, emit a simple entry
                        if (!emitter.isDisposed) {
                            // emitter.onNext(parseLineToEntry(line!!, lineNumber))
                        }
                    }
                }
                
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }
    
    override fun tailLogs(lines: Int): List<LogEntry> {
        if (!connected) {
            throw IllegalStateException("Not connected")
        }
        
        val command = buildKubectlCommand(follow = false, tail = lines)
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        
        val entries = mutableListOf<LogEntry>()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            var lineNumber = 0
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                // TODO: Parse line to LogEntry
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
                val command = buildKubectlCommand(follow = true)
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    var lineNumber = 0
                    while (reader.readLine().also { line = it } != null && !emitter.isDisposed) {
                        lineNumber++
                        // TODO: Parse line to LogEntry and emit
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
    
    private fun buildKubectlCommand(follow: Boolean = false, tail: Int? = null): List<String> {
        val command = mutableListOf("kubectl", "logs")
        
        if (namespace.isNotEmpty()) {
            command.addAll(listOf("-n", namespace))
        }
        
        command.add(podName)
        
        containerName?.let {
            command.addAll(listOf("-c", it))
        }
        
        if (follow) {
            command.add("-f")
        }
        
        tail?.let {
            command.addAll(listOf("--tail", it.toString()))
        }
        
        return command
    }
    
    companion object {
        fun listPods(namespace: String): List<String> {
            val command = if (namespace.isEmpty()) {
                listOf("kubectl", "get", "pods", "-o", "name")
            } else {
                listOf("kubectl", "get", "pods", "-n", namespace, "-o", "name")
            }
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val pods = mutableListOf<String>()
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Remove "pod/" prefix
                    val podName = line!!.removePrefix("pod/")
                    pods.add(podName)
                }
            }
            
            return pods
        }
        
        fun listNamespaces(): List<String> {
            val command = listOf("kubectl", "get", "namespaces", "-o", "name")
            
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            val namespaces = mutableListOf<String>()
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Remove "namespace/" prefix
                    val ns = line!!.removePrefix("namespace/")
                    namespaces.add(ns)
                }
            }
            
            return namespaces
        }
    }
}
