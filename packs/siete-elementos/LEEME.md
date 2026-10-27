# Pack: Siete Elementos

Contenido temático para RPGRoll. Un mundo de viajeros donde cada arma tiene un elemento: fuego, agua,
viento, rayo, flora, hielo y roca.

**Nivel recomendado:** 10 a 30. Sirve de contenido inicial.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll | 2 clases (Duelista Elemental y Catalista) y sus 2 rasgos |
| RPGRoll-Items | 4 espadas y lanzas, 1 arco, 1 orbe, 1 tomo, el Atuendo del Viajero (4 piezas), 3 reliquias-amuleto, 2 comidas y 1 material |
| RPGRoll-Magic | Escuela Los Siete Elementos y 7 hechizos, uno por elemento |
| RPGRoll-FX | 7 efectos, uno por elemento |
| RPGRoll-Mobs | Trasgo Enmascarado, 4 babosas elementales y el jefe Núcleo de los Siete |

Los 17 ítems y las 6 criaturas tienen modelo 3D propio.

## Instalación

Lo más cómodo es el instalador de packs (`RPGRoll-PackInstaller.jar`): reparte cada carpeta en la del
plugin que tengas instalado y se salta las de los que no. A mano es lo mismo, carpeta por carpeta:

```
packs/siete-elementos/RPGRoll-Items/packs/   ->  plugins/RPGRoll-Items/packs/
packs/siete-elementos/RPGRoll/classes/       ->  plugins/RPGRoll/classes/
```

Después recarga: `/rpg reload`, `/itemadmin reload`, `/magicadmin reload`, `/mobadmin reload`,
`/rpgfx reload` y `/srp rebuild` (los modelos), o reinicia el servidor.

**Nada se sobrescribe:** todos los ids son propios del pack y conviven con lo que ya tengas.

## Los modelos

| Carpeta | Qué es | Sin ella |
|---|---|---|
| `SackResourcePack/content/rpgroll-siete/` | Modelos 3D de los ítems y el aspecto de la armadura puesta | Los ítems se ven como su material vanilla |
| `FreeMinecraftModels/models/` | Las criaturas en 3D, con sus animaciones | Se ve el mob vanilla que las mueve |
| `Geyser-Spigot/packs/` y `custom_mappings/` | Los mismos ítems para jugadores de Bedrock | En Bedrock se ven como su material vanilla |

FreeMinecraftModels y Geyser son plugins de terceros: el pack solo deja ahí sus archivos. Si Geyser
corre en el proxy, lleva esas dos carpetas a `plugins/Geyser-Velocity/` del proxy.

Las armas de fuego usan la acción `SHOOT` y cooldowns en milisegundos (`600ms`), que llegan después
de RPGRoll-Items 1.1.6 y RPGRoll-Lib 1.1.6: con esas versiones o anteriores, las armas no disparan.

## Cómo encaja

- El **Duelista Elemental** lleva el mandoble y la lanza; la **Catalista**, el orbe y el tomo (que
  cura y regenera a los jugadores de alrededor). La espada, la hoja de escarcha, el arco y el
  Atuendo del Viajero los usa cualquiera.
- Las tres reliquias (flor, pluma y cáliz) son amuletos: llenan las tres ranuras de `/amuletos`.
- Trasgos en praderas y bosques; cada babosa en su bioma (fuego en sabanas, agua en ríos y
  pantanos, rayo en montañas, hielo en la nieve). Las babosas las mueve un lepisma grande, así que
  no se dividen al morir.
- El **Núcleo de los Siete** no aparece solo: `/mobadmin spawn siete_nucleo`. Vuela, se protege
  con sus fragmentos y descarga rayos al final.

## Dependencias

RPGRoll (núcleo) y los addons de la tabla. Si falta alguno, el resto funciona igual: cada plugin
solo carga su carpeta.
