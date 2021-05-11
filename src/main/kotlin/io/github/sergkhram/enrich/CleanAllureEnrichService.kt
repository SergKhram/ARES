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
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.v1.ListFileRequest
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import io.github.sergkhram.Configuration
import io.github.sergkhram.CustomException
import io.github.sergkhram.helpers.*
import io.github.sergkhram.helpers.isJsonNoTheResult
import io.github.sergkhram.helpers.isNotJson
import io.github.sergkhram.helpers.isResultJson
import io.github.sergkhram.helpers.pforEach
import io.github.sergkhram.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import kotlin.math.roundToInt

class CleanAllureEnrichService(
    private val mapper: ObjectMapper,
    private val projectDirectory: String
) : EnrichService {

    private fun File.checkDirectoryExisting(): File? {
        this.let {
            return if(it.exists()) it else null
        }
    }

    override fun iterableEnrich() {
        runBlocking {
            val androidHome: File? = when {
                Os.isFamily(Os.FAMILY_WINDOWS) -> {
                    val user = System.getenv("USER") ?: System.getenv("USERNAME") ?: null
                    user?.let {
                        File("C:\\Users\\$it\\AppData\\Local\\Android\\Sdk").checkDirectoryExisting()
                    }
                }
                Os.isFamily(Os.FAMILY_MAC) -> {
                    val user = System.getenv("USER") ?: null
                    user?.let {
                        File("/Users/$user/Library/Android/sdk").checkDirectoryExisting()
                    }
                }
                else -> {
                    val home = System.getenv("HOME") ?: null
                    home?.let {
                        File("$it/Android/Sdk").checkDirectoryExisting()
                    }
                }
            }
            androidHome?.let {
                logger.info("Android SDK directory is '$it'")
            }
            if(StartAdbInteractor().execute(androidHome = androidHome)) {
                val adb = AndroidDebugBridgeClientFactory().build()
                val devices: List<Device> = adb.execute(request = ListDevicesRequest())
                devices.forEach { device ->
                    logger.info("Transferring files from ${device.serial}")
                    val list: List<FileEntryV1> = adb.execute(
                        ListFileRequest(Configuration.remoteAllureFolder),
                        device.serial
                    )
                    val listOfAllureDeviceJsonFiles = list.filter { isResultJson(it.name!!) }
                    if (listOfAllureDeviceJsonFiles.size > 200) {
                        runBlocking(
                            newFixedThreadPoolContext(
                                listOfAllureDeviceJsonFiles.size,
                                "allure-results-enricher-pool"
                            )
                        ) {
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

                    list.filter {
                        it.isRegularFile() && (isNotJson(it.name!!) || isJsonNoTheResult(
                            it.name!!
                        ))
                    }.copyOtherFiles(this, adb, device)
                }
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
                                 file: FileEntryV1,
                                 prefix: String = "") {
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
                logger.info("Percentage: $percentage")
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