#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
chiseledBuild で生成した全 18 ターゲットの JAR を検証するスクリプト。

確認内容:
  - settings.gradle.kts に列挙した全ターゲットの JAR が存在するか
  - fabric.mod.json の environment ("client") と depends.minecraft
  - recordmc.mixins.json の compatibilityLevel
  - refmap (recordmc.refmap.json) が JAR に含まれているか
  - META-INF/LICENSE が同梱されているか
  - テストクラス (*Test.class) が混入していないか

終了コード 0 = 全て OK
"""

import io
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(path):
    return io.open(os.path.join(ROOT, path), encoding="utf-8").read()


def targets():
    """settings.gradle.kts の versions(...) からビルド対象を読む"""
    m = re.search(r"versions\(([^)]*)\)", read("settings.gradle.kts"), re.DOTALL)
    if not m:
        sys.exit("settings.gradle.kts から versions(...) を読めませんでした")
    return re.findall(r'"([^"]+)"', m.group(1))


def mod_version():
    m = re.search(r'^mod\.version\s*=\s*"([^"]+)"', read("stonecutter.properties.toml"), re.M)
    if not m:
        sys.exit("stonecutter.properties.toml から mod.version を読めませんでした")
    return m.group(1)


def main():
    version = mod_version()
    libs = os.path.join(ROOT, "build", "libs", version)
    if not os.path.isdir(libs):
        sys.exit("%s がありません。先に ./gradlew chiseledBuild を実行してください" % libs)

    all_targets = targets()
    bad = []
    print("RecordMC version: %s" % version)
    print("検証対象ターゲット数: %d" % len(all_targets))
    print()
    print("%-10s %-6s %-16s %-8s %-9s %s"
          % ("Target", "Java", "Minecraft", "Refmap", "License", "Status"))
    print("-" * 65)

    for target in all_targets:
        jar = os.path.join(libs, "RecordMC-%s+%s.jar" % (version, target))
        if not os.path.exists(jar):
            print("%-10s JAR が見つかりません (%s)" % (target, os.path.basename(jar)))
            bad.append(target)
            continue

        try:
            with zipfile.ZipFile(jar) as z:
                names = z.namelist()
                fmj = json.loads(z.read("fabric.mod.json").decode("utf-8"))
                mixins = json.loads(z.read("recordmc.mixins.json").decode("utf-8"))

                refmap_name = mixins.get("refmap", "recordmc.refmap.json")
                has_refmap = refmap_name in names
                has_license = "META-INF/LICENSE" in names
                env_ok = fmj.get("environment") == "client"

                # テスト混入チェック
                has_tests = any(n.endswith("Test.class") for n in names)

            java_level = mixins.get("compatibilityLevel", "").replace("JAVA_", "")
            mc_dep = fmj.get("depends", {}).get("minecraft", "?")

            is_ok = has_refmap and has_license and env_ok and not has_tests
            if not is_ok:
                bad.append(target)

            status_str = "OK" if is_ok else "NG!!"
            if has_tests:
                status_str += " (Tests found!)"

            print("%-10s %-6s %-16s %-8s %-9s %s" % (
                target,
                java_level,
                mc_dep,
                "OK" if has_refmap else "MISSING",
                "OK" if has_license else "MISSING",
                status_str,
            ))
        except Exception as e:
            print("%-10s 読み込みエラー: %s" % (target, str(e)))
            bad.append(target)

    print()
    if bad:
        print("検証失敗ターゲット: %s" % ", ".join(bad))
        return 1

    print("全 %d ターゲットの検証に合格しました！" % len(all_targets))
    return 0


if __name__ == "__main__":
    sys.exit(main())
