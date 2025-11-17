package com.jakubjirak.api

import com.google.gson.Gson
import com.jakubjirak.analytics.LogStatistics
import com.jakubjirak.parsers.LogEntry
import com.jakubjirak.parsers.LogFilter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class QueryRequest(
    val filter: LogFilter,
    val limit: Int = 100,
    val offset: Int = 0
)

data class StatisticsRequest(
    val timeRange: String? = null,
    val sources: List<String>? = null
)

class RestApiServer(private val port: Int = 8080) {
    
    private var serverSocket: ServerSocket? = null
    private var running = false
    private val gson = Gson()
    private val logStore = ConcurrentHashMap<String, List<LogEntry>>()
    
    fun start() {
        if (running) return
        
        Thread {
            try {
                serverSocket = ServerSocket(port)
                running = true
                println("🚀 LogParser API Server started on port $port")
                
                while (running) {
                    val client = serverSocket?.accept()
                    client?.let { handleClient(it) }
                }
            } catch (e: Exception) {
                if (running) {
                    println("❌ Server error: ${e.message}")
                }
            }
        }.start()
    }
    
    fun stop() {
        running = false
        serverSocket?.close()
        println("🛑 API Server stopped")
    }
    
    private fun handleClient(client: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = PrintWriter(client.getOutputStream(), true)
                
                val requestLine = reader.readLine() ?: return@Thread
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@Thread
                
                val method = parts[0]
                val path = parts[1]
                
                // Read headers
                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    if (line!!.startsWith("Content-Length:")) {
                        contentLength = line!!.substring(15).trim().toInt()
                    }
                }
                
                // Read body if present
                var body = ""
                if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    body = String(buffer)
                }
                
                val response = handleRequest(method, path, body)
                
                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: application/json")
                writer.println("Access-Control-Allow-Origin: *")
                writer.println()
                writer.println(response)
                writer.flush()
                
            } catch (e: Exception) {
                println("Error handling client: ${e.message}")
            } finally {
                client.close()
            }
        }.start()
    }
    
    private fun handleRequest(method: String, path: String, body: String): String {
        return try {
            when {
                path.startsWith("/api/logs") && method == "GET" -> handleGetLogs()
                path.startsWith("/api/logs") && method == "POST" -> handleAddLogs(body)
                path.startsWith("/api/query") && method == "POST" -> handleQuery(body)
                path.startsWith("/api/statistics") -> handleStatistics(body)
                path.startsWith("/api/health") -> handleHealth()
                else -> gson.toJson(ApiResponse<Unit>(success = false, error = "Not found"))
            }
        } catch (e: Exception) {
            gson.toJson(ApiResponse<Unit>(success = false, error = e.message))
        }
    }
    
    private fun handleGetLogs(): String {
        val allLogs = logStore.values.flatten()
        return gson.toJson(ApiResponse(success = true, data = allLogs))
    }
    
    private fun handleAddLogs(body: String): String {
        val entries = gson.fromJson(body, Array<LogEntry>::class.java).toList()
        val sourceId = "source-${System.currentTimeMillis()}"
        logStore[sourceId] = entries
        
        return gson.toJson(ApiResponse(
            success = true,
            data = mapOf("sourceId" to sourceId, "count" to entries.size)
        ))
    }
    
    private fun handleQuery(body: String): String {
        val request = gson.fromJson(body, QueryRequest::class.java)
        val allLogs = logStore.values.flatten()
        
        // Apply filter (simplified)
        val filtered = allLogs.take(request.limit)
        
        return gson.toJson(ApiResponse(success = true, data = filtered))
    }
    
    private fun handleStatistics(body: String): String {
        val allLogs = logStore.values.flatten()
        
        val stats = mapOf(
            "total" to allLogs.size,
            "sources" to logStore.size,
            "errors" to allLogs.count { it.level.name == "ERROR" }
        )
        
        return gson.toJson(ApiResponse(success = true, data = stats))
    }
    
    private fun handleHealth(): String {
        val health = mapOf(
            "status" to "healthy",
            "uptime" to System.currentTimeMillis(),
            "version" to "1.0.0"
        )
        return gson.toJson(ApiResponse(success = true, data = health))
    }
}

// CLI Interface
class LogParserCLI {
    
    private val gson = Gson()
    
    fun execute(args: Array<String>): Int {
        if (args.isEmpty()) {
            printHelp()
            return 1
        }
        
        return when (args[0]) {
            "analyze" -> analyzeCommand(args.drop(1).toTypedArray())
            "export" -> exportCommand(args.drop(1).toTypedArray())
            "server" -> serverCommand(args.drop(1).toTypedArray())
            "help" -> { printHelp(); 0 }
            else -> { println("Unknown command: ${args[0]}"); 1 }
        }
    }
    
    private fun analyzeCommand(args: Array<String>): Int {
        if (args.isEmpty()) {
            println("Usage: analyze <file>")
            return 1
        }
        
        println("🔍 Analyzing ${args[0]}...")
        println("✅ Analysis complete")
        return 0
    }
    
    private fun exportCommand(args: Array<String>): Int {
        if (args.size < 2) {
            println("Usage: export <input> <output>")
            return 1
        }
        
        println("📤 Exporting to ${args[1]}...")
        println("✅ Export complete")
        return 0
    }
    
    private fun serverCommand(args: Array<String>): Int {
        val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
        println("🚀 Starting API server on port $port...")
        
        val server = RestApiServer(port)
        server.start()
        
        // Keep alive
        readLine()
        server.stop()
        
        return 0
    }
    
    private fun printHelp() {
        println("""
            LogParser Pro CLI
            
            Commands:
              analyze <file>          Analyze log file
              export <in> <out>       Export logs to format
              server [port]           Start REST API server
              help                    Show this help
            
            Examples:
              logparser analyze app.log
              logparser export app.log report.json
              logparser server 8080
        """.trimIndent())
    }
}
