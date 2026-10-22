#!/usr/bin/env bash
# Lo que se espera de un gestor de mundos tipo Multiverse, contra un Paper recién levantado: Nether y
# End como mundos aparte, el generador de otro plugin (OtroGenerador, hecho para la prueba, hace de
# Terra o Iris), importar una carpeta de mundo que WorldForge no creó, ir de un mundo a otro y la
# gravedad por mundo de los jugadores.
set -uo pipefail

source "$(dirname "$0")/comun.sh"
NODE_PATH=${NODE_PATH:-/e/Dev/SackitoNetwork/e2e/bot/node_modules}
export NODE_PATH

atributo() { # nombre atributo valor-regex
    paso "$1" "attribute Viajero minecraft:$2 get" "$3"
}

echo "== Otro plugin con generador =="
OG=$(bash "$AQUI/otro-generador/construir.sh" | tail -1)
cp "$OG" "$S/plugins/"
reiniciar
grep -q "Enabling OtroGenerador" "$LOG" && resultado PASS "OtroGenerador cargado" "-" || resultado FAIL "OtroGenerador cargado" "-"

echo "== Nether y End =="
paso "presets de serie nether y end" "wf preset info nether" "proveedor vanilla|vanilla"
paso "crear t_nether" "wf world create t_nether nether" "Mundo t_nether creado con el preset nether" 120
paso "crear t_end" "wf world create t_end end" "Mundo t_end creado con el preset end" 120
paso "t_nether es un Nether" "wf world inspect t_nether" "entorno NETHER"
paso "t_end es un End" "wf world inspect t_end" "entorno THE_END"
forzar minecraft:t_nether 0 0
forzar minecraft:t_end 0 0
sleep 5
bloque "t_nether: suelo de bedrock" minecraft:t_nether 0 0 0 minecraft:bedrock
bloque "t_nether: techo de bedrock" minecraft:t_nether 0 127 0 minecraft:bedrock
prueba "t_end: bioma del End" "execute in minecraft:t_end if biome 0 64 0 minecraft:the_end"

echo "== Generador de otro plugin =="
cat > "$DATA/imports/ajeno.yml" <<'EOF'
schema: 1
id: ajeno
provider: plugin
generator: OtroGenerador:cristal
seed: 7
player-gravity: 1.5
EOF
printf 'schema: 1\nid: ajeno-falta\nprovider: plugin\ngenerator: NoExiste:x\n' > "$DATA/imports/ajeno-falta.yml"
printf 'schema: 1\nid: ajeno-menas\nprovider: plugin\ngenerator: OtroGenerador\nores: [{id: a, block: diamond_ore, veins-per-chunk: 1, size: 3, min-y: 0, max-y: 10}]\n' > "$DATA/imports/ajeno-menas.yml"
paso "importar ajeno" "wf preset import ajeno.yml" "Importado como borrador ajeno"
paso "publicar ajeno" "wf preset publish ajeno" "Borrador ajeno publicado"
paso "importar uno con un plugin que no está" "wf preset import ajeno-falta.yml" "Importado como borrador ajeno-falta"
paso "no se publica" "wf preset publish ajeno-falta" "No se pudo: generator: no hay ningún plugin «NoExiste» instalado"
paso "importar uno que pide menas al otro generador" "wf preset import ajeno-menas.yml" "Importado como borrador ajeno-menas"
paso "no se publica: WorldForge no mete menas en un generador ajeno" "wf preset publish ajeno-menas" "No se pudo: ores: el proveedor «plugin» no controla"
paso "crear t_ajeno" "wf world create t_ajeno ajeno" "Mundo t_ajeno creado con el preset ajeno \(semilla 7\)" 90
paso "inspect dice de quién es el generador" "wf world inspect t_ajeno" "Generador: prueba.OtroGenerador"
forzar minecraft:t_ajeno 0 0
sleep 3
bloque "t_ajeno: el cristal del otro plugin" minecraft:t_ajeno 5 -64 5 minecraft:glass
bloque "t_ajeno: encima, aire" minecraft:t_ajeno 5 -61 5 minecraft:air

