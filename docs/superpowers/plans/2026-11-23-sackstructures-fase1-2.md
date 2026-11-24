# RPGRoll-Structures — Plan 1 (fases 1 y 2: base, almacenamiento, mesa y menú principal)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un módulo `structures` (plugin RPGRoll-Structures) que arranca, guarda en SQLite/MySQL, y tiene la
mesa de construcción completa: ítem firmado, bloque con modelo o bloque vanilla, tres niveles con mejora
en el sitio, acceso por confianza y su menú principal con mejoras, acceso y ayuda.

**Architecture:** Módulo del monorepo `E:\Dev\RPGRoll` con `rpgroll.addon-conventions`, dependiente de
RPGRoll-Lib (`:common`). Lógica pura (reglas, firmas, repositorio) separada de los listeners y GUIs de
Paper para poder probarla sin servidor. Base de datos en un único hilo propio.

**Tech Stack:** Java 25, Paper API (la versión de `build-logic`), Gradle, JUnit 5 + Mockito, SQLite
(`sqlite-jdbc` que trae Paper), MariaDB/MySQL opcional por `libraries:`.

**Spec:** `E:\Dev\RPGRoll\docs\superpowers\specs\2026-11-23-sackstructures-design.md` (secciones 3, 5, 6,
7, 12, 13, 14 y 19). Bocetos de las mesas: `docs/superpowers/specs/sackstructures/bocetos-mesas.jpg`.

Planes siguientes (uno por fase o pareja de fases, mismo formato): 3 catálogo/lector/materiales/depósitos,
4 vista previa, 5 construcción, 6 recuperación/protección, 7 integraciones/módulos, 8 arte y ejemplos,
9 API/docs/QA.

## Global Constraints

- Carpeta `structures`, paquete `com.sack.rpgroll.structures`, `rootProject` `include("structures")`.
- Plugin `name: RPGRoll-Structures`, `base.archivesName` `RPGRoll-Structures`, `version` `1.0.0`, `depend: [RPGRoll-Lib]`, `softdepend: [SackResourcePack, WorldEdit, FastAsyncWorldEdit, WorldGuard, GriefPrevention, Vault, floodgate]`.
- Paper API: la de `build-logic/src/main/kotlin/rpgroll.plugin-conventions.gradle.kts` (hoy `26.1.1.build.29-alpha`); `api-version: "26.1.1"` como Emotes. No subir la versión solo en este módulo.
- No depende de `:core`. Licencia como los demás: `licensing.properties` con `structures=0`, `LicenseGate.verify(...)` al principio de `onEnable`.
- Permisos con prefijo `rpgrollstructures.`: `use`, `workbench.place`, `workbench.break`, `tier.<n>`, `bypass`, `admin`, `admin.give`, `admin.reload` (los de construir y categorías llegan en planes siguientes).
- Idiomas `lang/es.yml`, `lang/en.yml`, `lang/pt_BR.yml`; fallback `es`. Comando de jugador con `command-words.yml` (`estructuras`/`structures`/`estruturas`); admin `/structuresadmin` solo en inglés.
- Niveles: 1 Constructor, 2 Ingeniero, 3 Arquitecto. Tamaño máximo de fábrica 16×16×16, 32×32×32, 64×96×64 (ancho×alto×largo), configurable en `workbench.yml`.
- Siete estados de la mesa: `IDLE`, `WAITING_MATERIALS`, `READY`, `BUILDING`, `PAUSED`, `COMPLETED`, `ERROR`.
- Política al romper la mesa con obra activa: `block` (fábrica), `pause`, `cancel`.
- Sin ItemsAdder, Oraxen ni Nexo.
- Sack: sin commits ni push salvo que lo pida (los pasos «Commit» de abajo se saltan y se anota en el registro); preguntar antes de cualquier `gradlew jar`/`shadowJar`/`build` (`gradlew test` sí); producción solo con su permiso y el servidor vacío o con DarkSack solo.

