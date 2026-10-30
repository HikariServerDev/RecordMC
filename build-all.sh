#!/usr/bin/env bash
# Builds every supported version and collects the mod JARs in ./build.
set -euo pipefail
cd "$(dirname "$0")"

versions=(
    1.16.1
    1.18 1.18.1 1.18.2
    1.19.4
    1.20 1.20.1 1.20.6
    1.21 1.21.1 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11
)

rm -rf build
mkdir -p build
for version in "${versions[@]}"; do
    echo "=== RecordMC $version"
    (cd "$version" && ./gradlew clean build --console=plain --no-daemon)
    find "$version/build/libs" -name "recordmc-*.jar" ! -name "*-sources.jar" -exec cp {} build/ \;
done
echo
ls -l build
