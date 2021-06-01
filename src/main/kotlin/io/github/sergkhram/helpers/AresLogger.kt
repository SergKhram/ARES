package io.github.sergkhram.helpers

import io.github.sergkhram.configuration.Configuration
import org.gradle.api.logging.Logging
import java.text.SimpleDateFormat
import java.util.*


class AresLogger<T>(clazz: Class<T>) {
    private val logger = Logging.getLogger(clazz)
    val pattern: (String) -> String = {
        val formatter = SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z")
        val date = Date(System.currentTimeMillis())
        val currentTime = formatter.format(date)
        "$currentTime - [${clazz.`package`.name}.${clazz.simpleName}] --- $it"
    }

    fun info(string: String) {
        val output = pattern(string)
        logger.info(output)
        Configuration.logFile?.appendText("$output\n")
    }

    fun debug(string: String) {
        val output = pattern(string)
        logger.debug(output)
        Configuration.logFile?.appendText("$output\n")
    }
}