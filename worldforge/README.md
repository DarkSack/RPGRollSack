# RPGRoll-WorldForge

Motor de generación y administración de mundos para **Paper 26.1.2** (y Folia, con límites): mundos vacíos, planos,
vanilla o híbridos a partir de *presets* versionados; varios mundos a la vez como con Multiverse
(Nether y End aparte, importar carpetas, ir de uno a otro, gravedad por mundo, generadores de otros
plugins); **terrenos al estilo Tectonic o Terralith** (más altos, más llanos, continentes enormes,
archipiélagos); menas y plantillas (mazmorras, ruinas, islas…) en los chunks nuevos; dimensiones
nuevas por datapack; y regeneración de chunks con plan, confirmación, respaldo y recuperación.
Todo desde comandos o desde un menú en el juego.

> Versión 1.0.0. Lo que está aquí está probado en un servidor real; los resultados
> están en [`docs/TEST-REPORT.md`](docs/TEST-REPORT.md). Lo que no está probado lo dice este
> documento en «Límites».

## Requisitos

| | |
|---|---|
| Servidor | Paper 26.1.2 (probado con el build 74) o Folia 26.1.2 (build 8; ver [Folia](#folia)) |
| Java | 25 |
| Dependencias | ninguna: SQLite y YAML vienen con Paper. Sin RPGRoll-Lib |

## Instalación

1. Copia `RPGRoll-WorldForge-<versión>.jar` en `plugins/`.
2. Arranca el servidor una vez. Se crea `plugins/RPGRoll-WorldForge/license.yml`: pega ahí tu clave
   (empieza por `RPGR-`) en lugar de `%%__LICENSE__%%` y vuelve a arrancar. Sin una clave válida,
   WorldForge se desactiva.
3. Con la licencia, se crean también `config.yml`, `messages.yml`, `protections.yml`, la base de
   datos `worldforge.db`, los presets de serie en `presets/` y las carpetas `drafts/`, `imports/` y
   `exports/`; `structures/` y `backups/` aparecen la primera vez que hacen falta.
4. `/wf diagnostics` dice si todo está en orden (plataforma, base de datos, presets, zonas
   protegidas, trabajos pendientes, dimensiones).

WorldForge carga en `STARTUP`: así puede generar también el mundo principal y aplicar
regeneraciones pendientes antes de que el servidor abra los mundos.

## Primeros pasos

```text
/wf preset list                         presets disponibles
/wf world create islas void-platform    un mundo vacío con plataforma
/wf world inspect islas                 qué generador usa, su altura, su aparición…
/wf gui                                 lo mismo con menús
```

Para que el **mundo principal** use un preset, ponlo en `bukkit.yml` y reinicia:

```yaml
worlds:
  world:
    generator: RPGRoll-WorldForge:void-platform
```

Si al arrancar WorldForge no puede dar ese generador (no hay licencia válida o el preset no existe o
tiene errores), **para el servidor** antes de abrir los mundos y el registro dice por qué. Si siguiera,
Paper generaría ese mundo con el terreno normal de Minecraft y luego habría que regenerar esos chunks.
Arréglalo, o quita la línea `generator:`, y vuelve a arrancar. La licencia aguanta 7 días sin poder
hablar con la tienda (por ejemplo, si está caída) desde la última vez que se comprobó.

## Presets

Un preset dice cómo es un mundo. Los publicados viven en `presets/`; los borradores en `drafts/`.
Un borrador no crea mundos: se publica con `/wf preset publish`, que lo valida antes.
Cada preset tiene una **versión** (hash de su texto); un mundo recuerda con qué versión se creó.

| Preset de serie | Qué hace |
|---|---|
| `vanilla` | Minecraft sin cambios |
| `amplified` | tipo amplificado de vanilla |
| `empty` | ni un bloque |
| `void-platform` | vacío con una plataforma de piedra de 5×5 en la aparición |
| `flat` | capas planas (bedrock, tierra, hierba) |
| `resource-rich` | terreno vanilla con venas extra de carbón, hierro, oro y diamante |
| `custom` | base para el tuyo: duplícalo y edítalo |
| `nether`, `end` | un Nether o un End aparte, con su propia semilla |
| `montanas` | cordilleras altas con el motor de terreno de WorldForge (ver [Terrenos](#terrenos)) |

### Proveedores

Cada preset elige un proveedor. Un campo que el proveedor no controla es un **error** al validar:
nunca se ignora en silencio.

| Proveedor | Terreno | Menas | Plantillas | Biomas a elegir | Estructuras vanilla |
|---|---|---|---|---|---|
| `vanilla` | Minecraft (normal, amplificado, biomas grandes) | — | — | — | sí/no |
| `empty` | nada | — | sí | sí | — |
| `flat` | capas del preset | sí | sí | sí | — |
| `hybrid` | Minecraft | sí | sí | sí | sí/no |
| `terrain` | el motor de WorldForge con un terreno de `terrains/` | sí | sí | por altura y clima | sí/no |
| `plugin` | el generador de otro plugin (Terra, Iris…) | — | — | — | lo que haga ese plugin |

### Formato

```yaml
schema: 1
id: mi-mundo                 # minúsculas, números, - y _ (hasta 32)
name: Mi mundo
provider: hybrid
seed: 12345                  # opcional; sin semilla, aleatoria
environment: normal          # normal, nether o the_end
structures: true             # estructuras vanilla (vanilla e hybrid)
spawn: {x: 0, y: 70, z: 0}   # opcional
platform: {block: stone, radius: 2, y: 69}   # opcional
biomes: [plains, forest]     # opcional: solo estos biomas existentes
ores:
- id: mas-diamante
  block: deepslate_diamond_ore
  replaces: [deepslate, tuff]
  veins-per-chunk: 2
  size: 6
  min-y: -64
  max-y: -16
  distribution: triangle     # uniform o triangle
player-gravity: 0.5          # opcional: gravedad de los jugadores en este mundo (0.1 a 4)
# provider: terrain  →  terrain: montanas         (un terreno de terrains/)
# provider: plugin   →  generator: Terra:OVERWORLD (como en bukkit.yml)
templates:
- id: mazmorra
  structure: mazmorra        # structures/mazmorra.nbt
  rarity: 40                 # una de cada 40 chunks, de media
  placement: underground     # surface (sobre el suelo) o underground
  min-y: -40
  max-y: 0
  rotation: random           # random o none
```

**Menas, plantillas y biomas solo afectan a chunks que se generan a partir de ahora.** Lo ya
generado no cambia; para eso está la regeneración.

### Editar, exportar, importar

- `/wf preset duplicate <origen> <nuevo>` crea un borrador; `/wf preset seed <borrador> <número>`
  le fija la semilla; edítalo en `drafts/` si quieres más; `/wf preset publish <borrador>`.
- `/wf preset export <id>` escribe el preset en `exports/` con un manifiesto SHA-256.
- Un `.yml` en `imports/` entra con `/wf preset import <archivo.yml>` **siempre como borrador**
  (tamaño máximo en `config.yml`, por defecto 256 KB).

## Varios mundos (como Multiverse)

```text
/wf world create minas nether             un Nether aparte (también: end)
/wf world tp minas                        ir a un mundo; /wf world tp minas Ana manda a otro jugador
/wf world import viejo flat               adoptar una carpeta de mundo que WorldForge no creó
```

- Cada mundo creado vuelve solo al reiniciar, con su preset.
- `import` no toca los bloques: solo pasa a administrar la carpeta
  (`world/dimensions/minecraft/<nombre>/`, el formato de 26.1). Una carpeta del formato de antes
  (`level.dat` suelto) se rechaza: hay que abrirla antes con Paper 26.1 para que la convierta.
- `player-gravity` cambia la gravedad de los **jugadores** (salto y caída; la caída segura se ajusta
  a la par) al entrar, al cambiar de mundo y al reaparecer. A los mobs no les afecta.
- `provider: plugin` usa el generador de otro plugin igual que `bukkit.yml` o el `-g` de Multiverse;
  si ese plugin no está, el preset no se publica. WorldForge no mete sus menas ni plantillas dentro
  de un generador ajeno.

## Plantillas (estructuras y mazmorras)

1. Construye en un mundo. Los cofres pueden llevar tabla de botín, con el comando de vanilla:
   `/data merge block <x> <y> <z> {LootTable:"minecraft:chests/simple_dungeon"}`.
2. `/wf structure save <nombre> <x1> <y1> <z1> <x2> <y2> <z2> [mundo]` guarda esa caja (esquinas
   incluidas, sin entidades) en `structures/<nombre>.nbt`. Como mucho **32×64×32**, y la zona tiene
   que estar cargada.
3. Úsala en un preset (`templates:`), publícalo y crea un mundo con él.

- Un cofre con tabla de botín la conserva en cada copia colocada; el botín se sortea al abrirlo.
- Una plantilla guardada **no se sobrescribe**: guarda otra con otro nombre. Así un preset
  publicado sigue generando lo mismo.
- Sirve cualquier `.nbt` de Minecraft (el formato de los bloques de estructura) de hasta 2 MB y
  32×64×32: déjalo en `structures/`.
- `surface` la pone sobre el primer bloque sólido desde arriba (atraviesa hojas, troncos y
  plantas; nunca sobre agua o lava). `underground` la pone con la base a una altura al azar entre
  `min-y` y `max-y` (con `min-y` igual a `max-y`, a una altura fija: sirve para islas en el vacío).
- `/wf world inspect <mundo>` dice cuántas ha colocado cada regla desde que se cargó el mundo y
  dónde fueron las últimas.
- Crear o cargar un mundo vuelve a comprobar sus plantillas: si falta el archivo, no sigue.

## Dimensiones

```text
/wf dimension create minas tall overworld
```

Escribe la dimensión en el datapack `worldforge` del mundo principal. **Minecraft solo lee
dimensiones nuevas al arrancar**: tras reiniciar aparece como el mundo `world_worldforge_minas`
(entra con `/execute in worldforge:minas run tp @s 0 100 0`).

| Tipo | | Generador | |
|---|---|---|---|
| `overworld` | cielo, día y noche, −64 a 319 | `void` | vacío (bioma `the_void`) |
| `caves` | con techo, sin cielo | `flat` | capas planas |
| `tall` | como el normal pero hasta 383 | `overworld` | terreno normal |
| `towering` | como el normal pero hasta 575 | `amplified` | terreno amplificado |
| `nether` | como el Nether, 0 a 255 | `nether`, `end` | solo con su tipo |
| `end` | como el End | `terrain:<id>` | un [terreno](#terrenos) (tipos del normal) |

`/wf dimension remove <id>` quita la definición; deja de cargarse al reiniciar y **la carpeta se
conserva**. Una dimensión de datapack usa el generador de Minecraft: menas y plantillas de
WorldForge no se aplican ahí.

## Terrenos

Un terreno cambia la **forma** del mundo, como Tectonic, Terralith o Stratos: más alto o más llano,
continentes más anchos, picos más afilados, biomas más grandes. Viven en `terrains/*.yml`; los de
serie se copian ahí la primera vez y sirven de ejemplo (`/wf terrain list`).

| De serie | | Tipo |
|---|---|---|
| `suave` | colinas bajas, cómodas para construir | `overworld` |
| `continentes` | continentes tres veces más anchos, biomas grandes | `overworld` |
| `archipielago` | islas y estrechos | `overworld` |
| `montanas` | cordilleras casi el doble de altas | `towering` |
| `extremo` | relieve 2,4 veces más alto, picos muy dentados y continentes anchos | `towering` |

```yaml
id: mio
description: lo que verás en /wf terrain list
base: overworld        # overworld, amplified o large_biomes
height-scale: 1.5      # 0.25 a 3: estira la forma del relieve alrededor de y=64
height-offset: 0       # -96 a 96: sube o hunde toda la tierra
peaks: 1.0             # 0 a 4: picos dentados (0 los quita)
continent-size: 1.0    # 0.25 a 8: continentes y océanos
erosion-size: 1.0      # 0.25 a 8: zonas llanas y escarpadas
ridge-size: 1.0        # 0.25 a 8: crestas y valles
biome-size: 1.0        # 0.25 a 8: zonas de clima (temperatura y humedad)
sea-level: 63
aquifers: true
ore-veins: true
```

Se usa de tres maneras:

| Dónde | Cómo | Qué genera |
|---|---|---|
| una dimensión nueva | `/wf dimension create alpes towering terrain:montanas` y reiniciar | el generador de Minecraft con los números cambiados: sus cuevas, acuíferos, biomas y estructuras |
| el mundo normal | `/wf terrain main montanas` y reiniciar (`vanilla` lo deshace) | lo mismo, en el mundo principal, como hace Terralith |
| un mundo creado en caliente | un preset con `provider: terrain` y `terrain: montanas` | el motor propio de WorldForge (abajo) |

**Por datapack** (dimensión y mundo normal): WorldForge copia el *noise router* de vanilla del
propio jar del servidor y cambia sus números; con todo a 1 sale **igual** que el generador normal
(probado contando la piedra por franjas de altura en las mismas columnas; dos generaciones del mismo
sitio pueden diferir en algún bloque suelto de la frontera entre chunks). Para alturas por encima de y≈240 hace falta un tipo más alto:
`/wf dimension create` avisa si las cimas se van a cortar. Un terreno ya escrito no se pisa: si lo
editas, guárdalo con otro id.

`height-scale` estira la forma grande del terreno (continentes, valles, montañas y su dentado). El
detalle 3D de vanilla, que talla salientes y agujas, conserva su tamaño: las agujas más altas no
salen al doble. Con `height-scale: 2`, medido: el doble de piedra sobre y=64 y el suelo ×1,94
columna a columna (mediana).

**El mundo normal** (`terrain main`) cambia `minecraft:overworld`, así que también lo usan los
mundos con proveedor `vanilla` y las dimensiones con generador `overworld`. Solo afecta a chunks
nuevos: donde lo nuevo toque lo ya generado habrá un corte. Otro datapack que también lo cambie
(Terralith) compite con él; el comando lo avisa. Pruébalo antes en una copia del mundo.

**El motor propio** (`provider: terrain`) es para mundos creados sin reiniciar, donde Minecraft
no deja usar un datapack: los mismos ajustes, con su propia forma (no idéntica a vanilla). Encima
Minecraft pone su superficie, cuevas, decoración y estructuras (probado: bedrock, pizarra profunda,
carbón y pueblos); los biomas los elige el motor por altura y clima (picos arriba, océanos en el mar). Las cimas se comprimen al acercarse al
techo del mundo (y=319) en vez de cortarse.

**Con Terralith instalado**: las dimensiones y el motor de WorldForge no heredan su terreno (salen
del jar del servidor); el generador `overworld` sí. En Paper sin el mod lithostitched, los biomas
de Terralith solo aparecen en el mundo principal.

## Regeneración

Volver a generar chunks ya existentes **borra lo que hay en ellos**. Por eso siempre es:

1. **Plan**: `/wf regen plan <mundo> radius <x> <z> <radio>` (o `area <x1> <z1> <x2> <z2>`, o
   `all`). Dice cuántos chunks, cuántos se saltan por estar protegidos o sin generar, cuánto ocupa
   el respaldo y cuándo se aplicará. No toca nada.
2. **Confirmación** con el código del plan: `/wf regen confirm <trabajo> <código>` (caduca a los
   15 minutos, `regeneration.confirm-minutes`).
3. **Respaldo** de los archivos de región afectados en `backups/`, comprobado con SHA-256 justo
   antes de editar. WorldForge nunca borra respaldos.
4. **Ejecución**: un mundo secundario se descarga, se edita y se vuelve a cargar al momento (los
   jugadores que haya dentro se mueven al mundo principal). El mundo principal se aplica en el
   **próximo arranque**, antes de abrir los mundos.

Deshacer: `/wf regen restore <trabajo>` planifica una restauración de ese respaldo, que sigue el
mismo camino (plan, código, respaldo de lo actual).

### Zonas protegidas

`plugins/RPGRoll-WorldForge/protections.yml` (o `/wf protection add|remove|list`): la regeneración
nunca toca un chunk protegido, ni al planificar ni al ejecutar. Alrededor de la aparición de cada
mundo hay una zona protegida de `spawn-radius-chunks` chunks (por defecto 2; 0 la quita). Si el archivo no se
puede leer, la regeneración queda **bloqueada** hasta arreglarlo.

### Si el servidor se apaga a mitad

Un trabajo que estaba ejecutándose queda en `NEEDS_RECOVERY` y bloquea ese mundo: no se reanuda
solo. `/wf regen info <trabajo>` dice qué pasó; puedes restaurar su respaldo
(`/wf regen restore`) o aceptarlo tal como quedó (`/wf regen accept`).

## Menú

`/wf gui`: mundos (cargar, descargar, inspeccionar, cambiar de preset, regenerar o proteger
alrededor de ti), presets (validar, duplicar, semilla, publicar, exportar, crear mundo),
regeneración (confirmar, cancelar, restaurar, aceptar), zonas protegidas y diagnóstico. Cada botón
pide su permiso al pulsarlo; lo destructivo pide confirmación; si otro admin cambia lo que estás
viendo, el menú no guarda encima. Plantillas, dimensiones, terrenos, `world import` y `world tp` van
solo por comandos.

## Permisos

| Permiso | Qué permite |
|---|---|
| `worldforge.admin` (op) | todo |
| `worldforge.operator` | preparar sin ejecutar nada destructivo: editar borradores, exportar, validar, planificar regeneraciones, guardar plantillas |
| `worldforge.viewer` | solo lectura: mundos, presets, dimensiones, zonas, trabajos, plantillas, diagnóstico |

Sueltos: `worldforge.worlds.{view,create,manage,teleport,teleport.others}`, `worldforge.terrain.main` (solo admin), `worldforge.presets.{view,edit,import,export}`,
`worldforge.generation.{validate,apply}`, `worldforge.regeneration.{view,plan,execute,cancel}`,
`worldforge.protection.{view,manage}`, `worldforge.dimensions.{view,create}`,
`worldforge.templates.{view,save}`, `worldforge.diagnostics`, `worldforge.reload`.
La consola también pasa por los permisos.

## Comandos

| Comando | Permiso |
|---|---|
| `/wf help` · `status` · `gui` | worlds.view |
| `/wf diagnostics` | diagnostics |
| `/wf reload-config` | reload |
| `/wf world list` · `inspect <mundo>` | worlds.view |
| `/wf world create <nombre> <preset>` | worlds.create |
| `/wf world load|unload <mundo>` | worlds.manage |
| `/wf world import <nombre> <preset>` | worlds.create |
| `/wf world tp <mundo> [jugador]` | worlds.teleport (otro jugador: worlds.teleport.others) |
| `/wf preset list` · `info` · `validate <id>` | presets.view |
| `/wf preset create|duplicate|seed|publish` | presets.edit |
| `/wf preset export <id>` · `import <archivo.yml>` | presets.export · presets.import |
| `/wf generation validate|preview` · `apply <mundo> <preset>` | generation.validate · generation.apply |
| `/wf regen plan|restore` · `confirm|accept` · `cancel` · `list|info` | regeneration.plan · execute · cancel · view |
| `/wf dimension list` · `create|remove` | dimensions.view · dimensions.create |
| `/wf terrain list` · `main <terreno|vanilla>` | dimensions.view · terrain.main |
| `/wf structure list|info` · `save` | templates.view · templates.save |
| `/wf protection list` · `add|remove` | protection.view · protection.manage |

## Folia

Probado en **Folia 26.1.2 build 8** con un jugador conectado (resultados en
[`docs/TEST-REPORT.md`](docs/TEST-REPORT.md)). Folia no deja crear, cargar ni descargar mundos con
el servidor en marcha (lanza `UnsupportedOperationException`), así que todo lo que cambia mundos
pasa por el arranque:

| | En Folia |
|---|---|
| Presets, exportar e importar, zonas protegidas, plantillas, diagnóstico, menú | igual que en Paper |
| Mundo principal con un preset (`bukkit.yml`): vacío, plano, plantillas, menas, el motor de terreno | sí; los chunks se generan en paralelo por regiones |
| Dimensiones por datapack, también con terreno y tipo `towering` | sí, al reiniciar |
| Regenerar | siempre al reiniciar, antes de abrir los mundos |
| `/wf world tp` a otro mundo cargado | sí (probado con un jugador, al Nether) |
| `/wf world create`, `load`, `unload`, `import` | no: el comando explica por qué y propone una dimensión por datapack |
| Mundos administrados creados antes en Paper | no se cargan; el registro lo avisa |
| Nether y End aparte, `provider: plugin`, gravedad por mundo | no: dependen de crear mundos en caliente |

Folia no tiene `save-all`: lo recién generado llega a disco en el autoguardado o al parar, y un plan
de regeneración lo avisa si toca chunks que todavía no están guardados.

## Límites

- Menas, plantillas y biomas de un preset solo se aplican a **chunks nuevos**.
- Crear, cargar y descargar mundos en caliente es solo de Paper.
- Dimensiones nuevas solo al reiniciar (así funciona Minecraft) y con el generador de Minecraft.
- Las plantillas se colocan sin entidades (soportes de armadura, marcos…); los bloques con datos
  (cofres, generadores de monstruos, carteles) sí van. Como mucho 32×64×32. Cada chunk decide la
  suya sin mirar a los vecinos, así que dos plantillas grandes pueden solaparse: súbeles `rarity`.
- No hay estructuras «registradas» de Minecraft (jigsaw, `/locate`): las plantillas son de WorldForge.
- La gravedad por mundo es solo de los jugadores. Sin física ni bloques nuevos: WorldForge solo
  usa lo que Minecraft ya trae.
- Terrenos por datapack: solo en dimensiones nuevas y en el mundo principal, al reiniciar. El mundo
  principal no puede cambiar de altura (−64 a 319).
- Biomas nuevos no: los terrenos y el motor reparten los que ya existen (también los de datapacks).

## Problemas frecuentes

| Síntoma | Qué mirar |
|---|---|
| El servidor se para al arrancar y el registro dice «Se para el servidor» | las líneas de antes dicen si falta la licencia o qué preset de `bukkit.yml` falla |
| «tiene errores» al publicar | `/wf preset validate <id>` lista cada campo con su problema |
| Un mundo administrado no carga al arrancar | el registro dice por qué (preset con errores, carpeta que falta, plantilla que falta) |
| «Regeneración BLOQUEADA» en el diagnóstico | `protections.yml` mal escrito: arréglalo y `/wf reload-config` |
| Un trabajo en `NEEDS_RECOVERY` | `/wf regen info`, luego `restore` o `accept` |
| «✘ Plantilla …: esa regla no se aplica» | falta o está rota `structures/<nombre>.nbt` |
| El servidor no arranca tras `terrain main` | borra `world/datapacks/worldforge/data/minecraft/worldgen/noise_settings/overworld.json` y arranca |
| «ya está escrito en el datapack con otros valores» | editaste un terreno que ya usa una dimensión: guárdalo con otro id |

## Compilar y probar

```bash
cd E:/Dev/RPGRoll && ./gradlew -p worldforge build
```

WorldForge se compila aparte del resto de RPGRoll y no entra en su `release`
([`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), D10 y D15). Las pruebas con servidor real
están en [`docs/TESTING.md`](docs/TESTING.md).
