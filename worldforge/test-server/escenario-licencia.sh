#!/usr/bin/env bash
# El jar de VENTA (compilado sin -Prpgroll.dev) frente a la licencia. Arranca el servidor él mismo, una vez
# por caso. run.sh sigue pasando -Drpgroll.devmode=true a propósito: en un jar de venta no sirve de nada.
#
#   cd E:/Dev/RPGRoll && ./gradlew -p worldforge build
#   bash worldforge/test-server/escenario-licencia.sh sin                      sin clave (no pregunta a la tienda)
#   LICENCIA=<clave> bash worldforge/test-server/escenario-licencia.sh valida   una clave de verdad de la tienda
#   LICENCIA=RPGR-NO-VALIDA bash … invalida   pregunta a la tienda: cuenta como intento fallido de esta IP
#   JAR=<jar de desarrollo> bash … preset      preset roto en bukkit.yml (con el de venta haría falta la clave)
#
# La clave solo va a .work/plugins/RPGRoll-WorldForge/license.yml (en .gitignore, se borra en cada arranque).
set -uo pipefail

source "$(dirname "$0")/comun.sh"
CASO=${1:?caso: sin, valida, invalida o preset}

arranca() { # MAIN → deja en RC la salida de levantar.sh (0 arrancó, 3 se paró al arrancar)
    MAIN=$1 bash "$AQUI/levantar.sh" >/dev/null 2>&1
    RC=$?
}
en_registro() { # nombre regex
    local linea
    linea=$(grep -E -m1 -- "$2" "$LOG") && resultado PASS "$1" "$linea" || resultado FAIL "$1" "no aparece /$2/"
}
sin_chunks() { # nombre
    local n
    n=$(find "$S/world" -name '*.mca' 2>/dev/null | wc -l)
    [ "$n" -eq 0 ] && resultado PASS "$1" "0 archivos de región" || resultado FAIL "$1" "$n archivos de región"
}
jar_de_venta() {
    grep -q "Chequeo de licencia OMITIDO" "$LOG" && resultado FAIL "el flag de desarrollo no salta la licencia" "lo saltó" \
        || resultado PASS "el flag de desarrollo no salta la licencia" "-Drpgroll.devmode=true sin efecto"
}

case "$CASO" in
sin)
    unset LICENCIA
    echo "== Sin clave y con WorldForge en bukkit.yml: se para antes de abrir los mundos =="
    arranca void-platform
    [ $RC -eq 3 ] && resultado PASS "el servidor se para al arrancar" "levantar.sh: $RC" || resultado FAIL "el servidor se para al arrancar" "levantar.sh: $RC"
    jar_de_venta
    en_registro "dice que no hay licencia" "No se detectó una licencia"
    en_registro "dice por qué se para y qué hacer" "Se para el servidor: si siguiera"
    en_registro "nombra el mundo" "bukkit.yml genera con WorldForge: world\."
    sin_chunks "Minecraft no generó nada con su terreno"

    echo "== Sin clave y sin WorldForge en bukkit.yml: se desactiva y el servidor sigue =="
    arranca ninguno
    [ $RC -eq 0 ] && resultado PASS "el servidor arranca" "levantar.sh: $RC" || resultado FAIL "el servidor arranca" "levantar.sh: $RC"
    jar_de_venta
    en_registro "se desactiva" "Disabling RPGRoll-WorldForge"
    grep -q "WorldForge habilitado" "$LOG" && resultado FAIL "no llega a habilitarse" "se habilitó" \
        || resultado PASS "no llega a habilitarse" "sin «WorldForge habilitado»"
    grep -q "key: '%%__LICENSE__%%'" "$DATA/license.yml" 2>/dev/null \
        && resultado PASS "deja license.yml para pegar la clave" "$DATA/license.yml" \
        || resultado FAIL "deja license.yml para pegar la clave" "$(ls "$DATA" 2>&1)"
    ;;
invalida)
    : "${LICENCIA:?pon LICENCIA=<una clave falsa que empiece por RPGR->}"
    echo "== Clave que la tienda no reconoce =="
    arranca ninguno
    jar_de_venta
    en_registro "pregunta a la tienda" "Verificando licencia contra"
    en_registro "no la acepta y se desactiva" "no pudo verificar tu licencia"
    grep -q "WorldForge habilitado" "$LOG" && resultado FAIL "no llega a habilitarse" "se habilitó" \
        || resultado PASS "no llega a habilitarse" "-"
    ;;
valida)
    : "${LICENCIA:?pon LICENCIA=<clave de la tienda>}"
    echo "== Clave de verdad, con el mundo principal de WorldForge =="
    arranca void-platform
    [ $RC -eq 0 ] && resultado PASS "el servidor arranca" "levantar.sh: $RC" || resultado FAIL "el servidor arranca" "levantar.sh: $RC"
    jar_de_venta
    en_registro "la tienda la acepta" "✔ Licencia verificada"
    en_registro "WorldForge se habilita" "WorldForge habilitado en Paper"
    consola "forceload add 0 0"
    bloque "el mundo principal sale del preset: plataforma" minecraft:overworld 0 64 0 minecraft:stone
    bloque "el mundo principal sale del preset: vacío debajo" minecraft:overworld 0 -64 0 minecraft:air
    paso "los comandos funcionan" "wf status" "WorldForge"
    registro_limpio
    ;;
preset)
    unset LICENCIA
    echo "== Preset que no existe en bukkit.yml: se para antes de abrir los mundos =="
    arranca no-existe
    [ $RC -eq 3 ] && resultado PASS "el servidor se para al arrancar" "levantar.sh: $RC" || resultado FAIL "el servidor se para al arrancar" "levantar.sh: $RC"
    en_registro "dice qué preset falla" "El preset de bukkit.yml no existe o tiene errores.*world \(preset «no-existe»: no existe\)"
    sin_chunks "Minecraft no generó nada con su terreno"
    ;;
*)
    echo "caso desconocido: $CASO"; exit 2 ;;
esac

echo "FIN escenario-licencia ($CASO): $FALLOS fallo(s)"
[ $FALLOS -eq 0 ]
