#!/usr/bin/env bash
# Terrenos (estilo Tectonic/Terralith) contra un Paper recién levantado:
#   1. dimensiones con terreno por datapack: con los números de vanilla salen iguales que el generador
#      normal; con height-scale 2, el doble de altas; con 3 y 96 más, por encima de y=320 en towering;
#      suave, más bajas; montanas avisa de que en tall se corta;
#   2. el terreno del mundo normal sustituido como hace Terralith, y vuelta a vanilla;
#   3. el motor propio, para un mundo creado en caliente.
# Mide piedra por franjas en las mismas columnas (misma semilla, mismo sitio) sin dejar huella: la
# cambia por cristal para contarla y la devuelve. Un solo escenario a la vez contra wf-test.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
PACK=$S/world/datapacks/worldforge
T=$DATA/terrains

piedra() { # dimensión x z y0 y1 → bloques de piedra en el chunk de x,z entre y0 e y1 (incluidos)
    local y y1 total=0 n x0=$(( ($2 >> 4) << 4 )) z0=$(( ($3 >> 4) << 4 )) desde
    for ((y = $4; y <= $5; y += 128)); do
        y1=$((y + 127 < $5 ? y + 127 : $5))
        n=$(cuenta "$1" "$2" "$3" $y $y1 minecraft:stone)
        [ "$n" -lt 0 ] && { echo -1; return; }
        if [ "$n" -gt 0 ]; then
            desde=$(($(lineas) + 1))
            consola "execute in $1 run fill $x0 $y $z0 $((x0 + 15)) $y1 $((z0 + 15)) minecraft:stone replace minecraft:glass"
            espera "Successfully filled|No blocks were filled" 10 "$desde" >/dev/null
        fi
        total=$((total + n))
    done
    echo $total
}

franjas() { # dimensión x z techo → piedra por franjas de 128 desde y=-64
    local y out=""
    for ((y = -64; y < $4; y += 128)); do out="$out$(piedra "$1" "$2" "$3" $y $((y + 127))) "; done
    echo "${out% }"
}

altura() { # dimensión x z → y del suelo (mapa motion_blocking_no_leaves; los troncos cuentan), o -1
    local desde linea
    desde=$(($(lineas) + 1))
    consola "execute in $1 positioned $2 0 $3 positioned over motion_blocking_no_leaves run summon minecraft:marker ~ ~ ~ {Tags:[\"wfh\"]}"
    espera "Summoned new" 10 "$desde" | grep -q "Summoned" || { echo -1; return; }
    desde=$(($(lineas) + 1))
    consola "data get entity @e[type=minecraft:marker,tag=wfh,limit=1] Pos[1]"
    linea=$(espera "has the following entity data" 10 "$desde")
    desde=$(($(lineas) + 1))
    consola "kill @e[type=minecraft:marker,tag=wfh]"
    espera "Killed" 10 "$desde" >/dev/null
    grep -oE 'entity data: -?[0-9]+' <<< "$linea" | grep -oE -- '-?[0-9]+$' || echo -1
}

rejilla() { # dimensión x z → alturas de 5×5 columnas cada 48 bloques alrededor de x,z
    local dx dz out=""
    for dx in -96 -48 0 48 96; do for dz in -96 -48 0 48 96; do forzar "$1" $(($2 + dx)) $(($3 + dz)); done; done
    sleep 6
    for dx in -96 -48 0 48 96; do for dz in -96 -48 0 48 96; do out="$out$(altura "$1" $(($2 + dx)) $(($3 + dz))) "; done; done
    echo "${out% }"
}

maximo() { tr ' ' '\n' | sort -n | tail -1; }

