package io.github.sergkhram.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.github.sergkhram.configuration.ScreenRecordAttachment
import java.io.File

fun File.asJson(mapper: ObjectMapper) = mapper.readTree(
    this.readText(Charsets.UTF_8)
)

fun JsonNode.getPackageLabel() =
    this["labels"]!!.find {
            map -> map["name"]!!.asText() == "package"
    }!!["value"].asText()

fun JsonNode.getVideoAttachments() = (this["attachments"] as ArrayNode).filter {
        elem -> elem["type"].asText() == ScreenRecordAttachment.allureType
}

fun JsonNode.getHostLabel() = (this["labels"] as ArrayNode).first { node ->
    node.toString().contains("host")
}

fun JsonNode.getFullName() = this["fullName"].asText()

fun ObjectMapper.createRealHostTag(realHost: JsonNode) = this.createObjectNode()
.put("name", "tag")
.put("value", "device-${realHost["value"].asText()}")

fun JsonNode.getStartTime() = this["start"].asLong()
fun JsonNode.getStopTime() = this["stop"].asLong()

fun ObjectMapper.createModelTag(modelName: String) = this.createObjectNode()
        .put("name", "tag")
        .put("value", "model-${modelName.trim()}")

fun ObjectMapper.createOsVersionTag(osVersion: String) = this.createObjectNode()
        .put("name", "tag")
        .put("value", "OS_VERSION_${osVersion.trim()}")