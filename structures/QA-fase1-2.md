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

## Fases 3-5: qué probar en el banco

Requiere el JAR de Structures 1.1.0 con la Lib 1.1.14 (con otra Lib el plugin se desactiva con un SEVERE). Copiar `structures/caseta.yml` de las pruebas y un `caseta.nbt` a las carpetas del plugin. Cada fila se anota con OK o el fallo.

### Flujo de obra

| Prueba | Qué debe pasar |
|---|---|
| Depositar con el inventario lleno | No se pierde nada: lo que no cabe se devuelve o queda pendiente (botón «Recoger pendientes» del menú de materiales; solo aparece si hay pendientes) |
| Desconexión justo al depositar | Al volver, los materiales están en la obra o en pendientes; nunca duplicados ni perdidos |
| Reinicio a mitad de obra | La obra sigue en BUILDING desde el cursor guardado; sin bloques repetidos ni cobro doble |
| Apagar el servidor a mitad de obra | Cursor y consumos guardados (`flushAll` antes de cerrar la BD); sin «PENDIENTE PERDIDO» en el log |
| Cancelar | Devuelve según la política (P%) al dueño de la obra; segundo intento no devuelve más |
| Chunk descargado | La obra espera y sigue al cargarse; no carga chunks por su cuenta |
| Dos obras a la vez (mesas distintas) | Avanzan las dos dentro del presupuesto por tick; una mesa no puede tener dos obras |
| Solapes | Una obra que pisa a otra activa o terminada se rechaza |
| `.schem` sin WorldEdit | La estructura sale en `validate` con el motivo; el resto del catálogo carga |
| Apagar el servidor con un depósito o una devolución en vuelo | `runTask` en `onDisable` no corre: comprobar que no se pierde nada y que queda en el libro de movimientos o en pendientes (y no en ambos) |
| Autoguardado del jugador entre retirar del inventario y confirmar en la BD | Provocar un cierre brusco en esa ventana (latencia de BD): el material no puede desaparecer ni duplicarse |
| MySQL/MariaDB real | Migración v1→v2 con datos, `INSERT IGNORE`, `AUTO_INCREMENT`, columna `cursor` y `applyMovements` con savepoints; repetir el flujo de obra completo |
| Varias construcciones instantáneas seguidas | Transacciones por segundo razonables en la BD (SQLite y MySQL); sin retraso visible en el tick |

### Comprobaciones que solo se ven con servidor

| Prueba | Qué comprobar |
|---|---|
| Leer `.nbt` y `.schem` reales | Tamaño, bloques, estados y tiles coinciden con el original (probar uno hecho en vanilla y otro de WorldEdit/FAWE) |
| `BlockData#rotate` | Escaleras, puertas, cabezas y bloques con facing salen bien a 90, 180 y 270 grados |
| Tiles al construir girado | Cofre y cartel conservan contenido y orientación al rotar la obra |
| `isSimilar` | Un ítem renombrado o con lore NO cuenta como el vanilla del material |
| PDC `rpgroll:item_id` | Un ítem de otro módulo se reconoce; comprobar el tipo real del PDC (cadena) |
| Coste de vista previa o validación grande (MSPT) | Con una estructura cercana al tamaño máximo de Arquitecto, medir MSPT al abrir la vista previa y al hacer `validate`; el escaneo corre en un tick |
| `sendMultiBlockChange` | Los bloques falsos se ven; al cerrar la vista previa se restauran los reales, también tras salir del chunk y volver |
| Reabrir la vista previa | Mayús + clic derecho con la mesa la reabre sin perder rotación ni posición |
| Reabrir tras recargar chunks | Los bloques falsos se reenvían (límite conocido: no siempre) |

## Fases 6-7: qué probar en el banco

