# Pack: Yermo Atómico

Contenido temático para RPGRoll. Retrofuturo de después de la bomba: chapa remachada, rifles de
energía, mutantes y un desierto que brilla de noche.

**Nivel recomendado:** 20 a 45. Va después de Reino No-Muerto.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll | 2 clases (Saqueador y Acorazado) y sus 2 rasgos |
| RPGRoll-Items | 3 armas de fuego, 4 armas cuerpo a cuerpo, la Servoarmadura (4 piezas), una máscara, 3 consumibles, 2 materiales y 1 amuleto |
| RPGRoll-Magic | Escuela Radiación y 3 hechizos |
| RPGRoll-FX | 5 efectos (impactos, onda de choque, hongo atómico) |
| RPGRoll-Mobs | Necrófago, Radescorpión, Coloso del Yermo y el jefe Behemot del Yermo |

Los 18 ítems y las 4 criaturas tienen modelo 3D propio.

## Instalación

Lo más cómodo es el instalador de packs (`RPGRoll-PackInstaller.jar`): reparte cada carpeta en la del
plugin que tengas instalado y se salta las de los que no. A mano es lo mismo, carpeta por carpeta:

```
packs/yermo-atomico/RPGRoll-Items/packs/   ->  plugins/RPGRoll-Items/packs/
packs/yermo-atomico/RPGRoll/classes/       ->  plugins/RPGRoll/classes/
```

Después recarga: `/rpg reload`, `/itemadmin reload`, `/magicadmin reload`, `/mobadmin reload`,
`/rpgfx reload` y `/srp rebuild` (los modelos), o reinicia el servidor.

**Nada se sobrescribe:** todos los ids son propios del pack y conviven con lo que ya tengas.

## Los modelos

| Carpeta | Qué es | Sin ella |
|---|---|---|
| `SackResourcePack/content/rpgroll-yermo/` | Modelos 3D de los ítems y el aspecto de la armadura puesta | Los ítems se ven como su material vanilla |
| `FreeMinecraftModels/models/` | Las criaturas en 3D, con sus animaciones | Se ve el mob vanilla que las mueve |
| `Geyser-Spigot/packs/` y `custom_mappings/` | Los mismos ítems para jugadores de Bedrock | En Bedrock se ven como su material vanilla |

FreeMinecraftModels y Geyser son plugins de terceros: el pack solo deja ahí sus archivos. Si Geyser
corre en el proxy, lleva esas dos carpetas a `plugins/Geyser-Velocity/` del proxy.

Las armas de fuego usan la acción `SHOOT` y cooldowns en milisegundos (`600ms`), que llegan después
de RPGRoll-Items 1.1.6 y RPGRoll-Lib 1.1.6: con esas versiones o anteriores, las armas no disparan.

## Cómo encaja

- El **Saqueador** dispara (pistola, rifle láser, lanzador atómico); el **Acorazado** aguanta con la
  Servoarmadura y pega con el puño hidráulico y el martillo de reactor. Las armas de cada clase piden
  esa clase; la pistola, el garrote, el machete y la máscara los usa cualquiera.
- Necrófagos y radescorpiones salen solos en desiertos y badlands; el Coloso, en badlands. Sueltan
  chatarra, curas y, a veces, núcleos de fusión.
- El **Behemot** no aparece solo: `/mobadmin spawn yermo_behemot`. Es el que suelta el martillo, el
  lanzador y la Servoarmadura.
- La Radiación envenena: la Pastilla Antirradiación quita el veneno.

## Dependencias

RPGRoll (núcleo) y los addons de la tabla. Si falta alguno, el resto funciona igual: cada plugin
solo carga su carpeta.
