package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.sergkhram.*
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.helpers.*
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MarathonEnrichService(
    private val marathonAllureResDirectory: File,
    private val mapper: ObjectMapper,
    private val projectDirectory: String,
    private val allureDeviceResDirectory: File
) : EnrichService {
    val devicesInfo = ConcurrentHashMap<String, DeviceInfo>()

    override fun iterableEnrich() {
        logger.info("Getting results from Marathon")
        lateinit var listOfAllureDeviceJsonFiles: List<File>
        try {
            logger.info("Loading allure json files")
            listOfAllureDeviceJsonFiles = allureDeviceResDirectory.listFiles()!!.filter { isResultJsonFile(it) }
        } catch (e: Exception) {
            throw CustomException("There is no ${Configuration.getReportDirectory(projectDirectory)}allure-device-results or ${Configuration.getReportDirectory(projectDirectory)}device-results${Configuration.separator}allure-results directory. Check your buildType")
        }
        listOfAllureDeviceJsonFiles?.let {
            logger.debug("Count of allure device Json files ${it.size}")
            if (it.size > Configuration.startAsyncResultFilesTransferFrom) {
                runBlocking(newFixedThreadPoolContext(Configuration.asyncFilesTransferThreadsCount, "allure-results-enricher-pool")) {
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
        if(Configuration.copyCrashedTests) {
            copyCrashedTests(listOfAllureDeviceJsonFiles)
        }
        copyOtherFiles()
    }

    private fun copyOtherFiles() {
        copyVideos(projectDirectory)
        copyMarathonLogs(projectDirectory)
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

        var marathonAllureFile: File?
        val repeatedDeviceAllureFiles = allureDeviceResDirectory.listFiles()!!.filter {
            isResultJsonFile(it) &&
            it.asJson(mapper).getFullName() == currentDeviceFile.getFullName()
        }
        marathonAllureFile = if(repeatedDeviceAllureFiles.size == 1) {
            marathonAllureResDirectory.listFiles()?.firstOrNull {
                isResultJsonFile(it) &&
                simpleAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
            }
        } else {
            val sortedDeviceAllureFiles = repeatedDeviceAllureFiles.sortedBy { it.asJson(mapper).getStartTime() }.map { it.name }
            val indexOfRepeatedTestResult = sortedDeviceAllureFiles.indexOf(deviceAllureFile.name)
            marathonAllureResDirectory.listFiles()?.filter {
                isResultJsonFile(it) &&
                simpleAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
            }?.sortedBy { it.asJson(mapper).getStartTime() }?.getOrNull(indexOfRepeatedTestResult)
        }

//        var marathonAllureFile = marathonAllureResDirectory.listFiles()?.firstOrNull {
//            isResultJsonFile(it) &&
//            isAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
//        }
//        if(marathonAllureFile == null) {
//            val simpleMarathonFileFilterResults = marathonAllureResDirectory.listFiles()?.filter {
//                isResultJsonFile(it) &&
//                simpleAppropriateMarathonResultFile(it, mapper, currentDeviceFile)
//            }
//            simpleMarathonFileFilterResults?.let {
//                if(it.size == 1) {
//                    marathonAllureFile = simpleMarathonFileFilterResults.first()
//                }
//            }
//        }

        if(marathonAllureFile != null) {
            val currentMarathonFile = marathonAllureFile!!.asJson(mapper)

            val videoAttachments = currentMarathonFile.getVideoAttachments()
            val logAttachments = currentMarathonFile.getLogAttachments()

            if (videoAttachments.isNotEmpty() || logAttachments.isNotEmpty()) {
                if(currentDeviceFile["attachments"] == null) {
                    (currentDeviceFile as ObjectNode).put("attachments", mapper.createArrayNode())
                }
                if(videoAttachments.isNotEmpty()) {
                    (currentDeviceFile["attachments"] as ArrayNode).add(
                        prepareVideoAttachments(mapper, videoAttachments)
                    )
                }
                if(logAttachments.isNotEmpty()) {
                    (currentDeviceFile["attachments"] as ArrayNode).add(
                        prepareMarathonLogAttachments(mapper, logAttachments)
                    )
                }
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
            File("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${deviceAllureFile.name}").apply {
                this.setWritable(true)
                this.writeText(
                    mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(currentDeviceFile)
                )
            }
        } else {
            logger.info("Didn't find marathon file is appropriate to current allure-device-file ${deviceAllureFile.name}")
            File("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${deviceAllureFile.name}").apply {
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
            var deviceInfo = DeviceInfo()

            runBlocking {
                var adb: AdbManager? = null
                try {
                    Configuration.androidHome?.let {
                        logger.info("Android SDK directory is '$it'")
                    }
                    adb = AdbManager(Configuration.androidHome)
                    if(adb.startAdb()) {
                        logger.debug("Starting adb client factory")
                        adb.initAdbClient()
                        withTimeoutOrNull(5000L) {
                            var finalSerial: String
                            if(serial.contains("127.0.0.1")) {
                                finalSerial = serial.split(":").last()
                            } else {
                                finalSerial = serial
                            }
                            val model = adb.getModel(finalSerial)
                            val osVersion = adb.getOsVersion(finalSerial)
                            if(!model.isNullOrBlank() && !osVersion.isNullOrBlank()) deviceInfo = DeviceInfo(model, osVersion)
                        }
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
            devicesInfo[serial] = deviceInfo
        }
    }

    private fun copyCrashedTests(deviceAllureFiles: List<File>) {
        val marathonAllureFiles = marathonAllureResDirectory.listFiles()?.filter { marathonAllureFile ->
            isResultJsonFile(marathonAllureFile) &&
            deviceAllureFiles.map {
                deviceAllureFile -> deviceAllureFile.asJson(mapper).getFullName()
            }.filter {
                it.contains(marathonAllureFile.asJson(mapper).getPackageLabel() + "." + marathonAllureFile.asJson(mapper).getFullName())
            }.isNullOrEmpty()
        }
        marathonAllureFiles?.forEach { marathonAllureFile ->
            val currentMarathonFile = marathonAllureFile!!.asJson(mapper)
            val videoAttachments = currentMarathonFile.getVideoAttachments()
            val logAttachments = currentMarathonFile.getLogAttachments()

            if (videoAttachments.isNotEmpty() || logAttachments.isNotEmpty()) {
                if(videoAttachments.isNotEmpty()) {
                    (currentMarathonFile["attachments"] as ArrayNode).add(
                        prepareVideoAttachments(mapper, videoAttachments)
                    )
                }
                if(logAttachments.isNotEmpty()) {
                    (currentMarathonFile["attachments"] as ArrayNode).add(
                        prepareMarathonLogAttachments(mapper, logAttachments)
                    )
                }
            }
            File("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${marathonAllureFile.name}").apply {
                this.setWritable(true)
                this.writeText(
                    mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(currentMarathonFile)
                )
            }
        }
    }
}