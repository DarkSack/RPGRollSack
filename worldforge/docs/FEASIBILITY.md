# WorldForge — Matriz de viabilidad técnica

Fase 0, 2026-10-20; ampliada el 2026-10-21 con P16–P21 (terrenos) y con lo que
comprobaron los escenarios de las fases 3 a 8. Cada fila dice **qué se comprobó y
cómo**. «Probado» quiere decir que se ejecutó en un servidor real (Docker,
`eclipse-temurin:25-jre`), con un plugin de sondeo (P1–P15) o con WorldForge;
«API» quiere decir que el método existe en las fuentes de la API pero su efecto
no se ejecutó.

## 1. Versiones confirmadas

| Pieza | Valor | Fuente |
|---|---|---|
| Minecraft | **26.1.2** (world_version 4790, protocolo 775, datapack 101.1, resource pack 84) | `version.json` dentro del jar del servidor |
| Paper (servidor) | **26.1.2 build 74, STABLE** (2026-07-06) | `fill.papermc.io/v3/projects/paper/versions/26.1.2/builds/latest`; SHA-256 `1d70b1da…e5f7` = jar usado en las pruebas |
| Paper API | `io.papermc.paper:paper-api:26.1.2.build.74-stable` | `maven-metadata.xml` de `repo.papermc.io` |
| Folia (servidor) | **26.1.2 build 8, STABLE** (2026-05-06) | `fill.papermc.io/v3/projects/folia`; SHA-256 `607afd1c…f57b` verificado |
| Folia API | `dev.folia:folia-api:26.1.2.build.8-stable` | `maven-metadata.xml` |
| JDK | **25** | `java_version` del servidor; toolchain de RPGRoll |
| Gradle | **9.6.1** (wrapper del repo) | `gradle-wrapper.properties` |
| SQLite | `org.xerial:sqlite-jdbc:3.49.1.0` **incluido en el servidor** (Paper y Folia) | `META-INF/libraries.list` de ambos jars |
| YAML | `org.yaml:snakeyaml:2.2` incluido; Bukkit `YamlConfiguration` | `libraries.list` |

Paper ya va por la 26.3. La rama 26.1 está cerrada: la build 74 es la última.

**Decisión:** WorldForge compila contra `26.1.2.build.74-stable` y declara
`api-version: "26.1.2"`. El resto de RPGRoll compila contra
`26.1.1.build.29-alpha`; el cambio se hace solo en este módulo.

**Decisión:** WorldForge **no depende de RPGRoll-Lib**. La librería no declara
`folia-supported` y usa `BukkitScheduler` (`GUIListener`, `ChatPromptManager`),
que en Folia lanza `UnsupportedOperationException` (probado). Depender de ella
impediría cargar en Folia. WorldForge trae sus propios menús e idiomas y sí lleva
`:licensing` embebido, como el resto de productos.

## 2. Hallazgos probados en servidor real

