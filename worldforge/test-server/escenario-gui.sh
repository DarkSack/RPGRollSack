#!/usr/bin/env bash
# Escenario de la fase 6 (GUI) contra un wf-test recién levantado: prepara un mundo con una marca
# y deja que gui.js recorra los menús con dos jugadores. Luego comprueba el efecto desde la consola.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH

echo "== Preparación =="
paso "crear t_gui (flat)" "wf world create t_gui flat" "Mundo t_gui creado" 90
paso "forzar el chunk 6,6" "execute in minecraft:t_gui run forceload add 100 100" "force loaded|Marked"
sleep 3
paso "marca en 100 -60 100" "execute in minecraft:t_gui run setblock 100 -60 100 minecraft:gold_block" "Changed the block"
paso "guardar" "save-all flush" "Saved the game" 60

echo "== Jugadores en la GUI =="
node "$AQUI/gui.js"
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))

echo "== Efecto =="
sleep 3
bloque "la regeneración desde el menú quitó la marca" minecraft:t_gui 100 -60 100 minecraft:air
bloque "y dejó las capas planas" minecraft:t_gui 100 -61 100 minecraft:grass_block
paso "el preset publicado desde el menú existe" "wf preset info gui-flat" "Semilla 888"
paso "la zona creada y quitada ya no está" "wf protection list" "Alrededor de cada aparición"
grep -q "casa-gui" "$DATA/protections.yml" && resultado FAIL "protections.yml sin casa-gui" "sigue ahí" || resultado PASS "protections.yml sin casa-gui" "-"
registro_limpio
echo "FIN escenario-gui: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
