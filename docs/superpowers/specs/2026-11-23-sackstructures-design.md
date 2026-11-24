# RPGRoll-Structures — diseño (2026-11-23)

Plugin de estructuras prediseñadas que se construyen desde una mesa propia. Producto para la tienda
(shop.sackito.online) y para Sackito Network a la vez. Parte del «prompt maestro» de Sack
(StructureCraft) con las decisiones tomadas en la conversación del 2026-11-23.

## 1. Intención

Que un jugador fabrique una mesa, elija una estructura de un catálogo, la vea antes de ponerla, la gire,
entregue los materiales poco a poco y la vea levantarse sola, sin perder ni duplicar nada aunque el
servidor se caiga. Que un admin añada estructuras nuevas sin tocar Java. Que se venda como un módulo
más de RPGRoll y que en Sackito se vea y se use igual que el resto de la red.

## 2. Objetivos, no objetivos y antiobjetivos

Objetivos
- Mesa con tres niveles que se mejora en el sitio: 1 Constructor, 2 Ingeniero, 3 Arquitecto.
- Catálogo por categorías, ficha con requisitos, vista previa girable, depósitos parciales,
  construcción instantánea o progresiva, pausa/reanudar/cancelar con devolución.
- Todas las GUIs con fondo dibujado y todas las texturas y modelos propios (sección 8).
- Progreso y materiales persistentes y recuperables tras un apagado brusco.
- Integraciones opcionales: si falta un plugin, se apaga solo lo que depende de él.
- Editor de admin: de una selección de WorldEdit a una estructura publicada.
- API pública versionada con eventos.
- Cinco estructuras de ejemplo que se construyen de verdad.

No objetivos (esta versión)
- Mods de cliente. Todo es servidor + resource pack.
- Integraciones con ItemsAdder, Oraxen y Nexo (fuera por decisión de Sack).
- Reescribir el motor de estructuras de RPGRoll-Dungeons: se comparte (sección 5).
- Integraciones secundarias del prompt (ExecutableBlocks, crates, hogares…): solo el punto de
  extensión, sin adaptadores.
- Desplegar nada de Bedrock en Sackito (Sackito es solo Java desde el 2026-11-16).

Antiobjetivos (sería un fracaso aunque «funcione»)
- Que un depósito, una cancelación o un reinicio duplique o pierda materiales o dinero.
- Que una obra pise bloques ajenos o se salga de un terreno sin permiso.
- Que una obra grande o una vista previa grande tumbe TPS/MSPT.
- Recursos o integraciones presentados como hechos sin existir o sin probarse.

## 3. Restricciones

- Módulo del monorepo `E:\Dev\RPGRoll` (Gradle, `build-logic`), carpeta `structures`, paquete
  `com.sack.rpgroll.structures`. Nombre del plugin y del producto: **RPGRoll-Structures**.
- Depende de RPGRoll-Lib (`common`), como los demás módulos. No depende de `core`.
- Compila contra `paper-api:26.1.2.build.74-stable` y Java 25 como el resto del repo; `api-version`
  26.1. Se prueba además en un banco con Paper 26.3 (estable desde oct-2026, pide Java 25).
- Sackito (main) tiene WorldEdit 7.4.6, WorldGuard 7.0.18, GriefPrevention, LuckPerms, VaultUnlocked
  y Floodgate; no tiene FAWE, ItemsAdder, Oraxen, Nexo, MythicMobs ni DeluxeMenus.
- Reglas de Sack: preguntar antes de compilar jars (`gradlew test` sí); commit/push solo si lo pide;
  producción (main/VPS) solo con el servidor vacío o con DarkSack solo; tras tocar el pack, `srp
  publish`; nada de Bedrock en Sackito; la tienda y las licencias las decide Sack.
- Licencia como los demás módulos de pago (`licensing`, `license.yml`).

## 4. Enfoques

