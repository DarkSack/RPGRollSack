# RPGRoll-Emotes — diseño

Fecha: 2026-11-19. Autor: Claude con Sack. Estado: borrador para aprobar.

## Intención y por qué

Sack quiere que los jugadores de la network puedan expresarse con animaciones del personaje completo: bailar, saludar, aplaudir y más. Lo pidió como base para «hacer muchas más cosas» después. El emote principal es **conjurar**, que acompaña a cada hechizo de RPGRoll-Magic para que lanzar magia se vea, no solo que salgan partículas.

Se hace como un módulo más del ecosistema RPGRoll: depende de la Lib, se vende en la tienda como los demás y los packs temáticos pueden traer emotes propios.

La prueba de concepto del 2026-11-19 (servidor local Paper 26.1.2 + ViaVersion, cliente 26.3, modo online) confirmó lo siguiente:
- BetterModel 3.6.0 anima al jugador completo con su skin real, sin mods en el cliente.
- Al cambiar animaciones basta `/bettermodel reload`; el pack de recursos no cambia.
- Un emote sigue reproduciéndose si el jugador camina, pero las piernas se quedan quietas: el jugador se desliza.

Material: `E:\Dev\scratchpad\emotes-20261119` (`emotes.py`, servidor `emotes-qa`) y la memoria `emotes-bettermodel`.

## Objetivos

1. **Lanzar emotes con un clic.** El menú `/emotes` lleva fondo propio con MenuSkin, como el resto de menús de jugador. También existe `/emote <id>`, con sugerencias al escribir.
2. **Desbloqueos.** Hay emotes libres y emotes que se desbloquean. Cada jugador tiene guardados los que ha desbloqueado y sus favoritos.
3. **Corte y enfriamiento.** Los emotes se cortan al moverse, recibir daño, atacar o montar. Tienen enfriamiento global por jugador. Cada emote puede cambiar sus reglas de corte.
4. **Integración con Magic.** Cada hechizo lanzado con éxito reproduce **conjurar** y respeta el enfriamiento propio del hechizo. Si el jugador va caminando, se usa la variante **conjurar_caminando**, con las piernas en paso de caminar.
5. **Fuentes de desbloqueo.** Pase, cajas, misiones, menús de NPC y tienda desbloquean emotes por comando: `/emotesadmin give <jugador> <id>`. Ya todos aceptan recompensas `command:`. Un permiso por emote también lo desbloquea.
6. **Contenido de salida: 22 emotes**, elegidos por Claude (la elección de contenido está delegada en él), más los de los dos packs temáticos (ver «Contenido»).
7. **Extensible por packs.** Un pack temático añade emotes con su `.yml` y su animación dentro de `packs/<pack>/RPGRoll-Emotes/`.
8. **Idiomas.** es, en y pt_BR, como el resto de módulos.

## No objetivos

- Bedrock. BetterModel no lo soporta y Sack dejó Bedrock de lado el 2026-11-16. Los jugadores de Bedrock no ven el emote, pero tampoco se les rompe nada.
- Motores de animación distintos de BetterModel. ModelEngine es de pago y FMM no anima jugadores.
- Emotes entre dos jugadores (abrazo, chocar los cinco): necesitan sincronizar dos modelos. Quedan para una versión futura.
- Un editor de animaciones dentro del juego.

## Anti-objetivos (aunque funcione, sería un fallo)

- Que un jugador use los emotes para tener ventaja. Por ejemplo, ocultar su hitbox o que un emote en bucle anule el retroceso o el daño.
- Que los jugadores necesiten permisos de BetterModel (`/bm ...`). El plugin lanza todo por la API.
- Que Magic necesite RPGRoll-Emotes, o Emotes necesite Magic, para arrancar.
- Lag en el servidor principal: nada de tareas por tick por cada jugador si no tiene un emote activo.

## Restricciones

- **Plataforma:** Java, Paper `api-version: "26.1.1"` (main va en 26.1.2) y versión de módulo alineada con la release vigente.
- **Dependencias:** `depend: [RPGRoll-Lib, BetterModel]`. Magic no hace falta ni como softdepend: los eventos viven en la Lib.
  - API de BetterModel: `io.github.toxicity188:bettermodel-bukkit-api:3.6.0` como `compileOnly`, desde Maven Central.
  - BetterModel tiene licencia MIT. El comprador lo instala desde Modrinth y no se empaqueta.
- **Clientes:** según BetterModel, el modelo de jugador solo se ve con servidor y cliente ≥ 1.21.4. Con ViaVersion, los clientes más viejos no ven la animación.
- **Licencia de venta:** el módulo lleva `license/license.yml` como los demás, con su slug de producto `emotes`. Precio, ficha y publicación son de Sack.
- **Proceso:** preguntar a Sack antes de cualquier `shadowJar` o `jar`; `gradlew test` y `compileJava` sí se pueden lanzar. Sin commits ni push salvo que él lo pida. Main solo con su OK y 0 jugadores.
- **Jugadores de Bedrock:** lo que se usa a menudo va con un clic en un menú (regla de Sack). También hay botón en el menú del servidor de Extras: el `.yml` del menú de la brújula llama a `emotes`.

