package io.github.sergkhram.configuration

import java.io.File

object Configuration {
    var buildType: String = "debug"
    var screenRecordType: ScreenRecordType = ScreenRecordType.valueOf("SCREENSHOT")
    var enrichBy: EnrichVariant = EnrichVariant.valueOf("MARATHON")
    var remoteAllureFolder: String = "/sdcard/allure-results"
    var isMarathonCLI: Boolean = false
    val testDirectory: String by lazy {
        if (isMarathonCLI) "" else "${buildType}AndroidTest/"
    }
    var reportDirectory: String? = null
    var deviceSerials: String? = null

    var startAsyncResultFilesTransferFrom: Int = 200
    var startAsyncOtherFilesTransferFrom: Int = 500
    var asyncFilesTransferThreadsCount: Int = 10

    fun getReportDirectory(projectDirectory: String): String {
        return if (isMarathonCLI && !reportDirectory.isNullOrEmpty()) reportDirectory!! else "$projectDirectory/build/reports/marathon/${testDirectory}"
    }

    var logFile: File? = null
}

enum class EnrichVariant {
    MARATHON,
    CLEAN_ALLURE
}