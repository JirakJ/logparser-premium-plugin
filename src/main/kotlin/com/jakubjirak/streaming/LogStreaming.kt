package com.jakubjirak.streaming

import com.jakubjirak.parsers.LogEntry
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class LogStreamManager {
    
    private val streamSubject = PublishSubject.create<StreamEvent>()
    private val subscribers = mutableMapOf<String, StreamSubscriber>()
    
    fun publish(entry: LogEntry, source: String) {
        val event = StreamEvent.LogReceived(entry, source, System.currentTimeMillis())
        streamSubject.onNext(event)
    }
    
    fun publishBatch(entries: List<LogEntry>, source: String) {
        val event = StreamEvent.BatchReceived(entries, source, System.currentTimeMillis())
        streamSubject.onNext(event)
    }
    
    fun subscribe(subscriberId: String, filter: StreamFilter? = null): Observable<StreamEvent> {
        val subscriber = StreamSubscriber(subscriberId, filter)
        subscribers[subscriberId] = subscriber
        
        return if (filter != null) {
            streamSubject.filter { filter.matches(it) }
        } else {
            streamSubject
        }
    }
    
    fun unsubscribe(subscriberId: String) {
        subscribers.remove(subscriberId)
    }
    
    fun getActiveSubscribers(): List<String> = subscribers.keys.toList()
}

sealed class StreamEvent {
    abstract val timestamp: Long
    
    data class LogReceived(
        val entry: LogEntry,
        val source: String,
        override val timestamp: Long
    ) : StreamEvent()
    
    data class BatchReceived(
        val entries: List<LogEntry>,
        val source: String,
        override val timestamp: Long
    ) : StreamEvent()
    
    data class SourceConnected(
        val source: String,
        override val timestamp: Long
    ) : StreamEvent()
    
    data class SourceDisconnected(
        val source: String,
        override val timestamp: Long
    ) : StreamEvent()
}

data class StreamFilter(
    val sources: List<String>? = null,
    val levels: List<String>? = null,
    val keywords: List<String>? = null
) {
    fun matches(event: StreamEvent): Boolean {
        return when (event) {
            is StreamEvent.LogReceived -> {
                val sourceMatch = sources?.contains(event.source) ?: true
                val levelMatch = levels?.contains(event.entry.level.name) ?: true
                val keywordMatch = keywords?.any { 
                    event.entry.message.contains(it, ignoreCase = true) 
                } ?: true
                sourceMatch && levelMatch && keywordMatch
            }
            is StreamEvent.BatchReceived -> {
                sources?.contains(event.source) ?: true
            }
            else -> true
        }
    }
}

data class StreamSubscriber(
    val id: String,
    val filter: StreamFilter?
)

class WebhookManager {
    
    private val webhooks = mutableMapOf<String, Webhook>()
    
    fun registerWebhook(webhook: Webhook) {
        webhooks[webhook.id] = webhook
    }
    
    fun unregisterWebhook(webhookId: String) {
        webhooks.remove(webhookId)
    }
    
    fun triggerWebhook(webhookId: String, payload: WebhookPayload): Boolean {
        val webhook = webhooks[webhookId] ?: return false
        
        return try {
            sendWebhookRequest(webhook, payload)
            true
        } catch (e: Exception) {
            println("Webhook failed: ${e.message}")
            false
        }
    }
    
    fun triggerAll(event: WebhookEvent) {
        webhooks.values
            .filter { it.events.contains(event.type) }
            .forEach { webhook ->
                try {
                    val payload = WebhookPayload(
                        event = event.type,
                        data = event.data,
                        timestamp = System.currentTimeMillis()
                    )
                    sendWebhookRequest(webhook, payload)
                } catch (e: Exception) {
                    println("Webhook ${webhook.id} failed: ${e.message}")
                }
            }
    }
    
    private fun sendWebhookRequest(webhook: Webhook, payload: WebhookPayload) {
        println("📤 Webhook ${webhook.id} -> ${webhook.url}")
        println("   Payload: ${payload.event}")
        
        // Would use HTTP client to POST JSON
        // val client = HttpClient()
        // client.post(webhook.url, gson.toJson(payload))
    }
    
    fun getWebhooks(): List<Webhook> = webhooks.values.toList()
}

data class Webhook(
    val id: String,
    val name: String,
    val url: String,
    val events: List<WebhookEventType>,
    val headers: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

enum class WebhookEventType {
    LOG_ERROR,
    LOG_WARNING,
    PATTERN_MATCH,
    ALERT_TRIGGERED,
    SOURCE_CONNECTED,
    SOURCE_DISCONNECTED,
    BATCH_PROCESSED
}

data class WebhookEvent(
    val type: WebhookEventType,
    val data: Map<String, Any>
)

data class WebhookPayload(
    val event: WebhookEventType,
    val data: Map<String, Any>,
    val timestamp: Long
)

class RealTimeProcessor {
    
    private val streamManager = LogStreamManager()
    private val webhookManager = WebhookManager()
    private val processors = mutableListOf<LogProcessor>()
    
    fun addProcessor(processor: LogProcessor) {
        processors.add(processor)
    }
    
    fun processLogEntry(entry: LogEntry, source: String) {
        streamManager.publish(entry, source)
        
        processors.forEach { processor ->
            processor.process(entry, source)
        }
        
        if (entry.level.name == "ERROR") {
            webhookManager.triggerAll(
                WebhookEvent(
                    type = WebhookEventType.LOG_ERROR,
                    data = mapOf(
                        "message" to entry.message,
                        "source" to source,
                        "level" to entry.level.name
                    )
                )
            )
        }
    }
    
    fun subscribeToStream(
        subscriberId: String,
        filter: StreamFilter? = null
    ): Observable<StreamEvent> {
        return streamManager.subscribe(subscriberId, filter)
    }
}

interface LogProcessor {
    fun process(entry: LogEntry, source: String)
}

class ErrorCountProcessor : LogProcessor {
    private var errorCount = 0
    
    override fun process(entry: LogEntry, source: String) {
        if (entry.level.name == "ERROR") {
            errorCount++
            if (errorCount % 10 == 0) {
                println("⚠️  Error count reached: $errorCount")
            }
        }
    }
}

class MetricsProcessor : LogProcessor {
    private val metrics = mutableMapOf<String, Int>()
    
    override fun process(entry: LogEntry, source: String) {
        val key = "${source}:${entry.level.name}"
        metrics[key] = metrics.getOrDefault(key, 0) + 1
    }
    
    fun getMetrics(): Map<String, Int> = metrics.toMap()
}