## Enfoques considerados

1. **Orquestar sobre BetterModel (elegido por Sack).** El plugin decide quién, cuándo y si lo tiene desbloqueado, y BetterModel anima. Es lo más barato y ya está probado. Coste: depende de BetterModel y no hay Bedrock.
2. **Capa para varios motores.** Igual que la 1, pero con adaptadores para otros motores. Descartada: no existe hoy un segundo motor útil.
3. **Motor propio con ItemDisplay.** Sin dependencias. Descartada: semanas de trabajo y peor resultado.

Dentro de la opción 1 hay una interfaz pequeña, `EmoteEngine` (reproducir, parar, ¿está reproduciendo?). Tiene una implementación real para BetterModel y otra falsa para los tests.

## Diseño

### Piezas

| Pieza | Qué hace |
|---|---|
| `EmotesPlugin` | Arranque. Comprueba la licencia y si BetterModel está presente. Copia las animaciones, carga el catálogo y registra comandos, listeners y el puente con Magic. |
| `EmoteCatalog` | Lee `emotes/*.yml` y las carpetas de packs. Valida que cada `animation` exista en el `.bbmodel` y avisa de las que no. |
| `EmoteEngine` / `BetterModelEngine` | Reproduce y para la animación del jugador con la API de BetterModel. |
| `EmoteSession` | El emote activo de un jugador: id, inicio, posición inicial y si es bucle. Uno como máximo por jugador. |
| `EmoteService` | Lanzar, parar, enfriamiento y comprobaciones. Lo usan el menú, los comandos y el puente de Magic. |
| `CancelListener` | Movimiento, daño recibido, ataque, montar, planear con élitros, nadar, dormir, teletransporte, cambio de mundo y salir. Todos paran la sesión según las reglas del emote. |
| `EmotePlayerStore` | Un YAML por jugador en `data/players/<uuid>.yml`, como en Pass: desbloqueados, favoritos y último uso. |
| `EmotesMenu` | Menú paginado con MenuSkin. Ver «Menú». |
| `EmoteCommand` / `EmotesAdminCommand` | Comandos de jugador y de admin, con autocompletado. |
| `MagicBridge` | Escucha `SpellCastEvent` y lanza conjurar o su variante. |

### Formato de un emote (`emotes/<id>.yml`)

```yaml
id: saludar
name: "<gold>Saludar"          # MiniMessage, se traduce con lang si empieza por "lang:"
icon: PLAYER_HEAD               # o cabeza por textura, como en los demás menús
category: social                # social | baile | accion | magia | <categoría de pack>
animation: saludar              # nombre de la animación en el .bbmodel
model: rpgroll_emotes           # modelo de BetterModel (por defecto, el del plugin)
loop: false                     # true = sigue hasta que el jugador se mueva o pulse otro
unlock: free                    # free | locked  (locked = hace falta desbloquearlo o tener el permiso)
rarity: common                  # para color y orden en el menú
sound: { id: ENTITY_PLAYER_LEVELUP, volume: 0.6, pitch: 1.4 }   # opcional
particles: { type: NOTE, count: 6 }                             # opcional, en la cabeza
cancel: [move, damage, attack, mount]   # por defecto, todos menos los que se quiten aquí
hidden: false                   # true = no sale en el menú hasta tenerlo (secretos)
```

### Animaciones

- El jar trae `animations/rpgroll_emotes.bbmodel` con todas las animaciones base.
- Al arrancar se copia a `plugins/BetterModel/players/`. Si el archivo de destino tiene el mismo hash que la versión anterior del plugin, se reemplaza. Si el admin lo editó, se deja y se avisa en el log, igual que hace Crates con sus actualizaciones.
- Después se pide a BetterModel que recargue. Si la API no lo permite en caliente, se avisa de usar `/bettermodel reload`. Esto se averigua en el plan.
- Los packs traen su propio `.bbmodel`, con nombre de modelo distinto, y el plugin también lo copia.
- Las animaciones se generan con un script en `tools/emotes/construir_emotes.py`. Toma el `steve.bbmodel` de BetterModel como plantilla y aplica estas convenciones, ya probadas en el juego:
  - +z sube el brazo derecho por fuera.
  - +x lleva los brazos al frente.
  - +y gira el brazo derecho hacia dentro.
  - En el torso, −x inclina hacia delante.
- Las animaciones quedan como código revisable, igual que los modelos de la skill de minecraft-models.

### Reglas al lanzar

