package io.github.sergkhram

import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.tasks.ReportGenTask
import io.github.sergkhram.tasks.ReportSyncTask
import io.qameta.allure.gradle.AllurePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

open class AresPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: ConfigurationExtension = project.extensions.create("ares", ConfigurationExtension::class.java, project)
        project.plugins.apply(AllurePlugin::class)
        project.afterEvaluate {
            project.tasks.register<ReportSyncTask>("reportSync")
            project.tasks.register<ReportGenTask>("reportGen")
        }
    }
}