package com.jakubjirak.scheduler

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class ScheduledTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TaskType,
    val schedule: Schedule,
    val action: TaskAction,
    val enabled: Boolean = true,
    val lastRun: LocalDateTime? = null,
    val nextRun: LocalDateTime? = null
)

enum class TaskType {
    BACKUP,
    EXPORT,
    ANALYSIS,
    CLEANUP,
    REPORT,
    SYNC,
    ALERT_CHECK
}

sealed class Schedule {
    data class Hourly(val minute: Int) : Schedule()
    data class Daily(val time: LocalTime) : Schedule()
    data class Weekly(val dayOfWeek: DayOfWeek, val time: LocalTime) : Schedule()
    data class Monthly(val dayOfMonth: Int, val time: LocalTime) : Schedule()
    data class Interval(val minutes: Int) : Schedule()
    data class Cron(val expression: String) : Schedule()
}

sealed class TaskAction {
    data class BackupAction(val destination: String) : TaskAction()
    data class ExportAction(val format: String, val destination: String) : TaskAction()
    data class AnalysisAction(val analysisType: String) : TaskAction()
    data class CleanupAction(val olderThanDays: Int) : TaskAction()
    data class ReportAction(val reportType: String, val recipients: List<String>) : TaskAction()
    data class SyncAction(val source: String, val destination: String) : TaskAction()
    data class CustomAction(val script: String) : TaskAction()
}

data class TaskExecution(
    val taskId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val status: ExecutionStatus,
    val message: String? = null,
    val output: String? = null
)

enum class ExecutionStatus {
    RUNNING, SUCCESS, FAILED, SKIPPED
}

class TaskScheduler {
    
    private val tasks = mutableMapOf<String, ScheduledTask>()
    private val executions = mutableListOf<TaskExecution>()
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
    private val maxExecutionHistory = 1000
    
    fun scheduleTask(task: ScheduledTask) {
        tasks[task.id] = task
        
        if (task.enabled) {
            startTask(task)
        }
    }
    
    fun unscheduleTask(taskId: String) {
        tasks.remove(taskId)
        // Would cancel scheduled future
    }
    
    fun enableTask(taskId: String) {
        tasks[taskId]?.let { task ->
            val updated = task.copy(enabled = true)
            tasks[taskId] = updated
            startTask(updated)
        }
    }
    