- Floodgate (reflexión, FloodgateApi.getInstance().isFloodgatePlayer): con un jugador Bedrock, la vista previa solo dibuja contorno y avisa una vez (`preview.bedrock`); con Floodgate sin cargar, `/structuresadmin integrations` lo marca ✘.
- `/structuresadmin integrations` y `reload` (relee integrations.yml).
- Protección de la zona entera (`protect/AreaGuards`, WorldGuard y GriefPrevention por reflexión). Se mira la caja girada completa, una vez, por el UUID del dueño de la obra, al confirmar la vista previa y al arrancar (`ConstructionEngine.start`); la vista barata al mover no pregunta. Probar con un jugador sin `rpgrollstructures.bypass` (y otro con él: se salta todo):
  - Claim ajeno de GriefPrevention que cruza la caja (aunque sea una esquina): confirmar da `protect.griefprevention`; arrancar una obra READY ya creada da `build.protected` y sigue READY.
  - Claim propio (o con `/trust` de construir): se confirma y se construye. Caja entera dentro de una subdivisión con permiso pero sin permiso en el claim de fuera: se niega (atajo conocido).
  - Región de WorldGuard sin ser miembro: `protect.worldguard`. Como miembro o dueño: se construye. Región con `build allow` o `passthrough allow`, o con `block-place allow` y `block-break allow` (como WG al poner bloques): se construye. Región con `block-place deny` o `block-break deny` aunque el dueño sea miembro: se niega. `__global__` con `build deny`: se niega en todo el mundo.
  - Prioridades de WorldGuard: parcela (cuboide, prioridad 10, el dueño es miembro) dentro de una ciudad (prioridad 0, no es miembro). Caja entera dentro de la parcela: se construye. Caja que asoma de la parcela a la ciudad: se niega. Caja sobre naturaleza y una región propia: se construye. Parcela poligonal: se niega siempre que la ciudad no deje (atajo conocido: un polígono nunca «contiene» la caja).
  - Dueño desconectado: el dueño de la mesa arranca la obra de otro jugador que no está; se comprueba por el dueño (WG lo envuelve con `wrapOfflinePlayer`, GP por UUID) y da el mismo resultado que con él conectado. Los grupos de permisos de WG no cuentan para el dueño desconectado.
  - Romper la reflexión (versión incompatible): la primera comprobación niega con `protect.error`, la consola avisa UNA vez y `/structuresadmin integrations` marca esa integración ✘. Desde ahí se sigue negando con `protect.error` (sin volver a llamar a ese plugin) mientras esté activada en integrations.yml y el plugin esté cargado; `reload` la vuelve a probar. Apagarla en integrations.yml (o quitar el plugin) deja construir sin esa protección.
- Recuperación (Task 3), con una obra progresiva en marcha:
  - Mesa sin cargar: alejarse hasta que el chunk de la mesa se descargue (con la obra en chunks que sigan cargados, p. ej. con `/forceload`): la obra se queda en BUILDING sin avanzar y las demás siguen; al volver, sigue sola.
  - Protección a mitad de obra: crear un claim o región ajena sobre la caja mientras construye. En 60 s como mucho (`protection-recheck-seconds`) pasa a PAUSED, el dueño conectado recibe `project.paused.protected` y el menú Obra muestra ese motivo. Reanudar con la protección aún puesta da `build.protected` y sigue PAUSED; quitarla y reanudar sigue construyendo.
  - Reinicio: con la zona protegida y la obra en BUILDING, reiniciar: al instalar el plan se pausa con motivo. Una obra PAUSED con motivo sigue PAUSED tras reiniciar. Una obra arrancada con bypass en zona protegida se pausa tras reiniciar (el bypass solo vive en memoria) y otro con bypass la reanuda.
  - `/structuresadmin debug <id>` y `debug <jugador>` (conectado y desconectado ya visto por el servidor): estado, cursor/total, colocados, saltados, motivo, mesa, mundo y origen. Sin `rpgrollstructures.admin`: `admin.no_permission`.
