plugins {
    id("fabric-loom") apply false
    id("net.fabricmc.fabric-loom") apply false
}

fun prop(key: String): String = sc.properties.get<String>(key)
fun propOr(key: String, defaultVal: String): String = sc.properties.getOrNull<String>(key) ?: defaultVal

val isObfuscated = propOr("mod.obfuscated", "true").toBoolean()
apply(plugin = if (isObfuscated) "fabric-loom" else "net.fabricmc.fabric-loom")

version = "${prop("mod.version")}+${sc.current.version}"
group = prop("mod.group")

extra["recordmcTarget"] = mapOf(
    "minecraft" to sc.current.version,
    "modId" to prop("mod.id"),
    "modName" to prop("mod.name"),
    "modVersion" to prop("mod.version"),
    "mcCompat" to prop("mod.mc_compat"),
    "loaderMin" to prop("mod.loader_min"),
    "malilibMin" to prop("mod.malilib_min"),
    "java" to prop("mod.java"),
    "obfuscated" to isObfuscated.toString(),
    "loader" to prop("deps.fabric_loader"),
    "yarn" to propOr("deps.yarn", ""),
    "fabricApi" to prop("deps.fabric_api"),
    "malilib" to prop("deps.malilib"),
    "modmenu" to propOr("deps.modmenu", "")
)

apply(from = rootProject.file("gradle/minecraft.gradle"))
