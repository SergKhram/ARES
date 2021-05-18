package io.github.sergkhram

object Configuration {
    val buildType: String by lazy { System.getProperty("buildType") ?: "debug" }
    val screenRecordType: ScreenRecordType by lazy {
        try {
            ScreenRecordType.valueOf(System.getProperty("screenRecordType") ?: "SCREENSHOT")
        } catch (e: Exception) {
            throw CustomException("There is no chosen screenRecordType, only these values are supported : ${ScreenRecordType.values()}")
        }
    }
    val enrichBy: EnrichVariant by lazy {
        try {
            EnrichVariant.valueOf(System.getProperty("enrichVariant") ?: "MARATHON")
        } catch (e: Exception) {
            throw CustomException("There is no chosen enrichVariant, only these values are supported : ${EnrichVariant.values()}")
        }
    }
    val remoteAllureFolder: String by lazy { System.getProperty("remoteAllureFolder") ?: "/sdcard/allure-results"}
    val isMarathonCLI: Boolean by lazy {
        System.getProperty("isMarathonCLI")?.toBoolean() ?: false
    }
    val testDirectory = if(isMarathonCLI) "" else "${buildType}AndroidTest/"
}

enum class EnrichVariant {
    MARATHON,
    CLEAN_ALLURE
}