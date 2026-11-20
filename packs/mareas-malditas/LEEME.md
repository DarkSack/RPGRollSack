# Pack: Mareas Malditas

Contenido temático para RPGRoll. Piratas, una flota hundida y algo enorme que duerme bajo el arrecife:
armas de chispa, sirenas, un capitán ahogado, el Kraken Ancestral y **tesoros enterrados** con mapa.

**Nivel recomendado:** 15 a 35. Va antes de Yermo Atómico.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll | Raza Tritón, clases Bucanero y Brujo de las Mareas, y sus 3 rasgos |
| RPGRoll-Ascension | Rango de oficio «Buzo de las Mareas» (Pescador nivel 20) |
| RPGRoll-Items | 2 armas de fuego, 4 armas cuerpo a cuerpo, el conjunto del Capitán (4 piezas), 4 consumibles, 3 materiales y 1 amuleto |
| RPGRoll-Magic | Escuela Abismo, 4 hechizos y el catalizador Caracola de las Profundidades |
| RPGRoll-Enchantments | Botín Pirata, Corriente Favorable y Resaca |
| RPGRoll-FX | 6 efectos (disparos, impactos y hechizos) |
| RPGRoll-Mobs | Marinero Ahogado, Cangrejo de Arrecife, Sirena del Arrecife, Anguila Voltaica, el mini jefe Capitán Ahogado y el jefe Kraken Ancestral |
| RPGRoll-Quests | Historia de 5 misiones, 3 diarias y 1 misión del rango Buzo |
| RPGRoll-NPCs | Contramaestre Bruma, Cartógrafa Isla y Bruja Salobre, con sus menús |
| RPGRoll-Dungeons | Gruta del Naufragio, 6 salas |
| RPGRoll-Traps | Red de Pesca, Géiser Salobre, el Cañón del Naufragio (torreta) y su munición |
| RPGRoll-Crafting | Estación Astillero y 7 recetas |
| RPGRoll-Economy | Sección «Mareas Malditas» de `/tienda` |
| RPGRoll-Crates | Caja «Cofre de las Mareas» y 2 tesoros enterrados: Cofre del Capitán y Arcón de Coral |
| RPGRoll-Furniture | 7 muebles: Cofre del Capitán, Arcón de Coral, Baúl de Barco y Barril de Ron (con almacén), Timón, Farol de Barco y Mesa de Cartógrafo |
| RPGRoll-Fishing | 3 especies, el Señuelo de Coral, la Caña de Coral y un tesoro de pesca |
| RPGRoll-Emotes | 2 emotes bloqueados: Hornpipe Pirata (`pirata_hornpipe`, baile en bucle), que da «La Flota del Capitán», y Catalejo (`mareas_catalejo`), que da «La Cartógrafa» |

Los ítems, la caña, el cebo, los mapas, los muebles y las 6 criaturas tienen modelo 3D propio, en Java y
en Bedrock.

## Instalación

Lo más cómodo es el instalador de packs (`RPGRoll-PackInstaller.jar`): reparte cada carpeta en la del
plugin que tengas instalado y se salta las de los que no. A mano es lo mismo, carpeta por carpeta:

```
packs/mareas-malditas/RPGRoll-Items/packs/    ->  plugins/RPGRoll-Items/packs/
packs/mareas-malditas/RPGRoll-Crates/treasures/  ->  plugins/RPGRoll-Crates/treasures/
```

Después reinicia el servidor (o recarga cada plugin) y publica el pack de recursos con `/srp publish`.

**Nada se sobrescribe:** todos los ids empiezan por `mareas_` (salvo el emote `pirata_hornpipe`) y
conviven con lo que ya tengas.

## Versiones que necesita

- **RPGRoll-Crates 1.1.10 o posterior** para los tesoros enterrados (`treasures/` y el comando
  `/tesoro`). Con una versión anterior la caja funciona, pero los mapas no existen: las misiones,
  los mobs y la Cartógrafa que los dan solo dejan un aviso en consola.
- **RPGRoll-Magic, RPGRoll-Fishing y RPGRoll-Lib posteriores a 1.1.10** para dar la caracola, la
  caña y el señuelo desde misiones, menús y rangos (el nombre del jugador al final de `givecatalyst`,
  `giverod` y `givebait`). Con una versión anterior esos comandos fallan en consola y no llega nada.
- **RPGRoll-Items y RPGRoll-Lib posteriores a 1.1.6** para las armas de fuego (acción `SHOOT` y
  cooldowns en milisegundos). Con esas versiones o anteriores, las pistolas no disparan.

- **RPGRoll-Emotes 1.1.12 o posterior** (con BetterModel) para los dos emotes. Sin él, las misiones
  dejan un aviso en consola al dar el emote y el resto de la recompensa llega igual. Con
  SackResourcePack, publica el pack con `/srp publish` después de instalarlos.

## Lo que hay que colocar a mano

**Los NPCs.** Sus archivos traen una ubicación de ejemplo cerca de `0, 64, 0` del mundo `world`. Ponte
donde quieras cada uno, mira hacia donde debe mirar y cambia `location` en su archivo de
`plugins/RPGRoll-NPCs/npcs/` (o usa `/npcadmin edit <id>`). Ideal: los tres juntos en un puerto.

