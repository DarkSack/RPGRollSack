# RPGRoll-Structures — Plan 2 (fases 3, 4 y 5: catálogo, materiales, depósitos, vista previa y construcción)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que la mesa construya estructuras de verdad: catálogo por categorías leído de YAML, lector de bloques
compartido en la Lib (`.nbt` siempre, `.schem` con WorldEdit/FAWE), materiales con proveedores, depósitos sin
duplicar ni perder, vista previa girable con validación del terreno y construcción instantánea o progresiva
bajo un presupuesto global, con cursor persistente.

**Architecture:** Lógica pura (definiciones, validación, rotación, órdenes, cuentas de depósito, presupuesto,
choques) separada de los servicios de Paper y de los menús, para probarla sin servidor. Todo lo que toca el
mundo o inventarios va en el hilo principal; la base de datos, en su hilo (`Database.submit`). Los proyectos
viven en memoria (`ProjectService`) y se escriben en la base de datos en cada cambio.

**Tech Stack:** Java 25, Paper API de `build-logic`, Gradle, JUnit 5 + Mockito, SQLite/MariaDB (ya montados),
WorldEdit 7.4.5 `compileOnly` (como `dungeons`).

**Spec:** `E:\Dev\RPGRoll\docs\superpowers\specs\2026-11-23-sackstructures-design.md` (secciones 5, 6, 7, 9,
12, 13, 14, 17, 18 y 19). Plan anterior y su registro: `docs/superpowers/plans/2026-11-23-sackstructures-fase1-2.md`,
`.superpowers/sdd/2026-11-23-sackstructures-fase1-2/progress.md`.

Planes siguientes: 3 recuperación, protecciones (WorldGuard/GriefPrevention), límites finos, integraciones
(Floodgate, Vault en costes, MythicMobs) y módulos funcionales; 4 arte (fondos e iconos nuevos), las cinco
estructuras de ejemplo, editor de admin, API pública con eventos, docs y QA final.

## Global Constraints

- Módulo `structures`, paquete `com.sack.rpgroll.structures`; Lib en `common`, paquete nuevo `com.sack.rpgroll.common.structure`.
- Sin dependencia de `:core`. Sin ItemsAdder, Oraxen ni Nexo.
- Formatos: `.nbt` (Paper `StructureManager`) siempre; `.schem` solo con WorldEdit o FastAsyncWorldEdit activos. Ninguna clase fuera del lector `.schem` importa `com.sk89q.*`.
- Dungeons no se toca (spec 19): el lector de la Lib es código nuevo.
- Archivos en `plugins/RPGRoll-Structures/`: `structures/<id>.yml`, `schematics/`, `categories.yml`, `performance.yml` (nuevos aquí); `config.yml`, `gui.yml`, `workbench.yml`, `storage.yml` (existen).
- Ids de material con la sintaxis de la Lib: `minecraft:oak_planks`, `item:<id>`, `mueble:<id>` o cualquier tipo registrado en `Things`; también proveedores registrados por la API (`MaterialProvider`). Reconocer el ítem por identidad real (material vanilla sin datos, o PDC/`item_model`), nunca por nombre o lore.
- Materiales = precio (spec 19): la obra solo arranca con el 100 % depositado (estado `READY`); el consumo avanza en proporción al progreso; al cancelar se devuelve `cancel-refund` % (de fábrica 100) de lo no gastado.
- Invariante de depósito: materiales del jugador + depositados nunca crece y nada se pierde sin quedar en el libro (`ledger`).
- Vista previa: tope de 4096 bloques falsos (el resto, contorno), 64 bloques de distancia, 300 s de duración; contorno de partículas con aristas, origen y choques en rojo.
- Construcción: presupuesto global por tick (de fábrica 256 bloques), límites de obras simultáneas (servidor 10, mundo 5, jugador 2), solo en aire o reemplazables salvo `replace:` del YAML, `setBlockData(data, false)`, bloques-entidad al final de su capa, cursor guardado como mucho una vez por segundo por obra.
- Permisos nuevos: `rpgrollstructures.build`, `.preview`, `.build.<id>`, `.category.<cat>` (`default: true`), `rpgrollstructures.admin.validate` (`op`).
- Textos nuevos en `lang/es.yml`, `en.yml` y `pt_BR.yml` (el test `LangFilesTest` exige las mismas claves en los tres); títulos en `gui.yml`.
- Versiones al final: Lib `1.1.14`, Structures `1.1.0`, Structures exige Lib ≥ 1.1.14.
- Sack: sin commits ni push salvo orden suya (los pasos de commit se sustituyen por anotar los archivos tocados en el registro); preguntar antes de `gradlew jar`/`shadowJar`/`build` (`gradlew test` sí); nada en producción.

## Review Focus

1. Dos clics rápidos en «Depositar todo», o el jugador se desconecta justo después: se cobra y se apunta una sola vez (Task 7 `secondDepositWhileBusyIsDropped`, `depositIsIdempotentById`).
2. El servidor se cae a mitad de obra y arranca: la obra sigue desde el último cursor sin cobrar dos veces ni duplicar bloques (Task 11 `resumeFromSavedCursorDoesNotRecharge`).
3. Un jugador pone un bloque dentro de la caja de una obra ya validada: la obra no lo pisa (Task 11 `occupiedBlockIsSkippedNotReplaced`).
4. YAML con material inexistente, esquemático ausente o `.schem` sin WorldEdit: la estructura queda inválida con archivo, clave y motivo, y el resto del catálogo carga (Task 3 `badEntriesAreReportedAndOthersLoad`).
5. Inventario lleno al cancelar o recuperar: lo que no cabe queda pendiente de recoger, nunca en el suelo ni perdido (Task 8 `refundOverflowGoesToPending`).

---

### Task 1: Lector de bloques en la Lib (`BlockSource`)