| | A. Módulo sobre RPGRoll-Lib (**elegido**) | B. Plugin independiente |
|---|---|---|
| Qué es | Un módulo más del monorepo; reutiliza menús, comandos, idiomas, ids de ítems, resource pack y bloques con modelo | Un jar solo, con todo propio |
| A favor | Mucho menos trabajo, mismo aspecto que la red, un solo sitio donde arreglar fallos | No necesita la Lib |
| En contra | El comprador instala también la Lib (como con cualquier módulo) | 2-3 veces el trabajo, duplica la Lib, dos bases de código |

Descartado: híbrido (núcleo independiente con la Lib opcional) — suma lo peor de los dos.
Sack eligió A («Opción A si me parece bien»). ItemsAdder, Oraxen y Nexo quedan fuera (decisión de
Sack del 2026-11-23): no hay adaptadores para ellos; otros plugins pueden añadir su proveedor de
material por la API.

## 5. Arquitectura

Un módulo Gradle con paquetes en vez de once módulos (los once del prompt serían un jar cada uno o
módulos vacíos; la separación se mantiene por paquetes e interfaces):

```
structures/src/main/java/com/sack/rpgroll/structures/
  api/            API pública versionada (StructuresApi, eventos, SPI de proveedores y módulos)
  catalog/        StructureRegistry, StructureDefinition, cargador/validador YAML, categorías
  schematic/      BlockSource (interfaz) + lectores .schem (WorldEdit/FAWE) y .nbt (Paper)
  workbench/      WorkbenchService, bloque con modelo, niveles, estados, acceso
  material/       MaterialService, MaterialProvider (SPI) y proveedores
  deposit/        DepositService, TransactionService (libro de movimientos)
  preview/        PreviewService (bloques falsos por jugador + contorno de partículas)
  build/          ConstructionEngine, ConstructionScheduler (presupuesto global), órdenes de colocación
  protect/        ProtectionService + adaptadores (GriefPrevention, WorldGuard, permiso vanilla)
  module/         Módulos funcionales (granja de mobs, parcela, granja automática) y su SPI
  storage/        StorageService (SQLite por defecto, MySQL/MariaDB opcional), migraciones
  gui/            Todas las GUIs (sección 7)
  integration/    IntegrationManager y adaptadores opcionales (sección 10)
  command/        /estructuras y /structuresadmin
```

Motor de estructuras compartido: la lectura de `.schem` (WorldEdit) y `.nbt` (Paper
`StructureManager`) que hoy vive en `dungeons/.../structure` (`SchematicBridge`,
`StructurePasteEngine`) se sube a la Lib como lector de bloques (`BlockSource`: lista ordenada de
posición relativa + `BlockData` + datos de bloque-entidad opcionales, con rotación). Dungeons pasa a
usarlo sin cambiar su comportamiento. RPGRoll-Structures no pega el schematic entero de golpe: lee la
lista y la coloca él (instantáneo = todo en un tick con presupuesto; progresivo = por ciclos).

Formatos: `.schem` necesita WorldEdit o FAWE (soft-depend). `.nbt` (estructuras de Paper) funciona
siempre. Las cinco de ejemplo se entregan en los dos formatos.

## 6. La mesa

- Un solo objeto («Mesa de construcción») que se fabrica en nivel 1 y se mejora en el sitio desde su
  menú a nivel 2 y 3, pagando materiales y, si hay economía, dinero (configurable). Conserva su UUID,
  dueño, proyectos e historial; cambia su modelo al del nivel. Bajar de nivel no existe.
- Bloque colocado con modelo 3D (patrón `StationBlocks` de Enchantments/Machines: barrera + ItemDisplay
  con el modelo; sin pack, un bloque vanilla por nivel: mesa de crafteo, mesa de herrería, faro).
- Identidad: el ítem lleva en su PDC un id de tipo y un número de serie firmado (HMAC con secreto por
  servidor) para que no se pueda fabricar uno con `/give` o editando NBT; la mesa colocada guarda su
  UUID en la base de datos y en el PDC del chunk. Romperla devuelve el ítem con su UUID y nivel.
- Romper una mesa con proyecto activo: política configurable (`block` de fábrica: no se puede romper;
  `pause`: se rompe y la obra queda en pausa hasta volver a colocarla; `cancel`: cancela y devuelve
  según la política de cancelación).