# Dos conteos por franjas que solo difieren en algún bloque suelto. Lo que un chunk vecino coloca en la
# frontera depende del orden en que se generan, así que dos generaciones iguales pueden diferir en 1 o 2.
parecidas() { # "a1 a2 a3" "b1 b2 b3"
    [[ "$1$2" != *-1* ]] || return 1
    local a=($1) b=($2) i d
    [ ${#a[@]} -eq ${#b[@]} ] || return 1
    for i in "${!a[@]}"; do
        d=$((a[i] - b[i])); d=${d#-}
        [ $d -le $((8 + a[i] / 500)) ] || return 1
    done
}

sitio() { # dimensión bioma → "x z" del más cercano
    local desde linea
    desde=$(($(lineas) + 1))
    consola "execute in $1 run locate biome $2"
    linea=$(espera "nearest|Could not find" 120 "$desde") || return 1
    grep -oE '\[-?[0-9]+, [^,]+, -?[0-9]+\]' <<< "$linea" | tr -d '[],' | awk '{print $1, $3}'
}

echo "== Terrenos y dimensiones =="
paso "los de serie" "wf terrain list" "montanas — cordilleras"
paso "el principal sigue en vanilla" "wf terrain list" "mundo principal: vanilla"
paso "el diagnóstico lee los datos de vanilla" "wf diagnostics" "Terrenos: 5 en terrains/ · mundo principal: vanilla · datos de vanilla del servidor: legibles"
printf 'id: identidad\ndescription: los números de vanilla\n' > "$T/identidad.yml"
printf 'id: alto2\ndescription: el doble de alto y nada más\nheight-scale: 2\n' > "$T/alto2.yml"
printf 'id: torre\ndescription: el triple de alto y 96 más arriba\nheight-scale: 3\nheight-offset: 96\n' > "$T/torre.yml"
printf 'id: roto\nheight-scale: 7\n' > "$T/roto.yml"
paso "recargar avisa del que está mal" "wf reload-config" "terrains/roto.yml: .*height-scale"
paso "y lee los buenos" "wf terrain list" "identidad — los números de vanilla"
paso "un terreno que no existe" "wf dimension create x overworld terrain:nada" "No se pudo: generator: no hay un terreno «nada»"
paso "terreno en un tipo del Nether" "wf dimension create x nether terrain:suave" "No se pudo: generator: los terrenos usan el ruido del mundo normal"
paso "ref: el generador normal" "wf dimension create ref overworld overworld" "Dimensión ref escrita"
paso "ident: terreno con los números de vanilla" "wf dimension create ident overworld terrain:identidad" "Dimensión ident escrita .*terreno «identidad»"
paso "alto2 en towering" "wf dimension create alto2 towering terrain:alto2" "Dimensión alto2 escrita"
paso "torre en towering" "wf dimension create torre towering terrain:torre" "Dimensión torre escrita"
paso "suave" "wf dimension create suave overworld terrain:suave" "Dimensión suave escrita"
paso "archi" "wf dimension create archi overworld terrain:archipielago" "Dimensión archi escrita"
paso "montanas en tall avisa de que se cortan las cimas" "wf dimension create altas tall terrain:montanas" "las cimas saldrán cortadas"
paso "montanas en towering" "wf dimension create montes towering terrain:montanas" "Dimensión montes escrita"
for f in data/worldforge/worldgen/noise_settings/montanas/towering.json data/worldforge/worldgen/noise_settings/montanas/tall.json \
         data/worldforge/worldgen/density_function/montanas/overworld/offset.json data/worldforge/dimension_type/towering.json; do
    [ -f "$PACK/$f" ] && resultado PASS "escrito $f" "-" || resultado FAIL "escrito $f" "-"
done
sed -i 's/^height-scale: 1.7 /height-scale: 1.9 /' "$T/montanas.yml"
paso "recargar con montanas cambiado" "wf reload-config" "presets recargados"
paso "no pisa un terreno ya escrito" "wf dimension create otra towering terrain:montanas" "No se pudo: .*ya está escrito en el datapack con otros valores"
sed -i 's/^height-scale: 1.9 /height-scale: 1.7 /' "$T/montanas.yml"
rm -f "$T/roto.yml"
paso "recargar" "wf reload-config" "presets recargados"

echo "== Reinicio =="
reiniciar
for d in ref ident alto2 torre suave archi altas montes; do
    linea=$(aparece "Dimensión $d: READY" 30 1) && resultado PASS "$d queda READY" "$linea" || resultado FAIL "$d queda READY" "-"
done
paso "montes usa el tipo towering" "wf world inspect world_worldforge_montes" "Altura -64 a 575"
errores=$(grep -E "/ERROR\]" "$LOG" | head -3)
[ -z "$errores" ] && resultado PASS "Minecraft acepta los JSON (ningún ERROR al arrancar)" "-" || resultado FAIL "Minecraft acepta los JSON" "$errores"

echo "== Con los números de vanilla sale lo mismo que el generador normal =="
COLUMNAS="200 200|-600 300|1000 -1000"
IFS='|' read -ra CS <<< "$COLUMNAS"
for c in "${CS[@]}"; do set -- $c; forzar worldforge:ref $1 $2; forzar worldforge:ident $1 $2; done
declare -A ANTES
for c in "${CS[@]}"; do
    set -- $c
    a=$(franjas worldforge:ref $1 $2 320); b=$(franjas worldforge:ident $1 $2 320)
    ANTES[$c]=$a
    parecidas "$a" "$b" && [ "$a" != "0 0 0" ] && resultado PASS "ident = ref en $1,$2" "ref [$a] ident [$b]" \
        || resultado FAIL "ident = ref en $1,$2" "ref [$a] ident [$b]"
done

echo "== height-scale 2: el doble de alto =="
# locate da el borde más cercano del bioma, no la cima: se mide una rejilla alrededor.
read -r PX PZ <<< "$(sitio worldforge:ref minecraft:jagged_peaks)"
[ -n "${PZ:-}" ] && resultado PASS "picos en ref" "$PX $PZ" || resultado FAIL "picos en ref" "-"
forzar worldforge:ref $PX $PZ; forzar worldforge:alto2 $PX $PZ
r=$(piedra worldforge:ref $PX $PZ 64 319); a=$(piedra worldforge:alto2 $PX $PZ 64 575)
[ "$r" -gt 2000 ] && [ $((10 * a)) -ge $((14 * r)) ] && [ $((10 * a)) -le $((28 * r)) ] \
    && resultado PASS "sobre y=64 hay entre 1,4 y 2,8 veces la piedra de ref" "ref $r · alto2 $a" \
    || resultado FAIL "sobre y=64 hay entre 1,4 y 2,8 veces la piedra de ref" "ref $r · alto2 $a"
HR=($(rejilla worldforge:ref $PX $PZ)); H2=($(rejilla worldforge:alto2 $PX $PZ))
ratios=()
for i in "${!HR[@]}"; do
    [ "${HR[$i]}" -gt 72 ] && [ "${H2[$i]}" -gt 0 ] && ratios+=($(( 100 * (H2[i] - 64) / (HR[i] - 64) )))
done
mediana=$(printf '%s\n' "${ratios[@]}" | sort -n | awk '{v[NR]=$1} END {print (NR ? v[int((NR+1)/2)] : -1)}')
[ ${#ratios[@]} -ge 8 ] && [ "$mediana" -ge 160 ] && [ "$mediana" -le 240 ] \
    && resultado PASS "columna a columna, el suelo sobre y=64 sale ×2 (mediana ×$mediana/100)" "${#ratios[@]} columnas · ref [${HR[*]}] · alto2 [${H2[*]}]" \
    || resultado FAIL "columna a columna, el suelo sobre y=64 sale ×2" "mediana $mediana de ${#ratios[@]} · ref [${HR[*]}] · alto2 [${H2[*]}]"

echo "== height-scale 3 y 96 más arriba, en towering: por encima de y=320 =="
# En las mismas columnas que ref: cada una a 160 + 3·(ref − 64). La cumbre de vanilla no vale para esto:
# suele ser una aguja del ruido 3D, que height-scale no estira (solo la forma grande del terreno).
HT=($(rejilla worldforge:torre $PX $PZ))
ratios=(); altas=0
for i in "${!HR[@]}"; do
    [ "${HR[$i]}" -gt 72 ] && [ "${HT[$i]}" -gt 0 ] && ratios+=($(( 100 * (HT[i] - 160) / (HR[i] - 64) )))
    [ "${HT[$i]}" -gt 320 ] && altas=$((altas + 1))
done
mediana=$(printf '%s\n' "${ratios[@]}" | sort -n | awk '{v[NR]=$1} END {print (NR ? v[int((NR+1)/2)] : -1)}')
[ ${#ratios[@]} -ge 8 ] && [ "$mediana" -ge 240 ] && [ "$mediana" -le 360 ] \
    && resultado PASS "torre, columna a columna: 160 + ×3 sobre y=64 (mediana ×$mediana/100)" "${#ratios[@]} columnas · ref [${HR[*]}] · torre [${HT[*]}]" \
    || resultado FAIL "torre, columna a columna: 160 + ×3 sobre y=64" "mediana $mediana de ${#ratios[@]} · ref [${HR[*]}] · torre [${HT[*]}]"
mt=$(printf '%s ' "${HT[@]}" | maximo)
[ $altas -gt 0 ] && resultado PASS "torre pasa de y=320, que en vanilla no se puede" "$altas columnas por encima · máximo $mt" \
    || resultado FAIL "torre pasa de y=320" "máximo $mt"

echo "== suave: más bajo =="
HS=($(rejilla worldforge:suave $PX $PZ)); ms=$(printf '%s ' "${HS[@]}" | maximo)
[ "${ms:-999}" -gt 64 ] && [ "${ms:-999}" -lt 210 ] && resultado PASS "suave, en las mismas columnas: no pasa de y=210" "máximo $ms · ref hasta $(printf '%s ' "${HR[@]}" | maximo)" \
    || resultado FAIL "suave, en las mismas columnas: no pasa de y=210" "máximo ${ms:-} · [${HS[*]}]"

echo "== El terreno del mundo normal, como Terralith =="
paso "darle archipielago" "wf terrain main archipielago" "El mundo principal usará el terreno archipielago"
paso "avisa de los cortes con lo ya generado" "wf terrain main archipielago" "Solo cambia en los chunks que aún no existen"
paso "lo anota" "wf terrain list" "mundo principal: archipielago"
[ -f "$PACK/data/minecraft/worldgen/noise_settings/overworld.json" ] && resultado PASS "escribe noise_settings/overworld.json de minecraft" "-" \
    || resultado FAIL "escribe noise_settings/overworld.json de minecraft" "-"
paso "un terreno que no existe" "wf terrain main nada" "No se pudo: terrain: no hay un terreno «nada»"
reiniciar
NUEVAS="3000 3000|-3000 2000"
IFS='|' read -ra NS <<< "$NUEVAS"
for c in "${NS[@]}"; do set -- $c; for d in ref archi ident; do forzar worldforge:$d $1 $2; done; done
distinto=0
for c in "${NS[@]}"; do
    set -- $c
    a=$(franjas worldforge:ref $1 $2 320); b=$(franjas worldforge:archi $1 $2 320); i=$(franjas worldforge:ident $1 $2 320)
    parecidas "$a" "$b" && resultado PASS "ref (minecraft:overworld) sale como archi en $1,$2" "ref [$a] archi [$b]" \
        || resultado FAIL "ref (minecraft:overworld) sale como archi en $1,$2" "ref [$a] archi [$b]"
    parecidas "$a" "$i" || distinto=$((distinto + 1))
done
[ $distinto -gt 0 ] && resultado PASS "y ya no como vanilla" "$distinto de ${#NS[@]} columnas distintas de ident" \
    || resultado FAIL "y ya no como vanilla" "iguales a ident"
c=${CS[0]}; set -- $c
a=$(franjas worldforge:ref $1 $2 320)
[ "$a" = "${ANTES[$c]}" ] && resultado PASS "lo ya generado no cambia" "$1,$2: $a" || resultado FAIL "lo ya generado no cambia" "antes [${ANTES[$c]}] ahora [$a]"
paso "volver a vanilla" "wf terrain main vanilla" "vuelve al terreno de vanilla"
[ ! -f "$PACK/data/minecraft/worldgen/noise_settings/overworld.json" ] && resultado PASS "quita el archivo" "-" || resultado FAIL "quita el archivo" "-"
reiniciar
forzar worldforge:ref 5000 -5000; forzar worldforge:ident 5000 -5000
a=$(franjas worldforge:ref 5000 -5000 320); i=$(franjas worldforge:ident 5000 -5000 320)
parecidas "$a" "$i" && resultado PASS "de vuelta: ref otra vez como vanilla" "ref [$a] ident [$i]" || resultado FAIL "de vuelta: ref otra vez como vanilla" "ref [$a] ident [$i]"

echo "== Motor propio: un mundo creado en caliente =="
paso "el preset montanas usa el motor" "wf preset info montanas" "terrain"
paso "crear t_mont" "wf world create t_mont montanas" "Mundo t_mont creado con el preset montanas" 120
paso "inspect dice modo y terreno" "wf world inspect t_mont" "modo TERRAIN · terreno montanas"
paso "dónde aparece" "wf world inspect t_mont" "Aparición"
read -r AX AY AZ <<< "$(grep -oE 'Aparición -?[0-9]+, -?[0-9]+, -?[0-9]+' <<< "$ULTIMA" | tr -d ',' | awk '{print $2, $3, $4}')"
forzar minecraft:t_mont $AX $AZ
prueba "aparece en tierra, no en el agua" "execute in minecraft:t_mont unless block $AX $((AY - 1)) $AZ minecraft:water"
bloque "bedrock abajo (superficie de Minecraft encima del motor)" minecraft:t_mont $AX -64 $AZ minecraft:bedrock
n=$(cuenta minecraft:t_mont $AX $AZ -64 -1 minecraft:deepslate)
[ "$n" -gt 0 ] && resultado PASS "pizarra profunda bajo y=0" "$n" || resultado FAIL "pizarra profunda bajo y=0" "$n"
read -r TX TZ <<< "$(sitio minecraft:t_mont minecraft:jagged_peaks)"
[ -n "${TZ:-}" ] && resultado PASS "tiene biomas de picos" "$TX $TZ" || resultado FAIL "tiene biomas de picos" "-"
mt=$(rejilla minecraft:t_mont $TX $TZ | maximo)
[ "$mt" -gt 192 ] && [ "$mt" -le 316 ] && resultado PASS "y montaña alrededor, sin tocar el techo" "máximo $mt" \
    || resultado FAIL "montaña alrededor de los picos, sin tocar el techo" "máximo $mt"
read -r OX OZ <<< "$(sitio minecraft:t_mont minecraft:ocean)"
[ -n "${OZ:-}" ] && resultado PASS "y océanos" "$OX $OZ" || resultado FAIL "y océanos" "-"
paso "con pueblos de Minecraft" "execute in minecraft:t_mont run locate structure #minecraft:village" "nearest" 180
reiniciar
linea=$(aparece "Mundo administrado cargado: t_mont " 30 1) && resultado PASS "t_mont vuelve tras reiniciar" "$linea" || resultado FAIL "t_mont vuelve tras reiniciar" "-"

echo "== Registro =="
registro_limpio
echo "FIN escenario-terreno: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