**Files:**
- Create: `common/src/main/java/com/sack/rpgroll/common/structure/BlockSource.java`, `SourceBlock.java`, `Rotation.java`, `NbtBlockSource.java`, `SchemBlockSource.java`, `BlockSources.java`
- Modify: `common/build.gradle.kts` (WorldEdit `compileOnly` como en `dungeons/build.gradle.kts:39-46`; `version = "1.1.14"`), `common/src/main/resources/plugin.yml` (`version: 1.1.14`, `softdepend` añade `WorldEdit`, `FastAsyncWorldEdit` si no están)
- Test: `common/src/test/java/com/sack/rpgroll/common/structure/RotationTest.java`, `BlockSourcesTest.java`

**Interfaces:**
- Produces: `record SourceBlock(int x, int y, int z, BlockData data, BlockState tile)` — posición relativa a la esquina mínima, `tile` null salvo bloques-entidad (copia de su estado del `.nbt`).
- Produces: `interface BlockSource { int width(); int height(); int length(); List<SourceBlock> blocks(); }` — `blocks()` sin aire, orden indefinido.
- Produces: `enum Rotation { NONE, CW_90, CW_180, CCW_90 }` con `Rotation next()`, `StructureRotation toBukkit()`, `int[] apply(int x, int z, int width, int length)` (devuelve `{x', z'}` ya desplazado para que el resultado quede en `0..w'-1`, `0..l'-1`), `int rotatedWidth(int w, int l)`, `int rotatedLength(int w, int l)`; y `static List<SourceBlock> rotate(BlockSource src, Rotation r)` en `BlockSources` (rota posiciones y `BlockData#rotate(StructureRotation)`).
- Produces: `final class BlockSources` con `static BlockSource read(File file) throws IOException` (`.nbt` → `NbtBlockSource`; `.schem`/`.schematic` → `SchemBlockSource` si `schemAvailable()`, si no `IOException("WorldEdit o FastAsyncWorldEdit no está activo")`; otra extensión → `IOException("Formato no soportado: <nombre>")`), `static boolean schemAvailable()` (`isPluginEnabled("WorldEdit") || isPluginEnabled("FastAsyncWorldEdit")`).
- `NbtBlockSource`: `Bukkit.getStructureManager().loadStructure(file)`, primera paleta, `BlockState#getX/Y/Z`; `tile` = el `BlockState` si es `TileState`. `SchemBlockSource`: `ClipboardFormats.findByFile` + `BukkitAdapter.adapt(BlockState)` a `BlockData`, posiciones menos `clipboard.getMinimumPoint()`, `tile` siempre null (shortcut: los datos de bloque-entidad del `.schem` no se copian; las estructuras de ejemplo usan `.nbt`).

- [ ] **Step 1: Write the failing tests**

```java
@Test void cw90MapsCornersInsideTheRotatedBox() {
    // caja 3 de ancho (x) y 5 de largo (z): tras 90° es 5×3
    assertEquals(5, Rotation.CW_90.rotatedWidth(3, 5));
    assertArrayEquals(new int[]{4, 0}, Rotation.CW_90.apply(0, 0, 3, 5));
    assertArrayEquals(new int[]{0, 2}, Rotation.CW_90.apply(2, 4, 3, 5));
}
@Test void fourTurnsAreIdentity() { /* para cada (x,z) de una caja 4×7: CW_90 aplicado 4 veces con dims rotadas = (x,z) */ }
@Test void nextCycles() { assertEquals(Rotation.NONE, Rotation.CCW_90.next()); }
@Test void unknownExtensionIsRejected(@TempDir Path d) { /* read(d/"a.txt") lanza IOException con "Formato no soportado" */ }
@Test void schemWithoutWorldEditIsRejected(@TempDir Path d) { /* MockBukkit no; Bukkit mockeado: isPluginEnabled → false; read("a.schem") lanza "WorldEdit o FastAsyncWorldEdit" */ }
```

- [ ] **Step 2: Run** `./gradlew :common:test --tests "*structure*"` — Expected: FAIL (clases inexistentes).
- [ ] **Step 3: Implement** las clases de arriba. `Rotation.apply`: `CW_90 → (l-1-z, x)`, `CW_180 → (w-1-x, l-1-z)`, `CCW_90 → (z, w-1-x)` (mismo sentido que `StructureRotation` mirando desde arriba).
- [ ] **Step 4: Run** `./gradlew :common:test` — Expected: PASS (todo el módulo, no solo los nuevos).
- [ ] **Step 5: Registro** de archivos tocados (sin commit).

### Task 2: Ajustes nuevos (`categories.yml`, `performance.yml`, permisos)

**Files:**
- Create: `structures/src/main/resources/categories.yml`, `performance.yml`; `structures/.../config/CategorySettings.java`, `PerformanceSettings.java`
- Modify: `StructuresPlugin.java` (`saveDefaults` añade los dos archivos y crea las carpetas `structures/` y `schematics/`; `loadConfigs` los lee), `plugin.yml` (permisos de Global Constraints; `depend` sigue `[RPGRoll-Lib]`)
- Test: `structures/src/test/java/.../config/CategorySettingsTest.java`, `PerformanceSettingsTest.java`

**Interfaces:**
- Produces: `record Category(String id, String icon, int order)`; `record CategorySettings(List<Category> categories)` con `static CategorySettings from(ConfigurationSection, Consumer<String> warn)` y `Optional<Category> byId(String)`. Nombre visible: clave de lang `category.<id>`.
- Produces: `record PerformanceSettings(int blocksPerTick, int maxProjectsServer, int maxProjectsWorld, int maxProjectsPlayer, int previewMaxBlocks, int previewMaxDistance, int previewSeconds, int cancelRefundPercent)` con `from(...)`; valores de fábrica 256, 10, 5, 2, 4096, 64, 300, 100; un valor fuera de rango (≤0, o `cancelRefundPercent` fuera de 0..100) avisa con su clave y vuelve al de fábrica.
- Categorías de fábrica (spec 8), en este orden: `casas`, `granjas_mobs`, `granjas_auto`, `parcelas`, `decoracion`, `infraestructura`, `redstone`, `especiales`; iconos vanilla (`minecraft:oak_door`, `minecraft:spawner`, `minecraft:wheat`, `minecraft:oak_fence`, `minecraft:flower_pot`, `minecraft:rail`, `minecraft:redstone`, `minecraft:nether_star`).

