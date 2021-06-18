package io.github.sergkhram.configuration

data class TestExecutionBlock(
    var executeBy: String? = null,
    var executionIgnoreFailures: Boolean = false
)