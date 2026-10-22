#!/usr/bin/env bash
# WorldForge junto a Terralith 2.6.2 (datapack de Stardust Labs; solo para probar, su licencia no deja
# redistribuirlo: lo descarga a mano en test-server/.cache/terralith). Levantar con él en el mundo:
#   MAIN=vanilla DATAPACKS=$PWD/worldforge/test-server/.cache/terralith/Terralith_26.1_v2.6.2.zip bash worldforge/test-server/levantar.sh
# (MAIN=vanilla: con el void-platform de siempre el mundo principal no tendría los biomes de Terralith.)
# Comprueba que las dimensiones de terreno de WorldForge siguen cargando y que no heredan el terreno de
# Terralith (salen del jar del servidor), que el generador normal sí lo hereda, y que terrain main avisa.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
T=$DATA/terrains

franjas() { # dimensión x z → piedra por franjas de 128 entre -64 y 319 (cambiada por cristal: dimensiones desechables)
    local y out=""
    for ((y = -64; y < 320; y += 128)); do out="$out$(cuenta "$1" "$2" "$3" $y $((y + 127)) minecraft:stone) "; done
    echo "${out% }"
}

echo "== Terralith activo =="
grep -q "file/Terralith" "$LOG" && resultado PASS "el servidor carga Terralith" "$(grep -m1 'file/Terralith' "$LOG" | cut -c1-140)" \
    || resultado FAIL "el servidor carga Terralith" "-"
paso "el mundo normal tiene biomas de Terralith" "execute in minecraft:overworld run locate biome terralith:highlands" "nearest" 120

echo "== Dimensiones de WorldForge con Terralith =="
printf 'id: identidad\ndescription: los números de vanilla\n' > "$T/identidad.yml"
paso "recargar" "wf reload-config" "presets recargados"
paso "ref: el generador normal" "wf dimension create ref overworld overworld" "Dimensión ref escrita"
paso "ident: terreno de WorldForge con los números de vanilla" "wf dimension create ident overworld terrain:identidad" "Dimensión ident escrita"
paso "suave" "wf dimension create suave overworld terrain:suave" "Dimensión suave escrita"
reiniciar
for d in ref ident suave; do
    linea=$(aparece "Dimensión $d: READY" 30 1) && resultado PASS "$d queda READY" "$linea" || resultado FAIL "$d queda READY" "-"
done
forzar worldforge:ref 400 400; forzar worldforge:ident 400 400; forzar worldforge:ref -800 1200; forzar worldforge:ident -800 1200
distinto=0
for c in "400 400" "-800 1200"; do
    set -- $c
    a=$(franjas worldforge:ref $1 $2); b=$(franjas worldforge:ident $1 $2)
    echo "  $1,$2 ref [$a] ident [$b]"
    [[ "$a$b" != *-1* ]] && [ "$a" != "$b" ] && distinto=$((distinto + 1))
done
[ $distinto -gt 0 ] && resultado PASS "ref hereda el terreno de Terralith; ident no (sale del jar)" "$distinto de 2 columnas distintas" \
    || resultado FAIL "ref hereda el terreno de Terralith; ident no" "iguales"
paso "ref no tiene biomas de Terralith (su lista de parámetros, sin lithostitched, es la de vanilla)" \
    "execute in worldforge:ref run locate biome terralith:highlands" "Could not find"
paso "terrain main avisa de Terralith" "wf terrain main suave" "Terralith_26.1_v2.6.2.zip» también cambia el terreno del mundo normal"
paso "y se deshace" "wf terrain main vanilla" "vuelve al terreno de vanilla"

echo "== Registro =="
registro_limpio
echo "FIN escenario-terralith: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