echo "== Importar una carpeta que WorldForge no creó =="
paso "crear t_imp (flat)" "wf world create t_imp flat" "Mundo t_imp creado" 90
forzar minecraft:t_imp 0 0
sleep 3
paso "marca en t_imp" "execute in minecraft:t_imp run setblock 3 -60 3 minecraft:gold_block" "Changed the block"
parar
python -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); c.execute(\"DELETE FROM managed_worlds WHERE name='t_imp'\"); c.commit(); print(c.total_changes)" \
    "$(cygpath -w "$DATA/worldforge.db")" | grep -q 1 && resultado PASS "simular: t_imp deja de ser de WorldForge (su carpeta sigue)" "-" \
    || resultado FAIL "simular: t_imp deja de ser de WorldForge (su carpeta sigue)" "-"
mkdir -p "$S/viejo" && printf 'x' > "$S/viejo/level.dat"
arrancar
paso "t_imp no está cargado" "wf world inspect t_imp" "No se pudo: world: no hay un mundo «t_imp»"
paso "crear encima no pisa la carpeta" "wf world create t_imp flat" "No se pudo: name: ya existe la carpeta"
paso "importarla" "wf world import t_imp flat" "Mundo t_imp importado con el preset flat" 60
forzar minecraft:t_imp 0 0
sleep 3
bloque "conserva lo que tenía" minecraft:t_imp 3 -60 3 minecraft:gold_block
paso "ya es de WorldForge" "wf world list" "t_imp \[minecraft:t_imp\] cargado · READY"
paso "no se importa dos veces" "wf world import t_imp flat" "No se pudo: name: WorldForge ya administra «t_imp»"
paso "una carpeta que no existe" "wf world import nada flat" "No se pudo: name: no hay un mundo en"
paso "un mundo del formato de antes de 26.1" "wf world import viejo flat" "No se pudo: name: «viejo» está en el formato de antes de 26.1"
paso "con un preset sin publicar" "wf world import t_otro no-existe" "No se pudo: preset:"

echo "== Ir de un mundo a otro y gravedad =="
cat > "$DATA/imports/lunar.yml" <<'EOF'
schema: 1
id: lunar
provider: flat
layers: [{block: bedrock, height: 1}, {block: end_stone, height: 3}]
player-gravity: 0.5
EOF
paso "importar lunar" "wf preset import lunar.yml" "Importado como borrador lunar"
paso "publicar lunar" "wf preset publish lunar" "Borrador lunar publicado"
paso "crear t_luna" "wf world create t_luna lunar" "Mundo t_luna creado" 90
node "$AQUI/viajero.js" > "$S/viajero.out" 2>&1 &
VIAJERO=$!
for _ in $(seq 1 30); do grep -q "VIAJERO dentro" "$S/viajero.out" && break; sleep 1; done
grep -q "VIAJERO dentro" "$S/viajero.out" && resultado PASS "un jugador conectado" "Viajero" || resultado FAIL "un jugador conectado" "$(cat "$S/viajero.out")"
atributo "en el principal: gravedad normal" gravity "is 0\.08"
paso "tp a un mundo que no existe" "wf world tp no_existe Viajero" "No se pudo: no hay ningún mundo cargado llamado «no_existe»"
paso "tp de alguien que no está" "wf world tp t_luna Nadie" "No se pudo: «Nadie» no está conectado"
paso "tp desde la consola sin jugador" "wf world tp t_luna" "No se pudo: desde la consola indica el jugador"
paso "tp a t_luna" "wf world tp t_luna Viajero" "Viajero → t_luna"
sleep 2
paso "está en t_luna" "wf world inspect t_luna" "jugadores 1"
atributo "t_luna: gravedad a la mitad" gravity "is 0\.04"
atributo "t_luna: caída segura al doble" safe_fall_distance "is 6"
paso "tp a t_ajeno" "wf world tp t_ajeno Viajero" "Viajero → t_ajeno"
sleep 2
atributo "t_ajeno (generador de otro plugin): gravedad 1.5" gravity "is 0\.12"
paso "tp al principal" "wf world tp world Viajero" "Viajero → world"
sleep 2
atributo "de vuelta: gravedad normal" gravity "is 0\.08"
atributo "y caída segura normal" safe_fall_distance "is 3"
paso "otra vez a t_luna" "wf world tp t_luna Viajero" "Viajero → t_luna"
sleep 2
paso "muere en t_luna" "kill Viajero" "Killed Viajero"
for _ in $(seq 1 20); do grep -q "VIAJERO murió" "$S/viajero.out" && break; sleep 1; done
sleep 4
atributo "reaparece en el principal con gravedad normal" gravity "is 0\.08"
kill $VIAJERO 2>/dev/null; wait $VIAJERO 2>/dev/null

echo "== Tras reiniciar =="
reiniciar
for m in t_nether t_end t_ajeno t_imp t_luna; do
    # WorldForge los carga después del «Done», desde su propio hilo.
    linea=$(aparece "Mundo administrado cargado: $m " 30 1) && resultado PASS "$m vuelve solo" "$(echo "$linea" | cut -c1-120)" \
        || resultado FAIL "$m vuelve solo" "$(grep -m2 "$m" "$LOG" | tr '\n' ' ' | cut -c1-200)"
done
node "$AQUI/viajero.js" > "$S/viajero.out" 2>&1 &
VIAJERO=$!
for _ in $(seq 1 30); do grep -q "VIAJERO dentro" "$S/viajero.out" && break; sleep 1; done
paso "tp a t_luna" "wf world tp t_luna Viajero" "Viajero → t_luna"
sleep 2
atributo "la gravedad sigue tras reiniciar" gravity "is 0\.04"
kill $VIAJERO 2>/dev/null; wait $VIAJERO 2>/dev/null

echo "== Registro =="
registro_limpio
echo "FIN escenario-mv: $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
