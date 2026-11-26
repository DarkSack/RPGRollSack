# RPGRoll-Structures fases 6-7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recuperación, protecciones y límites (fase 6) e integraciones externas y módulos funcionales (fase 7), más reparar y mejorar desde «Mis construcciones» (spec 19).

**Architecture:** Las integraciones van por reflexión dentro de `structures/` (como `machines/.../quarry/Claims.java`): sin dependencias de compilación nuevas y sin tocar `common/`. Un registro `Integrations` dice qué está activo y por qué no. Una comprobación de zona (`AreaGuard`) sustituye al `BuildGuard` de dos esquinas y se consulta al confirmar, al arrancar, al reanudar y cada cierto tiempo. Los módulos funcionales son un SPI (`StructureModule`) que se engancha al registro de construcciones terminadas. Reparar y mejorar son obras con un `kind` propio que reutilizan el motor.

**Tech Stack:** Java 25, Paper 26.1.1, SQLite/MySQL (migraciones repetibles), JUnit 5 + MockBukkit (lo que ya usa `structures/`).

**Spec:** `E:\Dev\RPGRoll\docs\superpowers\specs\2026-11-23-sackstructures-design.md` (secciones 9-13 y 19). Plan anterior: `docs/superpowers/plans/2026-11-24-structures-fase3-5.md`; su registro con rulings: `.superpowers/sdd/2026-11-24-structures-fase3-5/progress.md`.

## Global Constraints

- Paper 26.1.1 · Java 25. Structures 1.1.0 sigue siendo la versión (no subirla en este plan).
- «Todas opcionales, cada una se activa en `integrations.yml`.» «Sin el plugin, la integración se apaga y la función que depende de ella se oculta con su motivo.»
- «Antes de escribir cada adaptador se lee la documentación oficial vigente; nada de métodos inventados.» Cada adaptador cita en su Javadoc la URL leída y la versión del plugin.
- Adaptadores por reflexión con el cargador de clases del plugin (patrón de `machines/src/main/java/com/sack/rpgroll/machines/quarry/Claims.java`): si la reflexión falla, la integración queda inactiva con motivo y se avisa UNA vez en el log; nunca se lanza al hilo principal.
- No tocar nada de `common/` (hay cambios de otra sesión en `Protection.java`, `ProtectionProviders.java`, `BlockPermissionEvent.java`, `VaultEconomy.java`, `PendingRewardMoney.java`). Sí se puede USAR la API comprometida de la Lib (`VaultEconomy.get()`, `SoftDepend.enabled`).
- Invariante: «la suma de materiales del jugador + depositados nunca crece y nunca se pierde sin dejar registro». Lo mismo con el dinero: cada cobro y cada devolución con id único en el libro (`ledger`), en la misma transacción que su efecto.
- Las devoluciones (ítems y dinero) van SIEMPRE al dueño de la obra (o a sus pendientes si no está).
- Todo lo de Bukkit en el hilo principal; la base de datos en su hilo; las vueltas al principal por el `Consumer<Runnable> main` (la cola de `MainQueue`).
- Migraciones: cada tarea que cambia el esquema añade SU versión (`Migrations.LATEST` sube de uno en uno) con sentencias repetibles (comprobar la columna antes de `ALTER`, como `v3`).
- Cada tarea añade sus claves de idioma en `lang/es.yml`, `lang/en.yml` y `lang/pt_BR.yml` (LangFilesTest en verde).
- Sin commits ni push (regla de Sack). Sin compilar jars. Pruebas: `cd E:/Dev/RPGRoll && ./gradlew :structures:test --offline`.
- TDD obligatorio: cada comportamiento con su prueba que falla antes (RED) y pasa después (GREEN). El código que solo se puede probar con servidor (reflexión) queda fino y se anota en `structures/QA-fase1-2.md` (sección nueva «Fases 6-7: qué probar en el banco»).

## Review Focus