| # | Prueba | Paper 26.1.2-74 | Folia 26.1.2-8 |
|---|---|---|---|
| P1 | `WorldCreator(name).generator(void).seed(123).createWorld()` | ✔ mundo `wf_void`, clave `minecraft:wf_void`, 209 ms, semilla propia | ✘ `UnsupportedOperationException` |
| P2 | Dónde guarda 26.1 un mundo creado así | **dentro del mundo principal**: `world/dimensions/minecraft/<nombre>/` | — |
| P3 | Generador híbrido (vanilla + `BlockPopulator`) | ✔ el populator escribe en cada chunk nuevo por `LimitedRegion` | sin probar |
| P4 | Generador vacío (`shouldGenerate*` = false) | ✔ aire en 0,64,0 | — |
| P5 | Dimensión por datapack (`data/<ns>/dimension/<id>.json` + `dimension_type` propio) | ✔ se carga al arrancar como mundo Bukkit `world_<ns>_<id>`, entorno `CUSTOM`, carpeta `world/dimensions/<ns>/<id>`, altura del tipo propio (−64…384) | ✔ igual |
| P6 | Datapack nuevo en `world/datapacks` | ✔ «Found new data pack file/…, loading it automatically»; solo al arrancar | ✔ igual |
| P7 | `World.regenerateChunk` | ✘ `UnsupportedOperationException: Not supported in this Minecraft version!` | — |
| P8 | Regenerar un chunk borrando su entrada del encabezado del `.mca` (formato Anvil) con el mundo **descargado** | ✔ el chunk vuelve a generarse; el vecino conserva el bloque marcado | — (Folia no descarga mundos) |
| P9 | Lo mismo en el **mundo principal**, con el servidor **apagado** | ✔ chunk (4,4) regenerado, chunk (0,0) intacto | método de archivo: aplica igual |
| P10 | `Bukkit.unloadWorld` | ✔ `true` | ✘ `UnsupportedOperationException` |
| P11 | `BukkitScheduler.runTask` | ✔ | ✘ `UnsupportedOperationException` |
| P12 | Leer un bloque desde el hilo de la consola | ✔ | ✘ «Thread failed main thread check» |
| P13 | Leer y escribir un bloque con `RegionScheduler.execute(world, cx, cz, …)` | — | ✔ |
| P14 | Mundos de `WorldCreator` tras reiniciar | no vuelven solos: el plugin tiene que recrearlos | — |
| P15 | `DatapackManager.getPacks()` | ✔ nombre, activo y origen (`world`, `built_in`, `feature`) | ✔ |
| P16 | Copiar las funciones de densidad de `minecraft:overworld` a un espacio propio con los parámetros cambiados, y una dimensión que use esos `noise_settings` | ✔ con los números de vanilla da la misma piedra por franjas que `minecraft:overworld`; con `height-scale: 2`, ×1,94 columna a columna | ✔ carga READY |
| P17 | `dimension_type` de 640 de alto (−64…575) | ✔ terreno hasta y=395 con un terreno de prueba (×3 y 96 más arriba) | ✔ carga READY |
| P18 | Sustituir `data/minecraft/worldgen/noise_settings/overworld.json` desde un datapack del mundo (como Terralith) | ✔ tras reiniciar, los chunks nuevos del principal usan el terreno; los ya generados no cambian | sin probar |
| P19 | Leer los JSON de worldgen de vanilla del jar del servidor en marcha (`getClassLoader().getResource("data/minecraft/…")`) | ✔ | ✔ |
| P20 | `ChunkGenerator` con alturas propias y superficie, cuevas, decoración y estructuras de Minecraft encima | ✔ bedrock, pizarra profunda, carbón, pueblos | ✔ mundo principal, 169 chunks en paralelo alrededor de un jugador |
| P21 | Terralith 2.6.2 (datapack) en el mismo mundo | ✔ hereda su terreno una dimensión que use `minecraft:overworld`; sus biomas no (su lista de parámetros, sin lithostitched, es la de vanilla) | — |

## 3. Matriz por característica

Estados: **S** soportado · **SR** soportado con restricciones · **DP** requiere datapack
(y reinicio) · **MOD** requiere mod o modificar el servidor · **NS** no soportado.
Las columnas Paper y Folia dicen dónde se comprobó (P = hallazgo de la sección 2,
o el escenario de `test-server/`); «sin probar» es literal.

