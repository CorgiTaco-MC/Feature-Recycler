pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.firstdarkdev.xyz/releases")
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    id("com.gradle.develocity") version("3.18.1")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

develocity.buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
}

include("common", "fabric", "forge", "neoforge")

rootProject.name = "Feature Recycler"