1. Dueño desconectado cuando se vuelve a comprobar la protección o cuando otro jugador de confianza vuelve a colocar la mesa: se usan los derechos del DUEÑO de la obra (por UUID), nunca los del que coloca. → prueba en Task 2 y Task 4.
2. Vault instalado pero sin plugin de economía (o economía que lanza): la integración está inactiva con motivo; nunca se cobra a medias ni se crea la obra sin cobrar. → prueba en Task 1 y Task 5.
3. Reinicio con una obra en PAUSED por protección o por mesa rota: sigue en pausa con su motivo tras cargar; no se reanuda sola dentro de un terreno protegido. → prueba en Task 3.
4. Un módulo que lanza en `onBuilt`/`onTick`: la construcción queda registrada, el error sale una vez en el log y las demás siguen. → prueba en Task 6.
5. Reparar o mejorar una construcción dañada por una explosión o con otra obra al lado: el choque solo exime la caja de ESA construcción; nunca se pisan bloques que no son suyos. → prueba en Task 9 y Task 10.

---

### Task 1: Registro de integraciones, `integrations.yml`, Bedrock por Floodgate

**Files:**
- Create: `structures/src/main/resources/integrations.yml`
- Create: `structures/src/main/java/com/sack/rpgroll/structures/integration/Hook.java`, `Integrations.java`, `BedrockCheck.java`, `FloodgateBedrock.java`
- Modify: `StructuresPlugin.java` (cargar, recargar con `structuresadmin reload`), `command/StructuresAdminCommand.java` (subcomando `integrations`), `preview/PreviewService.java` (Bedrock: solo contorno), `plugin.yml` (añadir `MythicMobs` a `softdepend`), lang ×3
- Test: `integration/IntegrationsTest.java`, `preview/PreviewServiceTest.java`, `command/StructuresAdminCommandTest.java`

**Interfaces:**
- Produces:
  - `enum Hook { WORLDGUARD("WorldGuard"), GRIEFPREVENTION("GriefPrevention"), VAULT("Vault"), FLOODGATE("floodgate"), MYTHICMOBS("MythicMobs"); String plugin(); String key(); }` (`key()` = nombre en minúsculas usado en el YAML: `worldguard`, `griefprevention`, `vault`, `floodgate`, `mythicmobs`).
  - `record Integrations.Status(Hook hook, boolean enabled, boolean present, boolean active, String reasonKey)` (`reasonKey` null si activa; si no, una de `integrations.reason.disabled`, `.missing`, `.broken`, `.no-economy`).
  - `static Integrations load(ConfigurationSection yml, Predicate<String> pluginEnabled, Map<Hook, Supplier<String>> probes, Logger log)`; `probes`: comprobación extra por hook que devuelve null (bien) o una clave de motivo (p. ej. Vault sin proveedor de `Economy` → `integrations.reason.no-economy`).
  - `boolean active(Hook h)`; `List<Status> report()`; `void markBroken(Hook h, String detail)` (un adaptador lo llama si su reflexión falla; avisa una vez).
  - `interface BedrockCheck { boolean isBedrock(UUID player); static BedrockCheck none(); }`; `FloodgateBedrock` = reflexión sobre `org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(UUID)`.

- [ ] **Step 1: Tests (RED)**
  - `IntegrationsTest.disabledInYamlIsInactiveWithReason`: `vault.enabled: false`, Vault presente → `active(VAULT)==false`, reason `integrations.reason.disabled`.
  - `missingPluginIsInactive`: `pluginEnabled` false → reason `.missing`.
  - `probeFailureIsInactive`: probe de VAULT devuelve `integrations.reason.no-economy` → inactiva con esa clave (Review Focus 2).
  - `probeThatThrowsCountsAsBroken`: probe lanza → inactiva `.broken`, sin propagar.
  - `markBrokenWarnsOnce`: dos `markBroken` → un solo aviso en el log.
  - `missingKeysDefaultToEnabled`: YAML vacío → todas `enabled=true`.
  - `PreviewServiceTest.bedrockPlayerGetsOutlineOnly`: con `BedrockCheck` que dice true → ningún `sendBlockChange`/`sendMultiBlockChange`, sí contorno; mensaje `preview.bedrock` una vez por sesión.
  - `StructuresAdminCommandTest.integrationsListsEveryHook`: salida con una línea por `Hook` (✔/✘ y motivo traducido).
- [ ] **Step 2:** `./gradlew :structures:test --offline` → FAIL (clases inexistentes).
- [ ] **Step 3:** Implementar. `integrations.yml`:
  ```yaml
  # Integraciones opcionales. false = apagada aunque el plugin esté.
  worldguard:
    enabled: true
  griefprevention:
    enabled: true
  vault:
    enabled: true
  floodgate:
    enabled: true
  mythicmobs:
    enabled: true
  ```
  Probe de Vault: `VaultEconomy.get().isPresent()` de la Lib. `/structuresadmin integrations` con permiso `rpgrollstructures.admin`. `PreviewService` recibe un `BedrockCheck` en su constructor.
