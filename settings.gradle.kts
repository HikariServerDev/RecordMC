pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("fabric-loom") version "1.17.19" apply false
}

stonecutter {
    create(rootProject) {
        versions(
            "1.16.1", "1.18", "1.18.1", "1.18.2", "1.19.4",
            "1.20", "1.20.1", "1.20.6",
            "1.21", "1.21.1", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"
        )
        vcsVersion = "1.18.2"
    }
}

rootProject.name = "RecordMC"