- Acceso: dueño + lista de confianza (desde su menú); `rpgrollstructures.bypass` para staff.
- Estados visuales: Inactiva, Esperando materiales, Lista, Construyendo, Pausada, Completada, Error.
  Cada nivel tiene un «piloto» (cristal o lámpara) en el modelo cuyo color cambia según el estado
  (variantes de modelo generadas, sección 8) + partículas y sonido al construir. Nada de bloques
  alrededor ni más de un ItemDisplay por mesa.
- Tamaño máximo por nivel (configurable): 1 → 16×16×16, 2 → 32×32×32, 3 → configurable (de
  fábrica 64×96×64). Cada estructura declara `tier:` mínimo; el catálogo muestra las de nivel mayor
  como bloqueadas con el motivo.

## 7. GUIs

Inventarios nativos (funcionan en Bedrock vía Geyser). Todas con fondo dibujado (`MenuSkin` de la Lib,
`menus.pack`) y botones dibujados en el fondo cuando no cambian; sin pack se ven como cofres normales.
**El fondo cambia con el nivel de la mesa**: tres familias nuevas en el generador de fondos
(`E:\Dev\sackito-network\infra\maquinas\menus_jugador.py`), una por nivel, a juego con los diseños
de Sack: *taller* (madera y cuero rojo, Constructor), *ingenio* (pizarra, hierro, redstone y
lámparas, Ingeniero) y *arcano* (pizarra negra con runas cian brillantes, Arquitecto). Las de admin
usan la familia Forja ya existente.

Menús de jugador (cada uno ×3 familias):
1. **Centro de construcción** (principal): catálogo, mis construcciones, materiales pendientes,
   obra activa, mejoras, acceso, ayuda.
2. **Categorías** del catálogo (iconos propios) → 3. **Lista** paginada de estructuras.
4. **Ficha** de estructura: dimensiones, nivel, materiales, coste, tiempo, permisos, desbloqueo;
   botones Vista previa y Elegir.
5. **Materiales**: cada recurso con verde/amarillo/rojo y «128/256»; depositar todo, por tipo, uno;
   recuperar; ver lo que falta; confirmar construcción.
6. **Vista previa**: girar 90°, mover ±1 en cada eje, cambiar modo (bloques/contorno), confirmar,
   cancelar. Mientras está abierta también funcionan atajos con la mano vacía.
7. **Obra activa**: % de progreso, bloques puestos, tiempo restante, estado, pausar/reanudar,
   cancelar.
8. **Confirmar** (cancelar obra, mejorar mesa): reutiliza el `confirmar` de la Lib con su familia.
9. **Mis construcciones / historial**.
10. **Mejoras de la mesa** (requisitos del siguiente nivel).
11. **Acceso** (lista de confianza).
12. **Ayuda**.

Menús de admin (Forja): lista de estructuras, editor (nombre, categoría, nivel, materiales — se
calculan solos del schematic y se pueden editar —, tiempo, modo, orden, permisos, reglas, coste,
módulo funcional), validar y publicar.

Textos en `lang/es.yml`, `en.yml`, `pt_BR.yml` y títulos/iconos editables en `gui.yml`.

## 8. Texturas y modelos (todo propio, por código)

Con la skill `minecraft-models` y los generadores de `E:\Dev\sackito-network\infra`:
- **Tres mesas 3D** siguiendo los bocetos de Sack (madera con tablero de crafteo y patas;
  banco de pizarra/hierro con redstone, lámparas y estante; plataforma negra con anillos y runas
  cian que brillan). Cada una: modelo de ítem, modelo colocado, siete variantes de piloto por estado
  (21 modelos que comparten geometría), `.bbmodel` editable, PNG de vista previa.
- **Fondos de GUI**: 12 menús × 3 familias + menús de admin, con sus fuentes (`font/menus/<id>.json`).
- **Iconos**: categorías (casas, granjas de mobs, granjas automáticas, parcelas, decoración,
  infraestructura, redstone, especiales), botones (girar, mover, depositar, recuperar, pausar…),
  piloto de estados y un icono por estructura de ejemplo.
