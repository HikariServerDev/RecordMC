plugins {
    id("fabric-loom")
}

// stonecutter.properties.toml の値を読む
fun prop(key: String): String = sc.properties.get<String>(key)

version = "${prop("mod.version")}+${sc.current.version}"
base.archivesName = prop("mod.name")

val javaRelease: Int = prop("mod.java").toInt()

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com/") {
        name = "TerraformersMC"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings("net.fabricmc:yarn:${prop("deps.yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")

    // malilib
    modImplementation(prop("deps.malilib")) { isTransitive = false }

    // ModMenu (開発/コンパイル用)
    modCompileOnly("maven.modrinth:modmenu:${prop("deps.modmenu")}") { isTransitive = false }
}

// ── ユニットテスト ─────────────────────────────────────────────────────────
val TEST_TARGET = "1.18.2"

if (sc.current.version == TEST_TARGET) {
    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
    tasks.test {
        useJUnitPlatform()
    }
} else {
    sourceSets["test"].java.setSrcDirs(emptyList<String>())
    sourceSets["test"].resources.setSrcDirs(emptyList<String>())
}

loom {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("recordmc.refmap.json")
    }

    mods {
        register("recordmc") {
            sourceSet(sourceSets["main"])
        }
    }

    runConfigs.all {
        runDirectory = rootProject.file("run/${sc.current.version}")
    }
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaRelease)
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf(
        "id" to prop("mod.id"),
        "name" to prop("mod.name"),
        "version" to project.version.toString(),
        "minecraft" to prop("mod.mc_compat"),
        "loader_min" to prop("mod.loader_min"),
        "malilib_min" to prop("mod.malilib_min"),
        "java" to prop("mod.java"),
    )
    props.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$javaRelease") }
}

tasks.jar {
    from(rootProject.file("LICENSE")) { into("META-INF") }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "jar をビルドして build/libs/<mod version>/ に集約する"

    dependsOn(tasks.build)
    from(tasks.remapJar.flatMap { it.archiveFile })
    from(tasks.remapSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${prop("mod.version")}"))
}