- [ ] **Step 1: Write the failing tests** — `defaultsMatchThePlan()` (los 8 valores de rendimiento y las 8 categorías en orden), `badValueWarnsWithKeyAndFallsBack()` (`blocks-per-tick: 0` → aviso que contiene `"blocks-per-tick"`, queda 256), `duplicateCategoryIdKeepsFirstAndWarns()`.
- [ ] **Step 2: Run** `./gradlew :structures:test --tests "*Settings*"` — Expected: FAIL.
- [ ] **Step 3: Implement.** Claves YAML: `blocks-per-tick`, `max-projects.server|world|player`, `preview.max-blocks|max-distance|seconds`, `cancel-refund`; `categories.<id>.icon|order`.
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 3: Definición de estructura, cargador y validador

**Files:**
- Create: `structures/.../catalog/StructureDefinition.java`, `MaterialSpec.java`, `BuildOrder.java`, `PlacementRules.java`, `StructureLoader.java`, `Problem.java`, `StructureRegistry.java`
- Test: `structures/src/test/java/.../catalog/StructureLoaderTest.java`, `StructureRegistryTest.java`

**Interfaces:**
- Consumes: `BlockSource`, `BlockSources.read` (Task 1); `CategorySettings` (Task 2); `WorkbenchTier`; `MaterialRegistry.knows(String)` (Task 5) llega como `Predicate<String> materialExists` para no acoplar.
- Produces: `record MaterialSpec(List<String> anyOf, int amount, boolean optional)` con `String key()` = `anyOf.getFirst()` (clave estable en la base de datos).
- Produces: `enum BuildOrder { BOTTOM_UP, LAYERS, CENTER_OUT, STAGES, CUSTOM }`.
- Produces: `record PlacementRules(boolean needsGround, boolean allowLiquids, List<String> worlds, List<String> biomes, Set<Material> replace)`; vacías = sin restricción.
- Produces: `record StructureDefinition(String id, Map<String,String> names, String category, WorkbenchTier tier, String icon, File schematic, BlockSource blocks, List<MaterialSpec> materials, double money, boolean instant, BuildOrder order, List<Material> customOrder, int blocksPerCycle, int ticksPerCycle, PlacementRules placement, String permission)` con `String name(String lang)` (cae a `es` y luego al id) y `int volume()` (bloques no aire).
- Produces: `record Problem(String file, String key, String reason)`; `record LoadResult(List<StructureDefinition> valid, List<Problem> problems)`; `StructureLoader.load(File structuresDir, File schematicsDir, CategorySettings cats, Predicate<String> materialExists, Function<File, BlockSource> reader)` → `LoadResult` (`reader` envuelve `BlockSources.read`; una `IOException` se convierte en `Problem` con la clave `schematic`).
- Produces: `final class StructureRegistry` con `void replace(LoadResult)`, `Optional<StructureDefinition> get(String id)`, `List<StructureDefinition> inCategory(String cat)` (ordenadas por nivel y luego nombre), `List<Problem> problems()`.
- Formato YAML (un archivo por estructura, el id es el nombre del archivo sin `.yml`):

```yaml
name: {es: "Casa de madera", en: "Wooden house", pt_BR: "Casa de madeira"}
category: casas
tier: 1                 # 1 constructor, 2 ingeniero, 3 arquitecto
icon: minecraft:oak_door
schematic: casa_madera.nbt
materials:
  - {item: minecraft:oak_planks, amount: 128}
  - {any_of: [minecraft:glass, minecraft:glass_pane], amount: 8}
  - {item: minecraft:torch, amount: 4, optional: true}
money: 0
build: {mode: progressive, order: bottom_up, blocks-per-cycle: 16, ticks-per-cycle: 20}
placement: {ground: true, liquids: false, worlds: [], biomes: [], replace: [minecraft:short_grass]}
permission: ""          # vacío = rpgrollstructures.build.<id>
```

- Validación (cada fallo es un `Problem`, la estructura no entra): id que no casa `[a-z0-9_-]+`; falta `name.es`; categoría desconocida; `tier` fuera de 1..3; esquemático ausente o ilegible; esquemático más grande que el tamaño máximo de su nivel en `workbench.yml` (en cualquiera de las dos rotaciones horizontales cabe si cabe sin rotar); material desconocido (`key` = `materials[<i>]`); `amount` ≤ 0; orden desconocido; `custom` sin `build.custom`; `blocks-per-cycle` o `ticks-per-cycle` ≤ 0.

- [ ] **Step 1: Write the failing tests** (`reader` falso que devuelve una caja de N bloques, sin servidor):

```java
@Test void validFileLoadsWithDefaults() { /* solo name.es, category casas, tier 1, schematic y un material → instant=false, order BOTTOM_UP, 16/20, permission "rpgrollstructures.build.<id>" */ }
@Test void badEntriesAreReportedAndOthersLoad() {
    // 4 archivos: ok.yml, mat.yml (material "minecraft:nope"), nofile.yml (esquemático ausente), big.yml (tier 1 con 17×1×1)
    var r = load(...);
    assertEquals(List.of("ok"), ids(r.valid()));
    assertTrue(r.problems().contains(new Problem("mat.yml", "materials[0]", "material desconocido: minecraft:nope")));
    assertTrue(r.problems().stream().anyMatch(p -> p.file().equals("nofile.yml") && p.key().equals("schematic")));
    assertTrue(r.problems().stream().anyMatch(p -> p.file().equals("big.yml") && p.key().equals("schematic")));
}
@Test void anyOfUsesFirstAsKey() { /* {any_of:[a,b], amount: 8} → key a, anyOf [a,b] */ }
@Test void nameFallsBackToSpanishThenId() { }
@Test void registryOrdersByTierThenName() { }
```

