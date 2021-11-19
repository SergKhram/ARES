package io.github.sergkhram.tasks

import io.github.sergkhram.recursivelyDelete
import io.qameta.allure.gradle.download.tasks.DownloadAllure
import io.qameta.allure.gradle.report.tasks.AllureReport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import io.github.sergkhram.logger as customLogger

open class ReportGenTask: DefaultTask() {
    init { outputs.upToDateWhen { false } }

    @OutputDirectory
    val outputDir = File(project.buildDir, "reports/allure-report")

    @TaskAction
    fun reportGen() {
        val reportSyncTask = project.tasks.getByName("reportSync")
        (reportSyncTask as ReportSyncTask).reportSync()
        customLogger.info("Applying Allure Gradle Plugin")
        val allureDownloadTask = project.tasks.getByName("downloadAllure")
        (allureDownloadTask as DownloadAllure)?.let {
            if(it.destinationDir?.get().asFile.exists()) {
                it.downloadAllure()
                customLogger.info("Allure cmd downloaded")
            }
        }
        val allureTask = project.tasks.getByName("allureReport")
        recursivelyDelete(outputDir)
        (allureTask as AllureReport).generateAllureReport()
    }
}