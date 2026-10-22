#!/usr/bin/env bash
# Escenario de la fase 8 contra un Folia recién levantado con levantar-folia.sh: lo que Folia no
# permite tiene que decirlo con un mensaje (no lanzar excepciones) y lo demás tiene que funcionar:
# presets, diagnóstico, protecciones, plantillas, dimensiones por datapack y regeneración al arrancar.
# Los bloques los pone y los mira un jugador (folia.js): en Folia la consola no toca el mundo.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH
trabajo() { grep -oE 'job-[0-9a-f]{8} [0-9a-f]{6}' <<< "$ULTIMA"; }

echo "== Arranque =="
grep -q "WorldForge habilitado en Folia" "$LOG" && resultado PASS "se habilita en Folia" "$(grep -m1 'WorldForge habilitado' "$LOG" | cut -c1-160)" \
    || resultado FAIL "se habilita en Folia" "$(grep -m3 -iE 'worldforge|folia-supported' "$LOG" | tr '\n' ' ' | cut -c1-200)"
paso "diagnóstico" "wf diagnostics" "Plataforma: Folia — crear, cargar y descargar mundos en caliente están DESACTIVADOS"
paso "SQLite" "wf diagnostics" "✔ SQLite: esquema 2"
paso "el mundo principal lo genera WorldForge" "wf world inspect world" "Generador: WorldForgeGenerator"
paso "lista de mundos" "wf world list" "world \[minecraft:overworld\]"

echo "== Lo que Folia no permite, explicado =="
paso "crear un mundo" "wf world create t_x flat" "No se pudo: Folia no permite crear mundos"
paso "descargar un mundo" "wf world unload world" "No se pudo: Folia no permite descargar mundos"

echo "== Presets, protecciones y plantillas =="
paso "duplicar" "wf preset duplicate flat f-copia" "f-copia"
paso "semilla" "wf preset seed f-copia 99" "Hecho\."
paso "publicar" "wf preset publish f-copia" "Borrador f-copia publicado"
paso "exportar" "wf preset export f-copia" "f-copia"
paso "zona protegida" "wf protection add casa world -16 -16 15 15 prueba" "Zona casa protegida en world"
paso "lista de zonas" "wf protection list" "casa · world"
paso "quitarla" "wf protection remove casa" "Zona casa ya no está protegida"
paso "lista de plantillas vacía" "wf structure list" "ninguna"

echo "== Menú con un jugador =="
node "$AQUI/folia.js" gui
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))

echo "== Dimensión por datapack =="
paso "crear" "wf dimension create minas tall overworld" "Dimensión minas escrita en el datapack"
paso "crear una con terreno" "wf dimension create alpes towering terrain:montanas" "Dimensión alpes escrita en el datapack"
paso "lista de terrenos" "wf terrain list" "mundo principal: vanilla"

echo "== Regeneración del principal (al arrancar) =="
node "$AQUI/folia.js" marca
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))
# Folia no tiene save-all: lo recién generado llega a disco al parar (o en el autoguardado).
consola "save-all flush"
sleep 2
grep -q "save-all flush<--\[HERE\]" "$LOG" && resultado PASS "Folia no tiene save-all (documentado)" "-" || resultado FAIL "Folia no tiene save-all (documentado)" "$(tail -3 "$LOG" | tr '\n' ' ')"
reiniciar
paso "planificar" "wf regen plan world radius 165 165 0" "Para confirmar: /wf regen confirm job-" 30
read -r JOB TOKEN <<< "$(trabajo)"
tail -n 12 "$LOG" | grep -q "PRÓXIMO ARRANQUE" && resultado PASS "en Folia siempre espera al reinicio" "-" || resultado FAIL "en Folia siempre espera al reinicio" "-"
paso "confirmar" "wf regen confirm $JOB $TOKEN" "Se aplicará en el PRÓXIMO ARRANQUE"

echo "== Reinicio =="
reiniciar
grep -q "$JOB (regen, world): 1 chunks regenerados" "$LOG" && resultado PASS "la regeneración se aplicó al arrancar" "$(grep -m1 "$JOB (regen" "$LOG" | cut -c1-160)" \
    || resultado FAIL "la regeneración se aplicó al arrancar" "$(grep -m3 "$JOB" "$LOG" | tr '\n' ' ' | cut -c1-200)"
paso "el trabajo queda COMPLETED" "wf regen info $JOB" "Trabajo $JOB \(regen\) · COMPLETED"
# Queda READY en el primer arranque tras crearla, que ya está en un registro anterior.
linea=$(cat "$S"/logs/antes-*.log "$LOG" | grep -m1 "Dimensión minas: READY") && resultado PASS "la dimensión carga en Folia" "$linea"     || resultado FAIL "la dimensión carga en Folia" "-"
paso "con su altura" "wf world inspect world_worldforge_minas" "Altura -64 a 383"
linea=$(cat "$S"/logs/antes-*.log "$LOG" | grep -m1 "Dimensión alpes: READY") && resultado PASS "la de terreno carga en Folia" "$linea" \
    || resultado FAIL "la de terreno carga en Folia" "-"
paso "con el tipo towering" "wf world inspect world_worldforge_alpes" "Altura -64 a 575"
node "$AQUI/folia.js" mira
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))

echo "== Registro =="
registro_limpio
echo "FIN escenario-folia: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