Un emote no se lanza si se cumple cualquiera de estas condiciones:
- no está desbloqueado ni se tiene el permiso `rpgrollemotes.emote.<id>`;
- hay enfriamiento (por defecto 3 s, en `config.yml`);
- el jugador está volando con élitros, nadando, montado, durmiendo o muerto;
- el jugador está en combate (`combat-tag-seconds`, por defecto 8 s desde el último daño hecho o recibido, solo por PvP o mobs).

Si ya hay un emote activo, el nuevo lo reemplaza. El jugador ve el motivo en la barra de acción.

### Corte

- **move:** el jugador se aleja más de 0,3 bloques en horizontal de donde empezó. Girar la cabeza no cuenta. Saltar en el sitio tampoco: solo cuenta la distancia horizontal.
- **damage:** recibe daño. **attack:** golpea.
- Los bucles (meditar, sentarse, bailes largos) siguen hasta que se cumpla un corte o el jugador pulse otro emote o `/emote stop`.
- Lo que no es bucle termina solo con la duración de la animación. El servicio se entera por el fin de la animación en BetterModel o, si eso no existe, por un temporizador con la duración leída del `.bbmodel`.

### Menú (`/emotes`)

```
┌ Emotes ─────────────────────────────────────┐
│ [Social] [Bailes] [Acción] [Magia] [Packs]   │  ← pestañas de categoría
│                                               │
│  👋 Saludar   👏 Aplaudir   🙇 Reverencia  …   │  ← 28 casillas por página
│  🔒 Robot (pase nivel 20)   ?  (secreto)      │  ← bloqueado: gris + cómo se consigue
│                                               │
│ ★ Favoritos: [1][2][3][4][5][6][7]           │  ← clic derecho en un emote = añadir
│ [◀] [Parar emote] [▶]                         │
└───────────────────────────────────────────────┘
```

- **Clic izquierdo:** lanza el emote y cierra el menú.
- **Clic derecho:** lo añade o quita de favoritos (máximo 7).
- Los favoritos también se lanzan con `/emote 1` … `/emote 7`, para atajos de macros.
- La línea de cómo se consigue un emote bloqueado sale del campo opcional `how-to-unlock` del `.yml`.

### Comandos y permisos

| Comando | Quién | Qué hace |
|---|---|---|
| `/emotes` | todos | Abre el menú. |
| `/emote <id\|1-7\|stop>` | todos | Lanza un emote, un favorito o para el actual. Sugiere solo los que tiene el jugador. |
| `/emotesadmin give\|take <jugador> <id>` | op | Desbloquea o quita un emote. Funciona desde la consola: así lo usan pase, cajas, misiones, NPC y tienda. |
| `/emotesadmin list <jugador>` | op | Muestra los desbloqueados. |
| `/emotesadmin play <jugador> <id>` | op | Fuerza un emote, sin desbloqueo ni enfriamiento. Para menús de NPC y escenas. |
| `/emotesadmin reload` | op | Recarga el catálogo y las animaciones. |

Permisos: `rpgrollemotes.use` (true), `rpgrollemotes.emote.<id>`, `rpgrollemotes.emote.*`, `rpgrollemotes.bypass.cooldown` y `rpgrollemotes.admin` (op).

### Integración con Magic

**Eventos nuevos en la Lib** (`:common`, paquete `com.sack.rpgroll.common.event.magic`). Son eventos Bukkit no cancelables. *Decisión de Claude:* van en la Lib, no en `:api`, porque `:api` va dentro del jar del core y Emotes solo depende de la Lib. Así Emotes los carga siempre, con o sin RPGRoll ni Magic.
- **`SpellCastEvent`** (jugador, `spellId`, `schoolId`, `tags`). Magic lo lanza en `SpellCastEngine.cast()` justo después de cobrar el coste y antes de `playCastFeedback`, solo cuando el lanzamiento tiene éxito.
- **`SpellChannelStartEvent`** (jugador, `spellId`, `schoolId`, `castTimeTicks`). Lo lanza `SpellChannelManager.start()` al empezar a canalizar un hechizo `HOLD`, como meteor.
- **`SpellChannelEndEvent`** (jugador, `spellId`, motivo `COMPLETED | MOVED | DAMAGED | OTHER`). Lo lanza `SpellChannelManager` al terminar o cancelar el canal.

Magic no sabe nada de Emotes y Emotes no compila contra Magic.

**`MagicBridge`** (activo si `magic.enabled: true`):
- **Canalización:** reproduce el emote en bucle `canalizar` (manos juntas, energía girando). Se configura por escuela con `magic.channel.schools.<schoolId>`; si no hay, usa `magic.channel.default: canalizar`.
  - Lo para con `SpellChannelEndEvent`.
  - Si el motivo es `COMPLETED`, enlaza con conjurar, que llega por `SpellCastEvent` justo después.
  - Magic ya obliga a estar quieto durante el canal, así que no hay que preocuparse por el deslizamiento.
