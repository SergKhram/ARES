package io.github.sergkhram.tasks

import com.malinskiy.marathon.MarathonRunTask
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.configuration.ScreenRecordAttachment
import io.github.sergkhram.helpers.CustomException
import io.github.sergkhram.logger
import io.github.sergkhram.marathonGifDir
import io.github.sergkhram.marathonVideoDir
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import java.io.File

fun marathonAfterExecutionAction(project: Project) {
    val buildType = Configuration.buildType
    val marathonTaskName = "marathon${buildType.capitalize()}AndroidTest"
    val marathonTask = project.tasks.get(marathonTaskName) as MarathonRunTask
    logger.info("Getting retry count config value")
    val retryCount: Int = marathonTask.marathonExtension.get().retryStrategy?.let { retryStrategy ->
        retryStrategy.fixedQuota?.let { fixedQuota ->
            fixedQuota.retryPerTestQuota
        }
    } ?: 3
    logger.info("Retry count value is $retryCount")
    val reportDirectory = Configuration.getReportDirectory(project.projectDir.path)
    logger.info("ReportDirectory is $reportDirectory")
    logger.info("Getting all failed test names")
    val currentMarathonVideosDir = File("${reportDirectory}${ScreenRecordAttachment.directoryName}")
    val marathonVideoDir = marathonVideoDir(reportDirectory)
    val marathonGifDir = marathonGifDir(reportDirectory)
    if(marathonVideoDir.exists() || marathonGifDir.exists()) {
        if (!currentMarathonVideosDir.exists()) throw CustomException("There is no $currentMarathonVideosDir directory. Check the attachmentType property")
        val failedTests = mutableMapOf<String, Int>()
        currentMarathonVideosDir.listFiles()!!.forEach { shardingStrategy ->
            shardingStrategy.listFiles()!!.forEach { deviceDir ->
                deviceDir.listFiles()!!.forEach { file ->
                    val testName = file.name.split("-").first()
                    if(failedTests.containsKey(testName)) {
                        failedTests.put(testName, failedTests[testName]!!.toInt() + 1)
                    } else {
                        failedTests.put(testName, 1)
                    }
                }
            }
        }
        logger.debug("Failed tests: $failedTests")
        failedTests.forEach { failedTest ->
            if(failedTest.value >= retryCount + 1) {
                failedTestsExceptionAction("marathon")
            }
        }
    }
}

fun failedTestsExceptionAction(executor: String) {
    val message = "At least one test failed during all attempts by $executor. See build/allure-results directory or generate report using allure gradle plugin"
    logger.info(message)
    throw GradleException(message)
}