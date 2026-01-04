import com.hypherionmc.modpublisher.properties.CurseEnvironment
import com.hypherionmc.modpublisher.properties.ModLoader
import com.hypherionmc.modpublisher.properties.ReleaseType

plugins {
    id("com.gradleup.shadow")
    id("com.hypherionmc.modutils.modpublisher") version "2.+"
}

architectury {
    platformSetupLoomIde()
    neoForge()
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
    getByName("developmentNeoForge").extendsFrom(configurations["common"])
    "shadowBundle" {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${project.properties["neoforge_version"]}")

    "common"(project(":common", "namedElements")) { isTransitive = false }
    "shadowBundle"(project(":common", "transformProductionNeoForge"))
}

tasks {
    processResources {
        inputs.property("version", project.version)

        val replaceProperties: MutableMap<String, Any?> = mutableMapOf(
            "neoforge_loader_version_range" to (project.findProperty("neoforge_loader_version_range") ?: ""),
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
            "neoforge_version_range" to (project.findProperty("neoforge_version_range") ?: ""),

            "neoforge_version" to (project.findProperty("neoforge_version") ?: ""),
            "minecraft_version" to (project.findProperty("minecraft_version") ?: project.properties["minecraft_version"]
            ?: "")
        )


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

        replaceProperties["mod_dependencies"] = modDependencies.map { (modId, props) ->
            val mandatory = props["mandatory"]?.toBooleanStrictOrNull() ?: true
            val version = props["version"] ?: "*"
            val loadOrder = props["load_order"] ?: "NONE"
            val side = props["side"] ?: "BOTH"

            """[[dependencies.$modId]]
                        |    mandatory = $mandatory
                        |    versionRange = "$version"
                        |    ordering = "$loadOrder"
                        |    side = "$side"
                    """.trimMargin()
        }.joinToString("\n")

        inputs.properties(replaceProperties)

        listOf("META-INF/neoforge.mods.toml", "pack.mcmeta").forEach { pattern ->
            filesMatching(pattern) {
                expand(replaceProperties + mapOf("project" to project))
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

    curseID.set(project.findProperty("publishing;curseforge;project_id") as String)
    modrinthID.set(project.findProperty("publishing;modrinth;project_id") as String)
    setReleaseType(ReleaseType.RELEASE)
    projectVersion.set(project.version.toString() + "-${project.name}")
    displayName.set(base.archivesName.get() + "-${project.version}")
    changelog.set(projectDir.toPath().parent.resolve("CHANGELOG.md").toFile().readText())
    artifact.set(tasks.remapJar)
    setGameVersions(minecraftVersion)
    setLoaders(ModLoader.NEOFORGE)
    setCurseEnvironment(CurseEnvironment.SERVER)
    setJavaVersions(JavaVersion.VERSION_21, JavaVersion.VERSION_22)
}

private fun getPublishingCredentials(): Pair<String?, String?> {
    val curseForgeToken = (project.findProperty("curseforge_token") ?: System.getenv("CURSEFORGE_TOKEN") ?: System.getenv("CURSEFORGE_KEY") ?: "") as String?
    val modrinthToken = (project.findProperty("modrinth_token") ?: System.getenv("MODRINTH_TOKEN") ?: System.getenv("MODRINTH_KEY") ?: "") as String?
    return Pair(curseForgeToken, modrinthToken)
}
