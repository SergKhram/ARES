# ARES
**ARES(Android Report Synchro) Plugin**

Based on files from execution of allure-kotlin and marathon.

allure-kotlin: https://github.com/allure-framework/allure-kotlin
marathon: https://github.com/Malinskiy/marathon

## Tasks
> - **reportSync** is required to copy videos, environment file and device allure results data to folder allure-results.
## Properties
> - **screenRecordType** is needed to choose the current screen record attachment type of marathon. There are two options:
> *SCREENSHOT* and *VIDEO* (default is *SCREENSHOT*, also for empty screenRecordType property).
> *SCREENSHOT* is for *GIF*, *VIDEO* is for *MP4*.
> - **buildType** is needed when you have more then two build types and variants (default is *debug*)