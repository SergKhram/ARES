package com.sergkhram.plugin

object ScreenRecordAttachment {
    private val attachmentType: String by lazy { System.getProperty("screenRecordType") ?: "SCREENSHOT" }
    val directoryName = if (ScreenRecordType.valueOf(attachmentType) == ScreenRecordType.SCREENSHOT) "screenshot" else "video"
    val allureType = if (ScreenRecordType.valueOf(attachmentType) == ScreenRecordType.SCREENSHOT) "image/gif" else "video/mp4"
}

enum class ScreenRecordType {
    VIDEO,
    SCREENSHOT
}