# Pack: Dinastía de Jade

Contenido temático para RPGRoll. Artes marciales, un templo en el monte y nueve sellos rotos: un
ejército de terracota despierto, zorros espectrales, grullas que se escapan de los biombos, el General
Sin Sombra, el Dragón de Jade y **tesoros enterrados** con mapa.

**Nivel recomendado:** 40 a 60.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll | Raza Kitsune, clases Monje del Puño y Espadachín del Loto, y sus 3 rasgos |
| RPGRoll-Ascension | Rango de oficio «Maestro del Té» (Alquimista nivel 25) |
| RPGRoll-Items | 6 armas (Dao del Tigre, Lanza Media Luna, Abanico de Guerra, Guantes de Jade, Arco del Loto y Shuriken Estelar), el conjunto del Guardián (4 piezas), 4 consumibles, 3 materiales y 1 amuleto |
| RPGRoll-Magic | Escuela Qi, 4 hechizos y el catalizador Pergamino del Qi |
| RPGRoll-Enchantments | Filo del Loto, Respiración del Qi y Furia del Dragón |
| RPGRoll-FX | 6 efectos (disparos, impactos y hechizos) |
| RPGRoll-Mobs | Guerrero de Terracota, Zorro Espectral, Grulla de Tinta, León de Piedra, el mini jefe General Sin Sombra y el jefe Dragón de Jade |
| RPGRoll-Quests | Historia de 5 misiones, 3 diarias y 1 misión del rango Maestro del Té |
| RPGRoll-NPCs | Maestra Lian, Mercader Wei y Monje Bao, con sus menús |
| RPGRoll-Dungeons | Templo de los Nueve Sellos, 7 salas |
| RPGRoll-Traps | Sello de Fuego, Dardos del Biombo, el Dragón de Bronce (torreta) y su munición |
| RPGRoll-Crafting | Estación Forja de Jade y 7 recetas |
| RPGRoll-Economy | Sección «Dinastía de Jade» de `/tienda` |
| RPGRoll-Crates | Caja «Cofre de la Dinastía» y 2 tesoros enterrados: Arca Lacada y Relicario del Dragón |
| RPGRoll-Furniture | 7 muebles: Arca Lacada, Relicario del Dragón y Gabinete de Té (con almacén), Linterna de Papel, Biombo, Mesa Baja y Bonsái |
| RPGRoll-Ranching | La raza Gallina Fénix, con modelo propio |
| RPGRoll-Emotes | 2 emotes bloqueados: Saludo Marcial (`jade_saludo_marcial`), que da «El General Sin Sombra», y Postura de la Grulla (`jade_postura_grulla`, en bucle), que da «Las Grullas de Tinta» |

Los ítems, los mapas, los muebles y las 7 criaturas (las 6 de RPGRoll-Mobs y la gallina) tienen
modelo 3D propio.

## Instalación

Lo más cómodo es el instalador de packs (`RPGRoll-PackInstaller.jar`): reparte cada carpeta en la del
plugin que tengas instalado y se salta las de los que no. A mano es lo mismo, carpeta por carpeta:

```
packs/dinastia-de-jade/RPGRoll-Items/packs/       ->  plugins/RPGRoll-Items/packs/
packs/dinastia-de-jade/RPGRoll-Crates/treasures/  ->  plugins/RPGRoll-Crates/treasures/
```

Después reinicia el servidor (o recarga cada plugin) y publica el pack de recursos con `/srp publish`.

**Nada se sobrescribe:** todos los ids empiezan por `jade_` y conviven con lo que ya tengas.

## Versiones que necesita

- **RPGRoll-Crates 1.1.10 o posterior** para los tesoros enterrados (`treasures/` y el comando
  `/tesoro`). Con una versión anterior la caja funciona, pero los mapas no existen: las misiones,
  los mobs y el Mercader que los dan solo dejan un aviso en consola.
- **RPGRoll-Magic, RPGRoll-Ranching y RPGRoll-Lib posteriores a 1.1.10** para dar el Pergamino del Qi
  desde la misión y el menú del Monje, y la Gallina Fénix al volverse Maestro del Té. Con una versión
  anterior esos comandos fallan en consola y no llega nada.
- **RPGRoll-Items y RPGRoll-Lib posteriores a 1.1.6** para el arco, el abanico y el shuriken (acción
  `SHOOT`). Con esas versiones o anteriores, no disparan.

- **RPGRoll-Emotes 1.1.12 o posterior** (con BetterModel) para los dos emotes. Sin él, las misiones
  dejan un aviso en consola al dar el emote y el resto de la recompensa llega igual. Con
  SackResourcePack, publica el pack con `/srp publish` después de instalarlos.

## Lo que hay que colocar a mano

**Los NPCs.** Sus archivos traen una ubicación de ejemplo cerca de `0, 64, 10` del mundo `world`. Ponte
donde quieras cada uno, mira hacia donde debe mirar y cambia `location` en su archivo de
`plugins/RPGRoll-NPCs/npcs/` (o usa `/npcadmin edit <id>`). Ideal: los tres juntos en una aldea entre
cerezos o bambú.

