plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.firstdarkdev.xyz/releases")
}

dependencies {
    implementation("com.hypherionmc.modutils.modpublisher:com.hypherionmc.modutils.modpublisher.gradle.plugin:2.+")
}