- [ ] **Step 4:** Tests en verde (suite entera).
- [ ] **Step 5:** Sin commit (regla de Sack). Informe con `## Archivos`.

### Task 2: Protección de la zona entera (`AreaGuard`, WorldGuard y GriefPrevention)

**Files:**
- Create: `structures/src/main/java/com/sack/rpgroll/structures/protect/AreaGuard.java`, `AreaGuards.java`, `WorldGuardGuard.java`, `GriefPreventionGuard.java`
- Delete: `preview/BuildGuard.java` (sustituido)
- Modify: `preview/PreviewService.java`, `preview/TerrainCheck.java` (quitar el `shortcut` de dos esquinas), `build/ConstructionEngine.java` (`start`), `gui/MaterialsMenu.java` (texto de `StartResult.PROTECTED`), `StructuresPlugin.java`, lang ×3, `structures/QA-fase1-2.md`
- Test: `protect/AreaGuardsTest.java`, `preview/PreviewServiceTest.java`, `build/ConstructionEngineTest.java`

**Interfaces:**
- Consumes: `Integrations.active(Hook)`, `Integrations.markBroken` (Task 1); `storage.Box`.
- Produces:
  - `interface AreaGuard { Optional<Denial> check(UUID owner, String world, Box box); record Denial(Hook hook, String langKey) {} static AreaGuard allowAll(); }` — hilo principal; `langKey` p. ej. `protect.worldguard`, `protect.griefprevention`.
  - `static AreaGuard AreaGuards.of(List<AreaGuard> guards)` (el primero que niega gana) y `static AreaGuard AreaGuards.fromIntegrations(Integrations i, Logger log)` (solo los hooks activos).
  - `ConstructionEngine.StartResult.PROTECTED` (nuevo valor).
- Comportamiento:
  - Se comprueba la CAJA ENTERA una vez, por UUID del dueño de la obra (vale con el dueño desconectado). Nada de eventos de prueba por bloque.
  - WorldGuard: regiones que cruzan la caja (consulta de regiones aplicables a un cuboide) y si el dueño puede construir en todas, con jugador envuelto desde `OfflinePlayer`. GriefPrevention: claims que cruzan el rectángulo XZ de la caja (por chunks); niega si en alguno el dueño no tiene permiso de construir. Wilderness = permitido. La API exacta sale de la documentación oficial (Global Constraints).
  - `rpgrollstructures.bypass` (jugador conectado que confirma o arranca) se salta la comprobación.
  - Al confirmar (comprobación completa de la vista previa) y en `ConstructionEngine.start`.

- [ ] **Step 1: Tests (RED)**
  - `AreaGuardsTest.firstDenialWins`, `allowAllAllows`, `onlyActiveHooksAreUsed` (con `Integrations` falso: WG inactivo → su guard no se llama).
  - `AreaGuardsTest.guardThatThrowsDeniesAndMarksBroken`: un guard que lanza → `Denial` con `protect.error` y `markBroken` llamado (falla cerrado: no se construye en lo dudoso).
  - `PreviewServiceTest.fullCheckUsesTheProjectOwnerAndWholeBox`: el guard recibe el UUID del jugador que confirma y la caja girada completa (no dos esquinas).
  - `PreviewServiceTest.bypassSkipsTheGuard`.
  - `ConstructionEngineTest.startInProtectedAreaReturnsProtected`: guard niega → `PROTECTED`, la obra sigue READY.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3:** Implementar. Adaptadores por reflexión, finos; anotar en la guía de QA: claim ajeno, claim propio, región sin ser miembro, región como miembro, dueño desconectado.
- [ ] **Step 4:** Verde.
- [ ] **Step 5:** Informe.

### Task 3: Recuperación — mesa sin cargar, protección al reanudar, pausa con motivo, `debug`

**Files:**
- Modify: `build/ConstructionEngine.java`, `project/Project.java` (motivo de pausa), `storage/ProjectRepository.java` (guardar el motivo en `error` también en PAUSED), `config/PerformanceSettings.java` + `performance.yml` (`protection-recheck-seconds: 60`), `gui/ProjectMenu.java`, `command/StructuresAdminCommand.java` (`debug`), `StructuresPlugin.java`, lang ×3
- Test: `build/ConstructionEngineTest.java`, `config/PerformanceSettingsTest.java`, `command/StructuresAdminCommandTest.java`, `project/ProjectServiceTest.java`

