package io.github.sergkhram.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

internal val isNotJsonFile: (File) -> Boolean = { !it.name.endsWith(".json") }
internal val isEnvironmentFile: (File) -> Boolean = { it.name.contains("environment") }
internal val isJsonFile: (File) -> Boolean = {it.isFile && it.extension == "json" }
internal val isAppropriateMarathonResultFile: (File, ObjectMapper, JsonNode) -> Boolean = { marathonAllureFile, mapper, currentDeviceFile ->
    val currentMarathonFile = marathonAllureFile.asJson(mapper)
    val packageLabel = currentMarathonFile.getPackageLabel()
    currentDeviceFile.getFullName() == (packageLabel + "." + currentMarathonFile.getFullName()) &&
            currentMarathonFile.getStartTime() in currentDeviceFile.getStartTime()..currentDeviceFile.getStopTime()
}