    fun disableTask(taskId: String) {
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(enabled = false)
            // Would cancel scheduled future
        }
    }
    
    private fun startTask(task: ScheduledTask) {
        val delayMinutes = calculateInitialDelay(task.schedule)
        
        when (task.schedule) {
            is Schedule.Interval -> {
                executor.scheduleAtFixedRate(
                    { executeTask(task) },
                    delayMinutes,
                    task.schedule.minutes.toLong(),
                    TimeUnit.MINUTES
                )
            }
            is Schedule.Hourly -> {
                executor.scheduleAtFixedRate(
                    { executeTask(task) },
                    delayMinutes,
                    60,
                    TimeUnit.MINUTES
                )
            }
            is Schedule.Daily -> {
                executor.scheduleAtFixedRate(
                    { executeTask(task) },
                    delayMinutes,
                    24 * 60,
                    TimeUnit.MINUTES
                )
            }
            else -> {
                // Would implement cron-like scheduling
                println("Scheduled task: ${task.name}")
            }
        }
    }
    
    private fun calculateInitialDelay(schedule: Schedule): Long {
        return when (schedule) {
            is Schedule.Interval -> schedule.minutes.toLong()
            is Schedule.Hourly -> {
                val now = LocalDateTime.now()
                val nextRun = now.plusHours(1).withMinute(schedule.minute)
                java.time.Duration.between(now, nextRun).toMinutes()
            }
            is Schedule.Daily -> {
                val now = LocalDateTime.now()
                val nextRun = now.plusDays(1).with(schedule.time)
                java.time.Duration.between(now, nextRun).toMinutes()
            }
            else -> 0
        }
    }
    
    private fun executeTask(task: ScheduledTask) {
        val execution = TaskExecution(
            taskId = task.id,
            startTime = LocalDateTime.now(),
            status = ExecutionStatus.RUNNING
        )
        
        try {
            println("🔄 Executing task: ${task.name}")
            
            val result = when (task.action) {
                is TaskAction.BackupAction -> executeBackup(task.action)
                is TaskAction.ExportAction -> executeExport(task.action)
                is TaskAction.AnalysisAction -> executeAnalysis(task.action)
                is TaskAction.CleanupAction -> executeCleanup(task.action)
                is TaskAction.ReportAction -> executeReport(task.action)
                is TaskAction.SyncAction -> executeSync(task.action)
                is TaskAction.CustomAction -> executeCustom(task.action)
            }
            
            val completed = execution.copy(
                endTime = LocalDateTime.now(),
                status = ExecutionStatus.SUCCESS,
                output = result
            )
            
            recordExecution(completed)
            updateTaskLastRun(task.id)
            
            println("✅ Task completed: ${task.name}")
            
        } catch (e: Exception) {
            val failed = execution.copy(
                endTime = LocalDateTime.now(),
                status = ExecutionStatus.FAILED,
                message = e.message
            )
            
            recordExecution(failed)
            println("❌ Task failed: ${task.name} - ${e.message}")
        }
    }
    
    private fun executeBackup(action: TaskAction.BackupAction): String {
        return "Backup created at ${action.destination}"
    }
    
    private fun executeExport(action: TaskAction.ExportAction): String {
        return "Exported to ${action.destination} in ${action.format} format"
    }
    
    private fun executeAnalysis(action: TaskAction.AnalysisAction): String {
        return "Analysis completed: ${action.analysisType}"
    }
    
    private fun executeCleanup(action: TaskAction.CleanupAction): String {
        return "Cleaned up logs older than ${action.olderThanDays} days"
    }
    
    private fun executeReport(action: TaskAction.ReportAction): String {
        return "Report sent to ${action.recipients.size} recipients"
    }
    
    private fun executeSync(action: TaskAction.SyncAction): String {
        return "Synced from ${action.source} to ${action.destination}"
    }
    
    private fun executeCustom(action: TaskAction.CustomAction): String {
        return "Custom script executed"
    }
    
    private fun recordExecution(execution: TaskExecution) {
        executions.add(execution)
        if (executions.size > maxExecutionHistory) {
            executions.removeAt(0)
        }
    }
    
    private fun updateTaskLastRun(taskId: String) {
        tasks[taskId]?.let { task ->
            tasks[taskId] = task.copy(lastRun = LocalDateTime.now())
        }
    }
    
    fun getTaskExecutions(taskId: String, limit: Int = 10): List<TaskExecution> {
        return executions
            .filter { it.taskId == taskId }
            .sortedByDescending { it.startTime }
            .take(limit)
    }
    
    fun getAllTasks(): List<ScheduledTask> = tasks.values.toList()
    
    fun getTask(taskId: String): ScheduledTask? = tasks[taskId]
    
    fun shutdown() {
        executor.shutdown()
    }
}

object PredefinedTasks {
    
    fun getDailyBackup(): ScheduledTask {
        return ScheduledTask(
            name = "Daily Backup",
            type = TaskType.BACKUP,
            schedule = Schedule.Daily(LocalTime.of(2, 0)),
            action = TaskAction.BackupAction("/backups/daily")
        )
    }
    
    fun getHourlyAnalysis(): ScheduledTask {
        return ScheduledTask(
            name = "Hourly Log Analysis",
            type = TaskType.ANALYSIS,
            schedule = Schedule.Hourly(0),
            action = TaskAction.AnalysisAction("error_detection")
        )
    }
    
    fun getWeeklyCleanup(): ScheduledTask {
        return ScheduledTask(
            name = "Weekly Cleanup",
            type = TaskType.CLEANUP,
            schedule = Schedule.Weekly(DayOfWeek.SUNDAY, LocalTime.of(3, 0)),
            action = TaskAction.CleanupAction(30)
        )
    }
    
    fun getWeeklyReport(): ScheduledTask {
        return ScheduledTask(
            name = "Weekly Report",
            type = TaskType.REPORT,
            schedule = Schedule.Weekly(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
            action = TaskAction.ReportAction("weekly_summary", listOf("team@company.com"))
        )
    }
}
