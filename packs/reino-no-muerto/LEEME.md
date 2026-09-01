# Pack: Reino No-Muerto

Contenido temático para RPGRoll. Criptas, nigromancia y una dinastía que no
aceptó estar muerta.

**Nivel recomendado:** 15 a 35. Está pensado como el tramo de media campaña,
no como contenido inicial.

## Qué incluye

| Módulo | Contenido |
|---|---|
| RPGRoll-Mobs | 3 enemigos normales, 1 mini jefe, 1 jefe de mazmorra |
| RPGRoll-Magic | Escuela Nigromancia, 4 hechizos, 1 catalizador |
| RPGRoll-Items | 1 arma, 1 armadura, 1 reliquia, 1 material |
| RPGRoll-Traps | 2 trampas, 1 torreta, 1 munición |
| RPGRoll-Dungeons | 1 mazmorra de 6 salas |
| RPGRoll-Quests | Cadena de 3 misiones |

## Instalación

Copia el contenido de cada carpeta dentro de la carpeta del plugin
correspondiente, en `plugins/`. Por ejemplo:

```
packs/reino-no-muerto/RPGRoll-Mobs/mobs/  ->  plugins/RPGRoll-Mobs/mobs/
```

Después, recarga cada plugin (`/mobadmin reload`, `/magicadmin reload`,
`/trapadmin reload`, etc.) o reinicia el servidor.

**Nada se sobrescribe:** todos los ids llevan nombres propios del pack, así que
conviven con el contenido que ya tengas.

## Lo único que tienes que ajustar

**Las coordenadas de la mazmorra.** `cripta_de_los_reyes.yml` trae posiciones
de ejemplo (`world`, alrededor de 0,50,0 hacia z negativa). Hay que moverlas a
donde construyas la cripta en tu mundo: `lobby`, `bounds` y el `entry` de cada
una de las 6 salas.

Sin eso la mazmorra carga bien pero las salas caen en cualquier parte.

## Cómo encaja

El bucle está pensado así: las misiones te empujan a la cripta, la cripta usa
los 5 mobs en orden de dificultad, el jefe suelta lo necesario para la última
misión, y la escuela de Nigromancia le da a un mago una razón para especializarse
en este contenido.

Las trampas y la torreta son para que **tú** las coloques dentro de la cripta
(`/trapadmin place losa_de_huesos`, `/trapadmin turret catalog`); no aparecen
solas.

## Dependencias

Requiere RPGRoll (núcleo) más los seis addons de la tabla. Si te falta alguno,
el resto del pack funciona igual: cada plugin solo carga su propia carpeta.
