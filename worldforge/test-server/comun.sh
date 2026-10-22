#!/usr/bin/env bash
# Funciones que comparten los escenarios contra wf-test (lo arranca levantar.sh): mandar un comando
# por la consola, esperar su respuesta en logs/latest.log y anotar una línea WFTEST por paso.
AQUI=$(cd "$(dirname "$0")" && pwd)
S=$AQUI/.work
C=wf-test
LOG=$S/logs/latest.log
DATA=$S/plugins/RPGRoll-WorldForge
FALLOS=0

consola() { docker exec $C sh -c 'printf "%s\n" "$1" > /tmp/console' sh "$1"; }
lineas() { wc -l < "$LOG"; }
resultado() { # PASS|FAIL nombre detalle
    echo "WFTEST $1 $2 :: $3"
    [ "$1" = PASS ] || FALLOS=$((FALLOS + 1))
}

# Espera la primera línea nueva que case con $1 (o con un error de WorldForge) desde la línea $3.
espera() {
    local fin=$((SECONDS + $2)) linea
    while [ $SECONDS -lt $fin ]; do
        linea=$(tail -n +"$3" "$LOG" | grep -E -m1 -- "$1|No se pudo:|No tienes permiso|Uso: /wf|No conozco|Test failed|not loaded") && { echo "$linea"; return 0; }
        sleep 0.4
    done
    return 1
}

# Como espera, pero solo con $1: no se para en el primer error (para mensajes que llegan más tarde).
aparece() { # regex segundos desde
    local fin=$((SECONDS + $2)) linea
    while [ $SECONDS -lt $fin ]; do
        linea=$(tail -n +"$3" "$LOG" | grep -E -m1 -- "$1") && { echo "$linea"; return 0; }
        sleep 0.4
    done
    return 1
}

paso() { # nombre comando regex [segundos]
    local desde linea
    desde=$(($(lineas) + 1))
    consola "$2"
    if linea=$(espera "$3" "${4:-20}" "$desde") && grep -qE -- "$3" <<< "$linea"; then
        ULTIMA=$linea
        resultado PASS "$1" "$linea"
    else
        resultado FAIL "$1" "esperaba /$3/, llegó: ${linea:-nada}"
    fi
}

# Un "execute if ..." que debe dar "Test passed"; reintenta mientras el chunk no esté cargado.
prueba() { # nombre comando
    local desde linea
    for _ in 1 2 3 4 5 6; do
        desde=$(($(lineas) + 1))
        consola "$2"
        linea=$(espera "Test passed" 10 "$desde")
        [[ "$linea" == *"not loaded"* ]] || break
        sleep 2
    done
    [[ "$linea" == *"Test passed"* ]] && resultado PASS "$1" "$linea" || resultado FAIL "$1" "${linea:-nada} ← $2"
}

bloque() { # nombre dimensión x y z bloque
    prueba "$1" "execute in $2 if block $3 $4 $5 $6"
}

# Cuenta los bloques $5 en la columna de 16x16 del chunk que contiene x,z entre y0 e y1
# (cambiándolos por cristal: el mundo es de prueba). Imprime el número.
cuenta() { # dimensión x z y0 y1 bloque
    local x0=$(( ($2 >> 4) << 4 )) z0=$(( ($3 >> 4) << 4 )) desde linea
    for _ in 1 2 3 4 5 6 7 8; do
        desde=$(($(lineas) + 1))
        consola "execute in $1 run fill $x0 $4 $z0 $((x0 + 15)) $5 $((z0 + 15)) minecraft:glass replace $6"
        linea=$(espera "Successfully filled|No blocks were filled|not loaded" 10 "$desde")
        case "$linea" in
            *"Successfully filled"*) grep -oE "filled [0-9]+" <<< "$linea" | grep -oE "[0-9]+"; return ;;
            *"No blocks were filled"*) echo 0; return ;;
        esac
        sleep 2
    done
    echo -1
}

forzar() { # dimensión x z...
    local d=$1; shift
    while [ $# -gt 0 ]; do consola "execute in $d run forceload add $1 $2"; shift 2; done
}

# Para el servidor guardando (java sale; el contenedor sigue vivo por el bucle de run.sh).
parar() {
    local desde
    desde=$(($(lineas) + 1))
    consola "stop"
    # Paper: «All dimensions are saved»; Folia: «[RegionShutdownThread] Saved all worlds».
    espera "All dimensions are saved|Saved all worlds" 90 "$desde" >/dev/null && resultado PASS "se para guardando" "-"         || resultado FAIL "se para guardando" "sin 'All dimensions are saved' ni 'Saved all worlds'"
    sleep 3
    mv "$LOG" "$S/logs/antes-$(date +%s%N).log"
}

arrancar() {
    docker restart $C >/dev/null
    for _ in $(seq 1 120); do grep -q "Done (" "$LOG" 2>/dev/null && break; sleep 2; done
    grep -q "Done (" "$LOG" && resultado PASS "vuelve a arrancar" "$(grep -m1 'Done (' "$LOG")" || resultado FAIL "vuelve a arrancar" "sin Done"
}

reiniciar() {
    parar
    arrancar
}

# Ninguna excepción ni error de WorldForge en todos los registros de esta ejecución.
registro_limpio() {
    local errores
    errores=$(cat "$S"/logs/antes-*.log "$LOG" 2>/dev/null | grep -nE "at com\.sack\.rpgroll|RPGRoll-WorldForge.*(Exception|SEVERE)|\[RPGRoll-WorldForge\].*falló" | head -5)
    [ -z "$errores" ] && resultado PASS "sin excepciones de WorldForge en el registro" "-" || resultado FAIL "sin excepciones de WorldForge en el registro" "$errores"
}
