import java.net.URI

group = "io.github.sergkhram"
version = "1.2.8-RELEASE"

plugins {
    kotlin("jvm")
    `kotlin-dsl`
    `maven-publish`
    signing
}

dependencies {
    implementation("com.malinskiy.marathon:marathon-gradle-plugin:0.6.2")
    implementation("com.malinskiy.marathon:base:0.6.2")
    implementation("com.malinskiy.marathon:core:0.6.2")
    implementation("io.qameta.allure:allure-gradle:2.8.1")
    implementation(project(":ares-plugin"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    (plugins) {
        create("aresExecPlugin") {
            id = "io.github.sergkhram.aresExecPlugin"
            implementationClass = "io.github.sergkhram.AresExecPlugin"
        }
    }
}

signing {
    sign(publishing.publications)
}

publishing {
    val sonatypeUsername = System.getProperty("sonatypeUsername")
    val sonatypePassword = System.getProperty("sonatypePassword")
    val javaPlugin = project.the(JavaPluginConvention::class)

    val sourcesJar by project.tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
        classifier = "sources"
        from(javaPlugin.sourceSets["main"].allSource)
    }
    val javadocJar by project.tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
        classifier = "javadoc"
        from(javaPlugin.docsDir)
        dependsOn("javadoc")
    }

    publications {
        create("default", MavenPublication::class.java) {
            from(project.components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ares-exec")
                url.set("https://github.com/SergKhram/ARES")
                description.set("Android report synchro plugin + execution")

                this.licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                this.developers {
                    developer {
                        id.set("serg-khram-team")
                        name.set("Sergei Khramkov")
                        email.set("quigon3@yandex.ru")
                    }
                }

                this.scm {
                    url.set("https://github.com/SergKhram/ARES")
                }
            }
        }
    }
    repositories {
        maven {
            url = URI.create("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}