**Interfaces:**
- Consumes: `AreaGuard` (Task 2).
- Produces:
  - Constructor de `ConstructionEngine` con dos parámetros nuevos al final: `Predicate<UUID> benchLoaded` (¿está cargado el chunk de la mesa de la obra? false también si la mesa es huérfana) y `AreaGuard guard`.
  - Claves de motivo de pausa (en `Project.error()` con estado PAUSED): `project.paused.protected`, `project.paused.bench` (Task 4 la usa), `project.paused.player` (pausa a mano).
  - `ConstructionEngine.resume(Project)` devuelve `StartResult` (STARTED, NOT_READY, PROTECTED).
- Comportamiento:
  - Mesa sin cargar: la obra espera sin cambiar de estado ni gastar presupuesto (como el chunk descargado). Ruling ya tomado: «se pausa» = espera.
  - Al instalar un Run (arranque del servidor, reanudar, primera vez) y cada `protection-recheck-seconds` mientras está BUILDING: `guard.check(owner, world, box)`. Si niega → PAUSED con `project.paused.protected`, guardado, aviso al dueño si está conectado. No se reanuda sola: el dueño (o el de la mesa, o bypass) pulsa Reanudar en `ProjectMenu`, que vuelve a comprobar.
  - Al cargar tras un reinicio, PAUSED sigue PAUSED con su motivo.
  - Cada paso a ERROR deja un `log.warning` con id de obra, estructura y motivo (revisar que todos los caminos lo hagan).
  - `/structuresadmin debug <id-de-obra | jugador>`: estado, cursor/total, colocados, saltados, motivo, mesa, mundo y origen.

- [ ] **Step 1: Tests (RED)**
  - `benchNotLoadedWaitsWithoutStateChange` (estado, cursor y presupuesto de otras obras intactos).
  - `protectionDeniedOnInstallPausesWithReason`, `protectionRecheckDuringBuildPauses` (reloj falso, 60 s).
  - `pausedForProtectionStaysPausedAfterReload` (Review Focus 3: `ProjectService.load` con una fila PAUSED + motivo → sigue PAUSED, el motor no la toca).
  - `resumeRechecksProtection` (niega → `PROTECTED`, sigue PAUSED).
  - `everyErrorTransitionIsLogged`.
  - `PerformanceSettingsTest.protectionRecheckDefaultsTo60`.
  - `StructuresAdminCommandTest.debugShowsProjectState`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 4: Romper la mesa con `break-policy: pause|cancel` y reconectar

**Files:**
- Modify: `workbench/BreakRules.java`, `workbench/WorkbenchService.java`, `workbench/WorkbenchBlocks.java`, `storage/WorkbenchRepository.java`, `StructuresPlugin.java`, lang ×3, `config.yml` (comentario de `break-policy`)
- Test: `workbench/BreakRulesTest.java`, `workbench/WorkbenchServiceTest.java`, `storage/WorkbenchRepositoryTest.java`

**Interfaces:**
- Consumes: `project.paused.bench` y `ConstructionEngine.resume` (Task 3); `ReturnService.cancel(Player, Project, int)`.
- Produces: `BreakRules.decide` devuelve `ALLOW_PAUSE` / `ALLOW_CANCEL` con obra activa (quitar el `shortcut`).
- Comportamiento:
  - `ALLOW_PAUSE`: la mesa pasa a huérfana (fila conservada, `markOrphan`) en vez de borrarse; la obra (cualquier estado activo) queda atada a su mesa; si estaba BUILDING pasa a PAUSED con `project.paused.bench`. Suelta el ítem con el mismo sello (mismo UUID). Al colocar ese ítem en cualquier sitio, el camino `reclaim` existente la reconecta; si la obra estaba PAUSED por `project.paused.bench`, se llama a `resume` (que comprueba la protección con el DUEÑO de la obra, no con quien coloca) y se avisa al dueño. La obra sigue en su sitio original.
  - `ALLOW_CANCEL`: primero `ReturnService.cancel` (devolución al dueño según `cancel-refund`); solo si la base de datos lo confirma se rompe la mesa como siempre. Si falla, la mesa no se rompe y sale `build.cancel-error`.
  - Arreglo pendiente de la memoria: una mesa huérfana NO ocupa su antigua posición (ni en la caché ni al cargar), así otra mesa puede ponerse en ese bloque.