- [ ] **Step 2: Run** `./gradlew :structures:test --tests "*catalog*"` — Expected: FAIL.
- [ ] **Step 3: Implement.** Motivos en español, fijos: `material desconocido: <id>`, `no existe el archivo <nombre>`, `más grande que el nivel <n> (<w>×<h>×<l>)`, `categoría desconocida: <id>`, etc.
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 4: Migración 2 y repositorio de proyectos

**Files:**
- Modify: `structures/.../storage/Migrations.java` (`LATEST = 2`, `v2(Dialect)`; `run` aplica v1 si `current == 0` y v2 si `current < 2`, en la misma transacción)
- Create: `structures/.../storage/ProjectRecord.java`, `ProjectRepository.java`
- Test: `structures/src/test/java/.../storage/MigrationsTest.java` (añadir casos), `ProjectRepositoryTest.java`

**Interfaces:**
- Produces tablas (v2, sentencias repetibles como v1, sin nombrar `workbenches_location`):
  - `projects(id PK, bench_id NOT NULL, structure_id, owner, state, world, x, y, z, rotation, cursor INTEGER, placed INTEGER, skipped INTEGER, total INTEGER, error TEXT, created_at, updated_at)`
  - `project_materials(project_id, material_key, required, deposited, consumed, PRIMARY KEY(project_id, material_key))`
  - `ledger(id PK, project_id, player, kind, material_key, amount, created_at)` — `kind` ∈ `DEPOSIT`, `WITHDRAW`, `REFUND`, `CONSUME`, `CLAIM`
  - `pending_returns(id PK, player, item BLOB, created_at, claimed_at BIGINT NULL)` — `item` = `ItemStack#serializeAsBytes()`
  - `builds(id PK, project_id, structure_id, owner, world, x, y, z, rotation, min_x, min_y, min_z, max_x, max_y, max_z, completed_at)`
- Produces: `enum ProjectState { WAITING_MATERIALS, READY, BUILDING, PAUSED, COMPLETED, CANCELLED, ERROR }` (en `structures/.../project/ProjectState.java`) y `ProjectRecord(UUID id, UUID bench, String structure, UUID owner, ProjectState state, String world, int x, int y, int z, Rotation rotation, int cursor, int placed, int skipped, int total, String error, long createdAt, long updatedAt)`.
- Produces `ProjectRepository(Database, Dialect)`, todos `CompletableFuture`: `insert(ProjectRecord, Map<String,Integer> required)`, `active()` → `List<ProjectRecord>` (estados distintos de COMPLETED/CANCELLED), `materials(UUID)` → `Map<String, MaterialRow>` (`record MaterialRow(int required, int deposited, int consumed)`), `applyMovement(Movement)` → `Boolean` (true si se aplicó, false si su id ya estaba: idempotente; una sola transacción que inserta en `ledger` y suma `deposited`/`consumed` según `kind`), `saveProgress(UUID, ProjectState, int cursor, int placed, int skipped, String error)`, `complete(UUID, BuildRecord)` (estado COMPLETED + fila en `builds`, una transacción), `addPending(UUID player, byte[] item)` → id, `pending(UUID player)` → `List<PendingRow>`, `claim(long pendingId)` → `Boolean` (pone `claimed_at` si era null; false si ya estaba), `boxesIn(String world)` → cajas de obras activas y construcciones terminadas.
- `record Movement(UUID id, UUID project, UUID player, Kind kind, String materialKey, int amount)`; `DEPOSIT` suma a `deposited`, `WITHDRAW` y `REFUND` restan de `deposited`, `CONSUME` suma a `consumed`; nunca deja `deposited < consumed` ni negativos (si lo haría, la transacción se deshace y devuelve false).

- [ ] **Step 1: Write the failing tests** (SQLite en `@TempDir`, como `WorkbenchRepositoryTest`): `migratesV1DatabaseToV2KeepingBenches`, `freshDatabaseIsAtV2`, `depositIsIdempotentById` (misma `Movement` dos veces → segunda devuelve false, `deposited` sumado una vez), `withdrawBelowConsumedIsRejected`, `completeWritesBuildAndState`, `claimOnlyOnce`.
- [ ] **Step 2: Run** `./gradlew :structures:test --tests "*storage*"` — Expected: FAIL.
- [ ] **Step 3: Implement.** `Rotation` se guarda por `name()`.
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 5: Proveedores de material

**Files:**
- Create: `structures/.../api/MaterialProvider.java`, `structures/.../material/MaterialRegistry.java`, `VanillaProvider.java`, `ThingsProvider.java`, `ItemIdentity.java`
- Test: `structures/src/test/java/.../material/MaterialRegistryTest.java`, `ItemIdentityTest.java`

**Interfaces:**
- Produces: `interface MaterialProvider { String type(); boolean knows(String id); boolean matches(String id, ItemStack stack); Optional<ItemStack> sample(String id); }` (`id` sin el prefijo de tipo; `stack` nunca null ni aire).
- Produces: `MaterialRegistry` con `void register(MaterialProvider)`, `boolean knows(String fullId)`, `boolean matches(String fullId, ItemStack)`, `Optional<ItemStack> sample(String fullId)`, `String normalize(String raw)` (`oak_planks` → `minecraft:oak_planks`; `tipo:id` en minúsculas), `int count(Inventory inv, MaterialSpec spec)` (suma de pilas que casan con cualquiera de `anyOf`).
- `VanillaProvider` (`minecraft`): `knows` = `Material.matchMaterial` es un ítem; `matches` = `stack.getType() == m && stack.asOne().isSimilar(ItemStack.of(m))` (sin datos propios: una espada con nombre o un ítem de RPGRoll del mismo material no cuenta).
- `ThingsProvider` (un proveedor por cada `Things.Give` que no sea `minecraft`, `type()` = su tipo): `knows` = `ids().contains`; `matches` = `ItemIdentity.of(stack)` no vacía y igual a `ItemIdentity.of(sample)`; `sample` = `create(id)`.
- `ItemIdentity.of(ItemStack)` → `Optional<String>`: primero el valor de la primera clave PDC presente entre `rpgroll-items:item-id`, `rpgroll:item_id`, `rpgroll-furniture:id` (prefijado con su clave: `"rpgroll-items:item-id=espada"`), si no `"model=" + item_model` si lo tiene, si no vacío. Un ítem sin identidad no casa con ningún material de tipo no vanilla.
- Los proveedores de `Things` se buscan cada vez (`Things.sources(Give.class)`), no al arrancar: los módulos pueden habilitarse después.

