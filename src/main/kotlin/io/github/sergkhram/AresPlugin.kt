package io.github.sergkhram

import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.tasks.ReportSyncTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class AresPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: ConfigurationExtension = project.extensions.create("ares", ConfigurationExtension::class.java, project)
        project.afterEvaluate {
            project.tasks.register<ReportSyncTask>("reportSync")
        }
    }
}