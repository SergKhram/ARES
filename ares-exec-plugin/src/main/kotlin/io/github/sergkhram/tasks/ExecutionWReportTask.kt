package io.github.sergkhram.tasks

import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.tasks.ConfigurationExtension
import io.github.sergkhram.configuration.ExecuteBy
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
            val executeBy = getPropertyExecuteBy(conf)
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