package io.github.sergkhram.configuration

import java.io.File

object Configuration {
    var buildType: String = "debug"
    var screenRecordType: ScreenRecordType = ScreenRecordType.VIDEO
    var enrichBy: EnrichVariant = EnrichVariant.MARATHON
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
    var executionIgnoreFailures: Boolean = false
}

enum class EnrichVariant {
    MARATHON,
    CLEAN_ALLURE
}

enum class ExecuteBy {
    MARATHON
}