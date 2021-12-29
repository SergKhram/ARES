package io.github.sergkhram.tasks

import groovy.lang.Closure
import io.github.sergkhram.androidHomeDir
import io.github.sergkhram.configuration.*
import io.github.sergkhram.helpers.CustomException
import org.gradle.api.Project
import java.io.File

open class ConfigurationExtension(project: Project) {
    var marathonBlock: MarathonBlock? = null
    var allureBlock: AllureBlock? = null
    var enrichBy: String? = null
    var startAsyncResultFilesTransferFrom: Int = 200
    var startAsyncOtherFilesTransferFrom: Int = 500
    var asyncFilesTransferThreadsCount: Int = 10
    var testExecutionBlock: TestExecutionBlock? = null
    var androidHome: String? = null

    fun marathonBlock(block: MarathonBlock.() -> Unit) {
        marathonBlock = MarathonBlock().also(block)
    }

    fun marathonBlock(closure: Closure<*>) {
        marathonBlock = MarathonBlock()
        closure.delegate = marathonBlock
        closure.call()
    }

    fun allureBlock(block: AllureBlock.() -> Unit) {
        allureBlock = AllureBlock().also(block)
    }

    fun allureBlock(closure: Closure<*>) {
        allureBlock = AllureBlock()
        closure.delegate = allureBlock
        closure.call()
    }

    fun testExecutionBlock(block: TestExecutionBlock.() -> Unit) {
        testExecutionBlock = TestExecutionBlock().also(block)
    }

    fun testExecutionBlock(closure: Closure<*>) {
        testExecutionBlock = TestExecutionBlock()
        closure.delegate = testExecutionBlock
        closure.call()
    }
}

fun ConfigurationExtension.provideConfiguration() {
    Configuration.buildType = System.getProperty("buildType")?.toString() ?: this.marathonBlock?.buildType ?: "debug"
    Configuration.screenRecordType = try {
        ScreenRecordType.valueOf(System.getProperty("screenRecordType")?.toString() ?: this.marathonBlock?.screenRecordType ?: "VIDEO")
    } catch (e: IllegalArgumentException) {
        throw CustomException("There is no chosen screenRecordType, only these values are supported : ${ScreenRecordType.values().map {it.name}}")
    }
    Configuration.enrichBy = try {
        EnrichVariant.valueOf(System.getProperty("enrichBy")?.toString() ?: this.enrichBy ?: "MARATHON")
    } catch (e: IllegalArgumentException) {
        throw CustomException("There is no chosen enrichBy variant, only these values are supported : ${EnrichVariant.values().map {it.name}}")
    }
    Configuration.isMarathonCLI = System.getProperty("isMarathonCLI")?.toBoolean() ?: this.marathonBlock?.marathonCLI ?: false
    Configuration.remoteAllureFolder = System.getProperty("remoteAllureFolder") ?: this.allureBlock?.remoteAllureFolder ?: "/sdcard/allure-results"
    Configuration.reportDirectory = System.getProperty("reportDirectory")?.toString() ?: this.marathonBlock?.reportDirectory
    Configuration.deviceSerials = System.getProperty("deviceSerials")?.toString() ?: this.allureBlock?.deviceSerials
    Configuration.startAsyncResultFilesTransferFrom =
        initAsyncFilesTransferring("startAsyncResultFilesTransferFrom", this.startAsyncResultFilesTransferFrom)
    Configuration.startAsyncOtherFilesTransferFrom =
        initAsyncFilesTransferring("startAsyncOtherFilesTransferFrom", this.startAsyncOtherFilesTransferFrom)
    Configuration.asyncFilesTransferThreadsCount =
        initAsyncFilesTransferring("asyncFilesTransferThreadsCount", this.asyncFilesTransferThreadsCount)
    Configuration.executionIgnoreFailures = System.getProperty("executionIgnoreFailures")?.toBoolean() ?: this.testExecutionBlock?.executionIgnoreFailures ?: false
    Configuration.androidHome = System.getProperty("androidHome")?.let { File(it) } ?: this.androidHome?.let{ File(it)} ?: androidHomeDir
    Configuration.copyCrashedTests = System.getProperty("copyCrashedTests")?.toBoolean() ?: this.marathonBlock?.copyCrashedTests ?: false
}

private fun initAsyncFilesTransferring(propertyName: String, defaultValue: Int): Int {
    val valueFromSystemProperty: Int? = System.getProperty(propertyName)?.toInt()
    if (valueFromSystemProperty != null) {
        if (valueFromSystemProperty < 1) {
            throw CustomException("Incorrect count for ${propertyName} : ${valueFromSystemProperty}")
        }
        return valueFromSystemProperty
    }
    return defaultValue;
}