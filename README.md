# 🛡️ RPGRoll Framework

> **RPGRoll**

Un framework RPG modular para **Minecraft Java Edition 26.1.1 (Paper)** diseñado para transformar un servidor vanilla en una experiencia de rol completa inspirada en juegos como **Dungeons & Dragons**, MMORPGs clásicos y sistemas RPG modernos.

Ya no es un único plugin: es un **ecosistema de 1 core + 20 addons independientes**, cada uno instalable por separado, más un asset pipeline de resource packs (`SackResourcePack`) que no depende de ningún otro módulo.

📖 **Documentación completa:** [rpg-roll-docs.vercel.app](https://rpg-roll-docs.vercel.app/)

☕ **¿Te gusta el proyecto?** Podés apoyarlo con una donación en [Ko-fi](https://ko-fi.com/sackito).

> ⚠️ **Estado del proyecto:** En desarrollo activo (Alpha)

---

# 🆕 Novedades recientes

Cambios recientes aplicados en todo el ecosistema:

- 🚀 **Migración completa a Paper 26.1.1 (Java 25)** — se abandona el soporte a 1.21.1: los 21 módulos (core + 20 addons) ahora compilan contra la nueva API de Paper. Incluye la renombrada de todas las constantes de `Attribute` (se les quitó el prefijo `GENERIC_`/`PLAYER_`/`ZOMBIE_`) y la adaptación a que `Sound` dejó de ser un `Enum` plano
- 🖱️ **Botones "Volver" arreglados en todas las GUIs** — encantamientos, tiendas de NPCs, chat, crates y SackEffects: el botón `Volver`/`Cerrar` reabría el menú anterior sin cerrar de verdad el inventario activo, dejando la GUI "congelada". Corregido en 59 archivos (el `reopen()` de cada GUI ahora usa `open()` en vez de `build()`)
- 🧟 **Mobs de RPGRoll-Mobs vuelven a moverse y patrullar** — se removía la IA nativa de Bukkit (`Mob#setAware(false)`) para evitar que interfiriera con el motor de IA propio, pero esa misma llamada también bloquea el sistema de pathfinding de Paper, dejando a los mobs completamente estáticos. Ya no se desactiva
- ✏️ **`/renchant remove` corregido** — ahora resuelve el encantamiento por su id real antes de intentar removerlo, en vez de fallar silenciosamente
- ⌨️ **Autocompletado (Tab) real en todos los plugins** — cada comando (`/rpg`, `/rpgeffects`, `/mobadmin`, `/renchant`, etc.) sugiere ahora desde el contenido real registrado: razas, clases, encantamientos, efectos, tipos de entidad, ítems, jugadores online, mundos, etc. — no solo la lista de subcomandos
- 🎨 **Colores de texto 100% configurables desde YAML** — todos los módulos soportan ahora los 4 formatos de color en cualquier mensaje configurable: códigos clásicos (`&b&l`), hex por carácter (`&#RRGGBB`), hex estilo BungeeCord (`&x&R&R&G&G&B&B`) y MiniMessage completo, incluyendo gradientes (`<gradient:#54daf4:#545eb6>texto</gradient>`). Antes el color venía parcialmente fijo en el código Java
- 🔎 **Buscador global en la documentación** — la web de docs ahora tiene una barra de búsqueda (Ctrl/Cmd+K) que indexa cada página y cada sección de cada addon
- 📚 **Ejemplo de referencia "todos los campos" en cada addon** — cada uno de los addons con contenido YAML incluye un archivo `reference_full.yml` (o equivalente) que documenta absolutamente todas las opciones disponibles para su tipo de contenido principal, espejado en la web de documentación
- 💰 **Nuevo addon: RPGRoll-Economy** — monedas múltiples, wallets, bancos y préstamos, mercado dinámico con oferta/demanda real, tiendas de jugador, subastas, empresas, impuestos y libro mayor de transacciones. También se registra como proveedor del servicio Economy de Vault, dejando funcional al resto del ecosistema (Jobs, Guilds, Items, Workers) sin tocar esos módulos
- ⚒️ **Nuevo addon: RPGRoll-Crafting** — recetas personalizadas con ingredientes/condiciones ricas y sistema de calidad, estaciones de crafteo propias (multi-etapa, con combustible), puente con las estaciones vanilla que exponen una API de receta genérica (mesa de crafteo, familia de hornos, cortadora de piedra, mesa de herrería) y motores dedicados de yunque y fermentación
- 🎭 **Reskin visual de mobs y animales** — RPGRoll-Mobs y RPGRoll-Ranching pueden ahora mostrar un modelo completamente custom (no solo el mob/animal vanilla) sin depender de ModelEngine/BetterModel: la entidad real queda invisible y se le monta una entidad `ItemDisplay` como pasajero, portando un ítem con `CustomModelData` propio. Ranching usa un reskin único por raza; **RPGRoll-Mobs va más allá y soporta una lista de skins por mob** (`model.skins`), sorteada por peso al spawnear y persistida (nunca vuelve a sortear en la vida del mob). Las texturas se sincronizan solas hacia SackResourcePack (si está instalado) desde una carpeta `resourcepack/` en cada plugin — mismo mecanismo que ya usa RPGRoll-Items
- 🎣 **RPGRoll-Fishing ahora sincroniza sus texturas solo** — las especies ya soportaban `custom-model-data`, pero faltaba conectar esa carpeta `resourcepack/` con SackResourcePack; ahora se sincroniza sola al arrancar el plugin, igual que Items/Mobs/Ranching

---

# ✨ Características

## Núcleo (`RPGRoll` / core)

- 🎭 Clases y 🧬 Razas (con bonificaciones, efectos pasivos y habilidades únicas)
- ⚒️ Trabajos (Jobs)
- ⭐ Niveles y 📈 Experiencia (XP)
- ❤️ Estadísticas y atributos del jugador (asignables)
- 💎 Traits y ⚔️ Skills (habilidades activas usables)
- 🖼️ Editores visuales (GUI) para razas, clases, trabajos, skills y traits
- 🧩 API pública (módulo `api`) para que otros plugins integren contra RPGRoll

## Ecosistema de addons

Cada addon extiende el core con un sistema completo propio, construido por **componentes/YAML configurables** (no hardcodeado en Java) y con su propio editor visual (GUI "Studio"):

| Addon                   | Qué agrega                                                                                                |
| ----------------------- | --------------------------------------------------------------------------------------------------------- |
| 🧑‍🤝‍🧑 **NPCs**             | NPCs interactuables (tiendas, diálogos, menús) vía ProtocolLib                                            |
| 🎁 **Crates**           | Cajas con ruleta de premios, integrables con DecentHolograms                                              |
| ✨ **Enchantments**     | Encantamientos personalizados basados en componentes (triggers/condiciones/efectos)                       |
| 📜 **Quests**           | Misiones ramificadas por etapas, objetivos, condiciones y eventos                                         |
| 🎒 **Items**            | Ítems personalizados con stats, sockets, skins, mejoras y recetas                                         |
| 🌟 **Ascension**        | Progresión avanzada: evolución de razas, especialización de clases, talentos, prestigio, afinidades       |
| 👹 **Mobs**             | Mobs, jefes e invocaciones a medida (componentes, fases, IA propia, loot, reskin visual propio)           |
| 🏰 **Dungeons**         | Mazmorras instanciadas: salas, oleadas, jefes, dificultades, ranking                                      |
| 🛡️ **Guilds**           | Equipos temporales (Teams) y organizaciones permanentes (Guilds)                                          |
| 💬 **Chat**             | Canales, proximidad, idiomas, roles, whisper, antispam, reacciones, logs                                  |
| 🎆 **SackEffects**      | Librería reusable de partículas con formas, sonidos, títulos/actionbar/bossbar                            |
| 🌀 **RPGRoll-Effects**  | Motor de efectos de estado (buffs/debuffs/auras) aplicable desde cualquier addon                          |
| 🪄 **RPGRoll-Magic**    | Escuelas de magia, hechizos por componentes, maná, catalizadores, grimorios, runas                        |
| 🍂 **RPGRoll-Seasons**  | Calendario y estaciones: clima, temperatura por bioma, vegetación dinámica, eventos mundiales             |
| 🎣 **RPGRoll-Fishing**  | Pesca como profesión: especies, cañas/carnadas, minijuego, tesoros, enciclopedia de capturas              |
| 🐄 **RPGRoll-Ranching** | Ganadería viva: genética hereditaria, reproducción, nutrición, bienestar, enfermedades/vacunas, reskin visual por raza |
| 👷 **RPGRoll-Workers**  | NPCs trabajadores autónomos con IA por reglas, necesidades, logística y economía                          |
| 📦 **SackResourcePack** | Asset pipeline standalone: fusión de resource packs, CustomModelData, build+hash, distribución automática |
| 💰 **RPGRoll-Economy**  | Monedas múltiples, wallets, bancos/préstamos, mercado dinámico con oferta/demanda, tiendas, subastas, empresas, impuestos y libro mayor — proveedor del servicio Economy de Vault |
| ⚒️ **RPGRoll-Crafting** | Recetas personalizadas (ingredientes/condiciones ricas, calidad), estaciones de crafteo propias, puente con estaciones vanilla y motores de yunque/fermentación |

Ver el detalle completo de cada uno (comandos, permisos, formato YAML, ejemplos) en el sitio de documentación (`UI/`).

---

# 🧩 Ecosistema de módulos

| Módulo           | Comando raíz                                     | Depende de (hard)       | Integraciones opcionales                                                | Precio estimado |
| ---------------- | ------------------------------------------------ | ----------------------- | ----------------------------------------------------------------------- | --------------- |
| `core` (RPGRoll) | `/rpg`                                           | —                       | Vault, PlaceholderAPI                                                   | **$20**         |
| NPCs             | `/npc`                                           | RPGRoll, ProtocolLib    | —                                                                       | **$8**          |
| Crates           | `/crate`                                         | RPGRoll                 | DecentHolograms                                                         | **$6**          |
| Enchantments     | `/renchant`                                      | RPGRoll                 | PlaceholderAPI                                                          | **$10**         |
| Quests           | `/quest`, `/questadmin`                          | RPGRoll                 | PlaceholderAPI                                                          | **$12**         |
| Items            | `/item`, `/itemadmin`                            | RPGRoll                 | Enchantments, Vault, PlaceholderAPI                                     | **$15**         |
| Ascension        | `/ascend`, `/ascendadmin`                        | RPGRoll                 | Enchantments, Quests, PlaceholderAPI                                    | **$16**         |
| Mobs             | `/mobadmin`                                      | RPGRoll                 | Items, Quests, PlaceholderAPI, SackResourcePack                         | **$15**         |
| Dungeons         | `/dungeon`, `/dungeonadmin`                      | RPGRoll, Mobs, Guilds   | Items, Quests, PlaceholderAPI                                           | **$15**         |
| Guilds           | `/guild`, `/team`, `/guildadmin`                 | RPGRoll                 | Items, Quests, Vault, PlaceholderAPI                                    | **$12**         |
| Chat             | `/channel`, `/w`, `/language`, `/chatadmin`, ... | RPGRoll                 | Guilds, PlaceholderAPI                                                  | **$10**         |
| SackEffects      | `/sackeffects`                                   | RPGRoll                 | —                                                                       | **$6**          |
| RPGRoll-Effects  | `/rpgeffects`                                    | RPGRoll                 | SackEffects, Guilds                                                     | **$10**         |
| RPGRoll-Magic    | `/magic`, `/magicadmin`                          | RPGRoll                 | SackEffects, RPGRoll-Effects                                            | **$18**         |
| RPGRoll-Seasons  | `/seasons`, `/seasonsadmin`                      | RPGRoll                 | SackEffects, RPGRoll-Effects, Mobs                                      | **$14**         |
| RPGRoll-Fishing  | `/fishing`, `/fishingadmin`                      | RPGRoll                 | SackEffects, RPGRoll-Effects, Seasons, SackResourcePack                 | **$14**         |
| RPGRoll-Ranching | `/ranching`, `/ranchingadmin`                    | RPGRoll                 | SackEffects, RPGRoll-Effects, Seasons, SackResourcePack                 | **$16**         |
| RPGRoll-Workers  | `/workers`, `/workersadmin`                      | RPGRoll                 | SackEffects, RPGRoll-Effects, Seasons, Ranching, Fishing, Guilds, Vault | **$16**         |
| SackResourcePack | `/srp`                                           | _(ninguno, standalone)_ | S3 (subida remota)                                                      | **$12**         |
| RPGRoll-Economy  | `/economy`, `/economyadmin`                      | RPGRoll                 | Vault, PlaceholderAPI, Guilds, Seasons                                  | **$18**         |
| RPGRoll-Crafting | `/crafting`, `/craftingadmin`                    | RPGRoll                 | Items, Economy, Guilds, Seasons                                         | **$17**         |

Todos los comandos administrativos, de jugador y con contenido dinámico (razas, ítems, encantamientos, especies, entidades, etc.) tienen **autocompletado real por Tab**.

Los precios son estimados (USD) en función de la complejidad de cada módulo — todavía no hay un canal de venta público (ver [Instalación](#-instalación)); se ajustarán al momento de publicarse.

---

# 🚀 Roadmap

El core y los 20 addons ya están funcionales end-to-end (motor + persistencia + GUI editor + comandos + ejemplos). Lo que queda pendiente es, en su mayoría, **contenido/expansión sobre sistemas ya construidos**, no sistemas nuevos:

- 🚧 RPGRoll-Magic: rituales, combos de hechizos, sinergias elementales, sobrecarga (overload), invocación de mobs desde hechizos
- 🚧 RPGRoll-Seasons: festivales, decoraciones estacionales, migración de animales
- 🚧 RPGRoll-Fishing: acuarios, mercado de peces, competencias de pesca, cocina con capturas
- 🚧 RPGRoll-Ranching: subastas, exhibiciones, edificios/instalaciones de granja dedicados
- 🚧 RPGRoll-Workers: empresas/compañías, equipos de trabajadores, transporte, construcción autónoma
- 🚧 RPGRoll-Economy: acciones/bolsa de valores para empresas, contratos entre jugadores, NPC merchants con IA de mercado, economías regionales, eventos económicos, integración activa con Guilds/Seasons (hoy solo dependencias de compilación)
- 🚧 RPGRoll-Crafting: estructuras multibloque para estaciones, mejoras/niveles de estación, recetas para Telar/Cartografía/Amolar/Comercio de Aldeanos/Crafter, auto-descubrimiento por experimentación, modelos 3D vía SackResourcePack
- 🚧 Empaquetado/distribución final (versionado conjunto, un solo release por versión del ecosistema)

---

# 📦 Compatibilidad

| Software        | Versión                        |
| --------------- | ------------------------------ |
| Minecraft       | **26.1.1**                     |
| Paper           | ✅                             |
| Java            | **25**                         |
| Vault           | Opcional (Soft Depend)         |
| PlaceholderAPI  | Opcional (Soft Depend)         |
| ProtocolLib     | Requerido por NPCs             |
| DecentHolograms | Opcional (Soft Depend, Crates) |

---

# 🔧 Instalación

> ⚠️ Los módulos de RPGRoll son de **pago** — se deben comprar para obtener el `.jar` de cada uno. El canal de venta todavía no está publicado; por ahora no hay forma pública de adquirirlos.

1. Descarga los `.jar` de los módulos que quieras usar.
2. Coloca **siempre `RPGRoll` (core) primero** dentro de `plugins/` — todos los addons dependen de él (excepto `SackResourcePack`, que es standalone).
3. Agrega los addons que quieras encima, respetando sus dependencias duras (ver tabla de [Ecosistema de módulos](#-ecosistema-de-módulos)) — por ejemplo, `Dungeons` requiere que `Mobs` y `Guilds` ya estén instalados.
4. Reinicia el servidor.

```
plugins/
├── RPGRoll.jar
├── RPGRoll-Items.jar
├── RPGRoll-Enchantments.jar
├── RPGRoll-Mobs.jar
├── RPGRoll-Guilds.jar
├── RPGRoll-Dungeons.jar
└── ...
```

> ⚠️ **No cambies `online-mode` una vez que el servidor tenga personajes creados.** RPGRoll guarda a cada jugador por **UUID** en `players.db` (y en el resto de bases de datos de los addons). Minecraft asigna un UUID distinto al mismo nombre de usuario según el servidor esté en modo online u offline, así que alternar `online-mode` hace que RPGRoll trate esa cuenta como un personaje totalmente nuevo — el original queda huérfano en la base de datos, no se fusiona ni se migra automáticamente. Elegí un modo antes de lanzar el servidor en serio y no lo cambies después.

---

# 🎮 Comandos

Comando principal del core:

```
/rpg
```

Alias:

```
/rpgroll
/dnd
```

Cada addon agrega su propio comando raíz (ver tabla de [Ecosistema de módulos](#-ecosistema-de-módulos)) — por ejemplo `/npc`, `/quest`, `/mobadmin`, `/magic`, `/srp`.

## 👤 Comandos de Jugador (core)

| Comando         | Descripción                  | Permiso                   |
| --------------- | ---------------------------- | ------------------------- |
| `/rpg create`   | Crear personaje              | `rpgroll.player.create`   |
| `/rpg stats`    | Ver estadísticas             | `rpgroll.player.stats`    |
| `/rpg mystats`  | Ver estadísticas detalladas  | `rpgroll.player.mystats`  |
| `/rpg level`    | Ver nivel y experiencia      | `rpgroll.player.level`    |
| `/rpg class`    | Ver o cambiar clase          | `rpgroll.player.class`    |
| `/rpg race`     | Ver o cambiar raza           | `rpgroll.player.race`     |
| `/rpg jobs`     | Ver y gestionar trabajos     | `rpgroll.player.jobs`     |
| `/rpg skills`   | Ver habilidades              | `rpgroll.player.skills`   |
| `/rpg traits`   | Ver traits                   | `rpgroll.player.traits`   |
| `/rpg allocate` | Gastar puntos de estadística | `rpgroll.player.allocate` |
| `/rpg useskill` | Usar una habilidad aprendida | `rpgroll.player.useskill` |

## 👑 Comandos de Administrador (core)

| Comando                           | Descripción                        | Permiso                    |
| --------------------------------- | ---------------------------------- | -------------------------- |
| `/rpg reload`                     | Recargar configuración             | `rpgroll.admin.reload`     |
| `/rpg addxp <jugador> <cantidad>` | Agregar experiencia                | `rpgroll.admin.addxp`      |
| `/rpg levelup <jugador>`          | Subir nivel (Debug)                | `rpgroll.admin.levelup`    |
| `/rpg gui`                        | Abrir interfaces en modo preview   | `rpgroll.admin.gui`        |
| `/rpg setrace <jugador> <raza>`   | Cambiar la raza de un jugador      | `rpgroll.admin.setrace`    |
| `/rpg setclass <jugador> <clase>` | Cambiar la clase de un jugador     | `rpgroll.admin.setclass`   |
| `/rpg resetstats <jugador>`       | Reiniciar y reembolsar atributos   | `rpgroll.admin.resetstats` |
| `/rpg job <sub> <jugador>`        | Gestionar trabajos de jugadores    | `rpgroll.admin.job`        |
| `/rpg content`                    | Editor visual de razas/clases/etc. | `rpgroll.admin.content`    |

---

# 🔐 Permisos

Cada módulo define su propio árbol de permisos con el mismo patrón `rpgroll<addon>.<player|admin>.*` (por ejemplo `rpgrollnpcs.admin.*`, `rpgrollmobs.admin.*`, `rpgrollmagic.use`). El del core es:

## Jugador

```
rpgroll.player.*
```

Incluye: `create`, `stats`, `mystats`, `level`, `class`, `race`, `jobs`, `skills`, `traits`, `allocate`, `useskill`.

## Administrador

```
rpgroll.admin.*
```

Incluye: `reload`, `addxp`, `levelup`, `gui`, `setrace`, `setclass`, `resetstats`, `job`, `content`.

## Acceso total

```
rpgroll.*
```

---

# 🎨 Personalización de textos

Todos los mensajes configurables desde YAML aceptan cualquiera de estos 4 formatos de color, elegidos libremente por quien edita el archivo (nunca fijos en el código Java):

- Legacy clásico: `&b&lArquero`
- Hex por carácter: `&#54DAF4B&#54C8EB...`
- Hex estilo BungeeCord: `&x&5&4&D&A&F&4...`
- MiniMessage, incluyendo gradientes: `<gradient:#54daf4:#545eb6>Arquero</gradient>`

---

# 🧩 Integraciones

Actualmente soporta (todas opcionales salvo donde se indica):

- **Vault** — RPGRoll-Economy se registra como proveedor del servicio Economy; Items/Guilds/Workers lo consumen para pagos y salarios
- **PlaceholderAPI** — placeholders en casi todos los addons
- **ProtocolLib** — requerido por NPCs
- **DecentHolograms** — hologramas en Crates
- **S3 (SigV4)** — subida remota de resource packs en SackResourcePack

---

# 🛠️ Tecnologías

- Java 25
- Gradle (multi-módulo, con `build-logic` como plugin de convenciones y Shadow)
- Paper API 26.1.1
- Adventure API (`Component`, MiniMessage, `LegacyComponentSerializer`)
- SQLite
- Vault API
- React + TypeScript (sitio de documentación en `UI/`)

---

# 📖 Filosofía del proyecto

RPGRoll no busca ser simplemente un plugin con niveles.

El objetivo es convertirse en un **Framework RPG** que permita a cualquier servidor construir su propio mundo de fantasía mediante sistemas modulares, altamente configurables y fáciles de extender — cada addon es su propio motor basado en componentes/YAML, no una lista fija de opciones.

La idea es que prácticamente cualquier mecánica RPG pueda implementarse utilizando este framework.

---

# ❤️ Estado del desarrollo

El core y los 20 addons descritos arriba están implementados y compilando (motor + GUI + comandos + ejemplos). El proyecto sigue en fase **Alpha**: las APIs internas y algunas funcionalidades pueden cambiar antes de la versión **1.0**, y varios addons todavía tienen expansiones de contenido pendientes (ver [Roadmap](#-roadmap)).

---

# ☕ Apoyá el proyecto

Los módulos todavía no están a la venta (ver [Instalación](#-instalación)), pero si querés bancar el desarrollo mientras tanto, podés donar en [Ko-fi](https://ko-fi.com/sackito). Cualquier aporte ayuda a que el proyecto siga avanzando.

---

# 📄 Licencia

Software propietario — todos los derechos reservados. No es código abierto: el uso está sujeto a los términos de la [Licencia de Uso Comercial (EULA)](LICENSE), que permite instalar y ejecutar los módulos comprados en tus propios servidores, pero prohíbe redistribuirlos, revenderlos o publicar el código fuente.

---

## 👨‍💻 Autor

**Sack**

Framework RPG para servidores Paper inspirado en Dungeons & Dragons y MMORPGs.