- Romper la mesa con obra activa (Task 4), en config.yml `break-policy` y `/structuresadmin reload`:
  - `pause` con la obra construyendo: el bloque y el display se van, sale el ítem (mismo nivel), `workbench.break.paused`; la obra pasa a PAUSED con `project.paused.bench` (el dueño de la obra, si es otro y está conectado, recibe ese aviso). Con la obra esperando materiales o lista no cambia de estado. Colocar el ítem en otro sitio: la mesa vuelve con sus obras, la obra sigue en su sitio original y se reanuda (`build.resumed`). Con la zona protegida para el dueño: sigue PAUSED con `project.paused.protected`, también si quien coloca tiene bypass. Reiniciar con la mesa recogida y colocarla después: igual.
  - Mesa recogida (o quitada con WorldEdit y ya huérfana): poner otra mesa en ese mismo bloque funciona, también tras reiniciar.
  - `cancel`: romper la mesa cancela la obra (lo devuelto llega al dueño de la obra según `cancel-refund`, a pendientes si no está) y luego suelta la mesa con `build.cancelled`. Con la base de datos parada (o la obra ocupada con un depósito), `build.cancel-error` y la mesa se queda.
- Dinero de las estructuras (Task 5), con una estructura de `money: 10` y EssentialsX + Vault:
  - Sin Vault (o `vault` apagado en integrations.yml): la ficha muestra «Elegir» con `structure.needs-vault`; «Vista previa» abre la vista y su botón Confirmar es una barrera con el mismo motivo; confirmar no cobra ni crea nada.
  - Con Vault y saldo de 9,99: `structure.no-money` con 10.00 y nada creado. Con 10,00 o más: se cobra entero al confirmar (`structure.charged`), el menú Materiales muestra «Dinero pagado» y en `ledger` hay un MONEY_CHARGE de 1000.
  - Cancelar con `cancel-refund: 100` una obra con 25 de 100 bloques puestos: vuelven 7,50 al DUEÑO de la obra (también si cancela el dueño de la mesa), MONEY_REFUND de 750 en `ledger` y fila en `pending_money` con `paid_at` puesto.
  - Cancelar con el dueño desconectado: se le ingresa igual (OfflinePlayer). Cancelar sin Vault (quitarlo tras crear la obra): la fila queda sin `paid_at`; al volver a poner Vault y reiniciar (o al entrar el jugador) se paga una vez y avisa `structure.money-paid`. Entrar y salir varias veces no paga dos veces.
  - Base de datos parada al confirmar: `catalog.create-error` y el dinero vuelve al momento (sin «DINERO PERDIDO» en la consola).
  - Economía que lanza en `withdrawPlayer` (plugin de economía roto): el jugador puede haber pagado o no; solo queda una línea SEVERE («Cobro de ... con error; revisar su saldo») y no se crea la obra. Revisar su saldo a mano.
  - Commit ambiguo (el alta llega a la base y el driver lanza): no se devuelve el dinero; la obra se recupera en memoria con un SEVERE. Si ni siquiera se puede mirar el libro: «COBRO SIN VERIFICAR» en la consola, sin devolución ni obra en memoria; un admin mira `projects` y devuelve a mano si no está.
- Módulos (Task 6), con una estructura con `module: {type: farm, note: {es: "...", en: "..."}}`:
  - La ficha muestra la nota en el idioma del servidor (sin ella, en español). `type: teleporter` sale en `/structuresadmin validate` como `module.type` y esa estructura no está en el catálogo.
  - Terminar una obra de la granja: el dueño recibe `build.completed` igual que sin módulo; tras reiniciar, la construcción sigue en «Mis construcciones».
  - `/structuresadmin delete <id-de-construcción>` (id en la tabla `builds`): `admin.delete.done`, los bloques se quedan y la caja ya no choca con una vista previa nueva. Repetirlo: `admin.delete.none`. Texto que no es un UUID: `admin.delete.usage`. Sin `rpgrollstructures.admin`: `admin.no_permission`.
