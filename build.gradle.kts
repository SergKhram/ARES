import java.net.URI

group = "io.github.sergkhram"
version = "1.2.9-RELEASE"

buildscript {
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-databind:2.12.3")
        classpath(kotlin("stdlib-jdk8"))
        classpath(gradleApi())
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
        classpath("com.malinskiy.adam:adam:0.4.3")
        classpath("io.qameta.allure.gradle.allure:allure-plugin:2.9.6")
        classpath("io.qameta.allure.gradle.report:allure-report-plugin:2.9.6")
        classpath("io.qameta.allure.gradle.base:allure-base-plugin:2.9.6")
        classpath("io.qameta.allure.gradle.adapter:allure-adapter-plugin:2.9.6")
        gradleApi()
    }

    repositories {
        jcenter()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version "1.4.32"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
