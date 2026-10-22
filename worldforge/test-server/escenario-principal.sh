#!/usr/bin/env bash
# El mundo principal con plantillas y menas puestas en bukkit.yml, en Paper (levantar.sh) o en Folia
# (levantar-folia.sh). El jugador construye y guarda la plantilla; se publica un preset que la usa, se
# cambia el generador del mundo principal en bukkit.yml, se reinicia y el jugador mira lo generado lejos.
# Comprueba también que el generador se construye en el arranque (STARTUP), antes de abrir los mundos.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH
PLATAFORMA=$(grep -oE "habilitado en (Paper|Folia)" "$LOG" | head -1 | awk '{print $3}')
echo "== Plataforma: ${PLATAFORMA:-?} =="

echo "== Plantilla hecha por un jugador =="
node "$AQUI/principal.js" construir
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))
paso "desde la consola, en la región del chunk" "wf structure save fol2 3 66 3 5 67 5 world" "Plantilla fol2 guardada: 3×2×3"

echo "== Preset en el mundo principal =="
cat > "$DATA/imports/tpl-principal.yml" <<'EOF'
schema: 1
id: tpl-principal
provider: flat
layers: [{block: bedrock, height: 1}, {block: stone, height: 60}]
ores:
- {id: dia, block: diamond_ore, replaces: [stone], veins-per-chunk: 4, size: 6, min-y: -40, max-y: -10}
templates:
- {id: islas, structure: fol, rarity: 2, placement: underground, min-y: 100, max-y: 100, rotation: none}
EOF
paso "importar" "wf preset import tpl-principal.yml" "Importado como borrador tpl-principal"
paso "publicar" "wf preset publish tpl-principal" "Borrador tpl-principal publicado"
printf 'worlds:\n  world:\n    generator: RPGRoll-WorldForge:tpl-principal\n' > "$S/bukkit.yml"
reiniciar
linea=$(grep -n "Preparing level" "$LOG" | head -1 | cut -d: -f1)
habil=$(grep -n "WorldForge habilitado" "$LOG" | head -1 | cut -d: -f1)
[ -n "$linea" ] && [ -n "$habil" ] && [ "$habil" -lt "$linea" ] && resultado PASS "WorldForge arranca antes de abrir los mundos" "línea $habil < $linea" \
    || resultado FAIL "WorldForge arranca antes de abrir los mundos" "habilitado=$habil preparing=$linea"
grep -q "✘ Plantilla" "$LOG" && resultado FAIL "la plantilla se lee en el arranque" "$(grep -m1 '✘ Plantilla' "$LOG")" \
    || resultado PASS "la plantilla se lee en el arranque" "sin «✘ Plantilla» en el registro"
paso "el mundo principal usa el preset nuevo" "wf world inspect world" "Generador: WorldForgeGenerator"

echo "== Lo generado lejos =="
node "$AQUI/principal.js" comprobar
[ $? -eq 0 ] || FALLOS=$((FALLOS + 1))

echo "== Registro =="
lejos=$(grep -hiE "far chunk|Detected setBlock|outside of (the )?region" "$S"/logs/*.log | head -3)
[ -z "$lejos" ] && resultado PASS "ninguna escritura fuera de la región del generador" "-" || resultado FAIL "ninguna escritura fuera de la región del generador" "$lejos"
registro_limpio
echo "FIN escenario-principal ($PLATAFORMA): $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