- Granja de mobs `mob_farm` (Task 7), con una estructura con `module: {type: mob_farm, spawner: [x,y,z], mob: zombie, max-entities: 12}` y un spawner en esa posición del esquemático:
  - Construir con cada giro: el spawner de ESA posición (girada) toma el tipo; si ahí no hay spawner, la consola avisa «no hay un spawner en ...». Una posición fuera de la caja en cualquier giro: `build.module-denied` al confirmar.
  - Vanilla: con `max-entities` zombis a 16 bloques el spawner se para (rango 0) y al matarlos se reactiva (16), en 5 s como mucho (tickEvery 100).
  - `mob: mythic:<Nombre>` sin MythicMobs: `/structuresadmin validate` da problema `module.mob`. Con MythicMobs 5.12.1: el spawner queda parado y, con un jugador a 16 bloques, sale un mob Mythic cada 5 s hasta `max-entities` (contados por etiqueta PDC `rpgrollstructures:mob_farm_mythic`; los Mythic del mismo nombre ajenos a la granja no cuentan). Nombre Mythic que no existe: no sale nada y no hay error. Reflexión rota (otra versión): el log avisa una vez y `/structuresadmin integrations` marca MythicMobs ✘.
  - `mob: spawner:<id>` se rechaza (la Lib no tiene fuente `spawner` en Things).
  - Mythic: el bloque sobre el spawner (el mob sale a +1) debe estar libre (aire); si es sólido el mob no sale bien. Dejar ese hueco en el esquemático.
- Parcela `parcel` (Task 8), con una estructura con `module: {type: parcel, claim: griefprevention}` (y otra con `claim: worldguard`, otra con `claim: none`):
  - `claim: towny` sale en `/structuresadmin validate` como `module.claim`. `claim: none`: se construye y no crea nada.
  - GriefPrevention 16.18.x: con menos bloques de terreno libres que el área XZ de la caja, confirmar da `parcel.not-enough-blocks`; con un claim (propio o ajeno) que cruza la caja, `parcel.overlaps-claim`. Con bloques de sobra: al terminar hay un claim del dueño sobre la caja (`/claimslist`), desde 5 bloques bajo el suelo de la caja (`ExtendIntoGroundDistance`) hasta arriba, y sus bloques libres bajan en el área. En `build_modules` queda `griefprevention:<id del claim>`.
  - WorldGuard 7.0.18: otra región (que no sea `__global__`) que cruza la caja: `parcel.overlaps-region`. Al terminar: región cuboide `rpgroll_parcel_<8 primeros del id de la construcción>` sobre la caja, con el dueño como owner (`/rg info`), y `worldguard:<nombre>` en `build_modules`. Con las regiones apagadas en el mundo: `parcel.no-regions`.
  - Dueño desconectado al terminar la obra: el claim o la región salen igual a su nombre (UUID).
  - Integración pedida pero apagada en integrations.yml (o sin el plugin): confirmar y arrancar dan `parcel.inactive`.
  - Fallo al crear (p. ej. gastar los bloques entre confirmar y terminar, o crear a mano un claim encima mientras construye): la construcción queda en «Mis construcciones», el dueño conectado recibe el motivo (`parcel.not-enough-blocks`, `parcel.overlaps-claim`...) o `parcel.failed`, y la consola lo avisa. Reflexión rota (otra versión): `parcel.error`/`parcel.failed`, aviso una vez y `/structuresadmin integrations` marca ese plugin ✘.
  - `/structuresadmin delete <id-de-construcción>`: el claim (o la región) guardado desaparece y su fila de `build_modules` también; si la integración ya no está activa, la consola dice que hay que quitarlo a mano y la fila se queda. Una región hija que alguien puso dentro de la parcela de WorldGuard se queda (sin padre).
  - Reglas de GP a mano (las mismas que la pala): mundo con claims apagados en GP → `parcel.claims-disabled`; dueño con `MaximumNumberOfClaimsPerPlayer` claims → `parcel.too-many-claims` (con `griefprevention.overrideclaimcountlimit` y conectado no cuenta; desconectado sí); caja más estrecha que `MinimumWidth` → `parcel.too-narrow`; área menor que `MinimumArea` → `parcel.too-small`. El permiso `griefprevention.createclaims` no se mira.
  - El plugin rechaza un terreno concreto (p. ej. otro plugin cancela `ClaimCreatedEvent` lanzando, o el mundo no está cargado): falla solo esa parcela con aviso en la consola, y la integración sigue activa en `/structuresadmin integrations`.
