import org.gradle.api.attributes.Attribute

plugins {
    id("multiloader-common")
    id("net.fabricmc.fabric-loom")
}

val minecraft_version: String by project
val fabric_loader_version: String by project

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraft_version")
    // Minecraft 26.1+ ships unobfuscated - there are no mappings to apply.
    "implementation"("net.fabricmc:fabric-loader:$fabric_loader_version")
}

val commonJava by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add(commonJava.name, file("src/main/java"))
    add(commonResources.name, file("src/main/resources"))
    add(commonResources.name, file("src/main/generated/resources"))
}

val loaderAttribute = Attribute.of("dev.corgitaco.featurerecycler.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "common")
        }
    }
}

sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "common")
            }
        }
    }
}

sourceSets.main.get().resources.srcDir("src/main/generated/resources")
