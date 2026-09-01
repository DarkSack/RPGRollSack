# Pack: Arsenal Elemental

Sin ambientación propia: encaja en cualquier servidor. Son **20 familias
visuales** y, por cada una, un hechizo, un encantamiento y un objeto que
disparan **el mismo efecto de RPGRoll-FX**.

## Qué incluye

| Módulo | Cantidad |
|---|---|
| RPGRoll-FX | 20 efectos |
| RPGRoll-Magic | 20 hechizos + 8 escuelas |
| RPGRoll-Enchantments | 20 encantamientos |
| RPGRoll-Items | 20 objetos |

**88 archivos.** Las 20 familias: incandescencia, ventisca, descarga, floración,
umbra, aureola, singularidad, hemorragia, vendaval, tectónica, plasma, prisma,
eclipse, maremoto, corona solar, espectro, fractura, nebulosa, hielo negro y
apoteosis.

Cada efecto tiene su propia coreografía de formas y tiempos — se distinguen por
cómo se mueven, no solo por el color. `apoteosis` es el más grande: título,
bossbar y tres capas encadenadas.

## Instalación

Copia el contenido de cada carpeta dentro del plugin correspondiente:

```
packs/arsenal-elemental/RPGRoll-FX/effects/  ->  plugins/RPGRoll-FX/effects/
```

Después recarga cada plugin o reinicia el servidor.

## RPGRoll-FX es opcional, pero es el punto

Sin él, los hechizos, encantamientos y objetos **siguen funcionando**: hacen su
daño y su efecto normal, solo que sin el espectáculo. La integración es blanda
en los tres módulos.

Para verlos sueltos: `/rpgfx test <id>`, por ejemplo
`/rpgfx test apoteosis`.

## Las 8 escuelas

El pack trae sus propias escuelas de magia (fuego, escarcha, tormenta,
naturaleza, sombra, luz, arcano, tierra) para no depender del contenido de
ejemplo. Si ya tienes escuelas propias, puedes borrar las que no uses y
cambiar el campo `school:` de los hechizos.