- **Resource pack Java**: módulo de SackResourcePack (`content/rpgroll-structures`, namespace
  `rpgroll-structures`) para quien use SackResourcePack; y un zip suelto con el mismo contenido para quien
  no lo use (sin fusionar packs ajenos).
- **Bedrock**: `.mcpack` y mappings de Geyser generados por la skill para los compradores que lo
  quieran. En Sackito no se despliegan.

## 9. Catálogo, materiales, vista previa y construcción

Definición de estructura (`structures/<id>.yml`): el ejemplo del prompt sirve de base, más `tier:`,
`icon:`, `module:` (funcional), `placement:` (reglas: suelo, aire, líquidos, mundos, biomas) y
`build.order:` (`bottom_up`, `layers`, `center_out`, `stages`, `custom`). Materiales con ids de la Lib
(`minecraft:oak_planks`, `item:…`, `mueble:…`) y de proveedores que registren otros plugins por la
API, con cantidad, alternativas (`any_of`) y opcionales. Si un material no existe
o su proveedor no está, la estructura se marca inválida con un mensaje que dice archivo, clave y motivo.

Proveedores de material (`MaterialProvider`): reconocen el ítem por su identidad real (material
vanilla o PDC de la Lib; los que se registren por la API, con la suya), nunca por nombre o lore.

Depósitos (invariante: la suma de materiales del jugador + depositados nunca crece y nunca se pierde
sin dejar registro):
- Todo pasa en el hilo principal en un solo paso (calcular, quitar del inventario, sumar al proyecto
  en memoria), se apunta en el libro de movimientos con un id único y se escribe en la base de datos
  en su hilo; tras confirmar la escritura se guarda el jugador (`Player#saveData`).
- Bloqueo por jugador y por proyecto: un depósito a la vez; clics rápidos se descartan.
- Al arrancar, los movimientos sin confirmar se reconcilian (idempotentes por id).
- Recuperar y cancelar devuelven al inventario; lo que no cabe queda pendiente de recoger en el menú.
- Política de devolución al cancelar configurable (de fábrica 100 % de lo no gastado).

Vista previa: bloques falsos solo para ese jugador (`Player#sendBlockChange`/`sendMultiBlockChange`),
contorno con partículas (aristas y origen; rojo donde choca). Tope de bloques enviados (de fábrica
4096, el resto se ve como contorno), de distancia y de duración. En Bedrock, solo contorno. Valida
protección, límites del mundo, bloques prohibidos y obras de otros.

Construcción:
- Instantánea o progresiva (N bloques cada X ticks por obra) bajo un **presupuesto global** de bloques
  por tick que reparten todas las obras; límite de obras simultáneas por servidor, mundo y jugador.
- Solo coloca en aire o reemplazables salvo que el YAML diga otra cosa; consume del depósito al
  colocar; bloques-entidad (cofres, spawners, carteles) con sus datos al final de su capa.
- Persistencia por cursor: se guarda el índice del siguiente bloque cada ciclo; al reanudar, se vuelve
  a comprobar desde el último guardado (colocar el mismo `BlockData` dos veces no cambia nada), así que
  no duplica bloques ni cobra dos veces.
- Chunk descargado o mesa sin cargar: la obra se pausa y sigue al volver. Errores: estado Error con el
  motivo en el menú y en el log de admin.

## 10. Integraciones (todas opcionales, cada una se activa en `integrations.yml`)

| Plugin | Uso |
|---|---|
| WorldEdit / FAWE | leer `.schem`; selección para `/structuresadmin create` |
| WorldGuard | ¿puede construir aquí? (consulta de región por bloque); bandera propia opcional |
| GriefPrevention | ¿puede construir aquí?; las parcelas pueden crear un terreno del dueño si el YAML lo pide (gasta sus bloques) |
| Vault / VaultUnlocked | costes, mejoras y reembolsos |
| LuckPerms | nada propio: los permisos son Bukkit; LuckPerms los gestiona |
| Floodgate / Geyser | saber si el jugador es Bedrock (contorno en vez de bloques, avisos) |
| MythicMobs | módulo de granja de mobs con mobs de Mythic |
| RPGRoll-Machines / Mobs | spawners de la Lib (`spawner:`) y mobs de RPGRoll en granjas |
| DeluxeMenus u otros | nada que integrar: basta con `/estructuras abrir` |

