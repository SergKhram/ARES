package com.sergkhram.plugin


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal val buildType: String by lazy {
    System.getProperty("apkBuildType") ?: "debug"
}

internal fun copyVideos(projectDirectory: String) {
    try {
        File(
                "$projectDirectory/build/reports/marathon/${buildType}AndroidTest/${ScreenRecordAttachment.directoryName}/omni"
        ).listFiles()!!.filter { it.isDirectory }.forEach { vidDir ->
            vidDir.listFiles()!!.filter { it.isFile }.forEach { video ->
                video.copyFile(projectDirectory)
            }
        }
    } catch (e: KotlinNullPointerException) {
        throw CustomException("There is no $projectDirectory/build/reports/marathon/${buildType}AndroidTest/${ScreenRecordAttachment.directoryName}/omni directory. Check the attachmentType property")
    }
}

internal fun copyFiles(dir: File, projectDirectory: String, condition: (File) -> Boolean) {
    val listOfAllureFiles = dir.listFiles()!!.filter { it.isFile && condition(it) }
    listOfAllureFiles?.let {
        if (it.size > 500) {
            runBlocking(newFixedThreadPoolContext(it.size, "allure-files-copier-pool")) {
                it.pforEach(this.coroutineContext) { file ->
                    file.copyFile(projectDirectory)
                }
            }
        } else {
            it.forEach { file ->
                file.copyFile(projectDirectory)
            }
        }
    }
}

internal fun File.copyFile(projectDirectory: String) {
    Files.copy(
            Paths.get(this.path),
            Paths.get("$projectDirectory/build/allure-results/${this.name}")
    )
}

internal fun createAllureResultsDirectory(projectDirectory: String) {
    val directory = File("$projectDirectory/build/allure-results");
    if (!directory.exists()) {
        directory.mkdir();
    } else {
        directory.listFiles().forEach {
            it.delete()
        }
    }
}



internal fun prepareVideoAttachments(mapper: ObjectMapper, videoAtt: List<JsonNode>) =
        mapper.createObjectNode().apply {
            this.put("name", "Video")
            this.put(
                    "source",
                    if(Os.isFamily(Os.FAMILY_WINDOWS)) {
                        videoAtt.first()["source"].asText().split("\\\\", "\\").last()
                    } else {
                        videoAtt.first()["source"].asText().split("/").last()
                    }
            )
            this.put("type", ScreenRecordAttachment.allureType)
        }!!

internal val isNotJsonFile: (File) -> Boolean = { !it.name.endsWith(".json") }
internal val isEnvironmentFile: (File) -> Boolean = { it.name.contains("environment") }
internal val isJsonFile: (File) -> Boolean = {it.isFile && it.extension == "json" }
internal val isAppropriateMarathonResultFile: (File, ObjectMapper, JsonNode) -> Boolean = { marathonAllureFile, mapper, currentDeviceFile ->
    val currentMarathonFile = marathonAllureFile.asJson(mapper)
    val packageLabel = currentMarathonFile.getPackageLabel()
    currentDeviceFile.getFullName() == (packageLabel + "." + currentMarathonFile.getFullName()) &&
            currentMarathonFile.getStartTime() in currentDeviceFile.getStartTime()..currentDeviceFile.getStopTime()
}

fun File.asJson(mapper: ObjectMapper) = mapper.readTree(
        this.readText(Charsets.UTF_8)
)

fun JsonNode.getPackageLabel() =
        this["labels"]!!.find {
            map -> map["name"]!!.asText() == "package"
        }!!["value"].asText()

fun JsonNode.getVideoAttachments() = (this["attachments"] as ArrayNode).filter {
    elem -> elem["type"].asText() == VideoAttachment.allureType
}

fun JsonNode.getHostLabel() = (this["labels"] as ArrayNode).first { node ->
    node.toString().contains("host")
}

fun JsonNode.getFullName() = this["fullName"].asText()

fun ObjectMapper.createRealHostTag(realHost: JsonNode) = this.crea
teObjectNode()
.put("name", "tag")
.put("value", "device-${realHost["value"].asText()}")

fun JsonNode.getStartTime() = this["start"].asLong()
fun JsonNode.getStopTime() = this["stop"].asLong()