package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.interactor.StopAdbInteractor
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.v1.ListFileRequest
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import io.github.sergkhram.*
import io.github.sergkhram.helpers.*
import io.github.sergkhram.helpers.isJsonNoTheResult
import io.github.sergkhram.helpers.isNotJson
import io.github.sergkhram.helpers.isResultJson
import io.github.sergkhram.helpers.pforEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.roundToInt

class CleanAllureEnrichService(
    private val mapper: ObjectMapper,
    private val projectDirectory: String
) : EnrichService {

    val devicesInfo = mutableMapOf<String, String?>()

    override fun iterableEnrich() {
        logger.info("Getting results from device")
        runBlocking {
            androidHome?.let {
                logger.info("Android SDK directory is '$it'")
            }
            if(StartAdbInteractor().execute(androidHome = androidHome)) {
                logger.debug("Starting adb client factory")
                val adb = AndroidDebugBridgeClientFactory().build()
                val devices: List<Device> = adb.execute(request = ListDevicesRequest())
                logger.info("Found devices: ${devices.map {it.serial}}")
                devices.forEach {
                    val output = adb.execute(ShellCommandRequest("getprop ro.product.model"), it.serial).output
                    devicesInfo.put(it.serial, if(!output.isNullOrBlank()) output else null)
                }
                devices.forEach { device ->
                    logger.info("Transferring Json files from ${device.serial}")
                    val list: List<FileEntryV1> = adb.execute(
                        ListFileRequest(Configuration.remoteAllureFolder),
                        device.serial
                    )
                    val listOfAllureDeviceJsonFiles = list.filter { isResultJson(it.name!!) }
                    logger.debug("Count of allure device Json files ${listOfAllureDeviceJsonFiles.size}")
                    if (listOfAllureDeviceJsonFiles.size > 200) {
                        runBlocking(
                            newFixedThreadPoolContext(
                                listOfAllureDeviceJsonFiles.size,
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
                                this,
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
                    }.copyOtherFiles(this, adb, device)
                }
                logger.debug("Stopping adb")
                StopAdbInteractor().execute()
            } else {
                throw CustomException("Check your ANDROID_HOME env")
            }
        }
    }

    private suspend fun List<FileEntryV1>.copyOtherFiles(scope: CoroutineScope,
                                                         adb: AndroidDebugBridgeClient,
                                                         device: Device) {
        val files = this
        logger.debug("Count of allure device other files ${files.size}")
        if(files.size > 500) {
            runBlocking(newFixedThreadPoolContext(this.size, "allure-files-copier-pool")) {
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
                                 adb: AndroidDebugBridgeClient,
                                 device: Device,
                                 file: FileEntryV1,
                                 prefix: String = "") {
        logger.info("Pulling allure file: ${file.name}")
        val pullDevicesRequest = PullFileRequest(
            "${Configuration.remoteAllureFolder}/${file.name}",
            File("${projectDirectory}/build/allure-results/${prefix}${file.name}"),
            coroutineContext = scope.coroutineContext)
        val channel = adb.execute(
            pullDevicesRequest,
            scope,
            device.serial
        )

        var percentage = 0
        for (percentageDouble in channel) {
            percentage = (percentageDouble * 100).roundToInt()
            progressPercentage(percentage, 100, file.name!!)
        }
    }

    private suspend fun pullResultFilesWithEnrich(scope: CoroutineScope, adb: AndroidDebugBridgeClient, device: Device, file: FileEntryV1) {
        pullFile(scope, adb, device, file, "temp-")

        var tempAllureResultFile = File("${projectDirectory}/build/allure-results/temp-${file.name}").asJson(mapper)
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
        devicesInfo[device.serial]?.let {
            (tempAllureResultFile["labels"] as ArrayNode).add(
                    mapper.createModelTag(devicesInfo[device.serial]!!)
            )
        }

        File("$projectDirectory/build/allure-results/${file.name}").apply {
            this.setWritable(true)
            this.writeText(
                mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tempAllureResultFile)
            )
        }
        File("${projectDirectory}/build/allure-results/temp-${file.name}").delete()
    }
}