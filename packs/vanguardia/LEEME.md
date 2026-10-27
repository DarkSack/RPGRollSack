# Pack: Vanguardia

Contenido temático para RPGRoll. Un escuadrón de élite con armas de energía, nanitos que curan y
robots que se quedaron sin guerra.

**Nivel recomendado:** 35 a 55. Es el tramo final.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll | 2 clases (Agente de Asalto y Médico de Campo) y sus 2 rasgos |
| RPGRoll-Items | 3 armas de energía, martillo, katana, báculo biótico, la armadura de Vanguardia (4 piezas), 2 consumibles, 1 material y 1 amuleto |
| RPGRoll-Magic | Escuela Nanotecnología y 4 hechizos |
| RPGRoll-FX | 8 efectos |
| RPGRoll-Mobs | Dron de Vigilancia, Autómata de Asalto y el jefe Titán de Asedio |

Los 14 ítems y las 3 criaturas tienen modelo 3D propio.

## Instalación

Lo más cómodo es el instalador de packs (`RPGRoll-PackInstaller.jar`): reparte cada carpeta en la del
plugin que tengas instalado y se salta las de los que no. A mano es lo mismo, carpeta por carpeta:

```
packs/vanguardia/RPGRoll-Items/packs/   ->  plugins/RPGRoll-Items/packs/
packs/vanguardia/RPGRoll/classes/       ->  plugins/RPGRoll/classes/
```

Después recarga: `/rpg reload`, `/itemadmin reload`, `/magicadmin reload`, `/mobadmin reload`,
`/rpgfx reload` y `/srp rebuild` (los modelos), o reinicia el servidor.

**Nada se sobrescribe:** todos los ids son propios del pack y conviven con lo que ya tengas.

## Los modelos

| Carpeta | Qué es | Sin ella |
|---|---|---|
| `SackResourcePack/content/rpgroll-vanguardia/` | Modelos 3D de los ítems y el aspecto de la armadura puesta | Los ítems se ven como su material vanilla |
| `FreeMinecraftModels/models/` | Las criaturas en 3D, con sus animaciones | Se ve el mob vanilla que las mueve |
| `Geyser-Spigot/packs/` y `custom_mappings/` | Los mismos ítems para jugadores de Bedrock | En Bedrock se ven como su material vanilla |

FreeMinecraftModels y Geyser son plugins de terceros: el pack solo deja ahí sus archivos. Si Geyser
corre en el proxy, lleva esas dos carpetas a `plugins/Geyser-Velocity/` del proxy.

Las armas de fuego usan la acción `SHOOT` y cooldowns en milisegundos (`600ms`), que llegan después
de RPGRoll-Items 1.1.6 y RPGRoll-Lib 1.1.6: con esas versiones o anteriores, las armas no disparan.

## Cómo encaja

- El **Agente de Asalto** lleva el rifle de riel, el lanzacohetes y la katana de plasma; el
  **Médico de Campo**, el báculo biótico, que cura y regenera a los jugadores de alrededor. La
  pistola de pulso, el martillo y la armadura los usa cualquiera.
- Drones y autómatas salen en colinas y costas pedregosas. El autómata marca a quien ve: lo hace
  brillar unos segundos.
- El **Titán de Asedio** no aparece solo: `/mobadmin spawn vanguardia_titan`. Cae del cielo, cañonea,
  bombardea al que tenga marcado y al final se sobrecarga.

## Dependencias

RPGRoll (núcleo) y los addons de la tabla. Si falta alguno, el resto funciona igual: cada plugin
solo carga su carpeta.
