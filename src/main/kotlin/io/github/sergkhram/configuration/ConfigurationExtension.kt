package io.github.sergkhram.configuration

import groovy.lang.Closure
import io.github.sergkhram.helpers.CustomException
import org.gradle.api.Project
import java.lang.IllegalArgumentException

open class ConfigurationExtension(project: Project) {
    var marathonBlock: MarathonBlock? = null
    var allureBlock: AllureBlock? = null
    var enrichBy: String? = null

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
}

fun ConfigurationExtension.provideConfiguration() {
    Configuration.buildType = System.getProperty("buildType")?.toString() ?: this.marathonBlock?.buildType ?: "debug"
    Configuration.screenRecordType = try {
        ScreenRecordType.valueOf(System.getProperty("screenRecordType")?.toString() ?: this.marathonBlock?.screenRecordType ?: "SCREENSHOT")
    } catch (e: IllegalArgumentException) {
        throw CustomException("There is no chosen screenRecordType, only these values are supported : ${ScreenRecordType.values()}")
    }
    Configuration.enrichBy = try {
        EnrichVariant.valueOf(System.getProperty("enrichBy")?.toString() ?: this.enrichBy ?: "MARATHON")
    } catch (e: IllegalArgumentException) {
        throw CustomException("There is no chosen enrichBy variant, only these values are supported : ${EnrichVariant.values()}")
    }
    Configuration.isMarathonCLI = System.getProperty("isMarathonCLI")?.toBoolean() ?: this.marathonBlock?.marathonCLI ?: false
    Configuration.remoteAllureFolder = System.getProperty("remoteAllureFolder") ?: this.allureBlock?.remoteAllureFolder ?: "/sdcard/allure-results"
    Configuration.reportDirectory = System.getProperty("reportDirectory")?.toString() ?: this.marathonBlock?.reportDirectory
    Configuration.deviceSerials = System.getProperty("deviceSerials")?.toString() ?: this.allureBlock?.deviceSerials
}