- [ ] **Step 1: Tests (RED)**
  - `BreakRulesTest.pausePolicyAllowsPause`, `cancelPolicyAllowsCancel`, `blockPolicyStillDenies`.
  - `WorkbenchServiceTest.breakWithPauseKeepsRowAsOrphanAndPausesProject`, `replacingTheItemReconnectsAndResumes`, `reconnectByTrustedPlayerChecksTheProjectOwner` (Review Focus 1), `breakWithCancelRefundsOwnerThenBreaks`, `cancelFailureKeepsTheBench`, `orphanDoesNotOccupyItsOldPosition`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 5: Coste en dinero con Vault (`money:`), devolución y dinero pendiente

**Files:**
- Create: `structures/src/main/java/com/sack/rpgroll/structures/deposit/MoneyService.java`, `storage/PendingMoneyRow.java`
- Modify: `storage/Migrations.java` (v4), `storage/ProjectRepository.java`, `storage/ProjectRecord.java` (`moneyCents`), `storage/Movement.java` (tipos `MONEY_CHARGE`, `MONEY_REFUND`), `project/ProjectService.java`, `deposit/ReturnService.java` (`cancel`), `gui/PreviewMenu.java`, `gui/DetailMenu.java`, `gui/MaterialsMenu.java`, `StructuresPlugin.java` (listener de entrada para pagar pendientes), lang ×3
- Test: `deposit/MoneyServiceTest.java`, `storage/MigrationsTest.java`, `storage/ProjectRepositoryTest.java`, `deposit/ReturnServiceTest.java`, `gui/StructuresMenusTest.java`

**Interfaces:**
- Consumes: `Integrations.active(VAULT)` (Task 1); `VaultEconomy.get()` (Lib).
- Produces:
  - v4: `projects.money_cents BIGINT NOT NULL DEFAULT 0`; tabla `pending_money(id autoId, player, amount_cents BIGINT NOT NULL, created_at BIGINT NOT NULL, paid_at BIGINT NULL)`.
  - `MoneyService`: `boolean available()`; `ChargeResult charge(Player p, long cents)` (`OK`, `NOT_ENOUGH`, `UNAVAILABLE`, `ERROR`); `void refund(UUID owner, long cents, String movementId)`; `CompletableFuture<Integer> payPending(UUID player)`.
- Comportamiento:
  - `money` en céntimos (`Math.round(money * 100)`). Sin Vault activo y `money > 0`: la estructura no se puede confirmar; `DetailMenu`/`PreviewMenu` muestran el motivo (`structure.needs-vault`).
  - Al confirmar: cobro en el hilo principal ANTES de crear; el `MONEY_CHARGE` va en la misma transacción que el alta de la obra (`ProjectRepository.insert`). Si el alta falla, se devuelve el dinero con `depositPlayer`; si eso también falla, `log.severe("DINERO PERDIDO ...")` con jugador y cantidad, y fila en `pending_money`.
  - Al cancelar: `round(money_cents × (total − cursor) / total × cancel-refund / 100)` (obra sin empezar: `cursor = 0`). La fila de `pending_money` y el `MONEY_REFUND` van en la transacción de `repo.cancel`; luego se intenta pagar (`OfflinePlayer` vale) y se marca `paid_at`. Sin Vault, o si falla, queda pendiente y se paga al entrar el jugador o al arrancar con Vault activo. Idempotente por id de fila.
  - Quitar el texto «(aún no se cobra)».

- [ ] **Step 1: Tests (RED)**
  - `chargeFailsWithoutEconomyAndCreatesNothing` (Review Focus 2), `chargeNotEnough`, `createFailureRefundsTheCharge`, `refundFailureLeavesPendingAndSevere`.
  - `ReturnServiceTest.cancelRefundsUnspentMoneyProportionally` (cursor 25 de 100, 100 %, 10,00 → 7,50), `cancelRefundGoesToOwnerNotCanceller`.
  - `payPendingPaysOnceEvenIfCalledTwice`.
  - `MigrationsTest.v4IsRepeatable`.
  - `StructuresMenusTest.moneyStructureWithoutVaultShowsReason`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 6: SPI de módulos funcionales, registro, ciclo de vida y módulo `farm`

