package io.github.sergkhram

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.configuration.ExecuteBy
import io.github.sergkhram.configuration.ScreenRecordAttachment
import io.github.sergkhram.helpers.AresLogger
import io.github.sergkhram.helpers.CustomException
import io.github.sergkhram.helpers.pforEach
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.attribute.BasicFileAttributes

val logger = AresLogger(AresPlugin::class.java)

val marathonVideoDir: (String) -> File = {
    File("${it}video")
}

val marathonGifDir: (String) -> File = {
    File("${it}screenshot")
}

val marathonLogsDir: (String) -> File = {
    File("${it}logs")
}

internal fun copyVideos(projectDirectory: String) {
    logger.info("Transferring videos")
    val reportDir = Configuration.getReportDirectory(projectDirectory)
    val marathonVideoDir = marathonVideoDir(reportDir)
    val marathonGifDir = marathonGifDir(reportDir)
    val currentMarathonScreenRecordDirectory = "${Configuration.getReportDirectory(projectDirectory)}${ScreenRecordAttachment.directoryName}"
    if(marathonVideoDir.exists() || marathonGifDir.exists()) {
        try {
            File(
                currentMarathonScreenRecordDirectory
            ).listFiles()!!.filter { it.isDirectory }.forEach { vidDir ->
                vidDir.copyFolder(projectDirectory)
            }
        } catch (e: KotlinNullPointerException) {
            throw CustomException("There is no $currentMarathonScreenRecordDirectory directory. Check the screenRecordType property")
        }
    }
}

@Throws(IOException::class)
fun File.copyFolder(projectDirectory: String) {
    val source = this
    val target = Paths.get("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${this.name}")
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

internal fun copyFiles(dir: File, projectDirectory: String, condition: (File) -> Boolean, description: String) {
    val listOfAllureFiles = dir.listFiles()!!.filter { it.isFile && condition(it) }
    listOfAllureFiles?.let {
        if (it.size > Configuration.startAsyncOtherFilesTransferFrom) {
            logger.debug("Parallel $description files transferring")
            runBlocking(
                newFixedThreadPoolContext(
                    Configuration.asyncFilesTransferThreadsCount,
                    "allure-files-copier-pool-${description}"
                )
            ) {
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
    copy(
        Paths.get(this.path),
        Paths.get("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results${Configuration.separator}${this.name}")
    )
}

internal fun createAllureResultsDirectory(projectDirectory: String) {
    val directory = File("$projectDirectory${Configuration.separator}build${Configuration.separator}allure-results")
    recursivelyDelete(directory)
}

internal fun recursivelyDelete(directory: File) {
    if (!directory.exists()) {
        directory.mkdir()
    } else {
        directory.listFiles()?.forEach {
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

fun progressPercentage(done: Int, total: Int, fileName: String) {
    val size = 20
    val iconLeftBoundary = "["
    val iconDone = "="
    val iconRemain = "."
    val iconRightBoundary = "]"
    require(done <= total)
    val donePercents = 100 * done / total
    val doneLength = size * donePercents / 100
    val bar = StringBuilder(iconLeftBoundary)
    for (i in 0 until size) {
        if (i < doneLength) {
            bar.append(iconDone)
        } else {
            bar.append(iconRemain)
        }
    }
    bar.append(iconRightBoundary)
    logger.info("[$fileName] $bar $donePercents%")
    if (done == total) {
        logger.info("\n")
    }
}

fun File.checkDirectoryExisting(): File? {
    this.let {
        return if(it.exists()) it else null
    }
}

val androidHomeDir: File? =
    System.getenv("ANDROID_HOME")?.let { File(it) } ?:
    System.getenv("ANDROID_SDK_ROOT")?.let{ File(it) } ?:
    when {
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

fun getPropertyExecuteBy(aresExtension: ConfigurationExtension): ExecuteBy? {
    return try {
        System.getProperty("executeBy")?.let { ExecuteBy.valueOf(it) }
            ?: aresExtension.testExecutionBlock?.executeBy?.let { ExecuteBy.valueOf(it) }
    } catch (e: IllegalArgumentException) {
        throw CustomException("There is no chosen executeBy variant, only these values are supported : ${ExecuteBy.values().map {it.name}}")
    }
}

internal fun prepareMarathonLogAttachments(mapper: ObjectMapper, logAtt: List<JsonNode>): JsonNode {
    val path = logAtt.first()["source"].asText()
    val separator = Paths.get(path).fileSystem.separator
    return mapper.createObjectNode().apply {
        this.put("name", "MarathonLog")
        this.put(
                "source",
                path.split("logs$separator").last()
        )
        this.put("type", "text/plain")
    }!!
}

internal fun copyMarathonLogs(projectDirectory: String) {
    logger.info("Transferring marathon logs")
    val reportDir = Configuration.getReportDirectory(projectDirectory)
    val marathonLogsDir = marathonLogsDir(reportDir)
    if(marathonLogsDir.exists()) {
        try {
            marathonLogsDir.listFiles()!!.filter { it.isDirectory }.forEach { logDir ->
                logDir.copyFolder(projectDirectory)
            }
        } catch (e: KotlinNullPointerException) {
            throw CustomException("There is no ${marathonLogsDir.absolutePath} directory. Check the screenRecordType property")
        }
    }
}