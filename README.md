![ARES](ares_white.jpg)
==========
# ARES
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sergkhram/ares-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.sergkhram%22%20AND%20a:%22ares-plugin%22)
[![Build Status](https://github.com/SergKhram/ARES/workflows/build/badge.svg)](https://github.com/SergKhram/ARES/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-purple.svg)](https://opensource.org/licenses/Apache-2.0)

**ARES(Android Report Synchro) Plugin**

Based on files from execution of allure-kotlin and marathon.

allure-kotlin: https://github.com/allure-framework/allure-kotlin
marathon: https://github.com/Malinskiy/marathon

## Quick start
```
repositories {
  mavenCentral()
}
```
```
implementation 'io.github.sergkhram:ares-plugin:1.0.0-RELEASE'
```
```
plugins {
  id 'io.github.sergkhram.aresPlugin'
}
```


## Tasks
> - **reportSync** is required to copy videos, environment file and device allure results data to folder allure-results.
## Properties
> - **screenRecordType** is needed to choose the current screen record attachment type of marathon. There are two options:
> *SCREENSHOT* and *VIDEO* (default is *SCREENSHOT*, also for empty screenRecordType property).
> *SCREENSHOT* is for *GIF*, *VIDEO* is for *MP4*.
> - **buildType** is needed when you have more then two build types and variants (default is *debug*)
