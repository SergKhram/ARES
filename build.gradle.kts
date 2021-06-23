import java.net.URI

group = "io.github.sergkhram"
version = "1.2.0-RELEASE"

buildscript {
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-databind:2.12.3")
        classpath(kotlin("stdlib-jdk8"))
        classpath(gradleApi())
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
        classpath("com.malinskiy:adam:0.2.3")
        classpath("io.qameta.allure:allure-gradle:2.8.1")
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
