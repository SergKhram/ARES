package io.github.sergkhram.tasks

import io.qameta.allure.gradle.task.AllureReport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import io.github.sergkhram.logger as customLogger

open class ReportGenTask: DefaultTask() {
    init { outputs.upToDateWhen { false } }

    @TaskAction
    fun reportGen() {
        val reportSyncTask = project.tasks.getByName("reportSync")
        (reportSyncTask as ReportSyncTask).reportSync()
        customLogger.info("Applying Allure Gradle Plugin")
        val allureTask = project.tasks.getByName("allureReport")
        (allureTask as AllureReport).generateAllureReport()
    }
}