| Característica | Mecanismo | API / formato | Paper | Folia | Limitaciones | Estrategia | Estado |
|---|---|---|---|---|---|---|---|
| Crear mundo nuevo | `WorldCreator` | `createWorld()` | P1, escenario | P1 ✘ | Folia no crea mundos en caliente | Paper: en caliente. Folia: dimensiones por datapack o el mundo principal | S (Paper) / DP (Folia) |
| Mundo vacío | `ChunkGenerator` sin fases | `shouldGenerate*` | P4, escenario | escenario-folia (principal) | — | proveedor `empty` | S |
| Plataforma inicial | la coloca el generador | `ChunkData` | escenario | escenario-folia | solo si el perfil la pide | parte del generador | S |
| Spawn | `getFixedSpawnLocation` | API | escenario | escenario-motor | — | parte del perfil; el motor busca tierra en espiral | S |
| Semilla | `WorldCreator.seed` | API | P1, escenario | — | — | campo del perfil | S |
| Altura del mundo | `dimension_type` (`min_y`, `height`) | JSON 26.1 | P5, P17 | P5, P17 | `WorldCreator` no admite tipo propio; el principal no cambia de altura | tipos `tall` (−64…383) y `towering` (−64…575) por datapack | DP |
| Vanilla sin cambios | sin generador | `WorldCreator` | P1, escenario | — | — | proveedor `vanilla` | S |
| Vanilla + extras | generador híbrido + populators | `BlockPopulator`, `LimitedRegion` | P3, escenario | populator en plano (escenario-principal) | el populator solo ve el chunk ± `getBuffer()` | proveedor `hybrid` | S |
| Terreno al estilo Tectonic/Terralith | funciones de densidad de vanilla copiadas con otros parámetros | JSON 26.1 (del jar del servidor, P19) | P16, P18 | P16 (dimensión) | solo chunks nuevos; dimensiones y principal al arrancar; mismos biomas | `terrains/*.yml` → datapack (`terrain:<id>`, `/wf terrain main`) | DP |
| Terreno propio para mundos en caliente | alturas propias + fases de Minecraft encima | `ChunkGenerator`, `BiomeProvider` | P20 | P20 | no es el router de vanilla: parecido, no igual | proveedor `terrain` | SR |
| Ruido de vanilla (`floating_islands`, `amplified`, `caves`…) | `noise_settings` | JSON 26.1 | P5 | P5 | solo en dimensiones por datapack | tipos y generadores de `/wf dimension` | DP |
| Elegir biomas existentes | `BiomeProvider` | `getBiome`, `getBiomes` | escenario | escenario-motor | no crea biomas nuevos | lista validada contra `Registry.BIOME` | SR |
| Crear biomas nuevos | `worldgen/biome` en datapack | JSON 26.1 | sin probar | sin probar | al arrancar | **no se ofrece** | NS (en WorldForge) |
| Menas (añadir) | populator en mundos con generador WorldForge | `LimitedRegion.setType` | P3, escenario | escenario-principal | solo mundos con generador de WorldForge; solo chunks nuevos | reglas → populator | SR |
| Menas en mundos existentes | — | — | — | — | no se recorre el mundo entero | **no se ofrece**; solo chunks nuevos | NS |
| Estructuras vanilla on/off | `shouldGenerateStructures` | API | escenario | escenario-motor | todo o nada | campo del perfil | S |
| Plantillas de mazmorra | `Structure.place(RegionAccessor, …)` dentro del populator | `StructureManager.loadStructure(File)` (`.nbt`) | escenario-tpl | escenario-principal | 32×64×32 como mucho; sin entidades; dos grandes pueden solaparse | `structure save` + reglas en el preset | SR |
| Estructuras registrables (jigsaw, `/locate`) | `worldgen/structure` + `structure_set` | JSON | sin probar | sin probar | al arrancar | **no se ofrece** | NS (en WorldForge) |
| Overworld / Nether / End existentes | mundos cargados | `Bukkit.getWorlds()` | P5 | P5 | el generador del principal no cambia en caliente | `bukkit.yml` (`generator: RPGRoll-WorldForge:<preset>`) + reinicio | SR |
| Nether y End aparte | `WorldCreator.environment` | API | escenario-mv | ✘ (P1) | — | presets `nether` y `end` | S (Paper) |
| Generador de otro plugin (Terra, Iris…) | `WorldCreator.generator(String)` | API | escenario-mv (plugin de prueba) | ✘ (P1) | WorldForge no añade menas ni plantillas a un generador ajeno | proveedor `plugin` | SR (Paper) |
| Importar una carpeta | cargar con `WorldCreator` | API | escenario-mv | ✘ (P1) | solo formato 26.1; los chunks existentes no cambian | `world import` | S (Paper) |
| Dimensión nueva | datapack `dimension` + `dimension_type` | JSON 26.1 | P5, escenario-dim | P5, escenario-folia | **requiere reinicio**; nombre `world_<ns>_<id>` fijado por el servidor | DRAFT → PENDING_RESTART → READY | DP |
| Gravedad de jugadores | atributo | `Attribute.GRAVITY`, `SAFE_FALL_DISTANCE` | escenario-mv | — (sin mundos en caliente) | solo jugadores | modificador al entrar en el mundo, quitado al salir o morir | SR |
| Gravedad de no vivos | `Entity.setGravity(boolean)` | API | — | — | solo encendida o apagada | **no se ofrece** | NS |
| Simulación con fuerzas o teletransportes | — | — | — | — | desincroniza, abre exploits | **no se implementa** | NS |
| Regenerar chunks (selectiva) | borrar entradas del encabezado `.mca` (region, entities, poi) con el mundo descargado | formato Anvil | P8, escenario-regen | escenario-folia (al arrancar) | principal o Folia solo al arrancar; los protegidos no se tocan | plan + código + respaldo verificado | SR |
| `World.regenerateChunk` | — | — | P7 ✘ | — | — | no se usa | NS |
| Protección en regeneración | exclusiones al calcular el plan | dominio propio | escenario-regen | — | no se detectan construcciones solas | zonas explícitas en `protections.yml` | S |
| Persistencia SQLite | JDBC | `sqlite-jdbc` del servidor | escenario | escenario-folia | — | executor propio de un hilo | S |
| GUI | inventarios | `Inventory`, `InventoryHolder` | escenario-gui | escenario-folia | sin campos de texto: texto por el chat | menús propios con sesión y versión | S |
| Folia: schedulers | `RegionScheduler`, `GlobalRegionScheduler`, `AsyncScheduler`, `EntityScheduler` | API de Paper | ✔ | P13, escenario-folia | — | capa `platform` que elige el scheduler | S |

