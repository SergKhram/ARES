package io.github.sergkhram

import com.malinskiy.marathon.MarathonPlugin
import com.malinskiy.marathon.MarathonRunTask
import io.github.sergkhram.configuration.ExecuteBy
import io.github.sergkhram.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class AresExecPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(AresPlugin::class)
        project.plugins.apply(MarathonPlugin::class)
        project.afterEvaluate {
            val aresExtension: ConfigurationExtension = project.extensions.get("ares") as ConfigurationExtension
            val executeBy = getPropertyExecuteBy(aresExtension)
            executeBy?.let {
                when(it) {
                    ExecuteBy.MARATHON -> {
                        val buildType = System.getProperty("buildType")?.toString() ?: aresExtension.marathonBlock?.buildType ?: "debug"
                        val marathonTaskName = "marathon${buildType.capitalize()}AndroidTest"
                        val marathonTask = project.tasks.get(marathonTaskName) as MarathonRunTask
                        marathonTask.enableAllureForMarathon()
                        aresExtension.setScreenRecordTypeFromMarathon(marathonTask)
                        marathonTask.marathonExtension.get().ignoreFailures = System.getProperty("marathonIgnoreFailures")?.toBoolean()
                            ?: true //if you want to use clean marathon tasks - needed to set -DmarathonIgnoreFailures=false when execute the command
                        val runTestsWReportSyncTask = project.tasks.create<ExecutionWReportTask>("runTestsWReportSync").dependsOn(marathonTaskName)
                        runTestsWReportSyncTask.mustRunAfter(marathonTaskName)
                        val runTestsWReportGenTask = project.tasks.create<ExecutionWReportGenTask>("runTestsWReportGen").dependsOn(marathonTaskName)
                        runTestsWReportGenTask.mustRunAfter(marathonTaskName)
                    }
                }
            }
        }
    }
}