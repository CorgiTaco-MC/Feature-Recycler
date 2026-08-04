plugins {
    id("multiloader-loader")
    id("java")
    id("idea")
    id("eclipse")
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
    id("mod-publishing")
}

val forge_version: String by project
val mod_id: String by project
val minecraft_version: String by project
val java_version: String by project

val mixinConfigs = listOf("featurerecycler-common.mixins.json")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(java_version.toInt()))

sourceSets.forEach {
    val dir = layout.buildDirectory.dir("sourcesSets/${it.name}")
    it.output.setResourcesDir(dir.get().asFile)
    it.java.destinationDirectory = dir
}

minecraft {
    runs {
        configureEach {
            workingDir = layout.projectDirectory.dir("run")

            systemProperty("eventbus.api.strictRuntimeChecks", "true")
            systemProperty("forge.enabledGameTestNamespaces", mod_id)
            mixinConfigs.forEach { config ->
                args("--mixin.config=$config")
            }
        }

        register("client")

        register("server") {
            args("--nogui")
        }
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${minecraft_version}-${forge_version}"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["MixinConfigs"] = mixinConfigs.joinToString(",")
    }
}

configurations.configureEach {
    resolutionStrategy.force("net.sf.jopt-simple:jopt-simple:5.0.4")
}
