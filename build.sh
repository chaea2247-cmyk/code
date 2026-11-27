#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
rm -rf build dist
mkdir -p build/classes dist
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d build/classes
printf 'Main-Class: com.codenote.CodeNoteApp\n' > build/MANIFEST.MF
jar cfm dist/CodeNote.jar build/MANIFEST.MF -C build/classes .
echo "Built: $ROOT/dist/CodeNote.jar"