## 4. Mecanismos descartados

- **NMS y reflexión**: no hacen falta para nada de lo anterior. No se usan.
- **`World.regenerateChunk`**: lanza en 26.1.2.
- **Editar región con el mundo cargado**: el servidor mantiene los `.mca` abiertos y
  sobrescribiría el cambio; solo se edita con el mundo descargado o antes de cargarlo.
- **Recargar el servidor (`/reload`)** para aplicar datapacks: no recarga dimensiones;
  el flujo pide reinicio.

## 5. Riesgos

- El encabezado `.mca` es un formato estable desde 1.2, pero no un API. WorldForge
  no comprueba ninguna firma: solo que el archivo tenga al menos el tamaño de las
  dos tablas (8 KiB). Si Mojang cambiara el formato, la regeneración podría dejar
  chunks mal; por eso respalda siempre antes, verifica el respaldo con SHA-256 y
  rechaza restaurar uno manipulado (`BackupsAndJobsTest`).
- `sqlite-jdbc` lo trae el servidor, no el plugin: si Paper lo quitara en otra
  versión, el diagnóstico lo dirá al arrancar.
- Los JSON de worldgen cambian entre versiones (26.1 añadió `attributes`,
  `default_clock`, `timelines` en `dimension_type`). Los terrenos se construyen a
  partir de los del servidor en marcha: si falta uno, da un error legible en vez de
  escribir un datapack roto (`TerrainTest`); si cambia su forma, la transformación
  podría no aplicarse y habría que revisarla. Un preset hecho para otra versión da
  un aviso, no un error.
- Un JSON de dimensión roto a mano impide arrancar al servidor (lo hace Minecraft,
  comprobado en `escenario-dim.sh`); el README explica cómo quitarlo.
- Folia: el plugin carga y funciona, pero crear, cargar o descargar mundos en
  caliente queda deshabilitado con mensaje.
