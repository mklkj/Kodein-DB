plugins {
    id("org.kodein.root")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.15.1")
    }
}

allprojects {
    group = "org.kodein.db"
    version = "0.6.1-beta"

    repositories {
        mavenLocal()
        google()
    }
}

val kotlinxAtomicFuVer by extra { "0.15.1" } // CAUTION: also change in buildscript!
val kotlinxSerializationVer by extra { "1.1.0" }
val kodeinLogVer by extra { "0.10.1" }
val kodeinMemoryVer by extra { "0.8.0" }

val androidNdkVer by extra { "21.3.6528147" } // CAUTION: also change in CI workflows!

val currentOs = org.gradle.internal.os.OperatingSystem.current()!!

when {
    currentOs.isWindows -> {
        extra["osName"] = "windows"
        extra["libExt"] = "dll"
    }
    currentOs.isMacOsX -> {
        extra["osName"] = "macos"
        extra["libExt"] = "dylib"
    }
    currentOs.isLinux -> {
        extra["osName"] = "linux"
        extra["libExt"] = "so"
    }
    else -> error("Unknown operating system ${currentOs.name}")
}

task<Delete>("clean") {
    group = "build"
    delete(rootProject.buildDir)
}
