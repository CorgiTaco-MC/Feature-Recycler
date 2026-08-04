plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
    id("mod-publishing")
}

val neoforge_version: String by project
val mod_id: String by project

neoForge {
    version = neoforge_version

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }
}
