package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.sergkhram.*
import io.github.sergkhram.configuration.Configuration
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
    val devicesInfo = mutableMapOf<String, DeviceInfo?>()

    override fun iterableEnrich() {
        logger.info("Getting results from Marathon")
        lateinit var listOfAllureDeviceJsonFiles: List<File>
        try {
            logger.info("Loading allure json files")
            listOfAllureDeviceJsonFiles = allureDeviceResDirectory.listFiles()!!.filter { isResultJsonFile(it) }
        } catch (e: Exception) {
            throw CustomException("There is no ${Configuration.getReportDirectory(projectDirectory)}allure-device-results directory. Check your buildType")
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
            isNotJsonFile,
            "is not json"
        )
        copyFiles(
            marathonAllureResDirectory,
            projectDirectory,
            isEnvironmentFile,
            "is environment"
        )
        copyFiles(
            marathonAllureResDirectory,
            projectDirectory,
            isJsonNotTheResultFile,
        "is json not the result"
        )
    }

    private fun enrichByMarathonResultFile(
        deviceAllureFile: File
    ) {
        logger.info("Enriching allure file: ${deviceAllureFile.name}")
        var currentDeviceFile = deviceAllureFile.asJson(mapper)
        val marathonAllureFile = marathonAllureResDirectory.listFiles()?.firstOrNull {
            isResultJsonFile(it) &&
            isAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
        }
        if(marathonAllureFile != null) {
            val currentMarathonFile = marathonAllureFile.asJson(mapper)

            val videoAttachments = currentMarathonFile.getVideoAttachments()

            if (videoAttachments.isNotEmpty()) {
                if(currentDeviceFile["attachments"] == null) {
                    (currentDeviceFile as ObjectNode).put("attachments", mapper.createArrayNode())
                }
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

            prepareDeviceInfoBySerial(realHost["value"].asText())

            devicesInfo[realHost["value"].asText()]?.let { deviceInfo ->
                deviceInfo.model?.let {
                    (currentDeviceFile["labels"] as ArrayNode).add(mapper.createModelTag(it))
                }
                deviceInfo.osVersion?.let {
                    (currentDeviceFile["labels"] as ArrayNode).add(mapper.createOsVersionTag(it))
                }
            }
            File("$projectDirectory/build/allure-results/${deviceAllureFile.name}").apply {
                this.setWritable(true)
                this.writeText(
                    mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(currentDeviceFile)
                )
            }
        } else {
            logger.info("Didn't find marathon file is appropriate to current allure-device-file ${deviceAllureFile.name}")
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

    fun prepareDeviceInfoBySerial(serial: String) {
        if(!devicesInfo.containsKey(serial)) {
            var deviceInfo: DeviceInfo? = null

            runBlocking {
                var adb: AdbManager? = null
                try {
                    androidHome?.let {
                        logger.info("Android SDK directory is '$it'")
                    }
                    adb = AdbManager(androidHome)
                    if(adb.startAdb()) {
                        logger.debug("Starting adb client factory")
                        adb.initAdbClient()
                        val model = adb.getModel(serial)
                        val osVersion = adb.getOsVersion(serial)
                        deviceInfo = if(!model.isNullOrBlank() && !osVersion.isNullOrBlank()) DeviceInfo(model, osVersion) else null
                    }
                } catch (e: Exception) {
                    logger.debug(e.message ?: e.localizedMessage)
                }
                try {
                    adb?.stopAdb()
                } catch (e: Exception) {
                    logger.debug(e.message ?: e.localizedMessage)
                }
            }
            devicesInfo.put(serial, deviceInfo)
        }
    }
}