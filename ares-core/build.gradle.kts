import java.net.URI

group = "io.github.sergkhram"
version = "1.2.11-RELEASE"

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("com.malinskiy.adam:adam:0.4.3")
    implementation("org.apache.ant:ant:1.8.2")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha5")
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
                name.set("ares-core")
                url.set("https://github.com/SergKhram/ARES")
                description.set("Android Core")

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