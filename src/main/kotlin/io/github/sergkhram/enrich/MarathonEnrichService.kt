package io.github.sergkhram.enrich

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.sergkhram.copyFiles
import io.github.sergkhram.copyVideos
import io.github.sergkhram.helpers.*
import io.github.sergkhram.helpers.isAppropriateMarathonResultFile
import io.github.sergkhram.helpers.isResultJsonFile
import io.github.sergkhram.helpers.pforEach
import io.github.sergkhram.prepareVideoAttachments
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File

class MarathonEnrichService(
        private val marathonAllureResDirectory: File,
        private val mapper: ObjectMapper,
        private val projectDirectory: String,
        private val allureDeviceResDirectory: File
): EnrichService {

    override fun iterableEnrich() {
        val listOfAllureDeviceJsonFiles = allureDeviceResDirectory.listFiles()?.filter { isResultJsonFile(it) }
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
        copyOtherFiles()
    }

    private fun copyOtherFiles() {
        copyVideos(projectDirectory)
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