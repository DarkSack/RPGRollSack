#!/usr/bin/env bash
# Escenario de la fase 4 (plantillas) contra un wf-test recién levantado: construye plantillas con
# marcas, las guarda, las usa en presets y comprueba bloque a bloque dónde, a qué altura y con qué
# giro las colocó el generador (las últimas colocaciones salen en /wf world inspect).
set -uo pipefail

source "$(dirname "$0")/comun.sh"
TPL=$DATA/structures

# Manda un comando y devuelve las líneas nuevas del registro tras unos segundos.
salida() { # comando [segundos]
    local desde=$(($(lineas) + 1))
    consola "$1"
    sleep "${2:-3}"
    tail -n +"$desde" "$LOG"
}

# Dónde queda el bloque local x,z tras el giro: Minecraft gira alrededor del punto de colocación.
gira() { # x z ROTACIÓN → "dx dz"
    case $3 in
        NONE) echo "$1 $2" ;;
        CLOCKWISE_90) echo "$((-$2)) $1" ;;
        CLOCKWISE_180) echo "$((-$1)) $((-$2))" ;;
        COUNTERCLOCKWISE_90) echo "$2 $((-$1))" ;;
    esac
}

# Oro en la esquina 0,0,0 y diamante en la esquina opuesta del suelo (ox,0,oz); si se pide, el cofre
# de 2,1,1 con su tabla de botín.
marcas() { # id mundo x y z rotación ox oz [cofre]
    local dx dz
    read -r dx dz <<< "$(gira "$7" "$8" "$6")"
    forzar "minecraft:$2" "$3" "$5" $(($3 + dx)) $(($5 + dz))
    bloque "$1 en $3 $4 $5 ($6): oro en el origen" "minecraft:$2" "$3" "$4" "$5" minecraft:gold_block
    bloque "$1 ($6): diamante en la esquina opuesta" "minecraft:$2" $(($3 + dx)) "$4" $(($5 + dz)) minecraft:diamond_block
    if [ -n "${9:-}" ]; then
        read -r dx dz <<< "$(gira 2 1 "$6")"
        paso "$1 ($6): el cofre conserva su tabla de botín" \
            "execute in minecraft:$2 run data get block $(($3 + dx)) $(($4 + 1)) $(($5 + dz)) LootTable" "simple_dungeon"
    fi
}

# Las colocaciones de un mundo: "id mundo x y z ROTACIÓN" por línea.
colocadas() { grep -oE "última: .*" <<< "$1" | sed 's/^última: //'; }

# Cada chunk decide su plantilla por su cuenta, así que dos grandes pueden solaparse y la segunda pisa
# a la primera. De la lista "id mundo x y z GIRO" quedan las que no se cruzan con ninguna otra.
sueltas() { # lista ancho fondo
    awk -v w="$2" -v d="$3" '{
        l[NR] = $0; x = $3; z = $5
        if ($6 == "NONE") { a = x; b = x + w - 1; c = z; e = z + d - 1 }
        else if ($6 == "CLOCKWISE_90") { a = x - (d - 1); b = x; c = z; e = z + w - 1 }
        else if ($6 == "CLOCKWISE_180") { a = x - (w - 1); b = x; c = z - (d - 1); e = z }
        else { a = x; b = x + d - 1; c = z - (w - 1); e = z }
        x0[NR] = a; x1[NR] = b; z0[NR] = c; z1[NR] = e
    } END {
        for (i = 1; i <= NR; i++) {
            ok = 1
            for (j = 1; j <= NR; j++) if (i != j && x0[i] <= x1[j] && x0[j] <= x1[i] && z0[i] <= z1[j] && z0[j] <= z1[i]) ok = 0
            if (ok) print l[i]
        }
    }' <<< "$1"
}
cuantas() { grep -oE "Plantilla $2: [0-9]+ colocadas, [0-9]+ sin sitio" <<< "$1" | grep -oE "[0-9]+" | tr '\n' ' '; }