- **Lanzamiento:** elige la animación por escuela con `magic.cast.schools.<schoolId>: <id de emote>`; si no hay, usa `magic.cast.default: conjurar`.
  - Si el jugador se mueve en ese momento (velocidad horizontal > 0,05), usa la variante `<id>_caminando` si existe.
- **Prioridad:** ignora desbloqueo, enfriamiento de emotes y combate, porque es un hechizo y no un emote. Reemplaza cualquier emote activo. Los cortes no aplican: conjurar dura 0,8 s y no se para al moverse, porque eso cortaría todos los hechizos lanzados corriendo.

Esto cambia Magic y la Lib, así que **RPGRoll-Lib y RPGRoll-Magic suben de versión** en la misma release que Emotes. El core no se toca.

### Contenido de salida (22, elegidos por Claude)

| Categoría | Emotes |
|---|---|
| Social | saludar, aplaudir, reverencia, asentir, negar, señalar, encogerse, facepalm, reír, llorar, pensar |
| Bailes | bailar, robot, saltitos (bucle), bailar_lento (bucle) |
| Acción | victoria, flexionar, saltar_alegría, sentarse (bucle), meditar (bucle) |
| Magia | conjurar, conjurar_caminando, canalizar (no salen en el menú; los usa Magic) |

- Todos son `free`, salvo robot, bailar_lento, flexionar y meditar, que son `locked`. Esos cuatro sirven para probar los desbloqueos por el pase y las cajas.
- Qué emote se da en cada nivel del pase o en cada caja queda como propuesta en el plan, para que Sack lo vea.
- **Mareas Malditas:** `pirata_hornpipe` (baile marinero en bucle) y `mareas_catalejo` (mirar por un catalejo imaginario).
- **Dinastía de Jade:** `jade_saludo_marcial` (puño contra la palma) y `jade_postura_grulla` (postura en bucle).
- Los de los packs se desbloquean con sus misiones o rangos, por comando.

### Riesgos conocidos (se comprueban en el plan)

1. **Velocity:** main recibe la skin por el forwarding de Velocity. Falta probar que BetterModel la recibe igual que en modo online.
2. **Ítem base:** BetterModel usa `leather_horse_armor` como ítem base, con su `item-namespace: bm_models`. Hay que comprobar que no choca con FMM ni con los modelos de RPGRoll en main, y si hace falta cambiarle el ítem en su config.
3. **Merge del pack (entra en este plan, decisión de Claude):** BetterModel genera su pack y main sirve el suyo por SackResourcePack (`srp publish`). Sin juntarlos, nadie en main ve los emotes, así que el trabajo no está terminado hasta que funcionen juntos.
   - El plan incluye que SRP recoja el pack de BetterModel (por su `merge-with-external-resources` o leyendo `BetterModel/build.zip`, lo que funcione), probado en `emotes-qa` con SRP instalado.
   - Instalarlo y desplegarlo en main es de Sack: queda preparado y documentado, y se despliega con su OK y 0 jugadores.
   - Los compradores sin SRP usan el pack de BetterModel como dice su documentación; la guía del módulo lo explica.
4. **API:** que la API pública de BetterModel 3.6.0 permita reproducir y parar la animación de jugador, y saber cuándo termina. Si no, se usa el temporizador con la duración del `.bbmodel`.
5. **Rendimiento:** BetterModel manda paquetes a cada jugador que ve el emote. Hay que medirlo con varios emotes a la vez en el servidor de prueba.
6. **Un solo servidor:** los desbloqueos se guardan en YAML en el propio servidor. Hoy la network tiene un único servidor de juego detrás de Velocity (main). Si algún día hay varios, habrá que llevarlos a una base compartida.

## Lo que queda al constructor

- Nombres de clases internas, estructura de paquetes y textos exactos de lang (en el tono de los demás módulos).
- La animación concreta de cada emote, siempre que se vea bien en el cliente real.
- Los valores por defecto no fijados aquí: partículas y sonidos de cada emote.

## Cómo se prueba

- **Tests unitarios:** catálogo y validación, almacén de jugadores, reglas de lanzamiento y corte (con un `EmoteEngine` falso), elección de animación del puente de Magic y autocompletado.
- **Test de packs:** todos los `RPGRoll-Emotes/*.yml` de `packs/` se parsean y sus animaciones existen.
- **Magic:** un test de que `SpellCastEngine.cast()` lanza el evento solo cuando el lanzamiento tiene éxito.
- **Prueba real en `emotes-qa`:** con el cliente 26.3, el plugin real y Magic, ver el menú, los desbloqueos, los cortes, conjurar quieto y caminando, y varios jugadores con bots. Antes de main, otra prueba detrás de Velocity.
