import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
}

val mod_id: String by project
val mod_name: String by project
val mod_author: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project
val java_version: String by project
val license: String by project
val credits: String by project
val fabric_api_version: String by project
val fabric_loader_version: String by project
val forge_version: String by project
val forge_loader_version_range: String by project
val forge_dependency_version_range: String by project
val neoforge_version: String by project
val neoforge_loader_version_range: String by project
val neoforge_dependency_version_range: String by project
val archives_base_name: String by project
val github_url: String by project

val modDescription = "Fixes the \"Feature order cycle\" error that occurs when various mods add placed features in different orders between their biomes. It does this by automatically recycling(resorting) their biome features to respect the rules set by a previous biome containing the same 2 elements."

base {
    archivesName.set("${archives_base_name}-${project.name}-${minecraft_version}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(java_version.toInt()))
    withSourcesJar()
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases/")
    exclusiveContent {
        forRepository {
            maven {
                name = "Fabric"
                url = uri("https://maven.fabricmc.net")
            }
        }
        filter { includeGroup("net.fabricmc") }
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to mod_name,
                "Specification-Vendor" to mod_author,
                "Specification-Version" to archiveVersion.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion.get(),
                "Implementation-Vendor" to mod_author,
                "Built-On-Minecraft" to minecraft_version,
            ),
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    description = "Processes resources for ${project.path}"

    val expandProps: Map<String, Any?> = mapOf(
        "version" to version,
        "group" to project.group,
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "fabric_api_version" to fabric_api_version,
        "fabric_loader_version" to fabric_loader_version,
        "forge_version" to forge_version,
        "forge_loader_version_range" to forge_loader_version_range,
        "forge_dependency_version_range" to forge_dependency_version_range,
        "neoforge_version" to neoforge_version,
        "neoforge_loader_version_range" to neoforge_loader_version_range,
        "neoforge_dependency_version_range" to neoforge_dependency_version_range,
        "mod_name" to mod_name,
        "mod_author" to mod_author,
        "mod_id" to mod_id,
        "license" to license,
        "credits" to credits,
        "description" to modDescription,
        "java_version" to java_version,
        "github_url" to github_url,
    )

    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        artifactId = "${archives_base_name}-${project.name}-${minecraft_version}"
        from(components["java"])
    }

    repositories {
        mavenLocal()
    }
}