- [ ] **Step 1: Write the failing tests** (Mockito para `ItemStack`/`ItemMeta`/PDC como `WorkbenchItemsTest`): `plainVanillaMatchesRenamedDoesNot`, `rpgrollItemOfSameMaterialIsNotVanilla`, `thingsMatchesByPdcIdNotByName`, `modelIsFallbackIdentity`, `normalizeAddsMinecraft`, `countSumsAnyOf`.
- [ ] **Step 2: Run** `./gradlew :structures:test --tests "*material*"` — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 6: Proyectos en memoria y cuentas de depósito

**Files:**
- Create: `structures/.../project/Project.java`, `ProjectState.java` (si Task 4 no lo creó ya), `DepositMath.java`, `ProjectService.java`
- Modify: `workbench/ProjectsView.java` (sin cambios de firma), `StructuresPlugin.java` (crea `ProjectService` antes que `WorkbenchService` y le pasa `projects::hasActive` en vez de `ProjectsView.none()`), `workbench/WorkbenchState.java` (comentario: el estado de la mesa sale del proyecto)
- Test: `structures/src/test/java/.../project/DepositMathTest.java`, `ProjectServiceTest.java`

**Interfaces:**
- Consumes: `ProjectRepository`, `Movement` (Task 4); `StructureRegistry`, `MaterialSpec` (Task 3).
- Produces: `final class Project` (mutable, solo hilo principal): `id()`, `bench()`, `structure()`, `owner()`, `state()`, `origin()` (`Location` sin mundo cargado = null), `rotation()`, `cursor()`, `placed()`, `skipped()`, `total()`, `error()`, `Map<String, MaterialRow> materials()`, `boolean busy()` / `setBusy(boolean)` (bloqueo de depósito por proyecto).
- Produces: `DepositMath` (puro):
  - `static Map<String,Integer> toTake(List<MaterialSpec> specs, Map<String,MaterialRow> rows, Map<String,Integer> available, String onlyKey)` — cuánto quitar de cada material (lo que falta, como mucho lo que hay; `onlyKey` null = todos).
  - `static boolean complete(List<MaterialSpec> specs, Map<String,MaterialRow> rows)` — todos los no opcionales con `deposited >= required`.
  - `static Map<String,Integer> consumedAt(Map<String,MaterialRow> rows, int placed, int total)` — `floor(required * placed / total)` por material, nunca más que `deposited`.
  - `static Map<String,Integer> refund(Map<String,MaterialRow> rows, int percent)` — `floor((deposited - consumed) * percent / 100)`.
- Produces: `ProjectService` con `void load()` (lee `active()` y sus materiales), `boolean hasActive(UUID bench)`, `Optional<Project> forBench(UUID bench)`, `Optional<Project> byId(UUID)`, `List<Project> ofPlayer(UUID)`, `CompletableFuture<Project> create(UUID bench, StructureDefinition def, UUID owner, Location origin, Rotation r)` (estado `WAITING_MATERIALS`, `total` = `def.volume()`), y el estado de la mesa: `WorkbenchState benchState(UUID bench)` (sin proyecto → `IDLE`; `WAITING_MATERIALS`→`WAITING_MATERIALS`, `READY`→`READY`, `BUILDING`→`BUILDING`, `PAUSED`→`PAUSED`, `ERROR`→`ERROR`; recién completado → `COMPLETED` hasta abrir el menú).

- [ ] **Step 1: Write the failing tests**: `toTakeNeverExceedsMissingOrAvailable`, `optionalDoesNotBlockComplete`, `consumedIsProportionalAndCapped` (`required 100, deposited 100, placed 1/3` → 33), `refundIsUnspentTimesPercent`, `oneActiveProjectPerBench` (crear otro en la misma mesa falla con `IllegalStateException`), `benchStateFollowsProject`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 7: Servicio de depósitos

**Files:**
- Create: `structures/.../deposit/DepositService.java`
- Test: `structures/src/test/java/.../deposit/DepositServiceTest.java`

**Interfaces:**
- Consumes: `Project`, `ProjectService`, `DepositMath` (Task 6); `ProjectRepository.applyMovement` (Task 4); `MaterialRegistry` (Task 5).
- Produces: `enum DepositResult { OK, NOTHING_TO_TAKE, BUSY, WRONG_STATE, ERROR }`; `CompletableFuture<DepositResult> deposit(Player p, Project project, String onlyKey, int maxPerKey)` (`onlyKey` null = todos; `maxPerKey` 1 para «uno», `Integer.MAX_VALUE` para «todo»).
- Orden (hilo principal, un solo paso): si `project.busy()` o el jugador ya tiene un depósito en curso → `BUSY` (se descarta, no se encola); si el estado no es `WAITING_MATERIALS` → `WRONG_STATE`; calcula `toTake`, quita del inventario las pilas que casan (por `MaterialRegistry.matches`), suma en memoria, marca `busy`; luego un `Movement` por material con id nuevo → `applyMovement` en el hilo de BD; al volver al hilo principal: si todo `true`, `p.saveData()` y, si `DepositMath.complete`, estado `READY` y `saveProgress`; si alguno falla, resta en memoria lo no escrito y devuelve esos ítems (al inventario o a pendientes, Task 8) y `ERROR`. Siempre quita `busy` al final. Lanza `StructureMaterialsDepositEvent` en el plan 4; aquí no hay eventos.