## Review Focus

1. Romper la mesa por explosión, pistón, creativo o `/fill`: el ítem no se pierde ni se duplica y la fila de la base de datos queda coherente.
2. Dos clics seguidos (o dos jugadores) en «Mejorar»: se cobra y se sube una sola vez.
3. Mesa en la base de datos cuyo mundo ya no existe o cuyo bloque ya no es la mesa: arranque sin errores y la fila se marca huérfana, no se borra a ciegas.
4. Colocar la mesa donde GriefPrevention/WorldGuard lo prohíben: no queda registrada (el listener ignora eventos cancelados).
5. Ítem de mesa falsificado (PDC copiado con otro nivel o serie inventada): no se puede colocar.

---

### Task 1: Módulo, plugin.yml, configuración y ajustes

**Files:**
- Modify: `settings.gradle.kts` (añadir `"structures"` tras `"emotes"`), `licensing.properties` (`structures=0`)
- Create: `structures/build.gradle.kts`, `structures/src/main/resources/plugin.yml`, `config.yml`, `workbench.yml`, `storage.yml`, `lang/es.yml`, `lang/en.yml`, `lang/pt_BR.yml`, `license/license.yml` (copia de la de Emotes con el nombre cambiado)
- Create: `structures/src/main/java/com/sack/rpgroll/structures/StructuresPlugin.java`, `structures/.../config/StructuresSettings.java`, `structures/.../config/WorkbenchSettings.java`
- Test: `structures/src/test/java/com/sack/rpgroll/structures/config/StructuresSettingsTest.java`, `WorkbenchSettingsTest.java`

**Interfaces:**
- Produces: `record StructuresSettings(String language, String menusPack, BreakPolicy breakPolicy)` con `static StructuresSettings from(ConfigurationSection root, Consumer<String> warn)`; `enum BreakPolicy { BLOCK, PAUSE, CANCEL }`.
- Produces: `record WorkbenchSettings(Map<WorkbenchTier, TierSettings> tiers, List<String> recipeShape, Map<Character, String> recipeKeys)` con `static WorkbenchSettings from(ConfigurationSection root, Consumer<String> warn)`; `record TierSettings(Size maxSize, List<Cost> upgradeCost, double upgradeMoney, String model, Material fallbackBlock)`; `record Size(int width, int height, int length)` con `boolean fits(Size other)`; `record Cost(String itemId, int amount)` (`itemId` con id de la Lib, p. ej. `minecraft:iron_block`).
- Produces: `enum WorkbenchTier { CONSTRUCTOR(1), INGENIERO(2), ARQUITECTO(3) }` con `int level()`, `Optional<WorkbenchTier> next()`, `static Optional<WorkbenchTier> ofLevel(int)` en `structures/.../workbench/WorkbenchTier.java` (se crea aquí porque los ajustes lo usan).

- [ ] **Step 1: Write the failing tests**

```java
@Test void defaultsMatchTheSpec() {
    var s = WorkbenchSettings.from(yaml(""), w -> fail(w));
    assertEquals(new Size(16, 16, 16), s.tiers().get(WorkbenchTier.CONSTRUCTOR).maxSize());
    assertEquals(new Size(32, 32, 32), s.tiers().get(WorkbenchTier.INGENIERO).maxSize());
    assertEquals(new Size(64, 96, 64), s.tiers().get(WorkbenchTier.ARQUITECTO).maxSize());
    assertEquals(Material.CRAFTING_TABLE, s.tiers().get(WorkbenchTier.CONSTRUCTOR).fallbackBlock());
    assertEquals(Material.SMITHING_TABLE, s.tiers().get(WorkbenchTier.INGENIERO).fallbackBlock());
    assertEquals(Material.BEACON, s.tiers().get(WorkbenchTier.ARQUITECTO).fallbackBlock());
}
@Test void badSizeFallsBackAndWarns() { /* width: -3 → warn con la clave "tiers.constructor.max-size" y queda 16 */ }
@Test void sizeFitsIsPerAxis() { assertTrue(new Size(16,16,16).fits(new Size(16,5,16))); assertFalse(new Size(16,16,16).fits(new Size(17,1,1))); }
@Test void breakPolicyDefaultsToBlockAndRejectsUnknown() { /* "", "nope" → BLOCK con aviso en el segundo */ }
@Test void tierLevelsAndNext() { assertEquals(Optional.of(WorkbenchTier.INGENIERO), WorkbenchTier.CONSTRUCTOR.next()); assertEquals(Optional.empty(), WorkbenchTier.ARQUITECTO.next()); assertEquals(Optional.empty(), WorkbenchTier.ofLevel(4)); }
```
`yaml(String)` = helper de test con `YamlConfiguration.loadFromString`.