Sin el plugin, la integración se apaga y la función que depende de ella se oculta con su motivo. Antes
de escribir cada adaptador se lee la documentación oficial vigente; nada de métodos inventados.

## 11. Módulos funcionales

`StructureModule` (SPI): `onBuilt`, `onTick` opcional, `onRemoved`, configuración en el YAML.
De fábrica: granja de mobs (spawner vanilla o de la Lib, MythicMobs opcional, límites de entidades,
bioma/dimensión), parcela (bordes, accesos, terreno de GriefPrevention/región de WorldGuard opcional),
granja automática (solo coloca; aclara en la ficha si funciona por mecánicas vanilla o si necesita
algo más; no promete producción). Casas: variantes de material y estilos como estructuras distintas
con `upgrade_from:`.

## 12. Persistencia

SQLite por defecto (el driver que trae Paper); MySQL/MariaDB opcional con su driver por `libraries:`
de `plugin.yml`. Un hilo de base de datos, nunca en el hilo principal. Tablas: mesas, accesos,
proyectos, depósitos por material, libro de movimientos, cursor de obra, historial, pendientes de
recoger, versión del esquema (migraciones). Copia del archivo SQLite al actualizar el esquema
(`backups/`).

## 13. Comandos y permisos

- Jugador: `/estructuras` (es) · `/structures` (en) · `/estruturas` (pt) vía `command-words.yml`, con
  autocompletado: `ayuda`, `abrir`, `lista`, `info <id>`, `estado`, `cancelar`.
- Admin (en inglés, como el resto de RPGRoll): `/structuresadmin give|reload|create|import|export|edit|delete|validate|integrations|debug`.
- La vista previa y la construcción se hacen desde la mesa, no por comando (`/estructuras` sin mesa
  solo muestra el catálogo e info), para que la mesa sea siempre el núcleo.
- Permisos `rpgrollstructures.use`, `.workbench.place`, `.workbench.break`, `.build`, `.preview`,
  `.build.<id>`, `.category.<cat>`, `.tier.<n>`, `.bypass`, `.admin` (+ `.admin.give`, `.admin.create`,
  `.admin.reload`).
- `/dar mesa:constructor` etc. y la receta de la mesa salen de la Lib (`Things`).

## 14. Archivos

`plugins/RPGRoll-Structures/`: `config.yml`, `gui.yml`, `workbench.yml` (niveles, recetas, mejoras,
tamaños), `integrations.yml`, `storage.yml`, `performance.yml`, `categories.yml`, `lang/`,
`structures/`, `schematics/`, `backups/`. Los modelos y el pack van en el jar y se instalan en
SackResourcePack (o se exportan en zip).

## 15. API pública

`StructuresApi` (servicio de Bukkit, versión semántica): registrar/consultar estructuras y categorías,
crear proyectos, consultar progreso, añadir materiales, validar requisitos, registrar proveedores de
material, módulos funcionales e integraciones. Eventos: `StructurePreviewEvent`,
`StructureBuildStartEvent`, `StructureBuildProgressEvent`, `StructureBuildCompleteEvent`,
`StructureBuildCancelEvent`, `StructureMaterialsDepositEvent`, `WorkbenchUpgradeEvent`; cada uno
documenta si es cancelable y en qué hilo se lanza (todos en el principal).

## 16. Estructuras de ejemplo (las elige Claude, delegado por Sack)

Casa de madera (nivel 1), granja de cultivos con agua y bordes (1), parcela delimitada de 16×16 con
valla y puerta (1), granja de mobs con spawner de zombi y caída a tolvas (2), torre decorativa de
piedra (2). Cada una: `.schem` + `.nbt` generados por código, YAML, icono y requisitos calculados.

