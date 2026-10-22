#!/usr/bin/env bash
# Compila el plugin de prueba OtroGenerador contra la API de Paper de la caché de Gradle (hace falta
# haber compilado WorldForge una vez). Deja el jar en test-server/.cache/otro-generador e imprime su ruta.
set -euo pipefail
AQUI=$(cd "$(dirname "$0")" && pwd)
OUT=$AQUI/../.cache/otro-generador
rm -rf "$OUT" && mkdir -p "$OUT/clases"
CP=""
while read -r f; do CP="$CP$(cygpath -w "$f");"; done < <(find ~/.gradle/caches/modules-2/files-2.1 -name "*.jar" ! -name "*sources*" \
    \( -path "*paper-api/26.1.2.build.74-stable*" -o -path "*net.kyori*" -o -path "*jspecify*" -o -path "*checker-qual*" \
       -o -path "*jetbrains*" -o -path "*guava*" -o -path "*joml*" -o -path "*bungeecord-chat*" \))
JAVAC=${JAVAC:-"/c/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot/bin/javac"}   # la API de Paper 26.1 es de Java 25
"$JAVAC" -nowarn -proc:none -d "$(cygpath -w "$OUT/clases")" -cp "$CP" "$(cygpath -w "$AQUI/src/prueba/OtroGenerador.java")"
cp "$AQUI/plugin.yml" "$OUT/clases/"
python - "$(cygpath -w "$OUT/clases")" "$(cygpath -w "$OUT/OtroGenerador.jar")" <<'PY'
import os, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk(src):
        for f in files:
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, src).replace(os.sep, "/"))
PY
echo "$OUT/OtroGenerador.jar"
