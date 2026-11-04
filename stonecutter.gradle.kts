plugins {
    id("dev.kikugie.stonecutter")
}

// IDE が開くアクティブバージョン。切り替えは `./gradlew "Set active project to <ver>"`
stonecutter active "1.18.2"

// 全バージョンノードの buildAndCollect に依存する集約タスク
tasks.register("chiseledBuild") {
    group = "project"
    description = "全ターゲットをビルドして build/libs/<mod version>/ に集約する"
    dependsOn(stonecutter.versions.map { ":${it.project}:buildAndCollect" })
}
