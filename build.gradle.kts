import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.17" apply false
    id("net.neoforged.moddev") version "2.0.143" apply false
}

repositories {
    mavenCentral()
}

allprojects {
    version = project.properties["mod_version"] as String
    group = project.properties["maven_group"] as String

    apply(plugin = "idea")

    configure<IdeaModel> {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}