- [ ] **Step 2: Run** `./gradlew :structures:test` — Expected: FAIL (clases inexistentes).
- [ ] **Step 3: Implement** `build.gradle.kts` (como el de Emotes, sin BetterModel; `compileOnly(project(":sackresourcepack"))`), `plugin.yml` (comandos no: se registran por Brigadier; permisos de Global Constraints con `default: true` para `use`, `workbench.place`, `workbench.break`, `tier.*`; `op` para el resto), los YAML de fábrica con los valores de arriba (receta de fábrica: `["PPP","CTC","L L"]` con P=`minecraft:oak_planks`, C=`minecraft:copper_ingot`, T=`minecraft:crafting_table`, L=`minecraft:oak_log`; mejora a 2: 16 `minecraft:iron_ingot`, 8 `minecraft:redstone`, 4 `minecraft:redstone_lamp`; mejora a 3: 8 `minecraft:diamond`, 16 `minecraft:prismarine_crystals`, 1 `minecraft:nether_star`; dinero 0), los ajustes y un `StructuresPlugin` cuyo `onEnable` hace licencia → `saveDefaultConfig` + `saveResource` de los otros YAML si faltan → `LangManager(this, List.of("es","en","pt_BR"), "es")` → `MenuSkin.register("structures", ...)`.
- [ ] **Step 4: Run** `./gradlew :structures:test` — Expected: PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): módulo RPGRoll-Structures y ajustes`

### Task 2: Base de datos y migraciones

**Files:**
- Create: `structures/.../storage/Database.java`, `storage/Dialect.java`, `storage/Migrations.java`, `storage/WorkbenchRepository.java`, `storage/WorkbenchRecord.java`
- Modify: `StructuresPlugin.java` (abrir al arrancar, cerrar en `onDisable`), `plugin.yml` (`libraries: [org.mariadb.jdbc:mariadb-java-client:<última 3.x estable de Maven Central, comprobada>]`)
- Test: `structures/src/test/java/.../storage/WorkbenchRepositoryTest.java`, `MigrationsTest.java` (con `testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")` como WorldForge)

