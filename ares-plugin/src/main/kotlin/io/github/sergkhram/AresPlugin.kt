package io.github.sergkhram

import io.github.sergkhram.configuration.ConfigurationExtension
import io.github.sergkhram.tasks.ReportGenTask
import io.github.sergkhram.tasks.ReportSyncTask
import io.qameta.allure.gradle.adapter.AllureAdapterPlugin
import io.qameta.allure.gradle.allure.AllurePlugin
import io.qameta.allure.gradle.base.AllureBasePlugin
import io.qameta.allure.gradle.download.AllureDownloadPlugin
import io.qameta.allure.gradle.report.AllureReportPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

open class AresPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: ConfigurationExtension = project.extensions.create("ares", ConfigurationExtension::class.java, project)
        project.plugins.apply(AllurePlugin::class)
        project.plugins.apply(AllureBasePlugin::class)
        project.plugins.apply(AllureAdapterPlugin::class)
        project.plugins.apply(AllureDownloadPlugin::class)
        project.plugins.apply(AllureReportPlugin::class)
        project.afterEvaluate {
            project.tasks.register<ReportSyncTask>("reportSync")
            project.tasks.register<ReportGenTask>("reportGen")
        }
    }
}