package io.github.sergkhram

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal val buildType: String by lazy {
    System.getProperty("buildType") ?: "debug"
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