**Interfaces:**
- Produces: `final class Database implements AutoCloseable` con `static Database open(Dialect dialect, String url, String user, String password, Logger log)`, `<T> CompletableFuture<T> submit(SqlWork<T> work)` (un único hilo «RPGRoll-Structures-DB»; nunca en el hilo principal), `void close()` (espera las tareas pendientes, máximo 10 s).
- Produces: `enum Dialect { SQLITE, MYSQL }`; `storage.yml`: `type: sqlite|mysql`, `sqlite.file: data.db`, `mysql.{host,port,database,user,password}`.
- Produces: `record WorkbenchRecord(UUID id, UUID owner, String world, int x, int y, int z, WorkbenchTier tier, long serial, boolean orphan, long createdAt)`.
- Produces: `WorkbenchRepository`: `CompletableFuture<Void> insert(WorkbenchRecord)`, `CompletableFuture<Optional<WorkbenchRecord>> find(UUID)`, `findAt(String world, int x, int y, int z)`, `CompletableFuture<Boolean> upgrade(UUID id, WorkbenchTier from, WorkbenchTier to)` (UPDATE condicionado a `tier = from`: true solo si cambió una fila), `move(UUID id, String world, int x, int y, int z)`, `remove(UUID id)`, `markOrphan(UUID id)`, `CompletableFuture<List<WorkbenchRecord>> all()`, `trust(UUID id, UUID player)`, `untrust(UUID id, UUID player)`, `CompletableFuture<Set<UUID>> trusted(UUID id)`.
- Tablas (versión 1): `schema_version(version)`, `workbenches(id PK, owner, world, x, y, z, tier, serial UNIQUE, orphan, created_at)`, índice único `(world,x,y,z)`, `workbench_access(workbench_id, player, PK(workbench_id, player))`. Antes de migrar un SQLite existente se copia a `backups/data-<versión>-<fecha>.db`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void insertFindAndFindAt() { /* inserta, find(id) y findAt(world,x,y,z) devuelven el mismo record */ }
@Test void upgradeOnlyOnceFromTheExpectedTier() {
    repo.insert(rec(CONSTRUCTOR)).join();
    assertTrue(repo.upgrade(id, CONSTRUCTOR, INGENIERO).join());
    assertFalse(repo.upgrade(id, CONSTRUCTOR, INGENIERO).join()); // el segundo clic no sube de nuevo
}
@Test void twoBenchesCannotShareALocation() { /* segundo insert en la misma posición → future completa excepcionalmente */ }
@Test void trustAndUntrust() { /* trusted() refleja altas y bajas; trust duplicado no falla */ }
@Test void migrationsAreIdempotent() { /* Migrations.run dos veces deja version 1 y no falla */ }
@Test void sqliteIsBackedUpBeforeMigratingAnOldSchema() { /* version 0 en disco → existe backups/data-0-*.db */ }
@Test void workRunsOffTheCallerThread() { assertNotEquals(Thread.currentThread().getName(), db.submit(c -> Thread.currentThread().getName()).join()); }
```

- [ ] **Step 2: Run** `./gradlew :structures:test --tests '*storage*'` — Expected: FAIL.
- [ ] **Step 3: Implement** con JDBC plano y sentencias preparadas; SQLite con `PRAGMA journal_mode=WAL` y `foreign_keys=ON`.
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): base de datos y repositorio de mesas`

### Task 3: Identidad de la mesa (ítem firmado)

**Files:**
- Create: `structures/.../workbench/SerialSigner.java`, `workbench/WorkbenchItems.java`, `workbench/ServerSecret.java`
- Test: `.../workbench/SerialSignerTest.java`, `WorkbenchItemsTest.java`

**Interfaces:**
- Produces: `final class SerialSigner` con `SerialSigner(byte[] key)`, `String sign(UUID id, WorkbenchTier tier, long serial)` (HMAC-SHA256 en Base64 URL sin relleno de `id|tier.level()|serial`), `boolean verify(UUID id, WorkbenchTier tier, long serial, String signature)` (comparación en tiempo constante).
- Produces: `ServerSecret.load(Path file)`: lee o crea `plugins/RPGRoll-Structures/secret.key` (32 bytes aleatorios de `SecureRandom`, permisos de solo dueño donde el SO lo permita).
- Produces: `record WorkbenchStamp(UUID id, WorkbenchTier tier, long serial, String signature)`; `WorkbenchItems`: `ItemStack create(WorkbenchStamp stamp, LangManager lang, TierSettings tier)` (nombre y lore por nivel, `item_model` del nivel si hay pack; claves PDC `rpgroll-structures:workbench_id`, `:tier`, `:serial`, `:sig`), `Optional<WorkbenchStamp> read(ItemStack)` (vacío si falta una clave), `boolean isGenuine(WorkbenchStamp, SerialSigner)`.
- Una mesa recién fabricada recibe `id = UUID.randomUUID()` y `serial` = siguiente número de una secuencia en la base de datos (añadir tabla `serials(next)` en la migración 1 si se hace aquí, o `SELECT max(serial)+1` dentro del hilo de la base de datos).

