#!/usr/bin/env bash
# Escenario de la fase 7 (regeneración, protección, respaldo y recuperación) contra un wf-test
# recién levantado con levantar.sh. Marca bloques con setblock, regenera y comprueba qué cambió.
# El "apagado a mitad" se simula con el servidor parado, poniendo el trabajo en RUNNING en la BD.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH

trabajo() { grep -oE 'job-[0-9a-f]{8} [0-9a-f]{6}' <<< "$ULTIMA"; } # "id código" del último plan
guardar() { paso "guardar el mundo" "save-all flush" "Saved the game" 60; }

echo "== Preparación: mundo plano con tres marcas =="
paso "inspect: el generador vacío no dice que haya estructuras" "wf world inspect world" "estructuras no"
paso "crear t_reg (flat)" "wf world create t_reg flat" "Mundo t_reg creado" 90
paso "forzar los chunks 5..7,5" "execute in minecraft:t_reg run forceload add 80 80 127 95" "force loaded|forceloaded|Marked"
sleep 3
for x in 85 101 117; do
    paso "marca en $x -60 85" "execute in minecraft:t_reg run setblock $x -60 85 minecraft:gold_block" "Changed the block"
done
paso "proteger el chunk 6,5 (casa)" "wf protection add casa t_reg 96 80 111 95 la casa de alguien" "Zona casa protegida en t_reg"
paso "una zona con el mismo nombre no" "wf protection add casa t_reg 0 0 1 1" "No se pudo: id: ya hay una zona «casa»"
guardar

echo "== Plan y confirmación =="
paso "planificar 5..6,5" "wf regen plan t_reg area 80 80 111 95" "Chunks a regenerar: 1 · protegidos \(no se tocan\): 1 · nunca generados \(se ignoran\): 0" 30
paso "el plan da el código" "wf regen list" "regen t_reg · PLANNED · 0/1"
ULTIMA=$(grep -E "Para confirmar: /wf regen confirm job-" "$LOG" | tail -1)
read -r JOB TOKEN <<< "$(trabajo)"
[ -n "${JOB:-}" ] && resultado PASS "leer trabajo y código del plan" "$JOB $TOKEN" || resultado FAIL "leer trabajo y código del plan" "$ULTIMA"
paso "un código equivocado no confirma" "wf regen confirm $JOB 000000" "No se pudo: token: el código no coincide"

node "$AQUI/visitante.js" 240 > "$S/visitante.out" 2>&1 &
for _ in $(seq 1 30); do grep -q "WFBOT dentro" "$S/visitante.out" && break; sleep 1; done
paso "un jugador entra en t_reg" "execute in minecraft:t_reg run tp Visitante 85 -59 85" "Teleported Visitante"
sleep 2
prueba "el jugador está en t_reg" "execute in minecraft:t_reg positioned 85 -59 85 if entity @a[name=Visitante,distance=..3]"

desde=$(($(lineas) + 1))
consola "wf regen confirm $JOB $TOKEN"
consola "wf regen confirm $JOB $TOKEN"
if linea=$(espera "Confirmado: se descarga el mundo" 20 "$desde") && [[ "$linea" == *Confirmado* ]]; then
    resultado PASS "confirmar con el código" "$linea"
else
    resultado FAIL "confirmar con el código" "${linea:-nada}"
fi
linea=$(aparece "$JOB: 1 chunks regenerados" 90 "$desde")
[[ "$linea" == *"vuelve a estar cargado"* ]] && resultado PASS "termina y vuelve a cargar el mundo" "$linea" \
    || resultado FAIL "termina y vuelve a cargar el mundo" "${linea:-nada}"
