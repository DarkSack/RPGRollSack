#!/usr/bin/env bash
# Escenario de la fase 5 (dimensiones por datapack) contra un wf-test recién levantado. Crea
# dimensiones, reinicia, comprueba que cargan con su tipo y generador, quita una y, al final,
# rompe a mano un JSON para dejar escrito qué hace Minecraft entonces.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
PACK=$S/world/datapacks/worldforge

echo "== Antes de reiniciar =="
paso "sin dimensiones" "wf dimension list" "ninguna; crea una"
paso "una combinación que no cabe" "wf dimension create x overworld nether" "No se pudo: generator: «nether» no cabe en la altura del tipo «overworld»"
paso "un nombre con ruta" "wf dimension create ../x overworld void" "No se pudo: id: «../x» no vale"
paso "crear minas (tall + overworld)" "wf dimension create minas tall overworld" "Dimensión minas escrita en el datapack"
paso "avisa de que hay que reiniciar" "wf dimension create vacio overworld void" "reinicia y aparecerá como el mundo world_worldforge_vacio"
paso "crear plana (caves + flat)" "wf dimension create plana caves flat" "Dimensión plana escrita"
paso "crear infierno (nether + nether)" "wf dimension create infierno nether nether" "Dimensión infierno escrita"
paso "crear islas (end + end)" "wf dimension create islas end end" "Dimensión islas escrita"
paso "repetida no" "wf dimension create minas overworld void" "No se pudo: id: ya existe la dimensión «minas» \(PENDING_RESTART\)"
[ -f "$PACK/pack.mcmeta" ] && [ -f "$PACK/data/worldforge/dimension/minas.json" ] && [ -f "$PACK/data/worldforge/dimension_type/tall.json" ] \
    && resultado PASS "archivos del datapack" "$(cd "$PACK" && find . -type f | sort | tr '\n' ' ')" || resultado FAIL "archivos del datapack" "$(find "$PACK" -type f 2>&1)"
paso "lista: esperan al reinicio" "wf dimension list" "minas · tall · overworld · PENDING_RESTART"
paso "todavía no existe el mundo" "wf world inspect world_worldforge_minas" "No se pudo"

echo "== Reinicio =="
reiniciar
grep -q "file/worldforge" "$LOG" && resultado PASS "el servidor encuentra el datapack" "$(grep -m1 'file/worldforge' "$LOG" | cut -c1-140)" \
    || resultado FAIL "el servidor encuentra el datapack" "-"
for d in minas vacio plana infierno islas; do
    linea=$(aparece "Dimensión $d: READY" 30 1) && resultado PASS "$d queda READY" "$linea" || resultado FAIL "$d queda READY" "-"
done
paso "aparecen como mundos" "wf world list" "world_worldforge_minas \[worldforge:minas\] cargado · DIMENSIÓN"
paso "minas usa el tipo alto" "wf world inspect world_worldforge_minas" "Altura -64 a 383"
paso "infierno usa el tipo del Nether" "wf world inspect world_worldforge_infierno" "Altura 0 a 255"
for d in minas vacio plana infierno islas; do forzar worldforge:$d 0 0; done
forzar worldforge:plana 80 80   # el chunk 5,5 (fuera del radio del spawn), para el plan de regeneración de más abajo
sleep 8
paso "minas: se puede construir en y=380 (no cabe en un mundo normal)" "execute in worldforge:minas run setblock 0 380 0 minecraft:glass" "Changed the block"
bloque "minas: fondo de bedrock del terreno normal" worldforge:minas 0 -64 0 minecraft:bedrock
bloque "vacio: nada" worldforge:vacio 0 64 0 minecraft:air
prueba "vacio: bioma the_void" "execute in worldforge:vacio if biome 0 64 0 minecraft:the_void"
bloque "plana: bedrock abajo" worldforge:plana 3 -64 3 minecraft:bedrock
bloque "plana: hierba arriba" worldforge:plana 3 -61 3 minecraft:grass_block
bloque "infierno: techo de bedrock" worldforge:infierno 0 127 0 minecraft:bedrock
bloque "infierno: suelo de bedrock" worldforge:infierno 0 0 0 minecraft:bedrock
prueba "islas: bioma del End" "execute in worldforge:islas if biome 0 64 0 minecraft:the_end"
paso "regenerar en una dimensión de datapack espera al reinicio" "wf regen plan world_worldforge_plana area 80 80 95 95" "Para confirmar: /wf regen confirm job-" 30
tail -n 12 "$LOG" | grep -q "PRÓXIMO ARRANQUE" && resultado PASS "el plan lo dice" "-" || resultado FAIL "el plan lo dice" "-"
read -r JOB _ <<< "$(grep -oE 'job-[0-9a-f]{8} [0-9a-f]{6}' <<< "$ULTIMA")"
paso "(se descarta ese plan)" "wf regen cancel $JOB" "cancelado"
paso "el diagnóstico lista las dimensiones" "wf diagnostics" "Dimensión minas \(tall, overworld\): READY · cargada como world_worldforge_minas"

echo "== Quitar una =="
paso "quitar vacio" "wf dimension remove vacio" "Dimensión vacio: PENDING_REMOVAL — definición quitada; deja de cargarse al reiniciar"
[ ! -f "$PACK/data/worldforge/dimension/vacio.json" ] && resultado PASS "su definición ya no está" "-" || resultado FAIL "su definición ya no está" "-"
reiniciar
linea=$(aparece "Dimensión vacio: REMOVED" 30 1) && resultado PASS "tras reiniciar: REMOVED" "$linea" || resultado FAIL "tras reiniciar: REMOVED" "-"
paso "ya no es un mundo" "wf world inspect world_worldforge_vacio" "No se pudo"
[ -d "$S/world/dimensions/worldforge/vacio" ] && resultado PASS "su carpeta se conserva" "world/dimensions/worldforge/vacio" \
    || resultado FAIL "su carpeta se conserva" "$(ls "$S/world/dimensions/worldforge" 2>&1)"
paso "se puede volver a crear" "wf dimension create vacio overworld void" "Dimensión vacio escrita"

echo "== JSON roto a mano (qué hace Minecraft) =="
printf '{"type":"minecraft:no_existe","generator":{"type":"minecraft:flat"}}\n' > "$PACK/data/worldforge/dimension/vacio.json"
parar
docker restart $C >/dev/null
for _ in $(seq 1 60); do grep -qE "Done \(|Failed to load datapacks|Stopping server" "$LOG" 2>/dev/null && break; sleep 2; done
if grep -q "Done (" "$LOG"; then
    resultado PASS "con un JSON roto el servidor arranca igualmente" "$(grep -m1 -iE 'vacio|no_existe' "$LOG" | cut -c1-160)"
    linea=$(aparece "Dimensión vacio: ERROR" 30 1) && resultado PASS "y WorldForge la marca ERROR" "$linea" || resultado FAIL "y WorldForge la marca ERROR" "-"
else
    resultado PASS "con un JSON roto Minecraft no arranca (documentado)" "$(grep -m1 -iE 'Failed to load datapacks|no_existe' "$LOG" | cut -c1-200)"
    rm -f "$PACK/data/worldforge/dimension/vacio.json"
    mv "$LOG" "$S/logs/antes-roto.log"
    arrancar
fi

echo "== Registro =="
registro_limpio
echo "FIN escenario-dim: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