- [ ] **Step 1: Write the failing tests**

```java
@Test void signatureRoundTrips() { assertTrue(signer.verify(id, CONSTRUCTOR, 7, signer.sign(id, CONSTRUCTOR, 7))); }
@Test void changingTheTierBreaksTheSignature() { assertFalse(signer.verify(id, ARQUITECTO, 7, signer.sign(id, CONSTRUCTOR, 7))); }
@Test void anotherServerKeyRejects() { assertFalse(new SerialSigner(otherKey).verify(id, CONSTRUCTOR, 7, signer.sign(id, CONSTRUCTOR, 7))); }
@Test void secretIsCreatedOnceAndReused(@TempDir Path d) { assertArrayEquals(ServerSecret.load(d.resolve("secret.key")), ServerSecret.load(d.resolve("secret.key"))); }
@Test void readIgnoresItemsWithoutAllKeys() { /* ItemStack simulado con PDC parcial → Optional.empty() */ }
```
Para `WorkbenchItemsTest` usar el patrón de mocks de `ItemMeta`/`PersistentDataContainer` que ya usan otros tests del repo (buscar `mock(PersistentDataContainer.class)`); si no hay, probar solo `read` con un PDC simulado con Mockito.

- [ ] **Step 2: Run** — Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — Expected: PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): ítem de mesa firmado`

### Task 4: Reglas de la mesa (acceso, mejora, rotura)

**Files:**
- Create: `structures/.../workbench/WorkbenchState.java`, `workbench/AccessRules.java`, `workbench/UpgradeRules.java`, `workbench/BreakRules.java`
- Test: `.../workbench/AccessRulesTest.java`, `UpgradeRulesTest.java`, `BreakRulesTest.java`

**Interfaces:**
- Produces: `enum WorkbenchState { IDLE, WAITING_MATERIALS, READY, BUILDING, PAUSED, COMPLETED, ERROR }` (hasta el plan 3 toda mesa está en `IDLE`).
- Produces: `AccessRules.canUse(UUID player, WorkbenchRecord bench, Set<UUID> trusted, boolean bypass)`; `canManage(...)` (solo dueño o bypass: mejorar, acceso, romper).
- Produces: `UpgradeRules.check(WorkbenchTier current, TierSettings next, Map<String,Integer> playerHas, double balance, boolean hasTierPermission)` → `sealed interface UpgradeCheck permits Ok, MaxTier, MissingItems(Map<String,Integer> missing), NotEnoughMoney(double missing), NoPermission`.
- Produces: `BreakRules.decide(BreakPolicy policy, boolean hasActiveProject, boolean canManage)` → `enum BreakDecision { DENY, ALLOW, ALLOW_PAUSE, ALLOW_CANCEL }` (sin obra activa: `ALLOW` si puede gestionar, si no `DENY`).

- [ ] **Step 1: Write the failing tests**

```java
@Test void ownerTrustedAndBypassCanUseStrangerCannot() { ... }
@Test void onlyOwnerOrBypassCanManage() { assertFalse(AccessRules.canManage(trustedPlayer, bench, trusted, false)); }
@Test void upgradeListsExactlyWhatIsMissing() {
    var r = UpgradeRules.check(CONSTRUCTOR, ingeniero, Map.of("minecraft:iron_ingot", 10, "minecraft:redstone", 8), 0, true);
    assertEquals(new MissingItems(Map.of("minecraft:iron_ingot", 6, "minecraft:redstone_lamp", 4)), r);
}
@Test void arquitectoIsMax() { assertInstanceOf(MaxTier.class, UpgradeRules.check(ARQUITECTO, null, Map.of(), 0, true)); }
@Test void breakPolicyTable() {
    assertEquals(DENY, BreakRules.decide(BLOCK, true, true));
    assertEquals(ALLOW_PAUSE, BreakRules.decide(PAUSE, true, true));
    assertEquals(ALLOW_CANCEL, BreakRules.decide(CANCEL, true, true));
    assertEquals(ALLOW, BreakRules.decide(BLOCK, false, true));
    assertEquals(DENY, BreakRules.decide(PAUSE, false, false));
}
```

- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): reglas de acceso, mejora y rotura`

