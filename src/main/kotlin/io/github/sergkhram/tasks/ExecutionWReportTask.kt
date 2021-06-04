package io.github.sergkhram.tasks

import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.configuration.ExecuteBy
import io.github.sergkhram.helpers.CustomException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

open class ExecutionWReportTask: DefaultTask(), VerificationTask {
    init { outputs.upToDateWhen { false } }

    @TaskAction
    fun runTestsWReportSync() {
        val reportSyncTask = project.tasks.getByName("reportSync")
        (reportSyncTask as ReportSyncTask).reportSync()
        if(!Configuration.executionIgnoreFailures) {
            val conf = project.extensions.getByName("ares") as? ConfigurationExtension
                ?: ConfigurationExtension(project)
            val executeBy = try {
                System.getProperty("executeBy")?.let { ExecuteBy.valueOf(it) }
                    ?: conf.testExecutionBlock?.executeBy?.let { ExecuteBy.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                throw CustomException("There is no chosen executeBy variant, only these values are supported : ${ExecuteBy.values().map {it.name}}")
            }
            executeBy?.let {
                when(it) {
                    ExecuteBy.MARATHON -> {
                        marathonAfterExecutionAction(project)
                    }
                }
            }
        }
    }

    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        Configuration.executionIgnoreFailures = ignoreFailures
    }
    override fun getIgnoreFailures(): Boolean  {
        return Configuration.executionIgnoreFailures
    }
}