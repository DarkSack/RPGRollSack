# QA en servidor de RPGRoll-Structures, fases 1-2 (2026-11-23)

## Banco

`structures-qa` es un contenedor Docker con Paper 26.1.2, en modo offline. Lleva estos plugins:
- RPGRoll-Lib 1.1.12 (de desarrollo, con el cambio de `/dar`)
- RPGRoll-Structures 1.0.0 (de desarrollo, `-Prpgroll.dev=true`)
- GriefPrevention 16.18.7
- VaultUnlocked 2.20.3, sin economía

Archivos y datos:
- La carpeta está en `E:\Dev\scratchpad\structures-20261123`.
- La base de datos y el mundo están en los volúmenes de Docker `structures-qa-db` y `structures-qa-world`.
- SQLite en modo WAL no se lee bien a través del montaje de Windows. Para leer la base, copia `data.db`, `data.db-wal` y `data.db-shm` con `docker cp`.

Las pruebas las hacen dos bots de mineflayer (QaDueno y QaOtro) con RCON. Script: `qa.js` con los pasos `base`, `reinicio`, `gp` y `receta`.

## Resultado

| Prueba | Resultado |
|---|---|
| `/structuresadmin give QaDueno constructor 1` | OK: 1 mesa |
| Colocar: se registra en la base y gasta el ítem | OK |
| Clic derecho abre «Centro de construcción» con las 9 casillas | OK |
| Mejora a Ingeniero con los materiales justos (confirmar) | OK: cobra 16/8/4 y el bloque pasa a mesa de herrería |
| Mejora a Arquitecto con doble clic en Confirmar y el doble de materiales | OK: cobra una sola vez y el bloque pasa a faro |
| Añadir de confianza por el menú y el chat | OK: queda en la base |
| El de confianza abre el menú, pero no Mejoras | OK: «Solo el dueño de la mesa puede abrir esto.» |
| Quitar de confianza | OK; sin confianza no abre: «No tienes acceso a esta mesa.» |
| Otro jugador intenta romperla | OK: no se rompe y no le cae un faro |
| Dos TNT junto a la mesa | OK: la mesa sigue |
| El dueño la rompe | OK: se borra la fila, el bloque queda en aire y suelta 1 mesa |
| Recolocarla | OK: mismo id, nivel ARQUITECTO y misma serie |
| Ítem falsificado (crafting_table con claves de mesa inventadas) | OK: no se coloca ni se registra |
| Reiniciar el servidor | OK: la mesa sigue sin marcarse huérfana y el menú abre |
| Colocar en un claim ajeno de GriefPrevention | OK: no se registra, no queda bloque y el ítem vuelve |
| Receta: la rejilla muestra la mesa | OK |
| Receta: Q sobre el resultado | OK: no gasta ingredientes ni suelta muestras |
| Receta: clic normal | OK: da la mesa y gasta una serie; la fabricada se coloca y se registra |
| Log del servidor durante todo el QA | Sin errores ni excepciones del plugin |

## Sin probar en servidor

- Mundos que cargan tarde (WorldForge o Multiverse) y el indexado en `WorldLoadEvent` (C1).
- Wither, pistones, Crafter automático, Mayús+clic, teclas numéricas y Ctrl+Q sobre el resultado.
- Modelo 3D con ItemDisplay: no hay resource pack en el banco y el modelo por defecto es `""`.
- Cambiar `fallback-block` y recargar chunks (I3).
- Vault con una economía real (el coste en dinero por defecto es 0).
- MySQL o MariaDB.
- `/dar mesa:<nivel> N` con N > 1.
- Cliente real de Sack: los menús solo los ha visto un bot. Falta el aspecto visual.

## Cliente real de Sack (Minecraft 26.3 vía ViaVersion, DarkSack), 2026-11-23

| Prueba | Resultado |
|---|---|
| Colocar la mesa y abrir «Centro de construcción» | OK, con 9 botones e iconos correctos |
| Mejoras: requisitos con ✔ en verde y ✘ en rojo, «(te faltan N)», «Mejorar → Pasa a: Ingeniero» | OK |
| Confirmar: botones «Confirmar» y «Cancelar» con su texto (arreglo I4) | OK |
| Mejora a Ingeniero | OK: «Mesa mejorada.», se cobran los materiales y el bloque pasa a mesa de herrería |
| Ayuda: 3 páginas | OK |
| Romper con hacha | OK: se borra la fila y vuelve «Mesa de construcción: Ingeniero», Serie #53 |
| Receta: el resultado muestra la mesa | OK |
| Receta: tecla numérica y Ctrl+Q sobre el resultado | OK: se cancelan; no gastan ingredientes ni sueltan nada |
| Receta: Mayús+clic | OK: 1 mesa (Serie #37) y una sola tanda de ingredientes |

Detalles visuales encontrados:
- Los nombres y el lore de los botones de los menús salen en cursiva. Falta `ITALIC false`. El ítem de la mesa sí sale bien.
- El relleno de cristal gris muestra un tooltip vacío; con F3+H muestra el id. Le falta ocultar el tooltip.
- La muestra de la receta dice «Serie: #0».
- Mejoras y Acceso no tienen una línea de lore que explique qué hacen.

## Pulido y fondos dibujados con el cliente real de Sack (2026-11-24)

Banco con JAR de desarrollo nuevo (129 pruebas en verde) y `menus.pack: "sackito:menus"`. El pack de producción se mezcló con los 15 fondos nuevos y lo sirve `server.properties` desde `http://127.0.0.1:25637/banco.zip`.

| Prueba | Resultado |
|---|---|
| Fondo de Centro, Mejoras, Confirmar, Acceso y Ayuda (taller, Constructor) | OK |
| Fondo de Centro (ingenio, Ingeniero) y Centro y Mejoras (arcano, Arquitecto) | OK |
| Botones invisibles sobre los iconos dibujados, con su tooltip | OK |
| Nombres y lore sin cursiva; descripción en los botones (Ayuda: «Cómo funciona la mesa») | OK |
| Relleno sin tooltip | OK |
| Mejoras: falta de materiales con ✘ y «(te faltan 3)» | OK |
| Mejoras en el nivel máximo | OK |
| Añadir de confianza por el chat desde Acceso | OK |

Capturas en `E:\Dev\scratchpad\structures-20261123\capturas\` (originales) y `E:\Dev\RPGRollDocs\public\capturas\structures\` (recortadas).

Sin probar todavía: capturas con el ratón encima de un botón (tooltips avanzados de F3+H activados en el cliente), MySQL real y mundos de WorldForge.
