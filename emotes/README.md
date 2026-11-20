# RPGRoll-Emotes

Emotes y animaciones del jugador para **Paper 26.1.x**, hechos con [BetterModel](https://github.com/toxicity188/BetterModel):
menú con favoritos, desbloqueos por comando o permiso, sonido, partículas y música opcionales, y animaciones de
conjuro para RPGRoll-Magic. Trae 23 animaciones de fábrica y se amplía con archivos `.yml` y `.bbmodel`.

> Versión 1.1.12. Solo Java: los jugadores de Bedrock no ven los emotes.

## Requisitos

| | |
|---|---|
| Servidor | Paper 26.1.x |
| Dependencias obligatorias | **RPGRoll-Lib 1.1.12** y **BetterModel** |
| Opcionales | SackResourcePack (reparte el pack de BetterModel), RPGRoll-Magic (animaciones de conjuro) |
| Clientes | Java 1.21.4 o posterior. Los anteriores no se admiten |

## Instalación

1. Instala **BetterModel** en `plugins/` si no lo tienes. RPGRoll-Emotes no arranca sin él.
2. Copia `RPGRoll-Emotes-<versión>.jar` en `plugins/`, junto a RPGRoll-Lib.
3. Arranca el servidor. Se crea `plugins/RPGRoll-Emotes/` con `config.yml`, `lang/`, `emotes/` y `animations/`, y la
   animación de fábrica se copia a `plugins/BetterModel/players/rpgroll_emotes.bbmodel`, que BetterModel lee en su carga inicial. Con
   `/emotesadmin reload` se recarga BetterModel si cambió alguna animación.
4. Haz llegar el pack de recursos a los jugadores (siguiente sección). Sin pack no se ven las animaciones.

Como en los demás módulos de RPGRoll, el plugin comprueba la licencia al arrancar y se desactiva si no es válida.

Si editas a mano `plugins/BetterModel/players/rpgroll_emotes.bbmodel`, las actualizaciones no lo pisan: el log avisa y
se queda tu versión. Para volver a la de fábrica, borra ese archivo y reinicia.

## El pack de recursos

BetterModel genera el pack con los modelos y las animaciones, y los jugadores tienen que recibirlo.

**Con SackResourcePack.** Tras cada recarga correcta de BetterModel (también la inicial), Emotes copia el `build.zip`
de BetterModel al módulo `content/bettermodel` de SackResourcePack. El admin lo aplica con:

```text
/srp publish
```

No uses `/srp rebuild`: deja el hash antiguo y los clientes no descargan el pack nuevo. Para que la copia funcione,
BetterModel debe generar el pack como zip (`pack-type: zip` en su `config.yml`); si no se puede copiar, el log lo dice.
Para desactivar la copia, pon `resource-pack.register-in-sackresourcepack: false` (necesita reiniciar).

**Sin SackResourcePack.** Usa las opciones de pack de BetterModel para repartir el suyo (su `build.zip` o su
alojamiento propio; consulta su documentación). Emotes no sirve ningún pack por sí mismo.

## Cómo se juega

| Acción | Qué hace |
|---|---|
| `/emotes` | Abre el menú. Clic izquierdo lanza el emote; clic derecho lo añade o quita de favoritos. |
| **Shift + F** (cambiar de mano agachado) | Abre el menú. Se desactiva con `hotkeys.sneak-swap: false`. |
| `/emotes rapido` | Abre un diálogo con los favoritos, «Abrir menú» y «Parar». Pensado para atarlo a una tecla con un macro. |
| `/emote <id>` | Lanza un emote que tengas. |
| `/emote 1` ... `/emote 7` | Lanza el favorito de ese hueco. |
| `/emote stop` | Para el emote en marcha. |

`/e` es un alias de `/emote`. La tecla vanilla «Acciones rápidas» no se puede usar desde un plugin con `plugin.yml`; por
eso existe `/emotes rapido`.

### Qué corta un emote

Un emote en marcha se corta si el jugador se mueve más de `cancel.move-distance` (0,3 bloques en horizontal), recibe
daño, ataca, se monta, se teletransporta, cambia de mundo, planea con élitros, nada, se duerme o muere. Los bucles siguen
hasta que ocurra algo de esto o el jugador pare o lance otro emote.

No se puede lanzar un emote:

- durante `combat-tag-seconds` (8 s) tras recibir daño de un jugador o un mob, o golpear a un ser vivo (los soportes de
  armadura no cuentan). El daño de caída, fuego y similares corta el emote pero no marca combate;
- antes de que pase el enfriamiento (`cooldown-seconds`, 3 s). El permiso `rpgrollemotes.bypass.cooldown` lo ignora;
- si el emote está bloqueado y el jugador no lo tiene.

## Comandos y permisos

| Comando | Permiso | Qué hace |
|---|---|---|
| `/emotes` | `rpgrollemotes.use` | Abre el menú. `/emotes rapido` abre el diálogo de favoritos. |
| `/emote <id\|1-7\|stop>` | `rpgrollemotes.use` | Lanza un emote, un favorito o para el actual. |
| `/emotesadmin give <jugador> <emote>` | `rpgrollemotes.admin` | Desbloquea un emote. |
| `/emotesadmin take <jugador> <emote>` | `rpgrollemotes.admin` | Quita un emote desbloqueado. |
| `/emotesadmin list <jugador>` | `rpgrollemotes.admin` | Lista los emotes que tiene. |
| `/emotesadmin play <jugador> <emote>` | `rpgrollemotes.admin` | Lo reproduce a un jugador conectado, sin desbloqueo ni enfriamiento. |
| `/emotesadmin reload` | `rpgrollemotes.admin` | Recarga config, idiomas, animaciones y catálogo. |

| Permiso | Por defecto | Para qué |
|---|---|---|
| `rpgrollemotes.use` | todos | Usar los emotes desbloqueados. |
| `rpgrollemotes.emote.<id>` | nadie | Tener ese emote sin desbloquearlo. |
| `rpgrollemotes.emote.*` | nadie | Tener todos. |
| `rpgrollemotes.bypass.cooldown` | op | Ignorar el enfriamiento. |
| `rpgrollemotes.admin` | op | Administrar. Incluye todos los emotes y el bypass. |

Los desbloqueos se guardan por jugador en `plugins/RPGRoll-Emotes/data/players/<uuid>.yml`.

### Desbloquear emotes desde el pase, cajas, misiones y NPC

Un emote con `unlock: locked` se da con `/emotesadmin give <jugador> <emote>`. Funciona desde la consola, así que sirve
como recompensa de cualquier sistema que ejecute un comando: pase de batalla, cajas, misiones, menús de NPC o tienda.
Pon como recompensa el comando. En los módulos de RPGRoll (por ejemplo, `command:` en el pase) el jugador es `{player}`:

```text
emotesadmin give {player} bailar_lento
```

En plugins de terceros, usa el marcador que documente ese plugin (a menudo `%player%`). Otra vía es dar
el permiso `rpgrollemotes.emote.<id>` desde tu plugin de permisos.

## Formato de un emote

Cada emote es un `.yml` dentro de `plugins/RPGRoll-Emotes/emotes/`. Se lee la carpeta entera, subcarpetas incluidas, y
vale cualquier nombre de archivo. Tras añadir o editar uno, ejecuta `/emotesadmin reload`.

```yaml
id: saludar                 # opcional: si falta, se usa el nombre del archivo
name: "lang:emotes.saludar" # texto con colores, o «lang:<clave>» para traducirlo con lang/
icon: FEATHER               # material de ítem; si no existe, se muestra papel
category: social            # pestaña del menú: social, baile, accion, magia o la que quieras
animation: saludar          # OBLIGATORIO: nombre de la animación dentro del modelo
model: rpgroll_emotes       # modelo de BetterModel; por defecto, el limb-model de config.yml
loop: false                 # true: se repite hasta que algo lo corte
unlock: free                # free | locked
rarity: common              # common, uncommon, rare, epic, legendary
hidden: false               # true: no sale en el menú hasta que el jugador lo tiene
how-to-unlock: "Completa la misión X"   # opcional: texto del menú para un emote bloqueado
sound: { id: "minecraft:block.wool.place", volume: 0.6, pitch: 1.3 }   # opcional
particles: { type: NOTE, count: 4 }     # opcional: sobre la cabeza
cancel: [move, damage, attack, mount]   # opcional: ver la tabla
music:                                  # opcional: ver «Música»
  sound: "mipack:emotes.mi_cancion"
  volume: 1.0
  length: 62.5
```

| Clave | Notas |
|---|---|
| `animation` | Obligatoria. Si no existe en el modelo, el emote se quita al cargar y el log lo avisa. |
| `unlock` | `free` o `locked`. Otro valor invalida el emote. |
| `rarity` | Ordena el menú (de común a legendario) y se muestra en la descripción. |
| `hidden` | Solo lo ve en el menú quien ya lo tiene. Los emotes de magia (`canalizar`, `conjurar`) van ocultos. |
| `sound` | `id` es una clave de sonido de Minecraft o de tu pack. `volume` y `pitch` valen 1 por defecto. |
| `particles` | `type` es un nombre de partícula de Bukkit, como `NOTE` o `HEART`. Un tipo desconocido, o que pida datos extra, se ignora y el emote sale igual. |
| `cancel` | Lista de motivos que cortan este emote: `move`, `damage`, `attack`, `mount`, `glide`, `swim`, `sleep`. Sin la clave, cortan todos. Un motivo desconocido invalida el emote. Teletransporte, cambio de mundo, muerte y dormirse en una cama cortan siempre, estén o no en la lista. |

Un `.yml` roto no detiene el servidor: se salta ese emote y el log dice cuál y por qué. Si dos emotes comparten `id`,
gana el último que se lee.

### Música

`music` reproduce un sonido de tu pack de recursos mientras dura el emote.

- `sound`: clave del sonido con espacio de nombres (`mipack:emotes.mi_cancion`). Si falta o no es una clave válida, el
  log avisa y se ignora la música; el emote sigue.
- `volume`: de 0 a 4 (1 por defecto).
- `length`: duración del sonido en segundos. En un emote con `loop: true`, la música se repite cada `length` segundos.
  Con 0, o sin ponerlo, suena una sola vez.
- Suena en la fuente «Discos»: cada jugador la baja con ese control de volumen. Se para al acabar el emote.

**Los emotes de fábrica no traen música.** Si añades música, usa solo audio original o con la licencia adecuada, y
ponlo tú en tu pack de recursos. No uses música comercial ni de otros juegos.

## Añadir animaciones propias

1. Crea un modelo en Blockbench a partir de la plantilla de jugador de BetterModel (`tools/emotes/plantilla/steve.bbmodel`)
   y añade tus animaciones. El nombre del archivo es el nombre del modelo.
2. Copia el `.bbmodel` a `plugins/RPGRoll-Emotes/animations/`. Al arrancar o con `/emotesadmin reload`, se copia a
   `plugins/BetterModel/players/` y se recarga BetterModel. El nombre `rpgroll_emotes.bbmodel` está reservado para el de fábrica.
3. Crea el `.yml` del emote con `model: <nombre del archivo sin .bbmodel>` y `animation: <nombre de tu animación>`.
4. Haz llegar el pack nuevo a los jugadores (ver «El pack de recursos»).

Para ver una animación sin pasar por el menú: `/bm play <modelo> <animación>`. Las animaciones de fábrica se generan con
[`tools/emotes/construir_emotes.py`](../tools/emotes/LEEME.md); los packs temáticos de RPGRoll traen sus emotes y su
`.bbmodel` con este mismo formato.

## Integración con RPGRoll-Magic

Si RPGRoll-Magic está instalado, sus hechizos animan al jugador. Emotes solo escucha los eventos de hechizo de
RPGRoll-Lib, así que no necesita nada más. `magic.enabled: false` lo apaga.

- **Hechizo instantáneo:** reproduce `conjurar`; si el jugador se mueve al lanzarlo, `conjurar_caminando`.
- **Hechizo mantenido o canalizado (HOLD):** reproduce `canalizar` en bucle mientras dura la canalización. Si el
  jugador se mueve, la canalización se cancela y el bucle para.
- Un hechizo reemplaza al emote que esté en marcha.
- Cada escuela puede tener sus animaciones: en `magic.cast.schools` y `magic.channel.schools` pon, por ejemplo,
  `fire: conjurar_fuego`. Esos emotes los creas tú, con su `.yml` y su animación.

## Configuración (`config.yml`)

| Clave | Por defecto | Qué hace |
|---|---|---|
| `language` | `es` | Idioma de los mensajes: `es`, `en`, `pt_BR`. |
| `cooldown-seconds` | `3` | Espera entre emotes de un jugador. |
| `combat-tag-seconds` | `8` | Segundos sin poder lanzar tras daño. |
| `cancel.move-distance` | `0.3` | Bloques de movimiento horizontal que cortan un emote. |
| `favorites.max` | `7` | Huecos de favoritos (1 a 7). Necesita reiniciar. |
| `hotkeys.sneak-swap` | `true` | Shift + F abre el menú. |
| `hotkeys.quick-actions` | `true` | Activa `/emotes rapido`. |
| `limb-model` | `rpgroll_emotes` | Modelo de BetterModel por defecto de los emotes. Necesita reiniciar. |
| `magic.enabled` | `true` | Animaciones de conjuro. |
| `magic.walking-speed` | `0.05` | Umbral para usar la variante `_caminando` del conjuro. Hoy no cambia nada: el plugin mira si el jugador pulsa alguna tecla de movimiento, no su velocidad. No frena al jugador. |
| `magic.cast.default` / `magic.channel.default` | `conjurar` / `canalizar` | Emote de cada hechizo. |
| `magic.cast.schools` / `magic.channel.schools` | vacío | Emote propio por escuela. |
| `menus.pack` | vacío | Fondos dibujados del paquete de recursos. Vacío = menús de cofre normales. |
| `resource-pack.register-in-sackresourcepack` | `true` | Copiar el pack de BetterModel a SackResourcePack. Necesita reiniciar. |

## Límites

- **Solo Java.** Los jugadores de Bedrock no ven los emotes.
- **Cliente 1.21.4 o posterior.** Los anteriores no se admiten.
- **Skins.** BetterModel viste las animaciones con la skin del jugador. En un servidor en modo offline muestra su skin
  de reserva, no la del jugador.
- **Detrás de Velocity**, el reenvío de información del jugador (player info forwarding) tiene que estar activado para
  que BetterModel reciba la skin. No se ha probado todavía en una red real.
- Sin el pack de recursos, el jugador no ve las animaciones. Con SackResourcePack, tras cada cambio de pack hay que
  ejecutar `/srp publish`.