echo "== Construir =="
paso "crear t_obra (flat)" "wf world create t_obra flat" "Mundo t_obra creado" 90
forzar minecraft:t_obra 100 100 100 200 131 231
sleep 4
paso "torre: piedra" "execute in minecraft:t_obra run fill 100 -60 100 104 -57 102 minecraft:stone_bricks" "Successfully filled"
paso "torre: oro en el origen" "execute in minecraft:t_obra run setblock 100 -60 100 minecraft:gold_block" "Changed the block"
paso "torre: diamante enfrente" "execute in minecraft:t_obra run setblock 104 -60 102 minecraft:diamond_block" "Changed the block"
paso "torre: cofre" "execute in minecraft:t_obra run setblock 102 -59 101 minecraft:chest" "Changed the block"
paso "torre: tabla de botín" "execute in minecraft:t_obra run data merge block 102 -59 101 {LootTable:\"minecraft:chests/simple_dungeon\"}" "Modified block data"
paso "ancha (32×32): suelo" "execute in minecraft:t_obra run fill 100 -60 200 131 -60 231 minecraft:smooth_stone" "Successfully filled"
paso "ancha: oro en el origen" "execute in minecraft:t_obra run setblock 100 -60 200 minecraft:gold_block" "Changed the block"
paso "ancha: diamante enfrente" "execute in minecraft:t_obra run setblock 131 -60 231 minecraft:diamond_block" "Changed the block"

echo "== Guardar =="
paso "guardar torre" "wf structure save torre 100 -60 100 104 -57 102 t_obra" "Plantilla torre guardada: 5×4×3"
paso "guardar ancha (el máximo)" "wf structure save ancha 100 -60 200 131 -60 231 t_obra" "Plantilla ancha guardada: 32×1×32"
[ -f "$TPL/torre.nbt" ] && resultado PASS "el archivo está en structures/" "$(ls -l "$TPL" | tail -n +2 | tr '\n' ' ')" \
    || resultado FAIL "el archivo está en structures/" "$(ls -la "$TPL" 2>&1)"
paso "no se sobrescribe" "wf structure save torre 100 -60 100 101 -60 101 t_obra" "No se pudo: nombre: ya existe la plantilla «torre»"
paso "demasiado ancha" "wf structure save x1 100 -60 100 140 -60 101 t_obra" "No se pudo: zona: mide 41×1×2; como mucho 32"
paso "demasiado alta" "wf structure save x2 100 -64 100 101 10 101 t_obra" "No se pudo: zona: mide 2×75×2"
paso "se sale del mundo" "wf structure save x3 100 -70 100 101 -66 101 t_obra" "No se pudo: zona: se sale del mundo"
paso "nombre con ruta" "wf structure save ../x 100 -60 100 101 -60 101 t_obra" "No se pudo: nombre: .*no vale como identificador"
paso "zona sin cargar" "wf structure save x4 5000 -60 5000 5002 -58 5002 t_obra" "No se pudo: zona: el chunk 312, 312 no está cargado"
paso "desde la consola hace falta el mundo" "wf structure save x5 100 -60 100 101 -60 101" "No se pudo: desde la consola indica el mundo"
paso "mundo que no existe" "wf structure save x6 1 1 1 2 2 2 no_existe" "No se pudo: no hay ningún mundo cargado llamado «no_existe»"
paso "coordenada que no es un número" "wf structure save x7 a 1 1 2 2 2 t_obra" "No se pudo: «a» no es un número entero"
printf 'esto no es nbt' > "$TPL/rota.nbt"
paso "la lista enseña la buena" "wf structure list" "torre · 5×4×3 · sha [0-9a-f]{16}"
paso "y marca la rota" "wf structure list" "✘ rota: .*no se puede leer"
paso "info de la rota" "wf structure info rota" "No se pudo: structure: «rota» no se puede leer"
rm -f "$TPL/rota.nbt"

