import java.net.URI

group = "io.github.sergkhram"
version = "1.1.0-RELEASE"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation(kotlin("stdlib-jdk8"))
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("com.malinskiy:adam:0.2.3")
}

plugins {
    kotlin("jvm") version "1.4.21"
    `kotlin-dsl`
    `maven-publish`
    signing
}

gradlePlugin {
    (plugins) {
        create("aresPlugin") {
            id = "io.github.sergkhram.aresPlugin"
            implementationClass = "io.github.sergkhram.AresPlugin"
        }
    }
}

repositories {
    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
                name.set("ares-plugin")
                url.set("https://github.com/SergKhram/ARES")
                description.set("Android report synchro plugin")

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