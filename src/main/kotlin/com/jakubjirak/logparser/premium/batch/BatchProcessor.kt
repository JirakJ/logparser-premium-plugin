package com.jakubjirak.logparser.premium.batch

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jakubjirak.logparser.licensing.LicenseManager
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class BatchProcessor(private val project: Project) {
    
    private val licenseManager = LicenseManager.getInstance()
    
    data class BatchJob(
        val id: String,
        val name: String,
        val files: List<Path>,
        val operation: BatchOperation,
        val status: JobStatus
    )
    
    enum class BatchOperation { PARSE, INDEX, EXPORT, ANALYZE }
    enum class JobStatus { PENDING, RUNNING, COMPLETED, FAILED }
    
    fun createBatchJob(name: String, files: List<Path>, operation: BatchOperation): Result<BatchJob> {
        if (!licenseManager.isPremium()) {
            return Result.failure(Exception("Premium feature required"))
        }
        
        return Result.success(BatchJob(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            files = files,
            operation = operation,
            status = JobStatus.PENDING
        ))
    }
    
    companion object {
        fun getInstance(project: Project): BatchProcessor = project.service()
    }
}