echo "== Presets =="
cat > "$DATA/imports/tpl-plano.yml" <<'EOF'
schema: 1
id: tpl-plano
provider: flat
seed: 4242
layers: [{block: bedrock, height: 1}, {block: stone, height: 2}, {block: grass_block, height: 1}]
templates:
- {id: torre, structure: torre, rarity: 3}
- {id: nunca, structure: torre, rarity: 1, biomes: [minecraft:the_void]}
EOF
cat > "$DATA/imports/tpl-vacio.yml" <<'EOF'
schema: 1
id: tpl-vacio
provider: empty
seed: 4242
templates:
- {id: sala, structure: torre, rarity: 2, placement: underground, min-y: 60, max-y: 60, rotation: none}
EOF
cat > "$DATA/imports/tpl-ancha.yml" <<'EOF'
schema: 1
id: tpl-ancha
provider: flat
seed: 4242
layers: [{block: bedrock, height: 1}, {block: dirt, height: 3}]
templates:
- {id: ancha, structure: ancha, rarity: 40}
EOF
printf 'schema: 1\nid: tpl-falta\nprovider: flat\nlayers: [{block: stone, height: 1}]\ntemplates: [{id: a, structure: nada, rarity: 3}]\n' > "$DATA/imports/tpl-falta.yml"
printf 'schema: 1\nid: tpl-vanilla\nprovider: vanilla\ntemplates: [{id: a, structure: torre, rarity: 3}]\n' > "$DATA/imports/tpl-vanilla.yml"
for p in tpl-plano tpl-vacio tpl-ancha; do
    paso "importar $p" "wf preset import $p.yml" "Importado como borrador $p"
    paso "publicar $p" "wf preset publish $p" "Borrador $p publicado"
done
paso "importar uno que pide una plantilla que no hay" "wf preset import tpl-falta.yml" "Importado como borrador tpl-falta"
paso "no se publica" "wf preset publish tpl-falta" "No se pudo: .*no existe la plantilla «nada»"
paso "importar uno vanilla con plantillas" "wf preset import tpl-vanilla.yml" "Importado como borrador tpl-vanilla"
paso "no se publica: vanilla no coloca plantillas" "wf preset publish tpl-vanilla" "No se pudo: .*no controla plantillas"
paso "info dice qué presets la usan" "wf structure info torre" "La usan: .*tpl-plano"

echo "== Generar =="
paso "crear t_tpl (plano)" "wf world create t_tpl tpl-plano" "Mundo t_tpl creado con el preset tpl-plano" 90
paso "crear t_tplv (vacío)" "wf world create t_tplv tpl-vacio" "Mundo t_tplv creado con el preset tpl-vacio" 90
paso "crear t_ancha" "wf world create t_ancha tpl-ancha" "Mundo t_ancha creado con el preset tpl-ancha" 90
for m in t_tpl t_tplv; do consola "execute in minecraft:$m run forceload add 0 0 127 127"; done
consola "execute in minecraft:t_ancha run forceload add 0 0 191 191"
sleep 30

ins=$(salida "wf world inspect t_tpl")
read -r n sin <<< "$(cuantas "$ins" torre)"
[ "${n:-0}" -ge 5 ] && [ "${sin:-1}" -eq 0 ] && resultado PASS "plano: torres colocadas, ninguna sin sitio" "$n colocadas, $sin sin sitio" \
    || resultado FAIL "plano: torres colocadas, ninguna sin sitio" "$(grep 'Plantilla' <<< "$ins")"
read -r n _ <<< "$(cuantas "$ins" nunca)"
[ "${n:--1}" -eq 0 ] && resultado PASS "el filtro de biomas deja fuera la regla de the_void" "0 colocadas" \
    || resultado FAIL "el filtro de biomas deja fuera la regla de the_void" "$(grep 'nunca' <<< "$ins")"
