# Animaciones de RPGRoll-Emotes

`construir_emotes.py` genera las animaciones y el catálogo de fábrica del módulo Emotes:

- `emotes/src/main/resources/animations/rpgroll_emotes.bbmodel`: el modelo de extremidades del jugador con las 38 animaciones.
- `emotes/src/main/resources/emotes/<categoría>/<id>.yml`: un emote por animación.

```
python -I tools/emotes/construir_emotes.py [--salida <carpeta>]
```

Las animaciones son código: para cambiar una, se edita el script y se vuelve a ejecutar. Los archivos generados se dejan en el repositorio. El script se comprueba a sí mismo al generar: tiempos, poses de inicio y fin, y codos y rodillas hacia su lado natural. Para verlas en el juego: `/bm play rpgroll_emotes <animación>`.

## Atribución

`plantilla/steve.bbmodel` es la plantilla del jugador de [BetterModel](https://github.com/toxicity188/BetterModel), de toxicity188, con licencia MIT. Se copia sin cambios; el script solo cambia el nombre del modelo y sus animaciones.

## Añadir un emote de fábrica

1. En `construir_emotes.py`, define la animación con `anim(nombre, largo, pistas, loop)`. `pistas` es
   `{hueso: [rot(t, x, y, z) | pos(t, x, y, z), ...]}`; el tiempo va en segundos y `loop` es `"once"` (por defecto),
   `"loop"` o `"hold"`.
2. Añade una línea a la lista `EMOTES`: `(animación, categoría, icono, rareza, desbloqueo, sonido, partículas)`, con
   `sonido(clave, volumen, tono)` y `particulas(tipo, cantidad)` o `None`. La categoría `magia` sale oculta del menú.
3. Ejecuta el script. Escribe el `.bbmodel` y un `.yml` por emote, y comprueba la animación antes (si falla, un `assert` dice dónde).
4. Pon el nombre del emote (`emotes.<id>`) en `emotes/src/main/resources/lang/{es,en,pt_BR}.yml`.
5. Añade la animación nueva **después** de las existentes: así los uuid del modelo de fábrica no cambian.

Los packs temáticos usan `--pack mareas-malditas|dinastia-de-jade` y la tabla `PACKS`
(`(animación, categoría, icono, rareza, nombre, cómo conseguirlo, partículas)`). Sus emotes son todos `locked`.
Escriben en `packs/<pack>/RPGRoll-Emotes/`, con un modelo propio llamado `<pack>_emotes` (con guiones bajos).

## Convenciones de ejes

Son las de rotación de Blockbench, en grados, probadas en el juego:

- `+z` sube el brazo derecho por fuera; el izquierdo, `-z`.
- `+x` lleva brazos y piernas al frente.
- `+y` gira el brazo derecho hacia dentro; el izquierdo usa `-y`.
- En los huesos del torso (cadera, cintura, pecho, cabeza), `-x` inclina hacia delante.
- Codos y rodillas se doblan en `x`: el antebrazo hacia `+x` y la pierna de abajo (`foreleg`) hacia `-x`.

Las posiciones van en píxeles (1/16 de bloque). Un bucle empieza y acaba en la misma pose. Un emote corto empieza y
acaba en la pose neutra (todo a 0). Un `hold` (como sentarse) empieza en la neutra y se queda en la última. Las poses
mantenidas se interpolan en lineal.
