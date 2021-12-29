package io.github.sergkhram.helpers

import io.github.sergkhram.configuration.Configuration
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


class AresLogger<T>(clazz: Class<T>) {
    init {
        System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    private val logger = LoggerFactory.getLogger(clazz)
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

    fun warn(string: String) {
        val output = pattern(string)
        logger.warn(output)
        Configuration.logFile?.appendText("$output\n")
    }
}