package io.github.sergkhram.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.configuration.EnrichVariant
import io.github.sergkhram.configuration.provideConfiguration
import io.github.sergkhram.createAllureResultsDirectory
import io.github.sergkhram.enrich.CleanAllureEnrichService
import io.github.sergkhram.enrich.MarathonEnrichService
import io.github.sergkhram.recursivelyDelete
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import io.github.sergkhram.logger as customLogger

open class ReportSyncTask: DefaultTask() {
    private val allureDeviceResDirectory: (String) -> File = {
        val marathonZeroSixOnePath = "${Configuration.getReportDirectory(it)}allure-device-results"
        val marathonZeroSixTwoPath = "${Configuration.getReportDirectory(it)}device-files/allure-results"
        if(File(marathonZeroSixOnePath).exists()) File(marathonZeroSixOnePath) else File(marathonZeroSixTwoPath)
    }
    private val marathonAllureResDirectory: (String) -> File = {
        File("${Configuration.getReportDirectory(it)}allure-results")
    }

    init { outputs.upToDateWhen { false } }

    @OutputDirectory
    val outputDir = File(project.buildDir, "ares-logs")

    @OutputFile
    val log = File(outputDir, "ares.log")

    @TaskAction
    fun reportSync() {
        recursivelyDelete(outputDir)
        Configuration.logFile = log
        customLogger.info("Applying ares plugin")
        val conf = project.extensions.getByName("ares") as? ConfigurationExtension
            ?: ConfigurationExtension(project)
        conf.provideConfiguration()
        val projectDirectory = project.projectDir.path
        customLogger.info("Creating directory for allure-results if not exists")
        createAllureResultsDirectory(projectDirectory) // create directory for allure-results if not exists

        val mapper = ObjectMapper()
        val service = when (Configuration.enrichBy) {
            EnrichVariant.MARATHON -> {
                customLogger.info("Selected method for getting results is from Marathon")
                MarathonEnrichService(
                    marathonAllureResDirectory(projectDirectory),
                    mapper,
                    projectDirectory,
                    allureDeviceResDirectory(projectDirectory)
                )
            }
            EnrichVariant.CLEAN_ALLURE -> {
                customLogger.info("Selected method for getting results is from device(clean allure)")
                CleanAllureEnrichService(
                    mapper,
                    projectDirectory
                )
            }
        }
        service.iterableEnrich()
    }
}