- [ ] **Step 1: Write the failing tests** (repositorio falso con futuros controlables; inventario de Mockito): `depositTakesOnlyWhatIsMissing`, `secondDepositWhileBusyIsDropped`, `failedWriteGivesItemsBack` (el `applyMovement` falso falla → inventario igual que antes, `deposited` igual), `reachingHundredPercentMakesReady`, `wrongStateIsRejected`, `renamedItemIsNotTaken`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 8: Recuperar, cancelar y pendientes de recoger

**Files:**
- Create: `structures/.../deposit/ReturnService.java`
- Modify: `DepositService.java` (usa `ReturnService.give` para devolver ítems)
- Test: `structures/src/test/java/.../deposit/ReturnServiceTest.java`

**Interfaces:**
- Produces: `void give(Player p, List<ItemStack> items)` — lo que no cabe (`addItem` devuelve sobrante) va a `addPending` uno por pila; nada al suelo.
- Produces: `CompletableFuture<Boolean> withdraw(Player p, Project project)` — solo en `WAITING_MATERIALS` o `READY` (vuelve a `WAITING_MATERIALS`): un `WITHDRAW` por material con todo lo depositado; tras escribir, crea los ítems con `MaterialRegistry.sample` (cantidad repartida en pilas de `getMaxStackSize`) y `give`.
- Produces: `CompletableFuture<Boolean> cancel(Player p, Project project, int refundPercent)` — cualquier estado salvo `COMPLETED`/`CANCELLED`; `REFUND` de `DepositMath.refund`, estado `CANCELLED`, `give`.
- Produces: `CompletableFuture<Integer> claimAll(Player p)` — por cada pendiente: `claim(id)` en BD y, si devuelve true, `give` del ítem deserializado (`ItemStack.deserializeBytes`) y `p.saveData()`; devuelve cuántos.
- Un material de tipo no vanilla cuyo proveedor ya no existe al devolver: el movimiento se apunta igual y la cantidad queda en el libro con motivo; se avisa al jugador con `deposit.provider-missing` y al log (no se inventa un ítem).

- [ ] **Step 1: Write the failing tests**: `refundOverflowGoesToPending`, `withdrawResetsToWaiting`, `cancelRefundsUnspentPercent` (depositado 100, consumido 40, 50 % → 30), `claimTwiceGivesOnce`, `missingProviderIsLoggedNotInvented`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 9: Validación del terreno y vista previa

**Files:**
- Create: `structures/.../preview/Footprint.java`, `TerrainCheck.java`, `BuildGuard.java`, `PreviewSession.java`, `PreviewService.java`
- Modify: `StructuresPlugin.java` (crea `PreviewService`, tarea repetida de partículas cada 10 ticks, limpia sesiones en `PlayerQuitEvent` y al deshabilitar)
- Test: `structures/src/test/java/.../preview/FootprintTest.java`, `TerrainCheckTest.java`

**Interfaces:**
- Consumes: `StructureDefinition`, `PlacementRules` (Task 3); `Rotation`, `BlockSources.rotate` (Task 1); `ProjectRepository.boxesIn` (Task 4); `PerformanceSettings` (Task 2).
- Produces: `record Footprint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)` con `static Footprint of(int ox, int oy, int oz, int w, int h, int l)`, `boolean intersects(Footprint)`, `List<int[]> edges()` (posiciones de las 12 aristas).
- Produces: `interface BuildGuard { boolean canBuild(Player p, Location at); static BuildGuard allowAll(); }` — el plan 3 pone WorldGuard/GriefPrevention.
- Produces: `TerrainCheck.check(...)` puro sobre una función `BlockLookup(int x,int y,int z) → (Material type, boolean replaceable, boolean liquid, String biome)`: devuelve `record CheckResult(List<int[]> collisions, String reason)` con `reason` = clave de lang del primer fallo (`preview.out-of-world`, `preview.world-not-allowed`, `preview.biome-not-allowed`, `preview.overlaps-project`, `preview.no-ground`, `preview.blocked`, `preview.protected`) o null si se puede. Ocupado = ni aire ni `replaceable` ni en `replace`; líquido cuenta como ocupado salvo `allowLiquids`; `needsGround` = todos los bloques bajo la capa y=0 de la huella que tienen bloque en la estructura están sobre algo sólido.
- Produces: `PreviewService` con `void start(Player p, UUID bench, StructureDefinition def)` (origen inicial: 2 bloques delante de la mesa, en la dirección en que mira el jugador, al nivel de la mesa), `rotate(Player)`, `move(Player, int dx, int dy, int dz)`, `toggleMode(Player)`, `Optional<PreviewSession> session(Player)`, `void stop(Player)` (repone con `sendBlockChange` el bloque real de cada posición enviada), `CheckResult check(Player)`.
- Envío de bloques falsos: `p.sendMultiBlockChange(Map<Location,BlockData>)` con como mucho `previewMaxBlocks` (los más bajos primero); antes de enviar una posición nueva se reponen las anteriores que ya no están. Modo `OUTLINE` o estructura más grande que el tope: solo partículas. Se corta solo si el jugador se aleja más de `previewMaxDistance` del origen o pasan `previewSeconds` (mensaje `preview.expired`).
- Partículas: `Particle.DUST` blanco en aristas (cada 2 bloques), verde en el origen, rojo en cada choque (como mucho 64).

- [ ] **Step 1: Write the failing tests**: `footprintRotatedSwapsWidthAndLength`, `intersectsIsInclusive`, `occupiedBlockIsCollision`, `replaceListIsNotCollision`, `liquidNeedsAllowLiquids`, `outOfWorldHeightFails` (minY −64, maxY 319), `noGroundFailsWhenFloating`, `overlapWithOtherProjectFails`.
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 10: Órdenes de colocación y planificador

