plugins {
    id("multiloader-loader")
    id("net.fabricmc.fabric-loom")
    id("mod-publishing")
}

val minecraft_version: String by project
val fabric_loader_version: String by project
val fabric_api_version: String by project

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraft_version")
    // Minecraft 26.1+ ships unobfuscated - there are no mappings to apply.
    "implementation"("net.fabricmc:fabric-loader:$fabric_loader_version")
    "api"("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
}