- Reparar (Task 9), desde «Mis construcciones» abierto en una mesa libre propia:
  - Romper 5 bloques de una casa terminada y pulsarla: confirmación con «faltan 5 de N»; aceptar crea la obra de reparación (`repair.created`) y abre Materiales con el precio proporcional (redondeo hacia arriba). Depositar y construir: solo vuelven esos 5 bloques; un bloque que otro jugador puso en un hueco se queda (cuenta como saltado). Al terminar, «Mis construcciones» sigue con UNA sola entrada con la fecha nueva, y en `builds` no hay fila nueva.
  - Con `money: 10` y 5 de 100 bloques faltando: la confirmación dice 0.50 y se cobra eso (`structure.charged`). Sin Vault: `structure.needs-vault`.
  - Casa intacta: `repair.nothing`. Toda la casa quitada (WorldEdit): `repair.vanished` y desaparece de la lista (en una parcela, su claim o región se borra como con `/structuresadmin delete`).
  - Estando lejos (parte de la caja en chunks sin cargar): `repair.not-loaded`. Con la mesa ocupada: `catalog.bench-busy`. Con la mesa de otro (con acceso) o de nivel bajo: `repair.needs-bench` / `catalog.locked-tier`. Cambiar el esquemático (otro hash) y recargar: `repair.changed`; una construcción de antes (sin hash en `builds`) se puede reparar.
  - Dos mesas reparando la misma construcción a la vez: la segunda da `repair.overlaps`.
  - Parcela con su terreno puesto: la reparación arranca (su `precheck` no se mira) y al terminar no se crea otro terreno.
  - Límite de lo pagado: romper 1 bloque, aceptar la reparación, romper luego 3 más y construir: vuelve SOLO 1 bloque (los otros 3 huecos cuentan como saltados). Reiniciar a mitad de una reparación: tras el reinicio no escribe más de lo pagado (puede colarse lo escrito en el último segundo antes de una caída sin guardar).
  - Una construcción grande (decenas de miles de bloques): el clic no da tirón (se lee en varios ticks, 4096 por tick); clics seguidos mientras se lee dan `repair.scanning`. Alejarse mientras se lee (un chunk se descarga): `repair.not-loaded`.
  - Bloques de la lista `replace` de la estructura (p. ej. tierra) en un hueco cuentan como faltantes y se pagan.
  - Límite conocido: una construcción desaparecida cuya estructura cambió (`repair.changed`) o que pide dinero sin Vault (`structure.needs-vault`) no se puede limpiar desde el menú: el motivo sale antes de mirar el mundo. Se quita con `/structuresadmin delete <id>`.
