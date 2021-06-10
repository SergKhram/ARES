package io.github.sergkhram.configuration

object ScreenRecordAttachment {
    val directoryName: String by lazy {
        if (Configuration.screenRecordType == ScreenRecordType.SCREENSHOT) "screenshot" else "video"
    }
    val allureType: String by lazy {
        if (Configuration.screenRecordType == ScreenRecordType.SCREENSHOT) "image/gif" else "video/mp4"
    }
}

enum class ScreenRecordType {
    VIDEO,
    SCREENSHOT
}