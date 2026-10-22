#!/usr/bin/env bash
# El motor de terreno propio como mundo principal (bukkit.yml), en Paper o en Folia, que en Folia es la
# única forma de usarlo (no se crean mundos en caliente) y donde los chunks se generan en paralelo:
#   MAIN=montanas bash worldforge/test-server/levantar.sh        (o levantar-folia.sh)
#   bash worldforge/test-server/escenario-motor.sh
# Un jugador entra y se queda: alrededor de él se generan chunks a la vez en varias regiones.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH

echo "== Mundo principal con el motor =="
paso "lo genera el motor con montanas" "wf world inspect world" "modo TERRAIN · terreno montanas"
paso "dónde aparece" "wf world inspect world" "Aparición"
read -r AX AY AZ <<< "$(grep -oE 'Aparición -?[0-9]+, -?[0-9]+, -?[0-9]+' <<< "$ULTIMA" | tr -d ',' | awk '{print $2, $3, $4}')"
[ "${AY:-0}" -gt 64 ] && resultado PASS "aparece en tierra firme (por encima del mar)" "$AX $AY $AZ" || resultado FAIL "aparece en tierra firme" "${AX:-} ${AY:-} ${AZ:-}"

echo "== Un jugador genera chunks alrededor =="
node "$AQUI/viajero.js" > "$S/viajero.out" 2>&1 &
VIAJERO=$!
for _ in $(seq 1 40); do grep -q "VIAJERO dentro" "$S/viajero.out" && break; sleep 1; done
grep -q "VIAJERO dentro" "$S/viajero.out" && resultado PASS "entra un jugador" "Viajero" || resultado FAIL "entra un jugador" "$(cat "$S/viajero.out")"
sleep 25
paso "los chunks de alrededor existen" "wf world inspect world" "chunks cargados ([5-9][0-9]|[1-9][0-9]{2,})"
# En 26.1 el chunk de aparición no se queda cargado solo: se mira con el jugador dentro. Folia no deja
# leer bloques desde la consola.
if ! grep -q "Folia" "$LOG"; then
    prueba "no aparece en el agua" "execute in minecraft:overworld unless block $AX $((AY - 1)) $AZ minecraft:water"
    n=$(cuenta minecraft:overworld "$AX" "$AZ" 0 127 minecraft:coal_ore)
    [ "$n" -gt 0 ] && resultado PASS "menas de Minecraft encima del motor (carbón en el chunk de aparición)" "$n" \
        || resultado FAIL "menas de Minecraft encima del motor" "$n"
fi
# Ir a otro mundo (en Folia, con teleportAsync de una región a otra).
paso "el Nether está cargado" "wf world list" "\[minecraft:the_nether\]"
NETHER=$(grep -oE '[A-Za-z0-9_]+ \[minecraft:the_nether\]' <<< "$ULTIMA" | awk '{print $1}')
paso "world tp al Nether" "wf world tp ${NETHER:-world_nether} Viajero" "Viajero → ${NETHER:-world_nether}"
sleep 3
paso "el jugador está en el Nether" "wf world inspect ${NETHER:-world_nether}" "jugadores 1"
kill $VIAJERO 2>/dev/null; wait $VIAJERO 2>/dev/null

echo "== Tras reiniciar =="
reiniciar
paso "sigue con el motor" "wf world inspect world" "modo TERRAIN · terreno montanas"

echo "== Registro =="
registro_limpio
echo "FIN escenario-motor: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
