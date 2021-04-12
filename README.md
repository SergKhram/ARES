![ARES](https://repository-images.githubusercontent.com/356149284/6aef6780-9bc1-11eb-8ee7-3ffa3532959b)
# ARES
[![Generic badge](https://img.shields.io/badge/mavenCentral-1.0.0RELEASE-000000.svg)](https://search.maven.org/artifact/io.github.sergkhram/ares-plugin/1.0.0-RELEASE/jar)
[![Build Status](https://github.com/SergKhram/ARES/workflows/build/badge.svg)](https://github.com/SergKhram/ARES/actions)
![GitHub](https://img.shields.io/github/license/SergKhram/ARES)

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