**Files:**
- Create: `structures/src/main/java/com/sack/rpgroll/structures/api/StructureModule.java`, `api/BuiltStructure.java`, `module/ModuleRegistry.java`, `module/ModuleService.java`, `module/FarmModule.java`
- Modify: `catalog/StructureDefinition.java` (`module`, `moduleConfig`), `catalog/StructureLoader.java`, `storage/ProjectRepository.java` (`allBuilds()`, `deleteBuild(UUID)`), `build/ConstructionEngine.java` o el callback `completed` de `StructuresPlugin` (llamar a `onBuilt`), `command/StructuresAdminCommand.java` (`delete <id-de-construcción>`), `gui/DetailMenu.java` (nota del módulo), lang ×3
- Test: `module/ModuleServiceTest.java`, `catalog/StructureLoaderTest.java`, `storage/ProjectRepositoryTest.java`

**Interfaces:**
- Produces:
  ```java
  public interface StructureModule {
      String type();                                         // valor de module.type en el YAML
      List<Problem> validate(ConfigurationSection config);   // claves relativas a "module."
      default Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection config) { return Optional.empty(); } // clave de idioma si no se puede
      void onBuilt(BuiltStructure b);
      default int tickEvery() { return 0; }                   // ticks; 0 = sin onTick
      default void onTick(BuiltStructure b) {}
      void onRemoved(BuiltStructure b);
  }
  public record BuiltStructure(BuildRecord record, StructureDefinition def, ConfigurationSection config) {}
  ```
  - `ModuleRegistry.register(StructureModule)`, `Optional<StructureModule> get(String type)`.
  - `ModuleService`: `onBuilt(BuildRecord)`, `tick()` (cada tick del servidor; llama `onTick` según `tickEvery` y solo si el chunk del origen está cargado), `remove(UUID buildId)` (borra la fila y llama `onRemoved`), `load()` (todas las construcciones al arrancar). Todo en el hilo principal; cada llamada a un módulo envuelta en try/catch con aviso una vez por construcción y método.
  - `precheck` se llama al confirmar y en `start` (Task 8 lo usa).
- Comportamiento:
  - YAML: `module:` como sección con `type:`; sin `module:` = ningún módulo. `type` desconocido → `Problem("module.type", ...)` y la estructura no carga.
  - `FarmModule` (`type: farm`): no hace nada en el mundo; `module.note.es|en|pt_BR` se enseña en `DetailMenu` (para aclarar si funciona por mecánicas vanilla; no promete producción).
  - `/structuresadmin delete <id-de-construcción>` borra el registro y llama a `onRemoved`; los bloques se quedan.

- [ ] **Step 1: Tests (RED)**
  - `unknownModuleTypeIsAProblem`, `farmNoteIsLoaded`.
  - `onBuiltIsCalledAfterRegistration`, `throwingModuleDoesNotBreakOthers` (Review Focus 4: dos construcciones, el módulo de una lanza en `onTick` → la otra sigue; un aviso).
  - `onTickRespectsTickEveryAndLoadedChunks`, `removeDeletesRowAndCallsOnRemoved`, `loadRegistersExistingBuilds`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 7: Módulo `mob_farm` (spawner vanilla, MythicMobs opcional)

**Files:**
- Create: `structures/src/main/java/com/sack/rpgroll/structures/module/MobFarmModule.java`, `module/MythicSpawner.java`
- Modify: `StructuresPlugin.java` (registrar), lang ×3, `structures/QA-fase1-2.md`
- Test: `module/MobFarmModuleTest.java`

**Interfaces:**
- Consumes: SPI (Task 6), `Rotation.apply` de la Lib, `Integrations.active(MYTHICMOBS)`.
- YAML:
  ```yaml
  module:
    type: mob_farm
    spawner: [3, 1, 3]     # posición en el esquemático, sin girar; ahí debe haber un spawner
    mob: zombie            # EntityType vanilla, o mythic:<Nombre>
    max-entities: 12       # mobs de ese tipo a 16 bloques del spawner
  ```