aparece "ya no espera confirmación|otra persona lo confirmó" 10 "$desde" >/dev/null
n=$(tail -n +"$desde" "$LOG" | grep -cE "ya no espera confirmación|otra persona lo confirmó")
k=$(tail -n +"$desde" "$LOG" | grep -cE "Confirmado: se descarga el mundo")
[ "$n" = 1 ] && [ "$k" = 1 ] && resultado PASS "el doble clic no lo lanza dos veces" "1 confirmado, 1 rechazado"     || resultado FAIL "el doble clic no lo lanza dos veces" "confirmados=$k rechazos=$n"
prueba "al jugador lo llevaron a la aparición principal" "execute in minecraft:overworld positioned 0 65 0 if entity @a[name=Visitante,distance=..5]"
sleep 4
bloque "chunk 5,5 regenerado: la marca ya no está" minecraft:t_reg 85 -60 85 minecraft:air
bloque "chunk 5,5 regenerado: capas planas" minecraft:t_reg 85 -61 85 minecraft:grass_block
bloque "chunk 6,5 protegido: conserva la marca" minecraft:t_reg 101 -60 85 minecraft:gold_block
bloque "chunk 7,5 fuera del plan: conserva la marca" minecraft:t_reg 117 -60 85 minecraft:gold_block
paso "el trabajo queda COMPLETED" "wf regen info $JOB" "Trabajo $JOB \(regen\) · COMPLETED"
bak=$(ls -d "$DATA"/backups/bak-* 2>/dev/null | head -1)
[ -n "$bak" ] && [ -s "$bak/manifest.txt" ] && resultado PASS "respaldo con manifiesto" "$(basename "$bak"): $(wc -l < "$bak/manifest.txt") archivo(s)" \
    || resultado FAIL "respaldo con manifiesto" "no hay backups/bak-*/manifest.txt"

echo "== Deshacer con el respaldo =="
paso "planificar la restauración" "wf regen restore $JOB" "Para confirmar: /wf regen confirm job-" 30
read -r JOBR TOKENR <<< "$(trabajo)"
desde=$(($(lineas) + 1))
paso "confirmar la restauración" "wf regen confirm $JOBR $TOKENR" "Confirmado: se descarga el mundo"
linea=$(aparece "$JOBR: [0-9]+ archivos devueltos al respaldo" 90 "$desde")
[[ "$linea" == *"vuelve a estar cargado"* ]] && resultado PASS "restaura y vuelve a cargar" "$linea" || resultado FAIL "restaura y vuelve a cargar" "${linea:-nada}"
sleep 4
bloque "la marca del chunk 5,5 vuelve" minecraft:t_reg 85 -60 85 minecraft:gold_block

echo "== Cancelar y caducar =="
paso "otro plan" "wf regen plan t_reg area 80 80 95 95" "Para confirmar: /wf regen confirm job-" 30
read -r JOBC TOKENC <<< "$(trabajo)"
paso "cancelarlo" "wf regen cancel $JOBC" "$JOBC cancelado; no se tocó nada"
paso "uno cancelado no se confirma" "wf regen confirm $JOBC $TOKENC" "ya no espera confirmación \(está en CANCELLED\)"
sed -i 's/confirm-minutes: 15/confirm-minutes: 0/' "$DATA/config.yml"
paso "recargar con 0 minutos" "wf reload-config" "recargados"
paso "plan que caduca" "wf regen plan t_reg area 80 80 95 95" "Para confirmar: /wf regen confirm job-" 30
read -r JOBE TOKENE <<< "$(trabajo)"
sleep 1
paso "un plan caducado no se confirma" "wf regen confirm $JOBE $TOKENE" "No se pudo: job: el plan caducó"
sed -i 's/confirm-minutes: 0/confirm-minutes: 15/' "$DATA/config.yml"
paso "recargar con 15 minutos" "wf reload-config" "recargados"

echo "== protections.yml roto bloquea la regeneración =="
cp "$DATA/protections.yml" "$S/protections.bien"
printf 'schema: 1\nareas: [esto no\n' > "$DATA/protections.yml"
paso "recargar avisa del error" "wf reload-config" "protections.yml"
paso "y no deja planificar" "wf regen plan t_reg all" "No se pudo: protections: protections.yml tiene un error" 30
paso "el diagnóstico lo dice" "wf diagnostics" "✘ Regeneración BLOQUEADA"
grep -q "esto no" "$DATA/protections.yml" && resultado PASS "el archivo roto no se reescribe" "-" || resultado FAIL "el archivo roto no se reescribe" "$(cat "$DATA/protections.yml")"
cp "$S/protections.bien" "$DATA/protections.yml"
paso "arreglado y recargado" "wf reload-config" "recargados"
paso "la zona sigue" "wf protection list" "casa · t_reg · 96, 80 → 111, 95"