lista=$(colocadas "$ins")
giros=$(awk '{print $6}' <<< "$lista" | sort -u | wc -l)
[ "$giros" -ge 2 ] && resultado PASS "con rotation: random salen giros distintos" "$(awk '{print $6}' <<< "$lista" | sort | uniq -c | tr '\n' ' ')" \
    || resultado FAIL "con rotation: random salen giros distintos" "$lista"
alturas=$(awk '{print $4}' <<< "$lista" | sort -u | tr '\n' ' ')
[ "$alturas" = "-60 " ] && resultado PASS "superficie: justo encima de la hierba (y=-60)" "$alturas" \
    || resultado FAIL "superficie: justo encima de la hierba (y=-60)" "$alturas"
while read -r id mundo x y z rot; do
    marcas "$id" "$mundo" "$x" "$y" "$z" "$rot" 4 2 cofre
done <<< "$(sueltas "$lista" 5 3 | awk '!visto[$6]++')"   # una colocación de cada giro que haya salido

ins=$(salida "wf world inspect t_tplv")
lista=$(colocadas "$ins")
read -r n sin <<< "$(cuantas "$ins" sala)"
[ "${n:-0}" -ge 5 ] && resultado PASS "vacío: salas colocadas" "$n colocadas, $sin sin sitio" || resultado FAIL "vacío: salas colocadas" "$(grep 'Plantilla' <<< "$ins")"
[ "$(awk '{print $4 "/" $6}' <<< "$lista" | sort -u)" = "60/NONE" ] && resultado PASS "underground a y=60 y sin girar" "$(head -2 <<< "$lista" | tr '\n' ';')" \
    || resultado FAIL "underground a y=60 y sin girar" "$lista"
while read -r id mundo x y z rot; do
    marcas "$id" "$mundo" "$x" "$y" "$z" "$rot" 4 2
done <<< "$(head -2 <<< "$lista")"

ins=$(salida "wf world inspect t_ancha")
lista=$(colocadas "$ins")
read -r n sin <<< "$(cuantas "$ins" ancha)"
[ "${n:-0}" -ge 1 ] && [ "${sin:-1}" -eq 0 ] && resultado PASS "32×32 cabe siempre en la región del generador" "$n colocadas, $sin sin sitio" \
    || resultado FAIL "32×32 cabe siempre en la región del generador" "$(grep 'Plantilla' <<< "$ins")"
while read -r id mundo x y z rot; do
    marcas "$id" "$mundo" "$x" "$y" "$z" "$rot" 31 31
done <<< "$(sueltas "$lista" 32 32 | awk '!visto[$6]++' | head -2)"

echo "== Tras reiniciar =="
reiniciar
consola "execute in minecraft:t_tpl run forceload add 400 400 463 463"
sleep 15
ins=$(salida "wf world inspect t_tpl")
read -r n _ <<< "$(cuantas "$ins" torre)"
[ "${n:-0}" -ge 1 ] && resultado PASS "el mundo vuelve con sus plantillas y sigue colocándolas" "$n colocadas desde el arranque" \
    || resultado FAIL "el mundo vuelve con sus plantillas y sigue colocándolas" "$(grep -E 'Plantilla|No se pudo' <<< "$ins")"

echo "== Si falta el archivo =="
mv "$TPL/torre.nbt" "$S/torre.nbt.aparte"
paso "validar lo detecta" "wf preset validate tpl-plano" "tiene errores"
paso "no crea mundos con ese preset" "wf world create t_tpl2 tpl-plano" "No se pudo: preset: «tpl-plano» ya no es válido"
mv "$S/torre.nbt.aparte" "$TPL/torre.nbt"
paso "con el archivo de vuelta vale otra vez" "wf preset validate tpl-plano" "es válido"

echo "== Registro =="
lejos=$(grep -hiE "far chunk|Detected setBlock|outside of (the )?region" "$S"/logs/*.log | head -3)
[ -z "$lejos" ] && resultado PASS "ninguna escritura fuera de la región del generador" "-" || resultado FAIL "ninguna escritura fuera de la región del generador" "$lejos"
registro_limpio
echo "FIN escenario-tpl: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
