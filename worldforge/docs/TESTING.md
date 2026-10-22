# WorldForge — Pruebas

## Unitarias (`./gradlew -p worldforge test` desde `E:\Dev\RPGRoll`)

WorldForge se compila aparte del resto del repo (ARCHITECTURE D15).

JUnit 5 sin servidor. Cubren: validación de perfiles, codec YAML (ida y vuelta,
migración de esquema, campos desconocidos), importación (tamaño, rutas
maliciosas), migraciones SQLite (BD nueva y BD de versión anterior), tabla de
estados de trabajos, selección y capacidades de proveedores, planificador de
regeneración (alcance, exclusiones, chunks sin generar), editor de encabezados
`.mca` (archivos sintéticos), respaldo (manifiesto y verificación),
recuperación de trabajos interrumpidos, rutas seguras, dimensiones (JSON del
datapack), reglas de plantillas (códec, validación) y su geometría (giros y
hueco dentro de la región del generador), terrenos (lectura, rangos, la
transformación sobre un router pequeño y sobre los datos reales de 26.1.2 si
está el jar de Folia en `test-server/.cache`), el motor de terreno (semilla,
reparto de mar y montaña, `height-scale`, biomas por altura, que todos existan
en 26.1.2) y los campos nuevos del perfil (`terrain`, `generator`,
`player-gravity`).

## Integración (servidor real en Docker)

`worldforge/test-server/levantar.sh` levanta un Paper 26.1.2-74 limpio en Docker
(contenedor `wf-test`, puerto 25602) con el jar de desarrollo (`-Prpgroll.dev=true`,
que permite `-Drpgroll.devmode=true` y salta la licencia) y LuckPerms; el mundo
principal se genera desde `bukkit.yml` con el preset `void-platform`.
`escenario.sh` manda cada comando por la consola, espera su respuesta en
`logs/latest.log` y comprueba bloques y biomas con comandos vanilla
(`execute if block`, `execute if biome`, y `fill … replace` para contar bloques
en mundos de prueba). `jugador.js` (mineflayer) entra como jugador sin op para
probar permisos y autocompletado. Cada paso imprime una línea
`WFTEST PASS|FAIL`; el escenario termina con código 1 si alguno falla.

| Escenario | Fase | Qué recorre |
|---|---|---|
| `escenario.sh` (+ `jugador.js`) | 2-3 | presets, generación, menas medidas frente a vanilla, permisos |
| `escenario-tpl.sh` | 4 | guardar plantillas, presets que las usan, colocación comprobada bloque a bloque, reinicio |
| `escenario-dim.sh` | 5 | dimensiones por datapack, reinicio, tipos y generadores, quitar, JSON roto |
| `escenario-gui.sh` (+ `gui.js`) | 6 | menús con dos jugadores (admin y solo lectura) |
| `escenario-regen.sh` (+ `visitante.js`) | 7 | regenerar, proteger, respaldo, deshacer, apagado a mitad, recuperación |
| `escenario-mv.sh` (+ `viajero.js`, `otro-generador/`) | — | Nether y End aparte, generador de otro plugin, importar una carpeta, `world tp`, gravedad por mundo, reinicio |
| `escenario-principal.sh` (+ `principal.js`) | — | mundo principal de `bukkit.yml` con plantillas y menas |
| `escenario-terreno.sh` | — | dimensiones con terreno (iguales a vanilla con todo a 1, el doble y el triple de altas, por encima de y=320, más bajas), `terrain main` y vuelta, motor propio en caliente |
| `escenario-terralith.sh` | — | con Terralith 2.6.2 en el mundo (`MAIN=vanilla DATAPACKS=… levantar.sh`): convivencia y avisos |
| `escenario-motor.sh` (+ `viajero.js`) | — | el motor como mundo principal (`MAIN=montanas`): aparición, chunks alrededor de un jugador, menas, `world tp`, reinicio |
| `escenario-folia.sh`, `escenario-principal.sh` y `escenario-motor.sh` en Folia (`levantar-folia.sh`) | 8 | lo que Folia permite y lo que WorldForge le dice al admin que no |

Cada escenario necesita un servidor recién levantado (`levantar.sh`); el jar se
elige con `JAR=...` (por defecto `worldforge/build/libs`).

Reglas: solo mundos de prueba desechables; nunca contra un servidor con datos
reales; recompilar sin `-Prpgroll.dev` después.

## Negativas

Perfil malformado, bioma o bloque inexistente, archivo demasiado grande, ruta
con `..`, permiso insuficiente, mundo inexistente, BD corrupta, reinicio a
mitad de trabajo, operación no permitida en Folia, característica no soportada
por el proveedor. Cada una debe terminar en un error controlado con mensaje.

## Informe

Los resultados reales de cada fase se anotan en `docs/TEST-REPORT.md`.