**Files:**
- Create: `structures/.../build/PlacementPlan.java`, `BuildOrders.java`, `BuildBudget.java`
- Test: `structures/src/test/java/.../build/BuildOrdersTest.java`, `BuildBudgetTest.java`

**Interfaces:**
- Consumes: `SourceBlock`, `Rotation` (Task 1); `BuildOrder` (Task 3).
- Produces: `BuildOrders.order(List<SourceBlock> rotated, BuildOrder order, List<Material> custom)` → `List<SourceBlock>` determinista (mismo resultado cada vez: el cursor es un índice en esta lista):
  - `BOTTOM_UP`: y, luego z, luego x.
  - `LAYERS`: y; dentro de cada capa, filas en zigzag (z par: x ascendente; z impar: x descendente).
  - `CENTER_OUT`: y, luego distancia al centro de la capa (cuadrado de la distancia, empates por z, x).
  - `STAGES`: tres pasadas completas en `BOTTOM_UP` — bloques sólidos de cubo entero (`data.isOccluding()`), luego los demás no adosables, luego adosables (`Tag`/`BlockData` de antorchas, botones, palancas, alfombras, raíles, polvo de redstone, carteles, puertas, camas).
  - `CUSTOM`: primero los materiales de `custom` en ese orden (cada grupo en `BOTTOM_UP`), luego el resto en `BOTTOM_UP`.
  - En todos: dentro de cada capa (y), los bloques con `tile != null` van al final de su capa (en `STAGES`, al final de su pasada).
- Produces: `PlacementPlan(List<SourceBlock> ordered)` con `int size()`, `SourceBlock at(int index)`.
- Produces: `BuildBudget` (puro): `BuildBudget(int perTick)`; `Map<UUID,Integer> share(List<UUID> ready, Map<UUID,Integer> wanted)` reparte `perTick` entre las obras que quieren colocar este tick (por turnos empezando por la siguiente a la última servida, para que una obra grande no deje sin turno a las demás); nunca da más de lo que cada una pide ni más de `perTick` en total.

- [ ] **Step 1: Write the failing tests**: `bottomUpIsYThenZThenX`, `tilesGoLastInTheirLayer`, `layersZigzag`, `stagesPutsTorchesLast`, `orderIsDeterministic` (dos llamadas → listas iguales), `budgetNeverExceedsPerTick`, `budgetRotatesBetweenProjects` (perTick 10, tres obras que piden 100: en 3 ticks cada una recibe ≥ 10).
- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 11: Motor de construcción

**Files:**
- Create: `structures/.../build/ConstructionEngine.java`, `BlockWriter.java`
- Modify: `StructuresPlugin.java` (crea el motor, tarea de 1 tick, `onDisable` guarda el cursor de todas las obras en curso de forma síncrona con `Database.submit(...).get(5 s)`), `ProjectService` (al cargar, las obras `BUILDING` siguen en `BUILDING`)
- Test: `structures/src/test/java/.../build/ConstructionEngineTest.java`

**Interfaces:**
- Consumes: `PlacementPlan`, `BuildOrders`, `BuildBudget` (Task 10); `ProjectService`, `DepositMath.consumedAt` (Task 6); `ProjectRepository.saveProgress/applyMovement/complete` (Task 4); `PerformanceSettings` (Task 2).
- Produces: `interface BlockWriter { boolean chunkLoaded(String world, int x, int z); BlockInfo read(String world, int x, int y, int z); void write(String world, int x, int y, int z, SourceBlock b); }` con `record BlockInfo(Material type, boolean replaceable, BlockData data)`; la real usa `Block#setBlockData(data, false)` y, si `tile != null`, `tile.copy(location).update(true, false)` (Paper).
- Produces: `ConstructionEngine` con `StartResult start(Player p, Project project)` (`enum StartResult { STARTED, NOT_READY, LIMIT_SERVER, LIMIT_WORLD, LIMIT_PLAYER, NO_PERMISSION }`; pide estado `READY`, permiso `rpgrollstructures.build` y el de la estructura, y los límites de `PerformanceSettings`), `void pause(Project)`, `void resume(Project)`, `void tick()` y `void flushAll()`.
- Cada `tick()`: obras `BUILDING` cuyo ciclo toca (`ticksPerCycle`; instantáneas, cada tick) piden `blocksPerCycle` (instantáneas: `Integer.MAX_VALUE`); `BuildBudget.share`; por cada índice desde `cursor`: chunk no cargado → la obra espera (no cambia el cursor ni el estado); bloque actual con el mismo `BlockData` → cuenta como colocado sin escribir; ocupado (ni aire, ni reemplazable, ni en `replace`) → `skipped++` sin escribir; si no, `write`. Avanza `cursor` y `placed`. Tras el tick, para cada obra que avanzó: `CONSUME` de la diferencia entre `consumedAt(nuevo)` y lo ya consumido (con id derivado de `project + cursor`, para que repetirlo tras una caída no cobre dos veces), y `saveProgress` como mucho una vez por segundo por obra (el último siempre al pausar, al terminar y en `flushAll`).
- Al llegar al final: `complete(id, BuildRecord)` con la caja rotada, estado `COMPLETED`, mensaje `build.completed` al dueño si está conectado, partículas `HAPPY_VILLAGER` y sonido `BLOCK_ANVIL_USE` en la mesa. Un error al escribir (excepción) → estado `ERROR` con `error` = mensaje corto, log de admin.

- [ ] **Step 1: Write the failing tests** (`BlockWriter` falso en memoria):

