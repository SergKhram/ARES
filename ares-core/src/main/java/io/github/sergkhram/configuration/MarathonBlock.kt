package io.github.sergkhram.configuration

data class MarathonBlock(
    var buildType: String? = null,
    var screenRecordType: String? = null,
    var marathonCLI: Boolean? = null,
    var reportDirectory: String? = null,
    var copyCrashedTests: Boolean = false
)
