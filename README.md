![ARES](ares_blank.png)
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
implementation 'io.github.sergkhram:ares-plugin:1.0.2-RELEASE'
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
> - **enrichBy** is needed to choose variant of getting allure-results. There are two options:
> *MARATHON* and *CLEAN_ALLURE* (default is *MARATHON*, also for empty enrichBy property).
> *MARATHON* is for getting from marathon orchestrator report, *CLEAN_ALLURE* is just for pulling results from device with data enrichment.
> - **remoteAllureFolder** is needed to set path of device allure-results directory (default is ```sdcard/allure-results```, also for empty remoteAllureFolder property).
> It's only for **enrichBy**=*CLEAN_ALLURE*
> - **isMarathonCLI** is needed to get results from marathon report, that has been generated by marathon cli (default is false)
> - **reportDirectory** is needed to set the report directory if you use marathon CLI (it's only for **isMarathonCLI**=*true*)
> - **deviceSerials** is needed to set serial numbers of devices from which you need to get allure results(it's only for **enrichBy**=*CLEAN_ALLURE*)
> - **startAsyncResultFilesTransferFrom** is the value of allure result files count(*-result.json)
> from which is needed to use parallel transferring (the default is 200)
> - **startAsyncOtherFilesTransferFrom** is the value of allure other
> files count(not the *-result.json, like attachments) from which is needed to use parallel transferring (the default is 500)
> - **asyncFilesTransferThreadsCount** is the value of threads count, that will be used for parallel
> transferring in case of **startAsyncResultFilesTransferFrom** or **startAsyncOtherFilesTransferFrom** (the dafeult is 10)
## ARES extension
You can use ```ares {...}``` in your build.gradle(:app) and set the configuration as extension. For example:
```
ares {
    enrichBy = "MARATHON"
    marathonBlock {
        buildType = "debug"
    }
    startAsyncResultFilesTransferFrom = 100
    startAsyncOtherFilesTransferFrom = 300
    asyncFilesTransferThreadsCount = 20
}
```
or 
```
ares {
    enrichBy = "CLEAN_ALLURE"
    allureBlock {
        deviceSerials = "emulator-5554,emulator-5556"
    }
    startAsyncResultFilesTransferFrom = 300
    startAsyncOtherFilesTransferFrom = 1000
    asyncFilesTransferThreadsCount = 50
}
```