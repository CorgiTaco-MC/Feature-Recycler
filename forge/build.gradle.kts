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

        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to project.version))
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