## 17. Pruebas

Unitarias (sin servidor): cálculo de materiales, rotaciones, órdenes de colocación, libro de
movimientos e invariantes de depósito, reconciliación tras caída, políticas de devolución, validación
de YAML, presupuesto del planificador, niveles. Integración con base de datos (SQLite de prueba).
En servidor de pruebas (`infra/capturas`, bots y cliente de Sack): todos los casos obligatorios del
prompt (inventario lleno, desconexión al depositar, reinicio a mitad de obra, cancelación, sin
permisos, WorldGuard/GriefPrevention, schematic inválido, bloques no compatibles, obras simultáneas,
mesa rota, chunk descargado, fallo de base de datos, jugador Bedrock, dependencias ausentes) y prueba
de rendimiento con una estructura grande midiendo MSPT. Lo que solo puede probarse a mano queda
listado en la guía de pruebas.

## 18. Fases (cada una termina con tests en verde antes de la siguiente)

1. Módulo base, config, almacenamiento y migraciones.
2. Mesa (ítem, bloque con modelo provisional, niveles, acceso) y menú principal.
3. Catálogo, lector de bloques compartido en la Lib, materiales y depósitos.
4. Vista previa y validación del terreno.
5. Construcción instantánea y progresiva.
6. Recuperación, protecciones y límites.
7. Integraciones externas y módulos funcionales.
8. Modelos, fondos, iconos, packs (Java y Bedrock) y las cinco estructuras.
9. API, documentación (RPGRollDocs: jugadores, admins, API, compatibilidad, limitaciones) y QA final.

Compilar jars, desplegar en Sackito y publicar en la tienda: cada uno con el permiso de Sack en su
momento.

## 19. Decisiones tras la revisión (calls de Claude, Sack puede corregirlas)

- **Materiales = precio, no bloque a bloque.** La lista del YAML es el coste de la estructura. El
  editor de admin la propone a partir del schematic (bloque → ítem que lo da), pero luego es
  independiente. La obra solo arranca al 100 % (estado «Lista»). El consumo del depósito avanza en
  proporción al progreso; al cancelar, «lo no gastado» es la parte proporcional a lo que falta.
- **Las estructuras terminadas se registran** (tabla de construcciones con mundo, origen, rotación y
  caja). Desde «Mis construcciones» se puede **reparar** (repone los bloques que falten pagando su
  parte) y **mejorar** si otra estructura declara `upgrade_from:` (puede reemplazar solo los bloques
  que pertenecen a la construcción original). No hay demolición con devolución en esta versión.
  `onRemoved` salta cuando un admin borra el registro o la construcción desaparece (comprobación al
  reparar).
- **Dungeons no se migra.** El lector de bloques (`BlockSource`) va a la Lib como código nuevo,
  inspirado en el de Dungeons. Dungeons sigue con su pegado actual. Una versión nueva de la Lib hace
  falta igualmente.
- **Entidades del schematic** (marcos, soportes de armadura, mobs) se ignoran de fábrica;
  `entities: true` en el YAML las coloca vacías (sin ítems dentro) para no abrir una vía de
  duplicación.
- **Mesa rota con política `pause`**: la obra se queda atada a su origen y su depósito, y el ítem
  guarda el UUID. Al colocar la mesa otra vez, en cualquier sitio, se reconecta. La obra sigue en su
  sitio original y la protección se vuelve a comprobar al reanudar.
- **Bocetos de las mesas**: `docs/superpowers/specs/sackstructures/bocetos-mesas.jpg` (los de
  Sack). Las tres familias de fondos (taller, ingenio y arcano) son propuesta de Claude. Antes de
  dibujar los 36 fondos se enseña una muestra por familia para aprobarla.

## 20. Lo que queda al constructor

Nombres de clases y tablas, formato exacto del libro de movimientos, el algoritmo de cada orden de
colocación, los valores de fábrica de rendimiento (dentro de lo dicho aquí), el dibujo concreto de
cada fondo e icono dentro de las tres familias, y qué API exacta de cada plugin externo usar tras
leer su documentación.