### Task 5: Servicio de mesas y bloque en el mundo

**Files:**
- Create: `structures/.../workbench/WorkbenchService.java`, `workbench/WorkbenchBlocks.java` (listener), `workbench/WorkbenchDisplay.java`, `workbench/ProjectsView.java`
- Modify: `StructuresPlugin.java` (crear servicio, registrar listener, cargar mesas al arrancar y al cargar chunks)
- Test: `.../workbench/WorkbenchServiceTest.java`

**Interfaces:**
- Consumes: Tasks 2–4.
- Produces: `interface ProjectsView { boolean hasActiveProject(UUID bench); }` — en este plan, implementación que devuelve `false` siempre; el plan 3 la sustituye.
- Produces: `WorkbenchService`: `CompletableFuture<PlaceResult> place(Player p, ItemStack item, Block at)`, `CompletableFuture<Optional<ItemStack>> breakBench(Player p, Block at)`, `Optional<WorkbenchRecord> at(Block)` (caché en memoria por posición, cargada desde la base de datos), `CompletableFuture<UpgradeCheck> upgrade(Player p, UUID bench)`, `trust/untrust(Player owner, UUID bench, UUID target)`. `enum PlaceResult { OK, NOT_GENUINE, NO_PERMISSION, OCCUPIED, ERROR }`.
- Produces: `WorkbenchDisplay`: con modelo (`TierSettings.model` no vacío y pack instalado) → barrera + un único `ItemDisplay` marcado con PDC `rpgroll-structures:display = <id>`; sin modelo → `fallbackBlock` del nivel. `void show(WorkbenchRecord)`, `void remove(WorkbenchRecord)`, `void refresh(WorkbenchRecord)` (al mejorar). Nunca más de un display por mesa: antes de crear, borra los que tengan su id en un radio de 0.6.
- Comportamiento del listener (`ignoreCancelled = true`, prioridad `HIGH` como `StationBlocks`):
  - `BlockPlaceEvent` con ítem de mesa: comprueba `rpgrollstructures.workbench.place` y `isGenuine`; si una de las dos falla, cancela y avisa. Registra la mesa en la base de datos de forma asíncrona y, al confirmar, pone su display. Si la escritura falla, quita el bloque y devuelve el ítem.
  - Romper el bloque o el display, `EntityExplodeEvent`/`BlockExplodeEvent` y pistones: las explosiones y los pistones nunca rompen ni mueven una mesa (se quitan de la lista). Al romperla, aplica `BreakRules` y, si se permite, borra la fila, quita el display y suelta el ítem con el mismo `id`/`tier`/`serial`/`sig`. Así cada mesa existe una sola vez, como ítem o como bloque. En creativo también suelta el ítem.
  - Clic derecho: si `canUse`, abre el menú principal (Task 6); si no, avisa.
  - Cada mesa colocada se apunta también en el PDC de su chunk (`rpgroll-structures:benches` = lista de `id@x,y,z`). Al cargar el chunk se cruza con la base de datos; el chunk es la segunda fuente para detectar huérfanas.
  - Al arrancar: carga `all()`, y las filas cuyo mundo no existe o cuyo bloque ya no es el esperado (al cargar su chunk) pasan a `markOrphan` con un aviso en el log, sin borrarse.

- [ ] **Step 1: Write the failing tests** (servicio con repositorio en SQLite temporal y `Player`/`Block` simulados con Mockito):

