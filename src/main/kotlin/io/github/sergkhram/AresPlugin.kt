package io.github.sergkhram

import com.malinskiy.marathon.MarathonPlugin
import com.malinskiy.marathon.MarathonRunTask
import com.malinskiy.marathon.android.configuration.AllureConfiguration
import com.malinskiy.marathon.device.DeviceFeature
import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.configuration.ExecuteBy
import io.github.sergkhram.configuration.MarathonBlock
import io.github.sergkhram.helpers.CustomException
import io.github.sergkhram.tasks.ExecutionWReportGenTask
import io.github.sergkhram.tasks.ExecutionWReportTask
import io.github.sergkhram.tasks.ReportGenTask
import io.github.sergkhram.tasks.ReportSyncTask
import io.qameta.allure.gradle.AllurePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.*
import io.github.sergkhram.logger as customLogger

class AresPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: ConfigurationExtension = project.extensions.create("ares", ConfigurationExtension::class.java, project)
        project.plugins.apply(MarathonPlugin::class)
        project.plugins.apply(AllurePlugin::class)
        project.afterEvaluate {
            project.tasks.register<ReportSyncTask>("reportSync")
            project.tasks.register<ReportGenTask>("reportGen")
            val executeBy = try {
                System.getProperty("executeBy")?.let { ExecuteBy.valueOf(it) }
                    ?: extension.testExecutionBlock?.executeBy?.let { ExecuteBy.valueOf(it) }
            } catch (e: IllegalArgumentException) {
                throw CustomException("There is no chosen executeBy variant, only these values are supported : ${ExecuteBy.values().map {it.name}}")
            }
            executeBy?.let {
                when(it) {
                    ExecuteBy.MARATHON -> {
                        val buildType = System.getProperty("buildType")?.toString() ?: extension.marathonBlock?.buildType ?: "debug"
                        val marathonTaskName = "marathon${buildType.capitalize()}AndroidTest"
                        val marathonTask = project.tasks.get(marathonTaskName) as MarathonRunTask
                        marathonTask.marathonExtension.get().apply {
                            if(this.allureConfiguration==null || !this.allureConfiguration!!.enabled) {
                                val allureResultsDir: String? = this.allureConfiguration?.relativeResultsDirectory
                                this.allureConfiguration = AllureConfiguration().apply {
                                    this.enabled = true
                                    allureResultsDir?.let { resDir -> this.relativeResultsDirectory = resDir }
                                }
                            }
                        }
                        val marathonVideoConfiguration = marathonTask.marathonExtension.get().screenRecordConfiguration?.preferableRecorderType?.name ?: DeviceFeature.VIDEO.name
                        marathonTask.marathonExtension.get().ignoreFailures = System.getProperty("marathonIgnoreFailures")?.toBoolean() ?: false //if you want to use runTests ares tasks - needed to set -DmarathonIgnoreFailures=true when execute the command
                        if(extension.marathonBlock == null) {
                            extension.marathonBlock = MarathonBlock(screenRecordType = marathonVideoConfiguration)
                        } else {
                            extension.marathonBlock!!.screenRecordType = marathonVideoConfiguration
                        }
                        val runTestsWReportSyncTask = project.tasks.create<ExecutionWReportTask>("runTestsWReportSync").dependsOn(marathonTaskName)
                        runTestsWReportSyncTask.mustRunAfter(marathonTaskName)
                        val runTestsWReportGenTask = project.tasks.create<ExecutionWReportGenTask>("runTestsWReportGen").dependsOn(marathonTaskName)
                        runTestsWReportGenTask.mustRunAfter(marathonTaskName)
                    }
                }
            } ?: customLogger.warn("You need to set executeBy variant from these values: ${ExecuteBy.values().map {it.name}} if you want to use ares runTests tasks")
        }
    }
}