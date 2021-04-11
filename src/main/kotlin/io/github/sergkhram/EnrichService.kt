package io.github.sergkhram

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.sergkhram.helpers.*
import io.github.sergkhram.helpers.isAppropriateMarathonResultFile
import io.github.sergkhram.helpers.isJsonFile
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File

class EnrichService(
        private val marathonAllureResDirectory: File,
        private val mapper: ObjectMapper,
        private val projectDirectory: String
) {

    fun iterableEnrich(listOfAllureDeviceJsonFiles: List<File>?) {
        listOfAllureDeviceJsonFiles?.let {
            if(it.size > 200) {
                runBlocking(newFixedThreadPoolContext(it.size, "allure-results-enricher-pool")) {
                    it.pforEach(this.coroutineContext) { deviceAllureFile->
                        enrichByMarathonResultFile(deviceAllureFile)
                    }
                }
            } else {
                it.forEach { deviceAllureFile->
                    enrichByMarathonResultFile(deviceAllureFile)
                }
            }
        }
    }

    private fun enrichByMarathonResultFile(
            deviceAllureFile: File
    ) {
        var currentDeviceFile = deviceAllureFile.asJson(mapper)
        marathonAllureResDirectory.listFiles()?.first {
            isJsonFile(it) &&
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
}