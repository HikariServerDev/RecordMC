#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RecordMC リリース用アセット（JAR、SHA256SUMS、artifacts.json）を準備するスクリプト。
"""

import hashlib
import io
import json
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(path):
    return io.open(os.path.join(ROOT, path), encoding="utf-8").read()


def targets():
    m = re.search(r"versions\(([^)]*)\)", read("settings.gradle.kts"), re.DOTALL)
    if not m:
        sys.exit("settings.gradle.kts から versions(...) を読めませんでした")
    return re.findall(r'"([^"]+)"', m.group(1))


def mod_version():
    m = re.search(r'^mod\.version\s*=\s*"([^"]+)"', read("stonecutter.properties.toml"), re.M)
    if not m:
        sys.exit("stonecutter.properties.toml から mod.version を読めませんでした")
    return m.group(1)


def sha256_file(filepath):
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def main():
    version = mod_version()
    src_dir = os.path.join(ROOT, "build", "libs", version)
    out_dir = os.path.join(ROOT, "build", "release-assets")

    if not os.path.isdir(src_dir):
        sys.exit("ビルドディレクトリが見つかりません: %s" % src_dir)

    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    all_targets = targets()
    print("RecordMC Version: %s" % version)
    print("対象ターゲット数: %d" % len(all_targets))

    artifacts_meta = []
    sha256_lines = []

    for target in all_targets:
        jar_name = "RecordMC-%s+%s.jar" % (version, target)
        src_jar = os.path.join(src_dir, jar_name)
        if not os.path.isfile(src_jar):
            sys.exit("JAR が存在しません: %s" % src_jar)

        dst_jar = os.path.join(out_dir, jar_name)
        shutil.copy2(src_jar, dst_jar)

        h = sha256_file(dst_jar)
        size = os.path.getsize(dst_jar)

        artifacts_meta.append({
            "name": jar_name,
            "minecraft": target,
            "kind": "mod",
            "sha256": h,
            "size": size
        })
        sha256_lines.append("%s  %s\n" % (h, jar_name))
        print("準備完了: %s (SHA256: %s...)" % (jar_name, h[:12]))

    # SHA256SUMS 書き出し
    sha256_path = os.path.join(out_dir, "SHA256SUMS")
    with open(sha256_path, "w", encoding="utf-8") as f:
        f.writelines(sha256_lines)

    # artifacts.json 書き出し
    artifacts_json_path = os.path.join(out_dir, "artifacts.json")
    with open(artifacts_json_path, "w", encoding="utf-8") as f:
        json.dump({"mod_version": version, "artifacts": artifacts_meta}, f, indent=2)

    print("\n全 %d 個の JAR および SHA256SUMS, artifacts.json の準備が完了しました: %s" % (len(all_targets), out_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
