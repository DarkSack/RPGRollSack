# WorldForge — Informe de pruebas

Resultados reales. Cada línea dice qué se ejecutó, dónde y qué salió. Nada de
aquí es una estimación: si una prueba no se ha ejecutado, no aparece como
superada, y lo que falta está en [«Sin probar»](#sin-probar).

## Resumen

- **91 pruebas unitarias**, 0 fallos (2026-10-21, sin `-Prpgroll.dev`).
- **13 escenarios en servidor real** (10 en Paper 26.1.2-74, 3 en Folia
  26.1.2-8), cada uno sobre un servidor limpio y con bots de mineflayer donde
  hace falta un jugador: todos terminan **sin fallos**. En la primera pasada de
  la tanda final fallaron cinco por cómo medían los propios escenarios (no por
  el plugin); se corrigieron los escenarios y se repitieron.
- En ningún escenario aparece una excepción de WorldForge en el registro.
- Jar final: `build/libs/RPGRoll-WorldForge-0.1.0.jar`, compilado sin
  `-Prpgroll.dev` (SHA-256 `e7a97245…fa8c5487`), con `folia-supported: true`.
  Sin publicar ni commitear.

## Entorno

| Pieza | Versión |
|---|---|
| Servidor | Paper 26.1.2 build 74 (estable) y Folia 26.1.2 build 8 (estable), Docker `eclipse-temurin:25-jre` |
| API de compilación | `io.papermc.paper:paper-api:26.1.2.build.74-stable` |
| JDK / Gradle | 25 / 9.6.1 |
| SQLite | `sqlite-jdbc 3.49.1.0`, el que trae el servidor (no va dentro del jar) |
| Permisos en la prueba con jugador | LuckPerms-Bukkit 5.5.71 (solo en el servidor de prueba) |
| Bots | mineflayer del e2e de SackitoNetwork, versión `26.1` |
| Generador ajeno | `test-server/otro-generador/`, un plugin mínimo de prueba (no Terra ni Iris) |
| Terralith | 2.6.2 para 26.1 (Stardust Labs), solo en el servidor de prueba: su licencia no deja redistribuirlo |
| Jar probado | el de desarrollo (`-Prpgroll.dev`): mismo código que el de venta, con la comprobación de licencia omitida |

## Pruebas unitarias (estado actual)

`./gradlew -p worldforge test` (2026-10-21, sin `-Prpgroll.dev`): **91 pruebas,
0 fallos, 0 omitidas** en 13 clases.

| Clase | Pruebas | Qué cubre (además de lo de la fase 2) |
|---|---|---|
| `ProfileCodecTest` | 26 | Los 10 presets de serie (con nether, end y montanas) se leen, validan y vuelven iguales |
| `ProfileValidatorTest` | 9 | Lista de proveedores con `terrain` y `plugin` |
| `ProfileServiceTest` | 8 | — |
| `NewFieldsTest` | 2 | Generador de otro plugin; gravedad de los jugadores |
| `TemplateRulesTest` | 5 | Reglas de plantillas: claves obligatorias, solo proveedores con populator, errores campo a campo |
| `TemplateMathTest` | 3 | La plantilla más ancha permitida (32×32) cabe en la región del populator en los cuatro giros; huella de cada giro |
| `GenerationMathTest` | 4 | Proveedores con `terrain` |
| `StorageTest` | 7 | Migración del esquema 1 al 2 conservando filas |
| `BackupsAndJobsTest` | 7 | Respaldo → verificar → restaurar; respaldo manipulado rechazado; rutas e ids que no escapan; protecciones persistentes y archivo roto que bloquea todo; cambios de estado condicionales (un trabajo corre una vez) |
| `RegenUnitTest` | 3 | Estados que solo avanzan; chunks generados con sus coordenadas; borrar solo ese chunk en region, entities y poi |
| `DimensionsTest` | 4 | Solo combinaciones tipo + generador cuya altura cabe; JSON bien formado; tipo `towering`; `terrain:<id>` |
| `TerrainTest` | 6 | Los 5 terrenos de serie × 3 bases contra los JSON reales del servidor 26.1.2; transformación sobre un router pequeño; estimación de cimas; archivos rotos rechazados con su campo |
| `TerrainEngineTest` | 7 | Motor propio: misma semilla → mismo terreno; reparto de mar, tierra y montañas; `height-scale` y `height-offset`; biomas según la altura; un terreno alto conserva sus picos; todos sus biomas existen en 26.1.2; el proveedor en un perfil |

Valores del motor con los números de vanilla (24 km², una muestra cada 96
bloques, salida de `TerrainEngineTest`): mar 33 %, mínimo 23, máximo 236,
mediana 71, p90 120, p99 167.

## Servidor real: tanda final

Todos los escenarios con el mismo jar de desarrollo, servidor limpio en cada
uno, 2026-10-21. «1.ª pasada» es la tanda completa; los que fallaron por el
propio escenario se corrigieron y se repitieron (columna «Final»).

| Escenario | Plataforma | 1.ª pasada | Final | Salida |
|---|---|---|---|---|
| `escenario.sh` (núcleo, fase 3) | Paper | 83 / 1 fallo | **84 / 0** | [`final-escenario.txt`](evidence/final-escenario.txt) |
| `escenario-tpl.sh` (plantillas) | Paper | 72 / 1 | **73 / 0** | [`final-escenario-tpl.txt`](evidence/final-escenario-tpl.txt) |
| `escenario-dim.sh` (dimensiones) | Paper | **48 / 0** | — | [`final-escenario-dim.txt`](evidence/final-escenario-dim.txt) |
| `escenario-gui.sh` (menú) | Paper | **37 / 0** | — | [`final-escenario-gui.txt`](evidence/final-escenario-gui.txt) |
| `escenario-regen.sh` (regeneración) | Paper | **72 / 0** | — | [`final-escenario-regen.txt`](evidence/final-escenario-regen.txt) |
| `escenario-mv.sh` (como Multiverse) | Paper | **65 / 0** | — | [`final-escenario-mv.txt`](evidence/final-escenario-mv.txt) |
| `escenario-principal.sh` (mundo principal) | Paper | **18 / 0** | — | [`final-escenario-principal.txt`](evidence/final-escenario-principal.txt) |
| `escenario-terreno.sh` (terrenos) | Paper | 70 / 3 | **74 / 0** | [`final-escenario-terreno.txt`](evidence/final-escenario-terreno.txt) |
| `escenario-terralith.sh` (con Terralith) | Paper | 15 / 1 | **16 / 0** | [`final-escenario-terralith.txt`](evidence/final-escenario-terralith.txt) |
| `escenario-motor.sh` (motor como principal) | Paper | 9 / 1 | **14 / 0** | [`final-escenario-motor.txt`](evidence/final-escenario-motor.txt) |
| `escenario-folia.sh` | Folia | **45 / 0** | — | [`final-folia-escenario-folia.txt`](evidence/final-folia-escenario-folia.txt) |
| `escenario-principal.sh` | Folia | **18 / 0** | — | [`final-folia-escenario-principal.txt`](evidence/final-folia-escenario-principal.txt) |
| `escenario-motor.sh` | Folia | **9 / 0** | **12 / 0** | [`final-folia-escenario-motor.txt`](evidence/final-folia-escenario-motor.txt) |

`escenario-motor.sh` ganó pasos en la repetición: el chunk de aparición se mira
con el jugador dentro, se cuentan las menas de Minecraft y se prueba `world tp`
a otro mundo (también en Folia).

## Fase 2 — pruebas unitarias (2026-10-20)

`./gradlew :worldforge:test` (2026-10-20): **47 pruebas, 0 fallos, 0 omitidas.**

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `ProfileCodecTest` | 20 | Los 7 presets de serie se leen y validan sin avisos; escritura determinista e ida y vuelta; claves desconocidas = aviso; claves obligatorias; esquema más nuevo rechazado; tipos erróneos nombran el campo; YAML roto = error, no excepción; normalización de nombres |
| `ProfileValidatorTest` | 9 | Un proveedor no promete lo que no controla (menas en vanilla, estructuras en vacío, world-type fuera de vanilla); bloques y biomas inexistentes; alturas y rangos; alturas del Nether; plano exige capas que quepan; ids de mena repetidos y biomas contradictorios; ids con ruta o mayúsculas |
| `ProfileServiceTest` | 8 | Presets de serie copiados una vez (la edición del admin sobrevive); archivo roto se informa y no se toca; duplicar → borrador no usable hasta publicar; bloqueo optimista al guardar; publicar con errores rechazado; exportación reproducible con manifiesto SHA-256; importación: nombre, tamaño, contenido, nunca publica; importación con errores semánticos queda como borrador |
| `StorageTest` | 6 | Migración a esquema actual y datos que sobreviven al reabrir; BD de un WorldForge más nuevo no se toca; archivo que no es SQLite da error legible; upsert de mundos; nombres de archivo que intentan salir de su carpeta; escritura atómica |
| `GenerationMathTest` | 4 | Alturas de mena dentro del rango y distribución triangular centrada; celdas de bioma estables por semilla; capacidades declaradas por proveedor; forma de ids de trabajo y tokens |

### Defecto encontrado por las pruebas

`StorageTest.a_database_from_a_newer_worldforge_is_not_touched` falló la
primera vez: al rechazar una BD de esquema más nuevo, `Database.open` dejaba la
conexión abierta (en Windows el archivo quedaba bloqueado) y además activaba WAL
antes de comprobar la versión, es decir, sí tocaba el archivo. Corregido:
primero se migra/comprueba y después se cambia el modo; la conexión se cierra
si algo falla.

## Fase 3 — servidor Paper real (2026-10-20)

`test-server/levantar.sh` + `test-server/escenario.sh` + `test-server/jugador.js`
(2026-10-20), jar de desarrollo, servidor limpio. **84 pasos, 0 fallos.**
Salida completa: [`evidence/fase3-paper.txt`](evidence/fase3-paper.txt).

| Bloque | Pasos | Qué se comprobó y cómo |
|---|---|---|
| Arranque | 5 | Se habilita sin errores; crea `config.yml`, `messages.yml`, `worldforge.db` y los 7 presets; SQLite abre desde el cargador del plugin (esquema 1) |
| Mundo principal desde `bukkit.yml` | 7 | `generator: RPGRoll-WorldForge:void-platform` → `inspect` dice WorldForgeGenerator, aparición 0,65,0; `execute if block` confirma la plataforma de piedra (radio 2), aire fuera y debajo hasta y=−64; bioma `the_void` |
| Vacío, plataforma, plano | 13 | Creados con `/wf world create`; aire donde debe, plataforma, capas bedrock/tierra/hierba en y −64…−61, aire en −60, biomas `the_void` y `plains`, aparición encima de las capas |
| Semillas y presets | 15 | Duplicar → fijar semilla → un borrador no crea mundos → publicar → mundo creado con semilla 20261020 |
| Amplified | 1 | Misma semilla, 4 chunks: piedra entre y=130 y 249 — vanilla 0 bloques, amplified 11 792 |
| Menas (híbrido) | 2 | Misma semilla y terreno, 4 chunks, y −64…−17: diamante vanilla 88 → rico 137 (+49; configurado 2 venas × 6 × 4 chunks = 48 como máximo); oro vanilla 46 → 98 (+52; máx. 96) |
| Exportar / importar | 9 | Exporta con manifiesto cuyo SHA-256 coincide con `sha256sum`; importa una copia y la publica; rechaza YAML roto, archivo de 300 010 bytes (límite 262 144), ruta `../presets/flat.yml`; un preset con un bioma que el proveedor no controla queda como borrador y no se publica |
| Negativos | 9 | Nombre repetido, nombre con ruta, nombre con guion, preset inexistente, cargar un mundo ajeno, semilla en un preset publicado, subcomando desconocido, faltan argumentos, aplicar un preset al mundo principal (explica `bukkit.yml`) |
| Descargar / cargar | 4 | Descargar guarda y marca UNLOADED; volver a cargar; cambiar el preset de un mundo lo recarga |
| Permisos (bot sin op + LuckPerms) | 9 | Sin permisos: denegado. `worldforge.viewer`: ve mundos y diagnóstico, no crea ni edita; el autocompletado solo ofrece `inspect` y `list`. `worldforge.operator`: edita borradores, no importa ni aplica generación |
| Reinicio | 8 | `stop` guarda; al volver, los mundos administrados se cargan solos con su preset (incluido el cambiado), los bloques siguen, el principal conserva la plataforma, el historial aparece en el diagnóstico |
| Registro | 1 | Ninguna excepción ni error de WorldForge en los dos arranques |

### Defectos encontrados en esta fase

- **Plataforma ausente en mundos de `bukkit.yml`.** Se construía solo al
  crear con `/wf world create`; un mundo principal con `void-platform` habría
  dejado a los jugadores cayendo al vacío. Ahora la coloca el generador
  (cualquier camino); la construcción posterior queda para vanilla.
- **`inspect` decía «estructuras sí»** en mundos cuyo generador no las crea
  (leía solo el ajuste del servidor). Corregido después de esta pasada;
  se comprueba en la del escenario de la fase 7.
- Tres fallos y un umbral eran del propio escenario (nombre de mundo con guion,
  que el validador rechazó como debía; umbral de menas mal planteado). Se
  corrigieron en el escenario, no en el plugin.

### Sin probar todavía en esta fase

- Folia (fase 8): probado después; ver [«Folia»](#folia).
- Que el registro de biomas esté listo en un `onEnable` de STARTUP: no hizo
  falta la revalidación diferida en este arranque, pero no se forzó el caso.

## Fase 4 — plantillas `.nbt` y menas

`escenario-tpl.sh` y `escenario-principal.sh`.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Guardar | Una torre 5×4×3 con oro en el origen, diamante en la esquina opuesta y un cofre con tabla de botín; una plantilla 32×1×32 (el máximo). Rechaza: sobrescribir, 41 de ancho, 75 de alto, salirse del mundo, nombre con ruta, chunk sin cargar, mundo inexistente, número mal escrito. Un `.nbt` corrupto aparece marcado en la lista |
| Presets | Un preset con una plantilla que no existe, o con plantillas en proveedor `vanilla`, no se publica; `structure info` dice qué presets la usan |
| Generar | Plano: 68 torres colocadas, 0 sin sitio; el filtro de biomas deja fuera la regla de `the_void` (0); `rotation: random` da los cuatro giros; cada torre comprobada con `execute if block`: oro en el origen, diamante en la esquina girada, cofre con `chests/simple_dungeon`. Vacío: 93 salas a y=60 sin girar. 32×32: 7 colocadas, 0 sin sitio, en dos giros |
| Reinicio | El mundo vuelve con sus plantillas y sigue colocando (33 desde el arranque) |
| Archivo que falta | `preset validate` lo detecta y no se crean mundos con ese preset; con el archivo de vuelta, vale |
| Mundo principal | Con un preset de plantillas en `bukkit.yml`: WorldForge arranca antes de abrir los mundos; 111 islas colocadas lejos del aparecer, oro y diamante donde tocan; 606 bloques de diamante a la vista de las menas de WorldForge |
| Registro | Ninguna escritura fuera de la región del generador; sin excepciones |

Menas: además de lo medido en la fase 3 (+49 diamante, +52 oro frente a vanilla
con la misma semilla), se ven en el mundo principal con preset propio.

## Fase 5 — dimensiones por datapack

`escenario-dim.sh`.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Antes de reiniciar | Rechaza un generador que no cabe en el tipo (`nether` en `overworld`), un nombre con ruta y un id repetido. Crea `minas` (tall + overworld), `vacio`, `plana` (caves + flat), `infierno` (nether) e `islas` (end): archivos en el datapack, estado PENDING_RESTART y el aviso de reinicio |
| Reinicio | El servidor encuentra el datapack; las cinco quedan READY como `world_worldforge_<id>`; `minas` va de −64 a 383 y deja construir en y=380; `infierno` de 0 a 255 con techo y suelo de bedrock; `vacio` sin bloques y con `the_void`; `plana` con bedrock y hierba; `islas` con bioma del End. Una regeneración en una dimensión de datapack espera al reinicio |
| Quitar | PENDING_REMOVAL → tras reiniciar REMOVED; ya no es un mundo; su carpeta se conserva; se puede volver a crear |
| JSON roto a mano | Minecraft no arranca (`No key settings in MapLike…`); documentado en el README; al quitarlo vuelve a arrancar |

## Fase 6 — menú

`escenario-gui.sh`, con bots que hacen clic en los inventarios.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Presets | Abrir menú, lista y preset; duplicar pide el id por el chat y abre el borrador; fijar semilla; si otro cambia el borrador, no publica hasta reabrir; publicar; `reload` cierra los menús abiertos |
| Regeneración | Exige estar en ese mundo; radio → plan → segunda confirmación → descarga, respalda, edita y avisa; la marca desaparece y las capas quedan planas |
| Zonas | Crear (nombre por el chat) y quitar con confirmación; `protections.yml` queda sin ella |
| Permisos | Un `worldforge.viewer` abre el menú e inspecciona, pero descargar pide `worldforge.worlds.manage` y restaurar `worldforge.regeneration.plan` |

## Fase 7 — regeneración, respaldos y recuperación

`escenario-regen.sh`.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Plan y confirmación | Plan de 2 chunks: 1 a regenerar, 1 protegido; un código equivocado no confirma; el jugador que estaba dentro acaba en la aparición principal; el doble clic no lo lanza dos veces (1 confirmado, 1 rechazado). Chunk regenerado sin la marca y con capas planas; protegido y fuera del plan conservan la suya; COMPLETED; respaldo con manifiesto |
| Deshacer | Restaurar desde el respaldo devuelve la marca |
| Cancelar y caducar | Un plan cancelado o caducado no se confirma |
| `protections.yml` roto | La recarga avisa, no deja planificar, el diagnóstico lo marca y el archivo no se reescribe; arreglado, la zona sigue |
| Mundo principal | Espera al reinicio; se aplica al arrancar antes de abrir el mundo (línea 35 < 50 del registro); la marca desaparece y la plataforma sigue |
| Apagado a mitad (simulado) | Un trabajo que quedó en RUNNING se detecta al arrancar, pasa a NEEDS_RECOVERY, no se reanuda, bloquea el mundo y `cancel` no vale; restaurar al reiniciar devuelve la marca, el trabajo queda RESTORED y el mundo se desbloquea |

## Mundos como Multiverse

`escenario-mv.sh`, con un plugin de prueba (`otro-generador/`) que trae su
propio generador.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Nether y End | Presets de serie; entorno NETHER con suelo y techo de bedrock; THE_END con su bioma |
| Generador de otro plugin | Preset `provider: plugin` publicado; uno con un plugin que no está no se publica; uno que pide menas a ese generador tampoco; el mundo tiene el cristal del otro plugin y `inspect` dice de quién es el generador |
| Importar una carpeta | No se pisa una carpeta ajena al crear; importarla conserva lo que tenía; no se importa dos veces; carpeta inexistente, formato anterior a 26.1 y preset sin publicar se rechazan |
| Ir de un mundo a otro | `world tp` con errores claros (mundo, jugador, consola) |
| Gravedad de jugadores | Atributo `Gravity` medido con `/attribute`: 0,08 en el principal, 0,04 en `t_luna` (con caída segura 6), 0,12 en el mundo del otro plugin; vuelve a 0,08 y 3 al salir y al morir; sigue tras reiniciar |
| Reinicio | Los cinco mundos vuelven solos con su preset |

## Terrenos

`escenario-terreno.sh`, `escenario-motor.sh` y 13
unitarias. Las alturas se miden con el mapa `motion_blocking_no_leaves` y la
piedra contando por franjas de 128 bloques en el mismo chunk (misma semilla,
mismo sitio); se devuelve después.

| Bloque | Qué se comprobó y cómo |
|---|---|
| Archivos | 5 terrenos de serie; uno con `height-scale: 7` se rechaza y no se toca; terreno inexistente y tipo del Nether rechazados; 8 dimensiones escritas; `montanas` en `tall` avisa de que se cortan las cimas (sube hasta y≈363, el tipo aplana desde 304); editar un terreno que ya usa una dimensión no lo pisa |
| Reinicio | Las 8 dimensiones READY; `montes` va de −64 a 575; ningún ERROR de Minecraft al leer los JSON |
| Con los números de vanilla | `ident` = generador normal en 3 columnas, franja a franja: 7 429 / 766 / 0, 7 669 / 226 / 0 y 10 363 / 2 879 / 0 bloques de piedra |
| `height-scale: 2` (`alto2`) | Sobre y=64, 2,07 veces la piedra (15 872 → 32 811) en un chunk de montaña; columna a columna, mediana ×1,94 en 22 columnas |
| `height-scale: 3` y `height-offset: 96` (`torre`, towering) | En las mismas 22 columnas, 160 + ×2,96 sobre y=64 (mediana); 2 columnas por encima de y=320 y la más alta en y=395, que en vanilla no se puede |
| `suave` | En las mismas columnas, como mucho y=109 (el generador normal llega a 141) |
| Mundo normal (`terrain main`) | Con `archipielago`, los chunks nuevos de una dimensión con `minecraft:overworld` salen como los de `archi` (8 911 frente a 8 910 bloques en una columna, 5 099 en la otra) y distintos de vanilla; lo ya generado no cambia; de vuelta a `vanilla`, otra vez igual que `ident` (9 967 / 1 271 / 0) |
| Motor propio (`t_mont`, creado en caliente) | `inspect` dice modo y terreno; aparece en tierra; bedrock abajo y 14 106 bloques de pizarra profunda bajo y=0 en un chunk; biomas de picos, montaña hasta y=304 sin tocar el techo, océanos, un pueblo de Minecraft a 131 bloques (semilla al azar en cada pasada); vuelve tras reiniciar |
| Motor como mundo principal | Paper (14 pasos) y Folia (12): aparición en tierra, 169 chunks alrededor de un jugador, 90 bloques de carbón de Minecraft en el chunk de aparición (Paper), `world tp`, reinicio |

Lo que significa `height-scale`: estira la forma grande del terreno
(continentes, valles, montañas y su dentado) alrededor de y=64. El ruido 3D de
vanilla, que talla salientes y agujas, conserva su tamaño y no se estira; por
eso la columna más alta de vanilla no sale al doble. Medido: en un cuadro de 6 km
la columna más alta de vanilla está en y=225 y es una aguja (113 bloques de
piedra por encima de y=192 en todo su chunk); con `height-scale: 2` la piedra de
ese chunk sobre y=64 casi se duplica (17 202 → 33 236), pero esa columna queda en
y=231; lo más alto que se encontró de `alto2` en esa zona es y=307. Con esta
semilla, cerca del origen, las montañas anchas no dan para pasar de 320 solo al
doble: por eso esa prueba usa `torre` (×3 y 96 más arriba).

## Con Terralith

`escenario-terralith.sh`, con Terralith 2.6.2 en `world/datapacks` desde el
primer arranque y el mundo principal en vanilla (`MAIN=vanilla`).

| Qué se comprobó | Resultado |
|---|---|
| El servidor carga Terralith y el mundo principal tiene sus biomas | `terralith:highlands` a 684 bloques |
| Dimensiones de WorldForge (`ref` con el generador normal, `ident` y `suave` con terreno) | las tres READY tras reiniciar |
| `ref` hereda el terreno de Terralith; `ident` no, porque sale del jar del servidor | 2 de 2 columnas distintas (400,400: ref 13 729 / 200 frente a ident 13 306 / 418 bloques de piedra por franjas) |
| `ref` no tiene biomas de Terralith | `locate` no encuentra `terralith:highlands` (sin lithostitched, su lista de parámetros es la de vanilla) |
| `/wf terrain main` avisa de que Terralith también cambia el terreno del mundo normal | aviso con el nombre del zip; se deshace con `vanilla` |

## Folia

Tres escenarios en Folia 26.1.2-8, con una copia del jar a la que
`levantar-folia.sh` añade `folia-supported: true` (el jar final ya lo declara
tras esta tanda). **75 pasos, 0 fallos.**

| Escenario | Pasos | Qué se comprobó y cómo |
|---|---|---|
| `escenario-folia.sh` | 45 | Se habilita (10 presets) y el diagnóstico explica lo desactivado; SQLite; el principal lo genera WorldForge. Crear y descargar mundos se rechazan con su porqué. Duplicar, semilla, publicar y exportar; zonas. Menú con un jugador: 8 pasos (pantallas, validar, inspeccionar, diagnóstico). Dimensiones `minas` (tall) y `alpes` (towering con `terrain:montanas`) READY tras reiniciar, con su altura. Regeneración del principal: siempre al reiniciar; la marca del jugador desaparece y la plataforma sigue |
| `escenario-principal.sh` | 18 | Plantilla guardada por un jugador en su región y desde la consola en la del chunk; preset con plantillas y menas en `bukkit.yml`; WorldForge arranca antes de abrir los mundos; 111 islas colocadas lejos del aparecer con oro y diamante donde tocan; 606 diamantes de las menas a la vista |
| `escenario-motor.sh` | 12 | El motor (`montanas`) como mundo principal: aparición en tierra (0, 79, 0); un jugador entra y se generan 169 chunks a su alrededor, en paralelo por regiones, sin errores; `world tp` al Nether con `teleportAsync`; sigue tras reiniciar |

Folia no tiene `save-all` (comprobado): lo recién generado llega a disco al
parar o en el autoguardado, y el plan de regeneración lo avisa.

## Defectos encontrados después de la fase 3

Del plugin:

- **El motor de terreno no hacía montañas.** Con los números de vanilla su
  máximo era y=181: el ruido simplex de Bukkit casi nunca pasa de ±0,6. Se
  amplía ×1,6 y se recorta a ±1; `TerrainEngineTest` exige ahora un máximo por
  encima de 170 (sale 236).
- **Sin biomas de picos en terrenos altos del motor.** Con `height-scale: 1.7`
  el bioma se elegía con la altura ya aplastada bajo el techo, que nunca
  llegaba al umbral de los picos. Ahora se elige con la altura sin aplastar;
  lo cubre `a_tall_terrain_keeps_its_peaks`.
- `inspect` decía «estructuras sí» en mundos cuyo generador no las crea
  (visto en la fase 3); corregido y comprobado en `escenario-regen.sh`.

Los fallos de los escenarios de las fases 4 a 7 que eran del plugin se
corrigieron sobre la marcha y no se llevó una lista aparte.

De los propios escenarios, en la tanda final (corregidos y repetidos):

| Escenario | Qué falló | Corrección |
|---|---|---|
| `escenario.sh` | esperaba 7 presets de serie; ya son 10 (nether, end, montanas) | el número |
| `escenario-tpl.sh` | dos plantillas de 32×32 se solaparon y la segunda pisó el oro de la primera | solo se comprueban las que no se cruzan con otra; el solape está en «Límites» del README |
| `escenario-terreno.sh` | `locate biome` da el borde del bioma, no la cima; subir por la ladera se queda en cimas pequeñas; buscar la cumbre en 6 km agotó los 2 GB del servidor de prueba, y la cumbre de vanilla resultó ser una aguja del ruido 3D, que `height-scale` no estira | «por encima de y=320» se mide con un terreno de prueba (×3 y 96 más arriba) en las mismas columnas que el generador normal |
| `escenario-terreno.sh` | ref y archi diferían en 1 bloque de piedra de 8 911 | tolerancia de unos bloques: dos generaciones del mismo sitio pueden diferir en algún bloque de la frontera entre chunks |
| `escenario-terralith.sh` | el mundo principal era el vacío de WorldForge, sin biomas de Terralith | se levanta con `MAIN=vanilla` |
| `escenario-motor.sh` | en 26.1 el chunk de aparición no se queda cargado sin nadie cerca | se mira con el jugador dentro |

## Sin probar

- **El jar de venta en un servidor**: en todas las pruebas va el de
  desarrollo, que se salta la licencia. El módulo de licencia no usa
  schedulers (hace su comprobación en `onEnable`), así que no debería cambiar
  nada en Folia, pero no se ejecutó.
- **Terra o Iris de verdad** con `provider: plugin`: se probó con un plugin de
  prueba que trae su propio generador.
- `/wf terrain main` en Folia (solo escribe un archivo del datapack; en Paper sí
  se probó con reinicio).
- El proveedor `hybrid` en Folia: el populator se probó en Folia en un mundo
  plano, no sobre vanilla.
- Árboles en el mundo del motor: no se contaron (sí bedrock, pizarra profunda,
  carbón, océanos, biomas de picos y pueblos).
- Rendimiento con carga: no se midieron TPS ni tiempos con muchos jugadores;
  solo hubo uno o dos bots conectados.
- Que el registro de biomas esté listo en un `onEnable` de STARTUP en todos los
  casos: no hizo falta la revalidación diferida, pero no se forzó.
- Otras versiones: solo Paper 26.1.2-74 y Folia 26.1.2-8.