- Comportamiento:
  - `validate`: `spawner` dentro del tamaño, `mob` válido (`EntityType` que se pueda generar, o `mythic:` con nombre no vacío); `mythic:` sin MythicMobs activo → Problem `module.mob` con motivo. Si `Things` de la Lib tiene una fuente de tipo `spawner`, aceptar `spawner:<id>`; si no, Problem (anotar en el informe lo que se encontró).
  - `onBuilt`: en la posición girada pone el tipo del `CreatureSpawner` (vanilla).
  - `tickEvery = 100`; vanilla: si hay `max-entities` o más de ese tipo cerca, el spawner se para (`setRequiredPlayerRange(0)`) y se reactiva (16) cuando bajan. `mythic:`: el spawner queda parado y el módulo genera el mob Mythic (API por reflexión) si hay un jugador a 16 bloques y menos de `max-entities`.
  - `onRemoved`: nada.

- [ ] **Step 1: Tests (RED)**: `validatesSpawnerInsideSize`, `rejectsMythicWithoutMythicMobs`, `onBuiltSetsSpawnerTypeAtRotatedPosition` (rotación CW_90), `stopsSpawnerAtMaxEntities`, `restartsSpawnerBelowMax`, `mythicSpawnsOnlyWithPlayerNearAndBelowMax` (con `MythicSpawner` falso).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 8: Módulo `parcel` (terreno de GriefPrevention o región de WorldGuard)

**Files:**
- Create: `structures/src/main/java/com/sack/rpgroll/structures/module/ParcelModule.java`, `module/ParcelClaims.java` (interfaz + adaptadores GP y WG por reflexión)
- Modify: `storage/Migrations.java` (v5: tabla `build_modules(build_id PRIMARY KEY, data TEXT NOT NULL)`), `storage/ProjectRepository.java` (`moduleData(UUID)`, `saveModuleData(UUID, String)`), `StructuresPlugin.java`, lang ×3, `structures/QA-fase1-2.md`
- Test: `module/ParcelModuleTest.java`, `storage/MigrationsTest.java`

**Interfaces:**
- Consumes: SPI con `precheck` (Task 6), `Integrations` (Task 1).
- Produces: `interface ParcelClaims { Optional<String> canCreate(UUID owner, String world, Box box); Optional<String> create(UUID owner, String world, Box box, String name); void remove(String world, String createdId); }` (`create` devuelve el id creado; las `Optional<String>` de `canCreate` son claves de motivo).
- YAML:
  ```yaml
  module:
    type: parcel
    claim: griefprevention   # griefprevention | worldguard | none
  ```
- Comportamiento:
  - `precheck` (al confirmar y al arrancar): GP → el dueño tiene bloques de terreno de sobra para el área XZ de la caja y no cruza otro claim; WG → no cruza otra región. Si no → no se confirma, con motivo. Integración pedida pero inactiva → la estructura no se puede confirmar, con motivo.
  - `onBuilt`: crea el claim (gasta bloques del dueño, a toda altura) o la región cuboide con el dueño como owner (nombre `rpgroll_parcel_<8 primeros del id>`); guarda el id en `build_modules`. Si falla, aviso al dueño y log; la construcción sigue registrada.
  - `onRemoved`: borra el claim o la región guardados.

- [ ] **Step 1: Tests (RED)**: `precheckRejectsNotEnoughClaimBlocks`, `precheckRejectsWhenIntegrationInactive`, `onBuiltStoresCreatedId`, `onBuiltFailureKeepsBuild`, `onRemovedDeletesStoredClaim`, `v5IsRepeatable`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 9: Reparar desde «Mis construcciones»

**Files:**
- Modify: `storage/Migrations.java` (v6: `projects.kind` VARCHAR(16) NOT NULL DEFAULT 'BUILD', `projects.build_id` NULL, `builds.source_hash` VARCHAR(64) NULL), `storage/ProjectRecord.java`, `storage/BuildRecord.java`, `storage/ProjectRepository.java`, `project/Project.java`, `project/ProjectService.java` (`createRepair`), `build/ConstructionEngine.java` (al terminar una REPAIR no se crea otra construcción), `gui/BuildsMenu.java`, lang ×3
- Create: `project/ProjectKind.java` (`BUILD, REPAIR, UPGRADE`), `project/RepairMath.java`
- Test: `project/RepairMathTest.java`, `project/ProjectServiceTest.java`, `build/ConstructionEngineTest.java`, `storage/MigrationsTest.java`