- Mejorar (Task 10), con `casa2.yml` que lleve `upgrade_from: casa` (y una tercera con `upgrade_from: nada` para ver el problema `upgrade_from` en `/structuresadmin validate`):
  - En «Mis construcciones», una casa terminada muestra «Clic derecho: mejorar a otra estructura». Clic derecho: lista con `casa2`; «Volver» regresa a la lista. Una construcción sin mejoras: `build-upgrade.none`.
  - Elegir `casa2`: confirmación con el precio ENTERO de `casa2` (materiales y `money:`); aceptar crea la obra (`build-upgrade.created`) en el mismo origen y giro y abre Materiales.
  - Antes de depositar, cambiar a mano un bloque de la casa (p. ej. un tablón por adoquín) y poner tierra en un hueco de la caja nueva: al construir, los bloques originales que siguen igual se cambian por los de `casa2`; el adoquín puesto a mano y la tierra se quedan (cuentan como saltados).
  - Al terminar: en «Mis construcciones» queda UNA entrada, `casa2`, con la caja nueva; en `builds` la fila vieja ya no está.
  - `casa2` más grande que la casa: si la caja nueva pisa otra construcción terminada, aceptar da `build-upgrade.overlaps` (sin cobrar; con dinero, se devuelve). Con otra obra activa en la caja nueva, igual.
  - Mejorar mientras la misma casa se repara (o mejora) desde otra mesa: `build-upgrade.overlaps`.
  - Granja → granja (`module.type: farm` en las dos): al terminar, el log no avisa de fallos; la granja nueva funciona. Parcela vieja con terreno → estructura sin parcela: al terminar se borra el terreno viejo.
  - Límite conocido (ruling): parcela con terreno → parcela no se puede mejorar: el `precheck` de la nueva choca con el terreno viejo (`parcel.overlaps-claim` / `parcel.overlaps-region`).
  - Cancelar a mitad una mejora: lo ya cambiado se queda puesto y la construcción sigue siendo la vieja. Repararla luego solo repone huecos: los bloques ya cambiados cuentan como ajenos y no se pisan.
  - Tras la revisión final: mejora junto al terreno de otro jugador (la caja nueva lo pisa): al aceptar sale el motivo de protección ANTES de cobrar; con bypass, deja. Mejora más alta que el techo del mundo: `preview.out-of-world`.
  - Casa girada 90/180/270 y `casa2` más grande: la parte «casa» de `casa2` cae exactamente sobre la vieja y la ampliación crece hacia el lado que le toca según el giro.
  - Un cofre con ítems (o un cartel con texto) de la casa vieja en una posición que la mejora cambia: el cofre se queda con sus ítems (cuenta como saltado).
  - Abrir «Mis construcciones», borrar la construcción (`/structuresadmin delete` o `repair.vanished`) y, con el menú viejo abierto, mejorarla: `build-upgrade.gone`, sin cobrar.

## Banco de los planes 2 y 3 (Lib 1.1.14 + Structures 1.1.0 de desarrollo, 2026-11-25/26)

Banco Docker `structures-qa`, bots QaDueno (dueño) y QaOtro, script `E:\Dev\scratchpad\structures-20261123\qa6.js` (pasos `construir`, `reparar`, `mejorar`, `girada`; `BX=<x>` cambia la mesa de `girada`). Las `.nbt` de prueba (`caseta` 5x4x5 con cofre y `caseta2` 7x4x5 con abeto, barril y cristal) salen de `hacer_nbt.py`.

| Prueba | Resultado |
|---|---|
| Arranque: «Base de datos migrada a la versión 6», sin errores | OK |
| `validate`: 2 válidas; `mala.yml` (`upgrade_from: nada`) sale con [upgrade_from] | OK |
| Construir `caseta` desde la mesa (catálogo, vista previa, confirmar, depositar, construir) | OK 4/4 |
| Reparar tras romper 3 bloques: confirmación con lo que falta, precio proporcional (2 tablones + 1 adoquín), los 3 bloques vuelven, sigue UNA construcción | OK (el script marcó 1 fallo suyo: la confirmación sí se abrió) |
| Mejorar a `caseta2` con clic derecho: paredes a abeto, el bloque cambiado a mano (stone_bricks) y el cofre con 5 diamantes se quedan, cristal en +x, `builds` cambia a `caseta2` con la caja nueva | OK 9/9 |
| Mejora de una caseta girada 180°: origen x-2, cristal en -x, nada en +x, `builds` con caja ox-2..ox+4 | OK |
| Terreno de GP de QaOtro donde iría la ampliación, con el dueño SIN op: «La zona cruza un claim de GriefPrevention…» antes de cobrar, sin obra nueva | OK |
| Lo mismo con el dueño op: deja mejorar (op lleva `rpgrollstructures.bypass`, default op) | OK, por diseño |

- Trampa del banco: los bots op se saltan la protección de zona. Para probar WorldGuard o GriefPrevention, `deop` al dueño.
- Sin probar en el banco: `build-upgrade.gone` (cubierto por prueba unitaria), granjas y parcelas en una mejora, MySQL.