```java
@Test void instantBuildsWithinBudget() { /* 300 bloques, perTick 256 → tras 1 tick 256 colocados, tras 2 COMPLETED */ }
@Test void progressiveRespectsCycle() { /* 16 cada 20 ticks: tras 20 ticks 16, tras 40 ticks 32 */ }
@Test void occupiedBlockIsSkippedNotReplaced() { /* una piedra puesta en mitad: no se escribe, skipped 1 */ }
@Test void unloadedChunkWaitsWithoutAdvancing() { }
@Test void resumeFromSavedCursorDoesNotRecharge() {
    // cursor guardado 100 de 200, se crea otro motor con la misma obra cargada de la BD: los CONSUME tienen el mismo id → applyMovement devuelve false para los repetidos; consumed final = required
}
@Test void limitsArePerServerWorldAndPlayer() { }
@Test void sameBlockDataIsNotRewritten() { }
```

- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 12: Menús (catálogo, ficha, materiales, vista previa, obra, mis construcciones) y comandos

**Files:**
- Create: `structures/.../gui/CategoriesMenu.java`, `ListMenu.java`, `DetailMenu.java`, `MaterialsMenu.java`, `PreviewMenu.java`, `ProjectMenu.java`, `BuildsMenu.java`
- Modify: `gui/CenterMenu.java` (CATALOGO, MIS_CONSTRUCCIONES, MATERIALES y OBRA abren sus menús; quita «Próximamente» de los que ya funcionan), `gui/StructuresMenus.java` (`Layout` nuevos: `categorias` 6 filas, `lista` 6 filas, `ficha` 6 filas, `materiales` 6 filas, `vista` 3 filas, `obra` 4 filas, `construcciones` 6 filas; añadirlos a `LAYOUTS`), `gui.yml`, `lang/*.yml`, `command/StructuresCommand.java` (`ayuda`, `abrir` = catálogo sin mesa (solo ficha, sin Elegir), `lista`, `info <id>`, `estado`, `cancelar` = abre el Confirmar de la obra activa del jugador), `command/StructuresAdminCommand.java` (`validate`: lista los `Problem` con archivo, clave y motivo; `reload` recarga también el catálogo)
- Test: `structures/src/test/java/.../gui/MenuLayoutTableTest.java` (cubre las tablas nuevas: casillas únicas y dentro del tamaño), `LangFilesTest` (ya existe: claves iguales en los tres idiomas)

**Interfaces:**
- Consumes: todo lo anterior.
- Flujo: Centro → Categorías (una casilla por categoría con estructuras; sin permiso `.category.<cat>` se ve bloqueada con motivo) → Lista paginada (28 por página; las de nivel mayor que la mesa, bloqueadas con `catalog.locked-tier`) → Ficha (dimensiones, nivel, materiales con cantidades, dinero, tiempo estimado = `ceil(volume / blocksPerCycle) * ticksPerCycle / 20` s, permiso; botones «Vista previa» y «Elegir»; ambos abren la vista previa) → Vista previa (girar, ±1 en x/y/z, modo, confirmar = `TerrainCheck` y `ProjectService.create`, cancelar) → Materiales (por material: verde completo, amarillo parcial, rojo nada, «128/256»; clic izquierdo uno, clic derecho todo de ese tipo, botón «Depositar todo», «Recuperar», «Construir» si `READY`, «Recoger pendientes» si hay) → Obra (%, bloques puestos/total, saltados, tiempo restante, estado, pausar/reanudar, cancelar → `ConfirmMenu` con el % de devolución).
- Mientras la vista previa está activa, clic derecho con la mano vacía reabre el menú de vista previa (listener en `PreviewService`).
- Mis construcciones: obras activas y terminadas del jugador (estructura, mundo, coordenadas, fecha); reparar y mejorar llegan en el plan 3 (botón con `menu.coming_soon`).
- Iconos: el `icon` de la estructura/categoría por `MaterialRegistry.sample`, si no `Material.PAPER`.

- [ ] **Step 1: Write the failing tests**: tablas de casillas de los 7 `Layout` nuevos en `MenuLayoutTableTest`; claves nuevas en los tres idiomas (`LangFilesTest` falla hasta que estén).
- [ ] **Step 2: Run** `./gradlew :structures:test` — Expected: FAIL.
- [ ] **Step 3: Implement** los menús siguiendo `CenterMenu`/`UpgradeMenu` (relleno, `dress`, títulos con `menuTitle`, fondo `<menu>_<familia>`).
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Registro.**

### Task 13: Cableado, versiones, ejemplo de prueba y guía

**Files:**
- Modify: `StructuresPlugin.java` (orden de arranque: ajustes → BD → `MaterialRegistry` → catálogo → `ProjectService.load` → `WorkbenchService` con `projects::hasActive` → `DepositService`/`ReturnService` → `PreviewService` → `ConstructionEngine`; `reloadAll` recarga categorías, rendimiento y catálogo; `onDisable` → `previews.stopAll()`, `engine.flushAll()`, luego `database.close()`), `structures/build.gradle.kts` (`version = "1.1.0"`), `plugin.yml` (`version: 1.1.0`), `structures/src/main/resources/structures/README.txt` (formato del YAML, copiado a la carpeta si falta)
- Create: `structures/src/test/resources/structures/caseta.yml` + prueba `CatalogSmokeTest` (carga el YAML de ejemplo con un `reader` falso y comprueba que es válido)
- Modify: `structures/QA-fase1-2.md` → añadir sección «Fases 3-5: qué probar en el banco» (depositar con inventario lleno, desconexión al depositar, reinicio a mitad de obra, cancelar, chunk descargado, dos obras a la vez, `.schem` sin WorldEdit)

- [ ] **Step 1: Write the failing test** `CatalogSmokeTest.exampleIsValid`.
- [ ] **Step 2: Run** — Expected: FAIL (no existe el ejemplo).
- [ ] **Step 3: Implement** el cableado y los archivos.
- [ ] **Step 4: Run** `./gradlew :common:test :structures:test` — Expected: PASS en los dos.
- [ ] **Step 5: Registro** y parar: compilar el jar y probar en el banco necesita permiso de Sack.
