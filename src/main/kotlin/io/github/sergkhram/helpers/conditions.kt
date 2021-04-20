package io.github.sergkhram.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

internal val isNotJsonFile: (File) -> Boolean = { !it.name.endsWith(".json") }
internal val isEnvironmentFile: (File) -> Boolean = { it.name.contains("environment") }
internal val isResultJsonFile: (File) -> Boolean = { it.isFile && it.extension == "json" && it.nameWithoutExtension.contains("-result") }
internal val isAppropriateMarathonResultFile: (File, ObjectMapper, JsonNode) -> Boolean = { marathonAllureFile, mapper, currentDeviceFile ->
    val currentMarathonFile = marathonAllureFile.asJson(mapper)
    val packageLabel = currentMarathonFile.getPackageLabel()
    currentDeviceFile.getFullName() == (packageLabel + "." + currentMarathonFile.getFullName()) &&
        (
            currentMarathonFile.getStartTime() in currentDeviceFile.getStartTime()..currentDeviceFile.getStopTime() ||
            currentMarathonFile.getStopTime() in currentDeviceFile.getStartTime()..currentDeviceFile.getStopTime() ||
            (currentMarathonFile.getStartTime() + currentMarathonFile.getStopTime())/2 in currentDeviceFile.getStartTime()..currentDeviceFile.getStopTime() ||
            currentMarathonFile.getStartTime() - currentDeviceFile.getStopTime() < 1000
        )
}
internal val isResultJson: (String) -> Boolean = { it.contains("-result.json") }
internal val isNotJson: (String) -> Boolean = { !it.contains(".json") }
internal val isJsonNoTheResult: (String) -> Boolean = { !it.contains("-result") && it.contains("json") }
internal val isJsonNotTheResultFile: (File) -> Boolean = { it.isFile && it.extension == "json" && !(it.nameWithoutExtension.contains("-result")) }