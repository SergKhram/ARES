package io.github.sergkhram.tasks

import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.configuration.ExecuteBy
import io.github.sergkhram.getPropertyExecuteBy
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

open class ExecutionWReportGenTask: DefaultTask(), VerificationTask {
    init { outputs.upToDateWhen { false } }

    @TaskAction
    fun runTestsWReportGen() {
        val reportGenTask = project.tasks.getByName("reportGen")
        (reportGenTask as ReportGenTask).reportGen()
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