echo "== Mundo principal: espera al reinicio =="
forzar minecraft:overworld 165 165
sleep 3
paso "marca en el principal" "execute in minecraft:overworld run setblock 165 64 165 minecraft:gold_block" "Changed the block"
guardar
paso "planificar en el principal" "wf regen plan world radius 165 165 0" "Para confirmar: /wf regen confirm job-" 30
read -r JOBM TOKENM <<< "$(trabajo)"
tail -n 12 "$LOG" | grep -q "PRÓXIMO ARRANQUE" && resultado PASS "el plan avisa de que es al reiniciar" "-" || resultado FAIL "el plan avisa de que es al reiniciar" "-"
paso "confirmar" "wf regen confirm $JOBM $TOKENM" "Se aplicará en el PRÓXIMO ARRANQUE"
paso "queda esperando" "wf regen list" "$JOBM regen world · PENDING_RESTART"
bloque "todavía no se tocó" minecraft:overworld 165 64 165 minecraft:gold_block
reiniciar
l_job=$(grep -n "$JOBM (regen, world): 1 chunks regenerados" "$LOG" | head -1 | cut -d: -f1)
l_mundo=$(grep -n 'Preparing level "world"' "$LOG" | head -1 | cut -d: -f1)
[ -n "$l_job" ] && [ -n "$l_mundo" ] && [ "$l_job" -lt "$l_mundo" ] \
    && resultado PASS "se aplica al arrancar, antes de abrir el mundo" "línea $l_job < línea $l_mundo" \
    || resultado FAIL "se aplica al arrancar, antes de abrir el mundo" "trabajo=${l_job:-?} mundo=${l_mundo:-?}"
forzar minecraft:overworld 0 0
sleep 3
bloque "la marca del principal desapareció" minecraft:overworld 165 64 165 minecraft:air
bloque "la plataforma de aparición sigue" minecraft:overworld 0 64 0 minecraft:stone

echo "== Apagado a mitad (simulado) y recuperación =="
parar
python -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); c.execute(\"UPDATE jobs SET state='RUNNING' WHERE id=?\", (sys.argv[2],)); c.commit(); print(c.total_changes)" \
    "$(cygpath -w "$DATA/worldforge.db")" "$JOBM" | grep -q 1 && resultado PASS "simular: el trabajo quedó en RUNNING" "$JOBM" || resultado FAIL "simular: el trabajo quedó en RUNNING" "-"
arrancar
grep -q "$JOBM estaba en RUNNING al apagarse" "$LOG" && resultado PASS "al arrancar lo detecta" "$(grep -m1 "$JOBM estaba" "$LOG" | cut -c1-160)" \
    || resultado FAIL "al arrancar lo detecta" "-"
paso "no se reanuda: NEEDS_RECOVERY" "wf regen info $JOBM" "Trabajo $JOBM \(regen\) · NEEDS_RECOVERY"
paso "el diagnóstico lo marca" "wf diagnostics" "✘ Trabajo $JOBM \(regen, world\): NEEDS_RECOVERY"
paso "el mundo queda bloqueado" "wf regen plan world radius 165 165 0" "No se pudo: world: el mundo tiene un trabajo en marcha o pendiente: $JOBM \(NEEDS_RECOVERY\)" 30
paso "cancel no sirve para esto" "wf regen cancel $JOBM" "para este usa restore o accept"
paso "planificar la restauración" "wf regen restore $JOBM" "Para confirmar: /wf regen confirm job-" 30
read -r JOBR2 TOKENR2 <<< "$(trabajo)"
paso "confirmarla (al reiniciar)" "wf regen confirm $JOBR2 $TOKENR2" "Se aplicará en el PRÓXIMO ARRANQUE"
reiniciar
grep -q "$JOBR2 (restore, world): [0-9]* archivos devueltos" "$LOG" && resultado PASS "restaurado al arrancar" "$(grep -m1 "$JOBR2 (restore" "$LOG" | cut -c1-160)" \
    || resultado FAIL "restaurado al arrancar" "-"
sleep 3
forzar minecraft:overworld 165 165
sleep 3
bloque "la marca del principal vuelve" minecraft:overworld 165 64 165 minecraft:gold_block
paso "el trabajo original queda RESTORED" "wf regen info $JOBM" "Trabajo $JOBM \(regen\) · RESTORED"
paso "y el mundo se desbloquea" "wf regen plan world radius 165 165 0" "Para confirmar: /wf regen confirm job-" 30
read -r JOBX _ <<< "$(trabajo)"
paso "(se cancela ese plan)" "wf regen cancel $JOBX" "cancelado"

echo "== Registro =="
registro_limpio
kill %1 2>/dev/null
echo "FIN escenario-regen: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
