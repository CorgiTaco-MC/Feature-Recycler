import com.hypherionmc.modpublisher.properties.CurseEnvironment
import com.hypherionmc.modpublisher.properties.ModLoader
import com.hypherionmc.modpublisher.properties.ReleaseType
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("com.hypherionmc.modutils.modpublisher")
}

val minecraft_version: String by project
val mod_name: String by project

fun Project.publishingCredentials(): Pair<String?, String?> {
    val curseForgeToken = (findProperty("curseforge_token") ?: System.getenv("CURSEFORGE_TOKEN") ?: System.getenv("CURSEFORGE_KEY") ?: "") as String?
    val modrinthToken = (findProperty("modrinth_token") ?: System.getenv("MODRINTH_TOKEN") ?: System.getenv("MODRINTH_KEY") ?: "") as String?
    return Pair(curseForgeToken, modrinthToken)
}

// Running an unqualified task name (e.g. `./gradlew publishMod`) from the root project makes
// Gradle execute that task in *every* project that declares it - which would publish every
// loader at once. Require these tasks to be invoked scoped to a single project, either via its
// full task path or by running the wrapper from inside that project's directory.
fun Project.guardPublishTaskScope(taskName: String) {
    tasks.named(taskName) {
        doFirst {
            val requested = gradle.startParameter.taskNames

            val explicitlyRequested = requested.any { it == taskName || it.endsWith(":$taskName") }
            if (!explicitlyRequested) return@doFirst

            val qualifiedPath = "${project.path}:$taskName"
            val currentDir = gradle.startParameter.currentDir.toPath().normalize()
            val projectDir = project.projectDir.toPath().normalize()
            val invokedFromThisProject = currentDir == projectDir || currentDir.startsWith(projectDir)

            val isScoped = requested.any { requestedTask ->
                requestedTask == qualifiedPath || requestedTask == ":$qualifiedPath" ||
                    (invokedFromThisProject && (requestedTask == taskName || requestedTask == ":$taskName"))
            }

            if (!isScoped) {
                throw org.gradle.api.GradleException(
                    "'$taskName' must be run scoped to a single project. " +
                        "Run './gradlew $qualifiedPath' instead of './gradlew $taskName'."
                )
            }
        }
    }
}

fun Project.configurePublisher(loaderSuffix: String, artifactFile: Any, modLoaders: Array<ModLoader>, curseEnvironment: CurseEnvironment?, requiredDepends: List<String>) {
    publisher {
        apiKeys {
            curseforge(publishingCredentials().first)
            modrinth(publishingCredentials().second)
        }

        curseID.set("1077985")
        modrinthID.set("IAzu52kG")
        setReleaseType(ReleaseType.RELEASE)
        projectVersion.set("${project.version}-$loaderSuffix")
        displayName.set("$mod_name-$loaderSuffix-${project.version}")
        changelog.set(projectDir.toPath().parent.resolve("CHANGELOG.md").toFile().readText())
        artifact.set(artifactFile)
        setGameVersions(minecraft_version)
        setLoaders(*modLoaders)
        curseEnvironment?.let { setCurseEnvironment(it) }
        setJavaVersions(JavaVersion.VERSION_25)
        curseDepends.required.set(requiredDepends)
        modrinthDepends.required.set(requiredDepends)
    }
}

when (project.name) {
    "fabric" -> {
        configurePublisher(
            loaderSuffix = "fabric",
            artifactFile = tasks.named("jar"),
            modLoaders = arrayOf(ModLoader.FABRIC, ModLoader.QUILT),
            curseEnvironment = CurseEnvironment.SERVER,
            requiredDepends = listOf("fabric-api"),
        )
    }

    "forge" -> {
        configurePublisher(
            loaderSuffix = "forge",
            artifactFile = tasks.named("jar"),
            modLoaders = arrayOf(ModLoader.FORGE),
            curseEnvironment = null,
            requiredDepends = emptyList(),
        )
    }

    "neoforge" -> {
        configurePublisher(
            loaderSuffix = "neoforge",
            artifactFile = tasks.named("jar", Jar::class.java).get().archiveFile.get().asFile,
            modLoaders = arrayOf(ModLoader.NEOFORGE),
            curseEnvironment = null,
            requiredDepends = emptyList(),
        )
    }

    else -> error("mod-publishing plugin applied to unsupported project '${project.name}' - expected fabric, forge, or neoforge")
}

listOf("publishMod", "publishCurseforge", "publishModrinth").forEach { taskName ->
    guardPublishTaskScope(taskName)
}
