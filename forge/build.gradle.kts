import com.hypherionmc.modpublisher.properties.CurseEnvironment
import com.hypherionmc.modpublisher.properties.ModLoader
import com.hypherionmc.modpublisher.properties.ReleaseType

plugins {
    id("com.gradleup.shadow")
    id("com.hypherionmc.modutils.modpublisher") version "2.+"
}

architectury {
    platformSetupLoomIde()
    forge()
}

val minecraftVersion = project.properties["minecraft_version"] as String

configurations {
    create("common")
    "common" {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    create("shadowBundle")
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    getByName("developmentForge").extendsFrom(configurations["common"])
    "shadowBundle" {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

loom {
    forge {
        mixinConfig("featurerecycler-common.mixins.json")
    }
}

dependencies {
    forge("net.minecraftforge:forge:$minecraftVersion-${project.properties["forge_version"]}")

    "common"(project(":common", "namedElements")) { isTransitive = false }
    "shadowBundle"(project(":common", "transformProductionForge"))
}

tasks {
    processResources {
        inputs.property("version", project.version)

        val replaceProperties: Map<String, Any?> = mapOf(
            "forge_loader_version_range" to (project.findProperty("forge_loader_version_range") ?: ""),
            "mod_license" to (project.findProperty("mod_license") ?: projectDir.toPath().parent.resolve("LICENSE").toFile().takeIf { it.exists() }?.readText() ?: ""),
            "mod_issue_tracker_url" to (project.findProperty("mod_issue_tracker_url") ?: ""),
            "mod_client_side_only" to (project.findProperty("mod_client_side_only") ?: "false"),

            "mod_id" to (project.findProperty("mod_id") ?: ""),
            "mod_version" to (project.findProperty("mod_version") ?: project.version.toString()),
            "mod_name" to (project.findProperty("mod_name") ?: ""),
            "mod_update_json_url" to (project.findProperty("mod_update_json_url") ?: ""),
            "mod_display_url" to (project.findProperty("mod_display_url") ?: ""),
            "mod_logo_file" to (project.findProperty("mod_logo_file") ?: ""),
            "mod_credits" to (project.findProperty("mod_credits") ?: ""),
            "mod_authors" to (project.findProperty("mod_authors") ?: ""),
            "mod_display_test" to (project.findProperty("mod_display_test") ?: ""),
            "mod_description" to (project.findProperty("mod_description") ?: ""),

            "minecraft_version_range" to (project.findProperty("minecraft_version_range") ?: ""),
            "forge_version_range" to (project.findProperty("forge_version_range") ?: ""),

            "forge_version" to (project.findProperty("forge_version") ?: ""),
            "minecraft_version" to (project.findProperty("minecraft_version") ?: project.properties["minecraft_version"]
            ?: "")
        )

        inputs.properties(replaceProperties)

        listOf("META-INF/mods.toml", "pack.mcmeta").forEach { pattern ->
            filesMatching(pattern) {
                expand(replaceProperties + mapOf("project" to project))
            }
        }


        var modDependencies = mapOf<String, Map<String, String>>()

        project.properties.filter { it.key.startsWith("dependencies;") }.forEach { (key, value) ->
            key.removePrefix("dependencies;").let { depKey ->
                depKey.substring(0, depKey.indexOf(';')).let { modId ->
                    if (modDependencies.get(modId) == null) {
                        modDependencies = modDependencies + (modId to mutableMapOf())
                    }

                    depKey.substring(depKey.indexOf(';') + 1).let { property ->
                        if (property.equals("version", ignoreCase = true)) {
                            (modDependencies[modId] as MutableMap)[property] = value as String
                        }
                        if (property.equals("mandatory", ignoreCase = true)) {
                            (modDependencies[modId] as MutableMap)[property] = value as String
                        }
                        if (property.equals("load_order", ignoreCase = true)) {
                            (modDependencies[modId] as MutableMap)[property] = value as String
                        }
                        if (property.equals("side", ignoreCase = true)) {
                            (modDependencies[modId] as MutableMap)[property] = value as String
                        }
                        if (property.equals("version_range", ignoreCase = true)) {
                            (modDependencies[modId] as MutableMap)["version"] = value as String
                        }
                    }
                }
            }
        }
        if (modDependencies.isNotEmpty()) {
            filesMatching("META-INF/mods.toml") {
                expand(mapOf("mod_dependencies" to modDependencies.map { (modId, props) ->
                    val mandatory = props["mandatory"]?.toBooleanStrictOrNull() ?: true
                    val version = props["version"] ?: "*"
                    val loadOrder = props["load_order"] ?: "NONE"

                    """[[dependencies.$modId]]
                        |    mandatory = $mandatory
                        |    versionRange = "$version"
                        |    ordering = "$loadOrder"
                    """.trimMargin()
                }.joinToString("\n")))
            }
        }
    }

    shadowJar {
        configurations = listOf(project.configurations.getByName("shadowBundle"))
        archiveClassifier.set("dev-shadow")
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        dependsOn(shadowJar)
    }
}

publisher {
    apiKeys {
        curseforge(getPublishingCredentials().first)
        modrinth(getPublishingCredentials().second)
    }

    curseID.set("1077985")
    modrinthID.set("IAzu52kG")
    setReleaseType(ReleaseType.RELEASE)
    projectVersion.set(project.version.toString() + "-${project.name}")
    displayName.set(base.archivesName.get() + "-${project.version}")
    changelog.set(projectDir.toPath().parent.resolve("CHANGELOG.md").toFile().readText())
    artifact.set(tasks.remapJar)
    setGameVersions(minecraftVersion)
    setLoaders(ModLoader.FORGE)
    setCurseEnvironment(CurseEnvironment.SERVER)
    setJavaVersions(JavaVersion.VERSION_21, JavaVersion.VERSION_22)
}

private fun getPublishingCredentials(): Pair<String?, String?> {
    val curseForgeToken = (project.findProperty("curseforge_token") ?: System.getenv("CURSEFORGE_TOKEN") ?: System.getenv("CURSEFORGE_KEY") ?: "") as String?
    val modrinthToken = (project.findProperty("modrinth_token") ?: System.getenv("MODRINTH_TOKEN") ?: System.getenv("MODRINTH_KEY") ?: "") as String?
    return Pair(curseForgeToken, modrinthToken)
}
