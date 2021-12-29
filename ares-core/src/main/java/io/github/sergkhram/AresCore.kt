package io.github.sergkhram

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.EnrichVariant
import io.github.sergkhram.enrich.CleanAllureEnrichService
import io.github.sergkhram.enrich.MarathonEnrichService
import java.io.File
import io.github.sergkhram.logger as customLogger

class AresCore(val outputDir: File, val log: File, val projectPath: String) {
    private val allureDeviceResDirectory: (String) -> File = {
        val marathonZeroSixOnePath = "${Configuration.getReportDirectory(it)}allure-device-results"
        val marathonZeroSixTwoPath = "${Configuration.getReportDirectory(it)}device-files${Configuration.separator}allure-results"
        if(File(marathonZeroSixOnePath).exists()) File(marathonZeroSixOnePath) else File(marathonZeroSixTwoPath)
    }
    private val marathonAllureResDirectory: (String) -> File = {
        File("${Configuration.getReportDirectory(it)}allure-results")
    }

    fun execute() {
        recursivelyDelete(outputDir)
        Configuration.logFile = log
        customLogger.info("Applying ares plugin")
        val projectDirectory = projectPath
        customLogger.info("Creating directory for allure-results if doesn't exist")
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