**Interfaces:**
- Produces:
  - `static RepairMath.Result RepairMath.of(PlacementPlan plan, BlockWriter w, String world, int ox, int oy, int oz)` → `record Result(int missing, int total)`; falta = posición del plan con aire o reemplazable donde el plan pone otra cosa.
  - `static Map<String,Integer> RepairMath.price(List<MaterialSpec> specs, int missing, int total)` → `ceil(cantidad × missing / total)` por material.
  - `CompletableFuture<Project> ProjectService.createRepair(UUID bench, StructureDefinition def, BuildRecord build, Map<String,Integer> price)`: obra `kind=REPAIR`, `build_id`, mismo origen y giro; el choque exime solo la caja de ESA construcción.
- Comportamiento:
  - Botón «Reparar» en `BuildsMenu` (quitar «Próximamente»), abierto desde una mesa libre del dueño con nivel suficiente. Requisitos: la estructura sigue en el catálogo y su hash coincide con `builds.source_hash` (las de antes de v6, sin hash: se permite). Si no, motivo.
  - `missing == 0` → «no le falta nada». `missing == total` → la construcción desapareció: se borra su registro con `ModuleService.remove` (llama `onRemoved`) y se avisa; no hay reparación.
  - La obra de reparación usa el plan completo; el motor ya salta lo ocupado (nunca pisa) y lo que ya es igual. Precio y consumo con la maquinaria de depósitos normal.
  - `complete` de una REPAIR: no crea fila en `builds`; actualiza `completed_at` de la construcción.
  - `ConstructionEngine.complete` guarda `source_hash` en `builds` para las nuevas.

- [ ] **Step 1: Tests (RED)**: `RepairMathTest.countsOnlyAirOrReplaceableMismatches`, `priceRoundsUp`, `ProjectServiceTest.repairOverlapExemptsOnlyItsOwnBuild` (Review Focus 5: otra obra al lado sigue chocando), `repairOfVanishedBuildRemovesRecord`, `repairRefusedWhenHashChanged`, `ConstructionEngineTest.repairNeverOverwritesForeignBlocks`, `repairCompletionCreatesNoNewBuild`, `MigrationsTest.v6IsRepeatable`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

### Task 10: Mejorar con `upgrade_from:`

**Files:**
- Modify: `catalog/StructureDefinition.java` (`upgradeFrom`), `catalog/StructureLoader.java`, `catalog/StructureRegistry.java` (validar referencias tras cargar todo), `project/ProjectService.java` (`createUpgrade`), `build/ConstructionEngine.java` (posiciones propias reemplazables), `build/PlacementPlan.java` si hace falta, `gui/BuildsMenu.java`, `module/ModuleService.java`, lang ×3
- Test: `catalog/StructureRegistryTest.java`, `project/ProjectServiceTest.java`, `build/ConstructionEngineTest.java`

**Interfaces:**
- Consumes: `ProjectKind.UPGRADE`, `projects.build_id` (Task 9), `ModuleService.remove`/`onBuilt` (Task 6).
- Produces: `CompletableFuture<Project> ProjectService.createUpgrade(UUID bench, StructureDefinition target, BuildRecord from)`.
- Comportamiento:
  - `upgrade_from: <id>` en el YAML de la estructura nueva; id inexistente → Problem `upgrade_from`. Los materiales del YAML de la nueva son el precio de la mejora (ruling).
  - Botón «Mejorar» en `BuildsMenu`, con una entrada por estructura que declara `upgrade_from` igual al id de esa construcción, si el jugador tiene permiso y nivel de mesa.
  - Mismo origen y giro que la construcción. El choque exime solo su caja.
  - Al escribir, una posición se puede REEMPLAZAR solo si pertenece a la construcción original: está en el plan de la estructura original y el bloque actual es igual al de ese plan. El resto sigue las reglas normales (aire o reemplazable; si no, se salta).
  - Al terminar: en una transacción, la fila de la construcción vieja se borra y entra la nueva; luego `onRemoved` del módulo viejo y `onBuilt` del nuevo.

- [ ] **Step 1: Tests (RED)**: `unknownUpgradeFromIsAProblem`, `upgradeReplacesOnlyOwnOriginalBlocks` (un bloque del jugador puesto encima a mano no se pisa; uno original sí), `upgradeOverlapExemptsOnlyItsOwnBuild`, `upgradeCompletionSwapsBuildRecordAndModules`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Verde. **Step 5:** Informe.

---

**Fuera de este plan (plan 4):** modelos y fondos nuevos, iconos, las cinco estructuras de ejemplo, el editor de admin (`create|import|export|edit`), entidades del esquemático (`entities: true`), API pública con eventos, documentación y QA final.