```java
@Test void forgedItemIsNotPlaced() { assertEquals(NOT_GENUINE, service.place(p, forged, block).join()); assertTrue(repo.all().join().isEmpty()); }
@Test void breakReturnsTheSameIdentity() { /* place → breakBench → read(item) tiene mismo id, tier y serial; repo vacío */ }
@Test void breakingTwiceGivesOneItem() { assertTrue(service.breakBench(p, block).join().isPresent()); assertTrue(service.breakBench(p, block).join().isEmpty()); }
@Test void doubleUpgradeChargesOnce() { /* dos upgrade() seguidos: un Ok y el segundo MaxTier/no-op; inventario simulado descontado una vez */ }
@Test void missingWorldMarksOrphanInsteadOfDeleting() { ... }
```

- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** Cobro de la mejora: en el hilo principal se comprueba, se quitan los ítems y el dinero (Vault si está) y se llama a `repo.upgrade(id, from, to)`. Si devuelve `false`, se devuelve lo cobrado en el hilo principal. Mientras tanto un candado por mesa (`Set<UUID>` de mejoras en curso) ignora los clics repetidos.
- [ ] **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): mesa en el mundo`

### Task 6: Menús de la mesa (principal, mejoras, acceso y ayuda)

**Files:**
- Create: `structures/.../gui/StructuresMenus.java` (skin y ids de fondo), `gui/CenterMenu.java`, `gui/UpgradeMenu.java`, `gui/AccessMenu.java`, `gui/HelpMenu.java`, `src/main/resources/gui.yml`
- Test: `.../gui/StructuresMenusTest.java`, `gui/CenterMenuLayoutTest.java`

**Interfaces:**
- Consumes: `WorkbenchService`, `UpgradeCheck`, `LangManager`, `MenuSkin` (Lib), `InventoryGUI` (Lib), el «confirmar» de la Lib si existe en `common` (si no, un `ConfirmMenu` propio de 3 filas con id de fondo `confirmar`).
- Produces: `StructuresMenus.background(String menu, WorkbenchTier tier)` → `menu + "_" + family(tier)`, con `family`: CONSTRUCTOR→`taller`, INGENIERO→`ingenio`, ARQUITECTO→`arcano` (p. ej. `centro_taller`). Son los ids que dibujará el plan 8, así que el código ya los pide aunque falte el fondo (sin pack, cofre normal).
- `CenterMenu` (6 filas). Casillas fijas, constantes públicas que el plan 8 copiará al generador de fondos: CATALOGO 20, MIS_CONSTRUCCIONES 22, MATERIALES 24, OBRA 29, MEJORAS 31, ACCESO 33, AYUDA 49, CERRAR 53, nivel de la mesa 4. En este plan, catálogo, mis construcciones, materiales y obra muestran su icono con la línea de lore `menu.coming_soon`; el plan 3 los activa.
- `UpgradeMenu` (4 filas): requisitos del siguiente nivel con verde/rojo por ítem y «Mejorar», que pide confirmación y llama a `WorkbenchService.upgrade`.
- `AccessMenu` (6 filas): cabezas de los de confianza, paginado de 45. Clic para quitar; «Añadir» pide el nombre por `ChatPromptManager` de la Lib.
- `HelpMenu`: páginas de texto de `lang` (`help.page.<n>`).
- Todos los botones que no cambian van con `skin.button(...)`, y el relleno con `skin.filler(...)`.
- Títulos de `gui.yml` (claves `center.title`…) con traducción en `lang`. Título de fábrica del principal: «Centro de construcción».

- [ ] **Step 1: Write the failing tests**

```java
@Test void backgroundFamilyFollowsTier() {
    assertEquals("centro_taller", StructuresMenus.background("centro", CONSTRUCTOR));
    assertEquals("centro_ingenio", StructuresMenus.background("centro", INGENIERO));
    assertEquals("centro_arcano", StructuresMenus.background("centro", ARQUITECTO));
}
@Test void centerSlotsAreDistinctAndInside() { /* las constantes no se repiten y están en 0..53 */ }
@Test void upgradeMenuMarksMissingRed() { /* con MissingItems, la línea del ítem que falta usa la clave lang upgrade.missing */ }
```

- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): menús de la mesa`

