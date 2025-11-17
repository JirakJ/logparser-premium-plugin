package com.jakubjirak.logparser.premium

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class LogPlaybackEngine(private val project: Project) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, PlaybackSession>()
    
    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    val state: StateFlow<PlaybackState> = _state
    
    data class PlaybackSession(
        val id: String,
        val events: List<LogEvent>,
        var currentIndex: Int = 0,
        var speed: Double = 1.0,
        var isPaused: Boolean = false
    )
    
    data class LogEvent(
        val timestamp: Instant,
        val message: String,
        val level: String,
        val source: String,
        val metadata: Map<String, Any>
    )
    
    sealed class PlaybackState {
        object Stopped : PlaybackState()
        data class Playing(val sessionId: String, val progress: Double) : PlaybackState()
        data class Paused(val sessionId: String, val progress: Double) : PlaybackState()
    }
    
    suspend fun startPlayback(sessionId: String, events: List<LogEvent>, speed: Double = 1.0) {
        val session = PlaybackSession(sessionId, events, speed = speed)
        sessions[sessionId] = session
        
        scope.launch {
            _state.value = PlaybackState.Playing(sessionId, 0.0)
            
            while (session.currentIndex < events.size && !session.isPaused) {
                val event = events[session.currentIndex]
                val nextEvent = events.getOrNull(session.currentIndex + 1)
                
                if (nextEvent != null) {
                    val delayMs = (nextEvent.timestamp.toEpochMilli() - event.timestamp.toEpochMilli()) / speed
                    delay(delayMs.toLong().coerceAtLeast(1))
                }
                
                session.currentIndex++
                val progress = session.currentIndex.toDouble() / events.size
                _state.value = PlaybackState.Playing(sessionId, progress)
            }
            
            if (session.currentIndex >= events.size) {
                _state.value = PlaybackState.Stopped
                sessions.remove(sessionId)
            }
        }
    }
    
    fun pause(sessionId: String) {
        sessions[sessionId]?.let { session ->
            session.isPaused = true
            val progress = session.currentIndex.toDouble() / session.events.size
            _state.value = PlaybackState.Paused(sessionId, progress)
        }
    }
    
    fun resume(sessionId: String) {
        sessions[sessionId]?.let { session ->
            session.isPaused = false
            scope.launch {
                startPlayback(sessionId, session.events.drop(session.currentIndex), session.speed)
            }
        }
    }
    
    fun stop(sessionId: String) {
        sessions.remove(sessionId)
        _state.value = PlaybackState.Stopped
    }
    
    fun setSpeed(sessionId: String, speed: Double) {
        sessions[sessionId]?.speed = speed
    }
    
    fun seek(sessionId: String, progress: Double) {
        sessions[sessionId]?.let { session ->
            val newIndex = (session.events.size * progress).toInt().coerceIn(0, session.events.size - 1)
            session.currentIndex = newIndex
        }
    }
    
    fun dispose() {
        scope.cancel()
        sessions.clear()
    }
}
