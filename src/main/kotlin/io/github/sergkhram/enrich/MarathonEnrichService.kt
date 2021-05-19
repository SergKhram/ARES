package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.interactor.StopAdbInteractor
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import io.github.sergkhram.*
import io.github.sergkhram.helpers.*
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File

class MarathonEnrichService(
    private val marathonAllureResDirectory: File,
    private val mapper: ObjectMapper,
    private val projectDirectory: String,
    private val allureDeviceResDirectory: File
) : EnrichService {
    val devicesInfo = mutableMapOf<String, String?>()

    override fun iterableEnrich() {
        logger.info("Getting results from Marathon")
        lateinit var listOfAllureDeviceJsonFiles: List<File>
        try {
            logger.info("Loading allure json files")
            listOfAllureDeviceJsonFiles = allureDeviceResDirectory.listFiles()!!.filter { isResultJsonFile(it) }
        } catch (e: Exception) {
            throw CustomException("There is no $projectDirectory/build/reports/marathon/${Configuration.testDirectory}allure-device-results directory. Check your buildType")
        }
        listOfAllureDeviceJsonFiles?.let {
            logger.debug("Count of allure device Json files ${it.size}")
            if (it.size > 200) {
                runBlocking(newFixedThreadPoolContext(it.size, "allure-results-enricher-pool")) {
                    logger.debug("Parallel files transferring")
                    it.pforEach(this.coroutineContext) { deviceAllureFile ->
                        enrichByMarathonResultFile(deviceAllureFile)
                    }
                }
            } else {
                it.forEach { deviceAllureFile->
                    enrichByMarathonResultFile(deviceAllureFile)
                }
            }
        }
        copyOtherFiles()
    }

    private fun copyOtherFiles() {
        copyVideos(projectDirectory)
        logger.info("Transferring other files")
        copyFiles(
            allureDeviceResDirectory,
            projectDirectory,
            isNotJsonFile
        )
        copyFiles(
            marathonAllureResDirectory,
            projectDirectory,
            isEnvironmentFile
        )
        copyFiles(
            marathonAllureResDirectory,
            projectDirectory,
            isJsonNotTheResultFile
        )
    }

    private fun enrichByMarathonResultFile(
        deviceAllureFile: File
    ) {
        logger.info("Enriching allure file: ${deviceAllureFile.name}")
        var currentDeviceFile = deviceAllureFile.asJson(mapper)
        marathonAllureResDirectory.listFiles()?.first {
            isResultJsonFile(it) &&
            isAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
        }?.let { marathonAllureFile ->
            val currentMarathonFile = marathonAllureFile.asJson(mapper)

            val videoAttachments = currentMarathonFile.getVideoAttachments()

            if (videoAttachments.isNotEmpty()) {
                (currentDeviceFile["attachments"] as ArrayNode).add(
                    prepareVideoAttachments(mapper, videoAttachments)
                )
            }

            val realHost = currentMarathonFile.getHostLabel()

            (currentDeviceFile.getHostLabel() as ObjectNode).replace(
                "value",
                realHost["value"]
            )
            (currentDeviceFile["labels"] as ArrayNode).add(
                mapper.createRealHostTag(realHost)
            )

            prepareModelBySerial(realHost["value"].asText())

            devicesInfo[realHost["value"].asText()]?.let {
                (currentDeviceFile["labels"] as ArrayNode).add(
                        mapper.createModelTag(it)
                )
            }


            File("$projectDirectory/build/allure-results/${deviceAllureFile.name}").apply {
                this.setWritable(true)
                this.writeText(
                    mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(currentDeviceFile)
                )
            }
        }
    }

    fun prepareModelBySerial(serial: String) {
        if(!devicesInfo.containsKey(serial)) {
            var model: String? = null

            runBlocking {
                try {
                    androidHome?.let {
                        logger.info("Android SDK directory is '$it'")
                    }
                    if(StartAdbInteractor().execute(androidHome = androidHome)) {
                        logger.debug("Starting adb client factory")
                        val adb = AndroidDebugBridgeClientFactory().build()
                        val output = adb.execute(ShellCommandRequest("getprop ro.product.model"), serial).output
                        model = if(!output.isNullOrBlank()) output else null
                    }
                } catch (e: Exception) {
                    logger.debug(e.message)
                }
                try {
                    StopAdbInteractor().execute()
                } catch (e: Exception) {
                    logger.debug(e.message)
                }
            }
            devicesInfo.put(serial, model)
        }
    }
}