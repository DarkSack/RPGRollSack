#!/usr/bin/env bash
# Folia 26.1.2-8 limpio en Docker, con el mismo contenedor (wf-test), puerto (25602) y carpeta (.work)
# que levantar.sh, así los escenarios comparten comun.sh. Lleva una COPIA del jar de desarrollo; si
# no declara `folia-supported: true` (los jar anteriores a pasar estas pruebas), se lo añade a la copia.
#
# Necesita en test-server/.cache/folia el jar de Folia verificado (SHA-256 abajo, de
# fill.papermc.io) y, para no descargarlas otra vez, libraries/, versions/ y cache/ de un arranque
# anterior. El mundo principal se genera con WorldForge (void-platform), como en levantar.sh.
#
#   JAR=<jar de desarrollo> bash worldforge/test-server/levantar-folia.sh && bash worldforge/test-server/escenario-folia.sh
set -euo pipefail

AQUI=$(cd "$(dirname "$0")" && pwd)
RPGROLL=$(cd "$AQUI/../.." && pwd)
CACHE=${E2E:-$AQUI/../../../SackitoNetwork/e2e/.work}
FC=$AQUI/.cache/folia
FOLIA=folia-26.1.2-8.jar
SHA=607afd1c3320008e1ffd2eaee6780ace4419d5f8c527b75e79f259be79ebf57b
S=$AQUI/.work
C=wf-test

echo "$SHA  $FC/$FOLIA" | sha256sum -c --quiet || { echo "falta $FC/$FOLIA o no es el verificado"; exit 1; }
docker rm -f $C >/dev/null 2>&1 || true
rm -rf "$S"
mkdir -p "$S/plugins"
cp "$FC/$FOLIA" "$S/server.jar"
for d in libraries versions cache; do [ -d "$FC/$d" ] && cp -r "$FC/$d" "$S/"; done
cp "$CACHE/trust.p12" "$S/"
python - "${JAR:-$RPGROLL/worldforge/build/libs/RPGRoll-WorldForge-1.0.0.jar}" "$(cygpath -w "$S/plugins/RPGRoll-WorldForge-folia-prueba.jar")" <<'EOF'
import sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "plugin.yml" and b"folia-supported" not in data:
            data += b"\n# Solo en la copia de prueba de levantar-folia.sh:\nfolia-supported: true\n"
        zout.writestr(item, data)
EOF
echo "eula=true" > "$S/eula.txt"   # EULA aceptada por Sack para los servidores de prueba (2026-09-27)
printf 'online-mode=false\nserver-port=25565\nlevel-seed=20261020\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\n' > "$S/server.properties"
# MAIN=<preset>: el del mundo principal (por defecto void-platform).
printf 'worlds:\n  world:\n    generator: RPGRoll-WorldForge:%s\n' "${MAIN:-void-platform}" > "$S/bukkit.yml"
TRUST="-Djavax.net.ssl.trustStore=/srv/trust.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
printf '#!/bin/sh\nrm -f /tmp/console; mkfifo /tmp/console\n(while true; do cat /tmp/console; done) | java %s -Xms1G -Xmx2G -Drpgroll.devmode=true -jar server.jar --nogui\n' "$TRUST" > "$S/run.sh"

MSYS_NO_PATHCONV=1 docker run -d --name $C -p 25602:25565 -v "$(cygpath -w "$S"):/srv" -w /srv \
    eclipse-temurin:25-jre sh /srv/run.sh >/dev/null
for _ in $(seq 1 150); do
    grep -q "Done (" "$S/logs/latest.log" 2>/dev/null && break
    sleep 2
done
grep "Done (" "$S/logs/latest.log" || { echo "Folia no arrancó en 5 min"; tail -30 "$S/logs/latest.log"; exit 1; }
