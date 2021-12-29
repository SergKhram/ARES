package io.github.sergkhram.tasks

import io.github.sergkhram.AresCore
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ReportSyncTask: DefaultTask() {
    init { outputs.upToDateWhen { false } }

    @OutputDirectory
    val outputDir = File(project.buildDir, "ares-logs")

    @OutputFile
    val log = File(outputDir, "ares.log")

    @TaskAction
    fun reportSync() {
        val conf = project.extensions.getByName("ares") as? ConfigurationExtension
            ?: ConfigurationExtension(project)
        conf.provideConfiguration()
        AresCore(outputDir, log, project.projectDir.path).execute()
    }
}