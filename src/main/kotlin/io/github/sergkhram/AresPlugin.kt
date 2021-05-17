package io.github.sergkhram

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sergkhram.enrich.CleanAllureEnrichService
import io.github.sergkhram.enrich.MarathonEnrichService
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import io.github.sergkhram.logger as customLogger

class AresPlugin : Plugin<Project> {

    private val allureDeviceResDirectory: (String) -> File = {
        File("$it/build/reports/marathon/${Configuration.testDirectory}allure-device-results")
    }
    private val marathonAllureResDirectory: (String) -> File = {
        File("$it/build/reports/marathon/${Configuration.testDirectory}allure-results")
    }

    override fun apply(project: Project) {
        project.task("reportSync") {
            doLast {
                val projectDirectory = project.projectDir.path
                customLogger.info("Creating directory for allure-results if not exists")
                createAllureResultsDirectory(projectDirectory) // create directory for allure-results if not exists

                val mapper = ObjectMapper()
                val service = when(Configuration.enrichBy) {
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
    }
}