### Task 7: Comandos, receta y `/dar`

**Files:**
- Create: `structures/.../command/StructuresCommand.java`, `command/StructuresAdminCommand.java`, `command/GiveArgs.java`, `workbench/WorkbenchRecipe.java`, `src/main/resources/command-words.yml`
- Modify: `StructuresPlugin.java`
- Test: `.../command/GiveArgsTest.java`, `StructuresAdminCommandTest.java`

**Interfaces:**
- `BrigadierCommands.register(this, "structures", ..., List.of(), cmd, cmd, "rpgrollstructures.use")`. `command-words.yml` con `structures: names {es: estructuras, pt: estruturas}`, y `subcommands` `help {es: ayuda, pt: ajuda}`, `open {es: abrir, pt: abrir}`. En este plan `help` y `open` abren la ayuda; `list`, `info`, `status` y `cancel` llegan en el plan 3.
- `/structuresadmin give <player> <constructor|ingeniero|arquitecto> [amount]` (máximo 64; cada unidad es una mesa nueva con su propio id y serie) y `/structuresadmin reload`, con permisos `rpgrollstructures.admin.give` y `.admin.reload`.
- `Things.give(this, "mesa", () -> List.of("constructor","ingeniero","arquitecto"), id -> ...)` → `/dar mesa:constructor`, que crea una mesa nueva igual que `give`.
- `WorkbenchRecipe`: receta con forma de `workbench.yml` cuyo resultado es una mesa Constructor. En `PrepareItemCraftEvent` se pone un ítem de muestra; en `CraftItemEvent` se cambia por una mesa nueva firmada. Con Mayús+clic se fabrica de una en una, para que cada mesa tenga su propia identidad.
- `GiveArgs.parse(String[] args)` → `record GiveArgs(String player, WorkbenchTier tier, int amount)` o error con la clave `lang` del problema.

- [ ] **Step 1: Write the failing tests**

```java
@Test void parsesGive() { assertEquals(new GiveArgs("Sack", INGENIERO, 3), GiveArgs.parse(new String[]{"Sack","ingeniero","3"}).orElseThrow()); }
@Test void amountIsClampedTo64AndAtLeast1() { ... }
@Test void unknownTierIsAnError() { /* "maestro" → error admin.give.bad_tier */ }
@Test void giveCreatesDistinctBenches() { /* amount 3 → 3 ítems con ids distintos y firmas válidas */ }
```

- [ ] **Step 2: Run** — FAIL. **Step 3: Implement.** **Step 4: Run** — PASS.
- [ ] **Step 5: Commit** (saltar salvo orden de Sack): `feat(structures): comandos, receta y /dar`

### Task 8: Comprobación en el servidor de pruebas

**Files:** ninguno nuevo, salvo notas en `structures/QA-fase1-2.md`.

- [ ] **Step 1:** `./gradlew :structures:test` completo — Expected: todo en verde.
- [ ] **Step 2:** Pedir a Sack permiso para compilar (`./gradlew :structures:jar`). Sin su «sí», esta tarea termina aquí y se anota.
- [ ] **Step 3:** En el banco `E:\Dev\sackito-network\infra\capturas` (nunca en main), con RPGRoll-Lib y GriefPrevention:
  - fabricar la mesa con la receta;
  - colocarla y abrir el menú;
  - mejorarla dos veces (la segunda con los materiales justos);
  - añadir y quitar a alguien de confianza;
  - intentar romperla siendo otro jugador;
  - reventarla con TNT;
  - romperla y volver a colocarla (mismo id);
  - reiniciar el servidor y comprobar que sigue;
  - colocarla en el terreno de otro (no debe registrarse).
- [ ] **Step 4:** Anotar los resultados en `structures/QA-fase1-2.md`. Lo que falle se convierte en un caso nuevo dentro de la tarea a la que pertenece.
