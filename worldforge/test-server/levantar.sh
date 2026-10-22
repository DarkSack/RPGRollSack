#!/usr/bin/env bash
# Paper 26.1.2 limpio en Docker (contenedor wf-test, puerto 25602) con el jar de DESARROLLO de
# WorldForge y LuckPerms (para probar permisos con un jugador). El mundo principal se genera con
# WorldForge desde bukkit.yml (preset void-platform), para probar ese camino también.
#
# Reutiliza la caché del e2e de SackitoNetwork (paper.jar, librerías, almacén de certificados):
# antes hay que haber corrido una vez E:/Dev/SackitoNetwork/e2e/levantar.sh.
#
#   cd E:/Dev/RPGRoll && ./gradlew -p worldforge assemble -Prpgroll.dev=true -Prpgroll.allowUnassignedIds
#   bash worldforge/test-server/levantar.sh && bash worldforge/test-server/escenario.sh
#
# Todo lo de .work se borra en cada arranque: son datos de prueba desechables.
# Después, recompilar SIN -Prpgroll.dev: ese jar se salta la licencia con -Drpgroll.devmode.
set -euo pipefail

AQUI=$(cd "$(dirname "$0")" && pwd)
RPGROLL=$(cd "$AQUI/../.." && pwd)
CACHE=${E2E:-$AQUI/../../../SackitoNetwork/e2e/.work}
S=$AQUI/.work
C=wf-test

docker rm -f $C >/dev/null 2>&1 || true
rm -rf "$S"
mkdir -p "$S/plugins"
cp "$CACHE/cache/paper.jar" "$CACHE/trust.p12" "$S/"
cp -r "$CACHE/survival/libraries" "$CACHE/survival/versions" "$CACHE/survival/cache" "$S/"
cp "${JAR:-$RPGROLL/worldforge/build/libs/RPGRoll-WorldForge-0.1.0.jar}" "$CACHE/cache/LuckPerms-Bukkit-5.5.71.jar" "$S/plugins/"
echo "eula=true" > "$S/eula.txt"   # EULA aceptada por Sack para los servidores de prueba (2026-09-27)
printf 'online-mode=false\nserver-port=25565\nlevel-seed=20261020\nspawn-protection=0\nview-distance=4\nsimulation-distance=4\n' > "$S/server.properties"
# MAIN=<preset>: el del mundo principal (por defecto void-platform).
printf 'worlds:\n  world:\n    generator: RPGRoll-WorldForge:%s\n' "${MAIN:-void-platform}" > "$S/bukkit.yml"
# DATAPACKS="a.zip b.zip": en el mundo desde el primer arranque (Terralith, solo para pruebas: su licencia no deja redistribuirlo).
for dp in ${DATAPACKS:-}; do mkdir -p "$S/world/datapacks" && cp "$dp" "$S/world/datapacks/"; done
TRUST="-Djavax.net.ssl.trustStore=/srv/trust.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
# MEM=4G: más memoria para el servidor (por defecto 2G).
printf '#!/bin/sh\nrm -f /tmp/console; mkfifo /tmp/console\n(while true; do cat /tmp/console; done) | java %s -Xms1G -Xmx%s -Drpgroll.devmode=true -jar paper.jar --nogui\n' "$TRUST" "${MEM:-2G}" > "$S/run.sh"

MSYS_NO_PATHCONV=1 docker run -d --name $C -p 25602:25565 -v "$(cygpath -w "$S"):/srv" -w /srv \
    eclipse-temurin:25-jre sh /srv/run.sh >/dev/null
for _ in $(seq 1 120); do
    grep -q "Done (" "$S/logs/latest.log" 2>/dev/null && break
    sleep 2
done
grep "Done (" "$S/logs/latest.log" || { echo "Paper no arrancó en 4 min"; tail -30 "$S/logs/latest.log"; exit 1; }
