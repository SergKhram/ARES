package io.github.sergkhram

object Configuration {
    val buildType: String by lazy { System.getProperty("buildType") ?: "debug" }
    val screenRecordType: ScreenRecordType by lazy {
        try {
            ScreenRecordType.valueOf(System.getProperty("screenRecordType") ?: "SCREENSHOT")
        } catch (e: Exception) {
            throw CustomException("There is no chose screenRecordType, only these values are supported : ${ScreenRecordType.values()}")
        }
    }
    val enrichBy: EnrichVariant by lazy {
        try {
            EnrichVariant.valueOf(System.getProperty("enrichVariant") ?: "MARATHON")
        } catch (e: Exception) {
            throw CustomException("There is no chose enrichVariant, only these values are supported : ${EnrichVariant.values()}")
        }
    }
    val remoteAllureFolder: String by lazy { System.getProperty("remoteAllureFolder") ?: "sdcard/allure-results"}
}

enum class EnrichVariant{
    MARATHON,
    CLEAN_ALLURE
}