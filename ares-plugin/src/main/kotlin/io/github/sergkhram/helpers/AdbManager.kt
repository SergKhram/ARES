package io.github.sergkhram.helpers

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.interactor.StopAdbInteractor
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.v1.ListFileRequest
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import io.github.sergkhram.configuration.Configuration
import io.github.sergkhram.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File

class AdbManager(val androidHome: File?) {
    var adb: AndroidDebugBridgeClient? = null

    suspend fun getModel(serial: String): String? {
        return adb?.execute(ShellCommandRequest("getprop ro.product.model"), serial)?.output
    }

    suspend fun getOsVersion(serial: String): String? {
        return adb?.execute(ShellCommandRequest("getprop ro.build.version.sdk"), serial)?.output
    }

    suspend fun stopAdb() {
        StopAdbInteractor().execute()
    }

    suspend fun startAdb(): Boolean {
        val additionalPath = "platform-tools" + Configuration.separator + if(Os.isFamily(Os.FAMILY_WINDOWS)) "adb.exe" else "adb"
        logger.info("ADB directory is $androidHome${Configuration.separator}$additionalPath")
        val adbBinary =  File(androidHome, additionalPath)
        logger.info("${adbBinary.canonicalPath} exists - ${adbBinary.exists()}")
        return StartAdbInteractor().execute(adbBinary = adbBinary)
    }

    fun initAdbClient() {
        adb = AndroidDebugBridgeClientFactory().build()
    }

    suspend fun getDeviceList(): List<Device>? {
        return adb?.execute(request = ListDevicesRequest())
    }

    suspend fun getFileList(serial: String): List<FileEntryV1>? {
        return adb?.execute(
            ListFileRequest(Configuration.remoteAllureFolder),
            serial
        )
    }

    suspend fun pullFiles(pullDevicesRequest: PullFileRequest, scope: CoroutineScope, serial: String): ReceiveChannel<Double>? {
        return adb?.execute(
            pullDevicesRequest,
            scope,
            serial
        )
    }
}