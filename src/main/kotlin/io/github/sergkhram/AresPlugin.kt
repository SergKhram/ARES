package io.github.sergkhram

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sergkhram.helpers.isEnvironmentFile
import io.github.sergkhram.helpers.isJsonFile
import io.github.sergkhram.helpers.isNotJsonFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class AresPlugin : Plugin<Project> {

    private val allureDeviceResDirectory: (String) -> File = {
        File("$it/build/reports/marathon/${buildType}AndroidTest/allure-device-results")
    }
    private val marathonAllureResDirectory: (String) -> File = {
        File("$it/build/reports/marathon/${buildType}AndroidTest/allure-results")
    }

    override fun apply(project: Project) {
        project.task("reportSync") {
            doLast {
                val projectDirectory = project.projectDir.path
                createAllureResultsDirectory(projectDirectory) // create directory for allure-results if not exists

                val listOfAllureDeviceJsonFiles = allureDeviceResDirectory(projectDirectory).listFiles()?.filter { isJsonFile(it) }

                EnrichService(
                    marathonAllureResDirectory(projectDirectory),
                    ObjectMapper(),
                    projectDirectory
                ).iterableEnrich(listOfAllureDeviceJsonFiles)

                copyVideos(projectDirectory) // copy videos to allure-results directory
                copyFiles(
                        allureDeviceResDirectory(projectDirectory),
                        projectDirectory,
                        isNotJsonFile
                )
                copyFiles(
                        marathonAllureResDirectory(projectDirectory),
                        projectDirectory,
                        isEnvironmentFile
                )
            }
        }
    }
}