**La mazmorra.** `mareas_gruta_del_naufragio.yml` trae coordenadas de ejemplo (alrededor de
`200, 40, -70`). Construye la gruta y cambia `lobby`, `bounds` y la `entry` de cada sala por las tuyas:
entrada, bodega, canal, camarote del Capitán, bolsa de aire y la fosa del Kraken, en ese orden y
bajando. Sin eso la mazmorra carga bien pero las salas caen en cualquier parte.

**Las trampas.** Se colocan con las herramientas de RPGRoll-Traps donde quieras: la Red de Pesca en la
bodega, el Géiser en el canal y el Cañón en el pasillo hacia el camarote.

**El Astillero.** Un barril con un tronco de abeto a cada lado (este y oeste). Clic derecho en el
barril abre la estación.

**La caja.** Colócala con los comandos de RPGRoll-Crates y reparte su llave como quieras (tienda
premium, votos, eventos).

## Los tesoros enterrados

1. Alguien consigue un mapa: lo sueltan los ahogados (raro), las sirenas, el Capitán y el Kraken; lo
   dan la segunda y la última misión, el rango de Buzo, la mazmorra y la caja; y la Cartógrafa los
   vende por oro y diamantes. Un admin los da con `/tesoro give <jugador> <tesoro>`.
2. Clic derecho con el mapa en la mano: el mapa elige un punto en una playa (Cofre del Capitán) o en
   un arrecife cálido (Arcón de Coral) y lo guarda. La barra de acción dice la distancia y el rumbo.
3. Cerca del punto aparece una X de partículas en el suelo. Cavando unos bloques sale el cofre, con
   su modelo, y los guardianes que vienen a por él.
4. Clic derecho en el cofre para abrirlo. Al cerrarlo, lo que quede cae al suelo y el cofre se va.
   Cada mapa da un solo tesoro y se gasta.

Los tesoros respetan las protecciones: si el jugador no puede romper bloques ahí, no sale nada.

## Cómo encaja

- **Historia:** la Contramaestre Bruma da «La Campana Hundida» desde nivel 15. Cada misión empieza la
  siguiente: la Cartógrafa, el arrecife, la flota del Capitán y, al matar al Capitán Ahogado en la
  Gruta, «El Despertar del Kraken». En esa, la Bruja Salobre hace subir al Kraken junto al jugador.
- **Diarias:** la Cartógrafa Isla, desde su menú.
- **Bucanero:** pistola, trabuco, sable, garfio y el conjunto del Capitán (que pide la clase). El
  **Brujo de las Mareas** lanza la escuela Abismo y pelea con el Tridente Abisal, que suelta el Kraken.
- **Buzo:** al llegar a Pescador 20 se recibe el rango, la Brújula Maldita, una carta de coral, la Caña
  de Coral y tres señuelos, y se desbloquea una misión semanal con la Bruja.
- Ahogados en playas y mar abierto, cangrejos en playas y costas, sirenas en mares cálidos y anguilas
  en mar profundo. El Capitán sale rara vez en playas; el Kraken no sale solo:
  `/mobadmin spawn mareas_kraken`, la última misión o la fosa de la mazmorra.

## La caracola, la caña y el señuelo

- **La Caracola de las Profundidades** (catalizador de RPGRoll-Magic): la regala la Bruja Salobre al
  terminar «El Canto del Arrecife» y la vende por 6 diamantes.
- **La Caña de Coral**: se recibe al volverse Buzo, junto con tres Señuelos de Coral.
- **El Señuelo de Coral**: la Bruja lo vende por 2 esmeraldas y la misión semanal del Buzo da dos.

Un admin los da a cualquier jugador con `/magicadmin givecatalyst <id> <jugador>`,
`/fishingadmin giverod <id> <jugador>` y `/fishingadmin givebait <id> <jugador>`.

## Los modelos

| Carpeta | Qué es | Sin ella |
|---|---|---|
| `SackResourcePack/content/rpgroll-mareas/` | Modelos 3D de los ítems, la caña, el cebo, los mapas y el aspecto de la armadura puesta | Los ítems se ven como su material vanilla |
| `SackResourcePack/content/rpgroll-mareas-muebles/` | Los muebles y los cofres de tesoro | Los muebles y los cofres se ven como papel |
| `FreeMinecraftModels/models/` | Las criaturas en 3D, con sus animaciones | Se ve el mob vanilla que las mueve |
| `Geyser-Spigot/packs/` y `custom_mappings/` | Lo mismo para jugadores de Bedrock | En Bedrock se ven como su material vanilla |

Las especies de pesca usan modelos que ya trae RPGRoll-Fishing.

FreeMinecraftModels y Geyser son plugins de terceros: el pack solo deja ahí sus archivos. Si Geyser
corre en el proxy, lleva esas dos carpetas a `plugins/Geyser-Velocity/` del proxy.

## Detalles que conviene saber

- Las misiones cuentan las muertes por el tipo vanilla que mueve a cada criatura: los ahogados, las
  sirenas, el Capitán y el Kraken son drowned; los cangrejos, arañas; las anguilas, guardianes. Un
  drowned vanilla también suma.
- El Kit de Calafate se gasta al usarlo con un `minecraft:clear` del propio kit.
- La tienda del pack solo vende: no compra ítems de RPGRoll.

## Dependencias

RPGRoll (núcleo) y los addons de la tabla. Si falta alguno, el resto funciona igual: cada plugin solo
carga su carpeta.
