package com.jakubjirak.logparser.premium

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service(Service.Level.PROJECT)
class LogVisualizationEngine(private val project: Project) {
    
    private val _visualizations = MutableStateFlow<List<Visualization>>(emptyList())
    val visualizations: StateFlow<List<Visualization>> = _visualizations
    
    sealed class Visualization {
        data class Timeline(
            val events: List<TimelineEvent>,
            val timeRange: Pair<Instant, Instant>
        ) : Visualization()
        
        data class Heatmap(
            val data: Map<String, Map<Instant, Int>>,
            val intensity: Map<String, Double>
        ) : Visualization()
        
        data class FlowDiagram(
            val nodes: List<FlowNode>,
            val edges: List<FlowEdge>
        ) : Visualization()
        
        data class MetricsGraph(
            val series: List<MetricSeries>,
            val timeRange: Pair<Instant, Instant>
        ) : Visualization()
    }
    
    data class TimelineEvent(
        val timestamp: Instant,
        val label: String,
        val severity: String,
        val duration: Long? = null,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class FlowNode(
        val id: String,
        val label: String,
        val type: String,
        val metrics: Map<String, Double>
    )
    
    data class FlowEdge(
        val from: String,
        val to: String,
        val label: String,
        val weight: Double
    )
    
    data class MetricSeries(
        val name: String,
        val dataPoints: List<DataPoint>
    )
    
    data class DataPoint(
        val timestamp: Instant,
        val value: Double
    )
    
    fun createTimeline(events: List<LogPlaybackEngine.LogEvent>): Visualization.Timeline {
        val timelineEvents = events.map { event ->
            TimelineEvent(
                timestamp = event.timestamp,
                label = event.message.take(100),
                severity = event.level,
                metadata = event.metadata
            )
        }
        
        val timeRange = events.minOf { it.timestamp } to events.maxOf { it.timestamp }
        return Visualization.Timeline(timelineEvents, timeRange)
    }
    
    fun createHeatmap(events: List<LogPlaybackEngine.LogEvent>, granularity: ChronoUnit = ChronoUnit.MINUTES): Visualization.Heatmap {
        val data = mutableMapOf<String, MutableMap<Instant, Int>>()
        val intensity = mutableMapOf<String, Double>()
        
        events.groupBy { it.level }.forEach { (level, levelEvents) ->
            val timeMap = mutableMapOf<Instant, Int>()
            
            levelEvents.forEach { event ->
                val bucket = event.timestamp.truncatedTo(granularity)
                timeMap[bucket] = timeMap.getOrDefault(bucket, 0) + 1
            }
            
            data[level] = timeMap
            intensity[level] = levelEvents.size.toDouble() / events.size
        }
        
        return Visualization.Heatmap(data, intensity)
    }
    
    fun createFlowDiagram(events: List<LogPlaybackEngine.LogEvent>): Visualization.FlowDiagram {
        val nodes = mutableMapOf<String, FlowNode>()
        val edges = mutableListOf<FlowEdge>()
        
        events.forEach { event ->
            val source = event.source
            if (!nodes.containsKey(source)) {
                nodes[source] = FlowNode(
                    id = source,
                    label = source,
                    type = "service",
                    metrics = emptyMap()
                )
            }
            
            event.metadata["destination"]?.let { dest ->
                val destStr = dest.toString()
                if (!nodes.containsKey(destStr)) {
                    nodes[destStr] = FlowNode(
                        id = destStr,
                        label = destStr,
                        type = "service",
                        metrics = emptyMap()
                    )
                }
                
                edges.add(FlowEdge(
                    from = source,
                    to = destStr,
                    label = event.metadata["operation"]?.toString() ?: "",
                    weight = 1.0
                ))
            }
        }
        
        return Visualization.FlowDiagram(nodes.values.toList(), edges)
    }
    
    fun createMetricsGraph(events: List<LogPlaybackEngine.LogEvent>): Visualization.MetricsGraph {
        val series = mutableListOf<MetricSeries>()
        val timeRange = events.minOf { it.timestamp } to events.maxOf { it.timestamp }
        
        events.groupBy { it.level }.forEach { (level, levelEvents) ->
            val dataPoints = levelEvents.map { event ->
                DataPoint(
                    timestamp = event.timestamp,
                    value = 1.0
                )
            }
            series.add(MetricSeries(level, dataPoints))
        }
        
        return Visualization.MetricsGraph(series, timeRange)
    }
    
    fun processRealtimeEvent(event: LogPlaybackEngine.LogEvent) {
        // Update visualizations with new event
    }
}
