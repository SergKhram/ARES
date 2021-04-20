package io.github.sergkhram

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.attribute.BasicFileAttributes


internal val buildType: String by lazy {
    System.getProperty("buildType") ?: "debug"
}

internal fun copyVideos(projectDirectory: String) {
    val marathonScreenRecordDirectory = "$projectDirectory/build/reports/marathon/${buildType}AndroidTest/${ScreenRecordAttachment.directoryName}"
    try {
        File(
            marathonScreenRecordDirectory
        ).listFiles()!!.filter { it.isDirectory }.forEach { vidDir ->
            vidDir.copyFolder(projectDirectory)
        }
    } catch (e: KotlinNullPointerException) {
        throw CustomException("There is no $marathonScreenRecordDirectory directory. Check the attachmentType property")
    }
}

@Throws(IOException::class)
fun File.copyFolder(projectDirectory: String) {
    val source = this
    val target = Paths.get("$projectDirectory/build/allure-results/${this.name}")
    Files.walkFileTree(source.toPath(), object : SimpleFileVisitor<Path>() {
        @Throws(IOException::class)
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            createDirectories(target.resolve(source.toPath().relativize(dir)))
            return FileVisitResult.CONTINUE
        }

        @Throws(IOException::class)
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            copy(file, target.resolve(source.toPath().relativize(file)))
            return FileVisitResult.CONTINUE
        }
    })
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
            if(it.isDirectory) it.deleteRecursively() else it.delete()
        }
    }
}

internal fun prepareVideoAttachments(mapper: ObjectMapper, videoAtt: List<JsonNode>): JsonNode {
    val path = videoAtt.first()["source"].asText()
    val separator = Paths.get(path).fileSystem.separator
    return mapper.createObjectNode().apply {
        this.put("name", "Video")
        this.put(
            "source",
            path.split("${ScreenRecordAttachment.directoryName}$separator").last()
        )
       this.put("type", ScreenRecordAttachment.allureType)
    }!!
}

