#!/usr/bin/env bash
# Escenario de la fase 3 contra wf-test (lo arranca levantar.sh). Cada paso manda un comando por la
# consola y espera su respuesta en logs/latest.log; los bloques se comprueban con comandos vanilla
# (execute if block / if biome, y fill ... replace para contar). Imprime una línea WFTEST por paso
# y termina con código 1 si alguno falla. Todo ocurre en mundos de prueba desechables.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
SEMILLA=20261020

echo "== Arranque =="
grep -q "Enabling RPGRoll-WorldForge" "$LOG" && ! grep -q "Error occurred while enabling RPGRoll-WorldForge" "$LOG" \
    && resultado PASS "WorldForge se habilita" "$(grep -m1 'Enabling RPGRoll-WorldForge' "$LOG")" \
    || resultado FAIL "WorldForge se habilita" "mira $LOG"
n=$(ls "$DATA"/presets/*.yml 2>/dev/null | wc -l)
[ -f "$DATA/config.yml" ] && [ -f "$DATA/messages.yml" ] && [ -f "$DATA/worldforge.db" ] && [ "$n" = 10 ] \
    && resultado PASS "crea config, textos, BD y 10 presets" "$(ls "$DATA" | tr '\n' ' ')" \
    || resultado FAIL "crea config, textos, BD y 10 presets" "$(ls "$DATA" "$DATA/presets" 2>&1 | tr '\n' ' ')"
paso "diagnóstico: SQLite desde el plugin" "wf diagnostics" "✔ SQLite: esquema 2"
paso "diagnóstico: presets" "wf diagnostics" "Presets: 10 utilizables, 0 borradores, 0 con errores"
paso "lista de presets" "wf preset list" "resource-rich — Rico en recursos · hybrid · listo"

echo "== Mundo principal desde bukkit.yml (void-platform) =="
paso "el mundo principal usa el generador de WorldForge" "wf world inspect world" "Generador: WorldForgeGenerator"
paso "aparición del preset" "wf world inspect world" "Aparición 0, 65, 0"
forzar minecraft:overworld 0 0
sleep 3
bloque "plataforma en el centro" minecraft:overworld 0 64 0 minecraft:stone
bloque "plataforma en la esquina (radio 2)" minecraft:overworld -2 64 2 minecraft:stone
bloque "fuera de la plataforma no hay nada" minecraft:overworld 3 64 0 minecraft:air
bloque "debajo, vacío hasta el fondo" minecraft:overworld 0 -64 0 minecraft:air
prueba "bioma the_void" "execute in minecraft:overworld if biome 8 64 8 minecraft:the_void"

echo "== Mundos nuevos: vacío, plataforma, plano =="
paso "crear t_empty (empty)" "wf world create t_empty empty" "Mundo t_empty creado con el preset empty" 90
paso "crear t_plat (void-platform)" "wf world create t_plat void-platform" "Mundo t_plat creado con el preset void-platform" 90
paso "crear t_flat (flat)" "wf world create t_flat flat" "Mundo t_flat creado con el preset flat" 90
forzar minecraft:t_empty 0 0
forzar minecraft:t_plat 0 0
forzar minecraft:t_flat 0 0
sleep 3
bloque "t_empty: nada en la aparición" minecraft:t_empty 0 64 0 minecraft:air
bloque "t_empty: nada en el fondo" minecraft:t_empty 5 -64 5 minecraft:air
prueba "t_empty: bioma the_void" "execute in minecraft:t_empty if biome 0 64 0 minecraft:the_void"
bloque "t_plat: plataforma" minecraft:t_plat 2 64 -2 minecraft:stone
bloque "t_plat: borde" minecraft:t_plat 0 64 3 minecraft:air
bloque "t_flat: bedrock abajo" minecraft:t_flat 5 -64 5 minecraft:bedrock
bloque "t_flat: tierra" minecraft:t_flat 5 -62 5 minecraft:dirt
bloque "t_flat: hierba arriba" minecraft:t_flat 5 -61 5 minecraft:grass_block
bloque "t_flat: aire encima" minecraft:t_flat 5 -60 5 minecraft:air
prueba "t_flat: bioma plains" "execute in minecraft:t_flat if biome 5 -61 5 minecraft:plains"
paso "t_flat: aparición encima de las capas" "wf world inspect t_flat" "Aparición 0, -60, 0"

echo "== Borradores con semilla fija: vanilla, amplified y rico en recursos =="
for par in "vanilla v-seed" "amplified a-seed" "resource-rich r-seed"; do
    set -- $par
    paso "duplicar $1 como $2" "wf preset duplicate $1 $2" "Borrador $2 creado a partir de $1"
    paso "$2: semilla $SEMILLA" "wf preset seed $2 $SEMILLA" "Hecho\."
    paso "$2: no crea mundos sin publicar" "wf world create t_sin_publicar $2" "no hay un preset publicado"
    paso "$2: publicar" "wf preset publish $2" "Borrador $2 publicado"
done
paso "crear t_vanilla" "wf world create t_vanilla v-seed" "Mundo t_vanilla creado con el preset v-seed \(semilla $SEMILLA\)" 120
paso "crear t_amp" "wf world create t_amp a-seed" "Mundo t_amp creado con el preset a-seed \(semilla $SEMILLA\)" 120
paso "crear t_rich" "wf world create t_rich r-seed" "Mundo t_rich creado con el preset r-seed \(semilla $SEMILLA\)" 120
SITIOS="0 0 320 0 0 320 -320 -320"
for m in t_vanilla t_amp t_rich; do forzar minecraft:$m $SITIOS; done
sleep 15
alto_v=0; alto_a=0; dia_v=0; dia_r=0; oro_v=0; oro_r=0
set -- $SITIOS
while [ $# -gt 0 ]; do
    alto_v=$((alto_v + $(cuenta minecraft:t_vanilla $1 $2 130 249 minecraft:stone)))
    alto_a=$((alto_a + $(cuenta minecraft:t_amp $1 $2 130 249 minecraft:stone)))
    dia_v=$((dia_v + $(cuenta minecraft:t_vanilla $1 $2 -64 -17 minecraft:deepslate_diamond_ore)))
    dia_r=$((dia_r + $(cuenta minecraft:t_rich $1 $2 -64 -17 minecraft:deepslate_diamond_ore)))
    oro_v=$((oro_v + $(cuenta minecraft:t_vanilla $1 $2 -64 -17 minecraft:deepslate_gold_ore)))
    oro_r=$((oro_r + $(cuenta minecraft:t_rich $1 $2 -64 -17 minecraft:deepslate_gold_ore)))
    shift 2
done
[ $alto_a -gt $((alto_v * 3 + 500)) ] && r=PASS || r=FAIL
resultado $r "amplified: mucha más piedra entre y=130 y 249 (4 chunks)" "vanilla=$alto_v amplified=$alto_a"
# Mismo terreno y semilla: la diferencia es lo que añaden las venas. Se exige al menos la mitad del
# máximo configurado (venas × tamaño × 4 chunks), porque parte de cada vena cae en roca que no reemplaza.
[ $((dia_r - dia_v)) -ge $((2 * 6 * 4 / 2)) ] && r=PASS || r=FAIL
resultado $r "rico en recursos: diamante extra (2 venas × 6, máx. 48)" "vanilla=$dia_v rico=$dia_r extra=$((dia_r - dia_v))"
[ $((oro_r - oro_v)) -ge $((3 * 8 * 4 / 2)) ] && r=PASS || r=FAIL
resultado $r "rico en recursos: oro extra (3 venas × 8, máx. 96)" "vanilla=$oro_v rico=$oro_r extra=$((oro_r - oro_v))"

echo "== Exportar e importar =="
paso "exportar flat" "wf preset export flat" "Exportado a exports.flat\.yml"
sha=$(sha256sum "$DATA/exports/flat.yml" | cut -d' ' -f1)
grep -q "sha256: $sha" "$DATA/exports/flat.manifest" && resultado PASS "el manifiesto lleva el SHA-256 del archivo" "$sha" \
    || resultado FAIL "el manifiesto lleva el SHA-256 del archivo" "$(cat "$DATA/exports/flat.manifest")"
sed -e 's/^id: flat$/id: flat-copia/' "$DATA/exports/flat.yml" > "$DATA/imports/flat-copia.yml"
paso "importar una copia" "wf preset import flat-copia.yml" "Importado como borrador flat-copia"
paso "publicar la copia importada" "wf preset publish flat-copia" "Borrador flat-copia publicado"
printf 'schema: 1\nid: [roto\n' > "$DATA/imports/roto.yml"
paso "importar YAML roto" "wf preset import roto.yml" "No se pudo: .*YAML mal formado"
{ printf 'schema: 1\n'; head -c 300000 /dev/zero | tr '\0' '#'; } > "$DATA/imports/enorme.yml"
paso "importar un archivo demasiado grande" "wf preset import enorme.yml" "No se pudo: .*límite"
paso "importar con ruta fuera de imports" "wf preset import ../presets/flat.yml" "No se pudo:"
printf 'schema: 1\nid: raro\nprovider: vanilla\nbiomes: [mordor]\n' > "$DATA/imports/raro.yml"
paso "importar un preset con errores lo deja como borrador" "wf preset import raro.yml" "Importado como borrador raro"
paso "y ese borrador no se publica" "wf preset publish raro" "No se pudo: .*(biomes|bioma)"

echo "== Negativos =="
paso "nombre ya usado" "wf world create t_flat flat" "No se pudo: name: ya hay un mundo administrado"
paso "nombre con ruta" "wf world create ../malo empty" "No se pudo: name: .*no vale como nombre"
paso "nombre con guion" "wf world create t_con-guion empty" "No se pudo: name: .*no vale como nombre"
paso "preset que no existe" "wf world create t_x nada" "No se pudo: preset: no hay un preset publicado «nada»"
paso "cargar un mundo que no es de WorldForge" "wf world load nada" "No se pudo: name: «nada» no es un mundo administrado"
paso "semilla en un preset publicado" "wf preset seed vanilla 5" "No se pudo: la semilla solo se cambia en un borrador"
paso "subcomando desconocido" "wf hechizo" "No conozco «hechizo»"
paso "faltan argumentos" "wf world create" "Uso: /wf world create <nombre> <preset>"
paso "aplicar a un mundo del servidor explica bukkit.yml" "wf generation apply world flat" "bukkit.yml"

echo "== Descargar y volver a cargar =="
paso "descargar t_empty" "wf world unload t_empty" "Mundo t_empty descargado y guardado" 60
paso "lista: t_empty sin cargar" "wf world list" "t_empty \[minecraft:t_empty\] sin cargar"
paso "cargar t_empty" "wf world load t_empty" "Mundo t_empty cargado" 60
paso "cambiar el preset de t_plat a flat" "wf generation apply t_plat flat" "Mundo recargado: los chunks nuevos ya usan «flat»" 60

echo "== Permisos con un jugador sin op =="
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules} node "$AQUI/jugador.js"
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))

echo "== Reinicio =="
reiniciar
paso "t_flat vuelve cargado" "wf world list" "t_flat \[minecraft:t_flat\] cargado · READY · preset flat"
paso "t_rich vuelve cargado" "wf world list" "t_rich \[minecraft:t_rich\] cargado · READY · preset r-seed"
paso "t_plat vuelve con su preset nuevo" "wf world list" "t_plat \[minecraft:t_plat\] cargado · READY · preset flat"
forzar minecraft:t_flat 0 0
forzar minecraft:overworld 0 0
sleep 3
bloque "t_flat conserva sus bloques" minecraft:t_flat 5 -61 5 minecraft:grass_block
bloque "el principal conserva la plataforma" minecraft:overworld 0 64 0 minecraft:stone
paso "historial en el diagnóstico" "wf diagnostics" "Últimas operaciones:"

echo "== Registro =="
registro_limpio

echo "FIN escenario: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
