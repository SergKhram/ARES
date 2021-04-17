package io.github.sergkhram

object ScreenRecordAttachment {
    val directoryName = if (Configuration.screenRecordType == ScreenRecordType.SCREENSHOT) "screenshot" else "video"
    val allureType = if (Configuration.screenRecordType == ScreenRecordType.SCREENSHOT) "image/gif" else "video/mp4"
}

enum class ScreenRecordType {
    VIDEO,
    SCREENSHOT
}