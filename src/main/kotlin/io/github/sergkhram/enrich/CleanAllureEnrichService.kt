package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.interactor.StopAdbInteractor
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.request.sync.v2.ListFileRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import io.github.sergkhram.Configuration
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

    override fun iterableEnrich() {
        runBlocking {
            StartAdbInteractor().execute()
            val adb = AndroidDebugBridgeClientFactory().build()
            val devices: List<Device> = adb.execute(request = ListDevicesRequest())
            devices.forEach { device ->
                val list: List<FileEntryV2> = adb.execute(ListFileRequest(Configuration.remoteAllureFolder, listOf(Feature.LS_V2)), device.serial)
                val listOfAllureDeviceJsonFiles = list.filter{ isResultJson(it.name!!) }
                if(listOfAllureDeviceJsonFiles.size > 200) {
                    runBlocking(newFixedThreadPoolContext(listOfAllureDeviceJsonFiles.size, "allure-results-enricher-pool")) {
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

                list.filter{ isNotJson(it.name!!) || isJsonNoTheResult(it.name!!) }.copyOtherFiles(this, adb, device)
            }
            StopAdbInteractor().execute()

        }
    }

    private suspend fun List<FileEntryV2>.copyOtherFiles(scope: CoroutineScope,
                                                         adb: AndroidDebugBridgeClient,
                                                         device: Device) {
        val files = this
        if(files.size > 500) {
            runBlocking(newFixedThreadPoolContext(this.size, "allure-files-copier-pool")) {
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
                                 file: FileEntryV2,
                                 prefix: String = "") {
            val pullDevicesRequest = PullFileRequest(
                "${Configuration.remoteAllureFolder}/${file.name}",
                File("${projectDirectory}/build/allure-results/${prefix}${file.name}"),
                listOf(Feature.SENDRECV_V2),
                coroutineContext = scope.coroutineContext)
            val channel = adb.execute(
                pullDevicesRequest,
                scope,
                device.serial
            )

            var percentage = 0
            for (percentageDouble in channel) {
                percentage = (percentageDouble * 100).roundToInt()
                println(percentage)
        }
    }

    private suspend fun pullResultFilesWithEnrich(scope: CoroutineScope, adb: AndroidDebugBridgeClient, device: Device, file: FileEntryV2) {
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