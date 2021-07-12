package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import io.github.sergkhram.*
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.helpers.*
import io.github.sergkhram.helpers.isJsonNoTheResult
import io.github.sergkhram.helpers.isNotJson
import io.github.sergkhram.helpers.isResultJson
import io.github.sergkhram.helpers.pforEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.roundToInt
import java.util.concurrent.ConcurrentHashMap

class CleanAllureEnrichService(
    private val mapper: ObjectMapper,
    private val projectDirectory: String
) : EnrichService {
    val devicesInfo = ConcurrentHashMap<String, DeviceInfo>()

    override fun iterableEnrich() {
        logger.info("Getting results from device")
        runBlocking {
            Configuration.androidHome?.let {
                logger.info("Android SDK directory is '$it'")
            }
            val adb = AdbManager(Configuration.androidHome)
            if(adb.startAdb()) {
                logger.debug("Starting adb client factory")
                adb.initAdbClient()
                val devices: List<Device> = adb.getDeviceList() ?: listOf<Device>()
                logger.info("Found devices: ${devices.map {it.serial}}")
                val neededDeviceSerials = Configuration.deviceSerials?.split(",") ?: emptyList<String>()
                logger.debug("Devices from filter: $neededDeviceSerials")
                val filteredDevices = if(neededDeviceSerials.isNotEmpty()) devices.filter { neededDeviceSerials.contains(it.serial) } else devices
                logger.info("Filtered devices: ${filteredDevices.map { it.serial }}")
                if(filteredDevices.isNotEmpty()) {
                    logger.debug("Started getting info about devices")
                    filteredDevices.forEach {
                        var deviceInfo = DeviceInfo()
                        try {
                            withTimeoutOrNull(5000L) {
                                val model = adb.getModel(it.serial)
                                val osVersion = adb.getOsVersion(it.serial)
                                if(!model.isNullOrBlank() && !osVersion.isNullOrBlank()) deviceInfo = DeviceInfo(model, osVersion)
                            }
                        } catch (e: Exception) {
                            logger.debug(e.message ?: e.localizedMessage)
                        }
                        devicesInfo[it.serial] = deviceInfo
                    }
                    logger.debug("Started transferring files from devices")
                    filteredDevices.forEach { device ->
                        transferringFilesByDevice(this, adb, device)
                    }
                } else {
                    logger.info("Nothing to do because the filtered list of devices is empty")
                }
                logger.debug("Stopping adb")
                adb.stopAdb()
            } else {
                throw CustomException("Check your ANDROID_HOME")
            }
        }
    }

    private suspend fun List<FileEntryV1>.copyOtherFiles(scope: CoroutineScope,
                                                         adb: AdbManager,
                                                         device: Device) {
        val files = this
        logger.debug("Count of allure device other files ${files.size}")
        if(files.size > Configuration.startAsyncOtherFilesTransferFrom) {
            runBlocking(newFixedThreadPoolContext(Configuration.asyncFilesTransferThreadsCount, "allure-files-copier-pool")) {
                logger.debug("Parallel files transferring")
                files.pforEach(this.coroutineContext) {
                    pullFile(this, adb, device, it)
                }
            }
        } else {
            files.forEach {
                pullFile(scope, adb, device, it)
            }
        }
    }

    private suspend fun pullFile(scope: CoroutineScope,
                                 adb: AdbManager,
                                 device: Device,
                                 file: FileEntryV1,
                                 prefix: String = "") {
        logger.info("Pulling allure file: ${file.name}")
        val pullDevicesRequest = PullFileRequest(
            "${Configuration.remoteAllureFolder}/${file.name}",
            File("${projectDirectory}${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${prefix}${file.name}"),
            coroutineContext = scope.coroutineContext)
        val channel = adb.pullFiles(pullDevicesRequest, scope, device.serial)!!

        var percentage = 0
        for (percentageDouble in channel) {
            percentage = (percentageDouble * 100).roundToInt()
            progressPercentage(percentage, 100, file.name!!)
        }
    }

    private suspend fun pullResultFilesWithEnrich(scope: CoroutineScope, adb: AdbManager, device: Device, file: FileEntryV1) {
        pullFile(scope, adb, device, file, "temp-")

        var tempAllureResultFile = File("${projectDirectory}${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}temp-${file.name}").asJson(mapper)
        val realHost = mapper.createObjectNode()
            .put("name", "host")
            .put("value", device.serial)

        (tempAllureResultFile.getHostLabel() as ObjectNode).replace(
            "value",
            realHost["value"]
        )
        (tempAllureResultFile["labels"] as ArrayNode).add(
            mapper.createRealHostTag(realHost)
        )
        devicesInfo[device.serial]?.let { deviceInfo ->
            deviceInfo.model?.let {
                (tempAllureResultFile["labels"] as ArrayNode).add(
                        mapper.createModelTag(it)
                )
            }
            deviceInfo.osVersion?.let {
                (tempAllureResultFile["labels"] as ArrayNode).add(
                        mapper.createOsVersionTag(it)
                )
            }
        }

        File("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${file.name}").apply {
            this.setWritable(true)
            this.writeText(
                mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tempAllureResultFile)
            )
        }
        File("${projectDirectory}${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}temp-${file.name}").delete()
    }

    private suspend fun transferringFilesByDevice(scope: CoroutineScope,
                                                  adb: AdbManager,
                                                  device: Device) {
        logger.info("Transferring Json files from ${device.serial}")
        val list: List<FileEntryV1> = adb.getFileList(device.serial) ?: listOf<FileEntryV1>()
        val listOfAllureDeviceJsonFiles = list.filter { isResultJson(it.name!!) }
        logger.debug("Count of allure device Json files ${listOfAllureDeviceJsonFiles.size}")
        if (listOfAllureDeviceJsonFiles.size > Configuration.startAsyncResultFilesTransferFrom) {
            runBlocking(
                newFixedThreadPoolContext(
                    Configuration.asyncFilesTransferThreadsCount,
                    "allure-results-enricher-pool"
                )
            ) {
                logger.debug("Parallel Json files transferring")
                listOfAllureDeviceJsonFiles.pforEach(this.coroutineContext) {
                    pullResultFilesWithEnrich(
                        this,
                        adb,
                        device,
                        it
                    )
                }
            }
        } else {
            listOfAllureDeviceJsonFiles.forEach {
                pullResultFilesWithEnrich(
                    scope,
                    adb,
                    device,
                    it
                )
            }
        }

        logger.info("Transferring other files from ${device.serial}")
        list.filter {
            it.isRegularFile() && (isNotJson(it.name!!) || isJsonNoTheResult(
                it.name!!
            ))
        }.copyOtherFiles(scope, adb, device)
    }
}