**La mazmorra.** `jade_templo_de_los_nueve_sellos.yml` trae coordenadas de ejemplo (alrededor de
`-200, 70, 0`). Construye el templo y cambia `lobby`, `bounds` y la `entry` de cada sala por las tuyas:
puerta de las nubes, patio de barro, jardín de tinta, sala de los leones, sala de meditación, salón del
General y el santuario del Dragón, en ese orden. Sin eso la mazmorra carga bien pero las salas caen en
cualquier parte.

**Las trampas.** Se colocan con las herramientas de RPGRoll-Traps donde quieras: el Sello de Fuego en el
patio, los Dardos del Biombo en el jardín de tinta y el Dragón de Bronce en el pasillo hacia el salón del
General.

**La Forja de Jade.** Una mesa de herrería con terracota verde vidriada a cada lado (este y oeste). Clic
derecho en la mesa abre la estación.

**La caja.** Colócala con los comandos de RPGRoll-Crates y reparte su llave como quieras (tienda
premium, votos, eventos).

## Los tesoros enterrados

1. Alguien consigue un pergamino: lo sueltan las terracotas, los zorros y los leones (raro), el General y
   el Dragón; lo dan la segunda y la última misión, el rango
   de Maestro del Té, la misión semanal del té, la mazmorra y la caja; y el Mercader Wei los vende por
   esmeraldas y diamantes. Un admin los da con `/tesoro give <jugador> <tesoro>`.
2. Clic derecho con el pergamino en la mano: elige un punto entre cerezos o bambú (Arca Lacada) o en lo
   alto del monte (Relicario del Dragón) y lo guarda. La barra de acción dice la distancia y el rumbo.
3. Cerca del punto aparece una X de partículas en el suelo. Cavando unos bloques sale el cofre, con su
   modelo, y los guardianes que vienen a por él: soldados de terracota para el arca, un león y un zorro
   para el relicario.
4. Clic derecho en el cofre para abrirlo. Al cerrarlo, lo que quede cae al suelo y el cofre se va. Cada
   pergamino da un solo tesoro y se gasta.

Los tesoros respetan las protecciones: si el jugador no puede romper bloques ahí, no sale nada.

## Cómo encaja

- **Historia:** la Maestra Lian da «El Ejército de Barro» desde nivel 40. Cada misión empieza la
  siguiente: los zorros del Monje Bao, las grullas del Mercader Wei, los leones del General y, al matar
  al General Sin Sombra en el Templo, «El Despertar del Dragón». En esa, el Monje Bao rompe el noveno
  sello y el Dragón despierta junto al jugador.
- **Diarias:** el Mercader Wei, desde su menú.
- **Monje del Puño:** Guantes de Jade y Shuriken Estelar (los dos piden la clase). El **Espadachín del
  Loto** pelea con el Dao, la Lanza y el Abanico, y viste el conjunto del Guardián (que pide la clase).
  Los dos pueden aprender la escuela Qi.
- **Pergamino del Qi:** lo regala «Las Grullas de Tinta» y el Monje Bao lo vende por 12 diamantes.
- **Maestro del Té:** al llegar a Alquimista 25 se recibe el rango, un Arca Lacada y una Gallina Fénix
  a tu lado, y se desbloquea una misión semanal con el Monje Bao.
- Terracotas en junglas de bambú y cerezos, zorros en cerezos y bosques de abetos, grullas en ríos,
  pantanos y cerezos, y leones en los picos. El General sale rara vez en la jungla de bambú; el Dragón no sale solo: `/mobadmin spawn jade_dragon`,
  la última misión o el santuario de la mazmorra.

Un admin da el catalizador y la gallina con `/magicadmin givecatalyst jade_pergamino_del_qi <jugador>` y
`/ranchingadmin spawn chicken jade_gallina_fenix <dueño>` (desde la consola, la gallina sale junto a su
dueño).

## Los modelos

| Carpeta | Qué es | Sin ella |
|---|---|---|
| `SackResourcePack/content/rpgroll-jade/` | Modelos 3D de los ítems, los pergaminos y el aspecto de la armadura puesta | Los ítems se ven como su material vanilla |
| `SackResourcePack/content/rpgroll-jade-muebles/` | Los muebles y los cofres de tesoro | Los muebles y los cofres se ven como papel |
| `FreeMinecraftModels/models/` | Las criaturas en 3D, con sus animaciones | Se ve el mob vanilla que las mueve |
| `Geyser-Spigot/packs/` y `custom_mappings/` | Lo mismo para jugadores de Bedrock | En Bedrock se ven como su material vanilla |

FreeMinecraftModels y Geyser son plugins de terceros: el pack solo deja ahí sus archivos. Si Geyser
corre en el proxy, lleva esas dos carpetas a `plugins/Geyser-Velocity/` del proxy.

## Detalles que conviene saber

- Las misiones cuentan las muertes por el tipo vanilla que mueve a cada criatura: las terracotas son
  husks; los zorros, pececillos de plata; las grullas, vex; los leones, devastadores; el General, un
  esqueleto wither; y el Dragón, un blaze. Un husk vanilla también suma.
- El Incienso de Concentración se gasta al usarlo con un `minecraft:clear` del propio incienso.
- La tienda del pack solo vende: no compra ítems de RPGRoll.

## Dependencias

RPGRoll (núcleo) y los addons de la tabla. Si falta alguno, el resto funciona igual: cada plugin solo
carga su carpeta.
