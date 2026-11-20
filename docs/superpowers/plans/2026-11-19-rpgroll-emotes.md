# RPGRoll-Emotes: plan de implementación

> **Para agentes:** SUB-SKILL OBLIGATORIA: superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans, tarea por tarea. Los pasos usan casillas (`- [ ]`).

**Objetivo:** un módulo nuevo, RPGRoll-Emotes. Los jugadores lanzan emotes de cuerpo entero desde un menú o con `/emote`, y Magic anima cada hechizo con «canalizar» y «conjurar».

**Arquitectura:**
- **Animación:** el módulo depende de RPGRoll-Lib y de BetterModel. Las animaciones son las de jugador de BetterModel, en `BetterModel/players/*.bbmodel`; el plugin las instala y las reproduce por la API.
- **Integración con Magic:** Magic lanza tres eventos Bukkit nuevos que viven en la Lib. Emotes los escucha sin compilar contra Magic.
- **Pack de recursos:** el pack que genera BetterModel se registra en SackResourcePack con un método nuevo de su API.

**Tecnología:**
- Java 25, Paper API `26.1.1.build.29-alpha`, JUnit 5 + Mockito (las convenciones del repo).
- `io.github.toxicity188:bettermodel-api:3.6.0` y `bettermodel-bukkit-api:3.6.0` (Maven Central, `compileOnly`).
- Python 3 sin dependencias para generar las animaciones.

**Spec:** `docs/superpowers/specs/2026-11-19-rpgroll-emotes-design.md`. Léelo antes de empezar: este plan argumenta desde él.

## Restricciones globales

- **plugin.yml:**
  - `api-version: "26.1.1"`, `version` = la de los demás módulos (`1.1.11`; la subida de release es de Sack).
  - `depend: [RPGRoll-Lib, BetterModel]`, `softdepend: [SackResourcePack]`. Nunca depender de RPGRoll (core) ni de RPGRoll-Magic.
- **Eventos nuevos:** en `:common` (la Lib), paquete `com.sack.rpgroll.common.event.magic`. Nunca en `:api`, que va dentro del core.
- **Licencia:** módulo vendible con `license/license.yml` (copiado del de Furniture con el nombre cambiado), `emotes=0` en `licensing.properties` y `LicenseGate.verify(...)` al empezar `onEnable`, como Furniture.
- **Idiomas:** es, en y pt_BR. Por defecto, `es`.
- **Comandos:** con `BrigadierCommands.register(...)` de la Lib, con autocompletado en todos los argumentos.
- **Menús:** con `InventoryGUI` y `MenuSkin` registrado como módulo `emotes`, desde `menus.pack` de `config.yml` (vacío de fábrica).
- **Sin permisos de `/bm`:** los jugadores no los necesitan; todo va por la API de BetterModel.
- **Rendimiento:** ninguna tarea por tick por jugador sin un emote activo.
- **Compilar:** preguntar a Sack antes de cualquier `shadowJar`, `jar` o `build`. `./gradlew <m>:test` y `compileJava` sí se pueden lanzar.
- **Git:** sin commits ni push salvo orden de Sack. Los pasos «Commit» de este plan son puntos de control: se anotan en el registro y se commitea solo si Sack lo pide.
- **Producción:** main se toca solo con su OK y 0 jugadores.
- **Valores fijados por el spec** (en `config.yml` con estos defaults):

  | Clave | Valor |
  |---|---|
  | `cooldown-seconds` | 3 |
  | `combat-tag-seconds` | 8 |
  | `cancel.move-distance` | 0.3 (horizontal) |
  | `magic.walking-speed` | 0.05 |
  | `favorites.max` | 7 |
  | `magic.cast.default` | `conjurar` |
  | `magic.channel.default` | `canalizar` |
  | `magic.enabled` | true |
  | `limb-model` | `rpgroll_emotes` |

## Puntos que revisar

1. **Salir, morir, teletransportarse o cambiar de mundo con un emote en marcha.** La sesión y el tracker se cierran y no queda nada en memoria (Tarea 7, test `quitClosesSession`).
2. **Lanzar un hechizo con un bucle (meditar) en marcha.** Conjurar lo reemplaza y el bucle no vuelve después (Tarea 8, test `castReplacesLoopAndLoopDoesNotResume`).
3. **Recargar BetterModel (`/bm reload` o la API) con emotes en marcha.** El cierre del tracker limpia la sesión (Tarea 6, test `trackerClosedExternallyEndsSession`).
4. **Un emote desbloqueado que desaparece del catálogo.** El id se queda en el YAML del jugador, el menú y las sugerencias lo ignoran y nada falla (Tarea 4, test `unknownUnlockedIdsAreKeptButHidden`).
5. **Spam de `/emote` o favorito vacío.** Se respeta el enfriamiento; `/emote 5` sin favorito en ese hueco da un mensaje y no lanza nada (Tarea 6, test `emptyFavoriteSlotFails`).

---

### Tarea 1: eventos de magia en la Lib

**Archivos:**
- Crear: `common/src/main/java/com/sack/rpgroll/common/event/magic/SpellCastEvent.java`, `SpellChannelStartEvent.java`, `SpellChannelEndEvent.java`
- Test: `common/src/test/java/com/sack/rpgroll/common/event/magic/SpellEventsTest.java`

**Interfaces:**
- Produce:
  - `SpellCastEvent(Player player, String spellId, String schoolId, Set<String> tags)`, con getters `getPlayer()`, `spellId()`, `schoolId()` y `tags()`.
  - `SpellChannelStartEvent(Player player, String spellId, String schoolId, int castTimeTicks)`.
  - `SpellChannelEndEvent(Player player, String spellId, Reason reason)`, con `enum Reason { COMPLETED, MOVED, DAMAGED, OTHER }`.
  - Los tres extienden `org.bukkit.event.Event` y tienen `static HandlerList getHandlerList()`. No son cancelables, y `tags` es una copia inmutable.

- [ ] **Paso 1: test que falla**: `SpellEventsTest`:
  - `castEventExposesFieldsAndCopiesTags`: los tags mutados después no cambian el evento.
  - `eachEventHasItsOwnHandlerList`: tres `getHandlerList()` distintos y no nulos, y `getHandlers()` devuelve el estático.
  - `channelEndCarriesReason`.
  - El jugador es un mock de Mockito.
- [ ] **Paso 2:** `./gradlew :common:test --tests "*SpellEventsTest"`. Esperado: FALLA porque no compila (las clases no existen).
- [ ] **Paso 3:** crear las tres clases. Javadoc en español: quién las lanza (Magic) y para qué (Emotes y otros).
- [ ] **Paso 4:** el mismo comando. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(lib): eventos de lanzamiento y canalización de hechizos`

### Tarea 2: Magic lanza los eventos

**Archivos:**
- Modificar: `magic/src/main/java/com/sack/rpgroll/magic/engine/SpellCastEngine.java` (constructor y `cast`, líneas ~48-110)
- Modificar: `magic/src/main/java/com/sack/rpgroll/magic/listener/SpellChannelManager.java` (`start`, `cancelChannel`, final del canal ~60-160)
- Test: `magic/src/test/java/com/sack/rpgroll/magic/engine/SpellCastEventFiringTest.java` y `magic/src/test/java/com/sack/rpgroll/magic/listener/SpellChannelEventsTest.java`

**Interfaces:**
- Consume (Tarea 1): los tres eventos.
- Produce:
  - Constructor nuevo `SpellCastEngine(Plugin, SchoolManager, RuneManager, LangManager, Consumer<Event> events)`. El constructor actual delega con `event -> Bukkit.getPluginManager().callEvent(event)`.
  - Lo mismo en `SpellChannelManager(Plugin, SpellCastEngine, LangManager, Consumer<Event> events)`.

- [ ] **Paso 1: tests que fallan**:
  - `castFiresEventOnlyOnSuccess`: con un hechizo en enfriamiento no se lanza ningún evento. Con éxito se lanza exactamente un `SpellCastEvent` con el `spellId` y el `schoolId` del hechizo, después de cobrar el coste.
  - `channelStartFiresStartEvent` (`castTimeTicks` del hechizo).
  - `channelMoveFiresEndMoved`, `channelDamageFiresEndDamaged`, `channelCompleteFiresEndCompleted`.
  - Para conseguir las ramas, mirar los tests de Magic que ya existen y montar con Mockito lo mínimo.
- [ ] **Paso 2:** `./gradlew :magic:test --tests "*SpellCastEventFiringTest" --tests "*SpellChannelEventsTest"`. Esperado: FALLA.
- [ ] **Paso 3:** lanzar los eventos.
  - `SpellCastEvent` va justo después de `spellbook.startCooldown(...)` y antes de `playCastFeedback`.
  - `SpellChannelStartEvent` va al programar el canal.
  - `SpellChannelEndEvent` va en cada salida del canal: completado antes de `engine.cast`, movido y dañado. Cualquier otra limpieza usa `OTHER`.
- [ ] **Paso 4:** el mismo comando y después `./gradlew :magic:test`. Esperado: PASA, con toda la suite de Magic en verde.
- [ ] **Paso 5: commit** `feat(magic): lanza SpellCastEvent y los eventos de canalización`

### Tarea 3: esqueleto del módulo

**Archivos:**
- Modificar: `settings.gradle.kts` (añadir `"emotes"` antes de `"packinstaller"`) y `licensing.properties` (`emotes=0`)
- Crear: `emotes/build.gradle.kts`
- Crear en `emotes/src/main/resources/`: `plugin.yml`, `config.yml`, `license/license.yml`, `lang/es.yml`, `lang/en.yml`, `lang/pt_BR.yml`
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/EmotesPlugin.java`, `EmotesSettings.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/EmotesSettingsTest.java`

**Interfaces:**
- Produce `record EmotesSettings(int cooldownSeconds, int combatTagSeconds, double moveDistance, double walkingSpeed, int maxFavorites, String limbModel, boolean magicEnabled, String castDefault, Map<String,String> castBySchool, String channelDefault, Map<String,String> channelBySchool)`.
  - `static EmotesSettings from(ConfigurationSection root)` aplica los defaults de las restricciones globales.
  - `String castEmoteFor(String schoolId)` y `String channelEmoteFor(String schoolId)`.

- [ ] **Paso 1: test que falla**:
  - `defaultsMatchSpec`: una sección vacía da exactamente los valores de la tabla.
  - `schoolOverridesWin`: `magic.cast.schools.fire: conjurar_fuego` hace que `castEmoteFor("fire")` sea `conjurar_fuego` y `castEmoteFor("frost")` sea `conjurar`.
- [ ] **Paso 2:** `./gradlew :emotes:test`. Esperado: FALLA (el módulo o la clase no existen).
- [ ] **Paso 3: crear el módulo.**
  - **`build.gradle.kts`:** como el de Furniture, con `archivesName` `RPGRoll-Emotes`, `version = "1.1.11"`, `compileOnly` de las dos APIs de BetterModel 3.6.0 y `compileOnly(project(":sackresourcepack"))`. En test, `testImplementation` de las dos APIs.
  - **`plugin.yml`:** con la `description` en español y los permisos del spec:
    - `rpgrollemotes.use`: true
    - `rpgrollemotes.emote.*`: false
    - `rpgrollemotes.bypass.cooldown`: op
    - `rpgrollemotes.admin`: op, con `bypass.cooldown` de hijo
  - **`EmotesPlugin.onEnable`:**
    1. Licencia.
    2. `saveDefaultConfig`.
    3. `LangManager(this, List.of("es","en","pt_BR"), "es")`.
    4. Settings.
    5. `MenuSkin.register("emotes", MenuSkin.from(getConfig().getConfigurationSection("menus")))`.
    6. Un log `✔ RPGRoll-Emotes habilitado`.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): esqueleto del módulo`

### Tarea 4: modelo de emote y catálogo

**Archivos:**
- Crear en `emotes/src/main/java/com/sack/rpgroll/emotes/catalog/`: `Emote.java`, `EmoteParser.java`, `EmoteCatalog.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/catalog/EmoteCatalogTest.java`

**Interfaces:**
- Produce `record Emote(String id, String name, String icon, String category, String animation, String model, boolean loop, boolean locked, String rarity, EmoteSound sound, EmoteParticles particles, Set<CancelReason> cancel, boolean hidden, String howToUnlock)`.
  - `enum CancelReason { MOVE, DAMAGE, ATTACK, MOUNT }`.
  - `record EmoteSound(String id, float volume, float pitch)` y `record EmoteParticles(String type, int count)`, los dos opcionales (null).
- `EmoteParser.parse(String fileId, ConfigurationSection yml, String defaultModel) → Emote`:
  - Lanza `IllegalArgumentException` con un mensaje en español si falta `animation`, o si `unlock` o un `cancel` son inválidos.
  - El `id` por defecto es el nombre del archivo.
  - `cancel` ausente equivale a los cuatro motivos.
  - `unlock: free|locked`.
- `EmoteCatalog`:
  - `load(List<File> folders, Consumer<String> warn)` lee los `*.yml` recursivamente. Si dos emotes tienen el mismo id, gana el último y se avisa.
  - `checkAnimations(BiPredicate<String,String> exists, Consumer<String> warn)` quita y avisa los emotes cuyo `(model, animation)` no existe.
  - Además: `Optional<Emote> get(String id)`, `List<Emote> all()` (orden: categoría, rareza, nombre), `List<String> categories()` y `List<Emote> visibleTo(Predicate<Emote> owns)` (sin los `hidden` que no se tienen).

- [ ] **Paso 1: tests que fallan**:
  - `parsesFullEmote`: con todos los campos del ejemplo del spec.
  - `defaultsCancelToAllAndIdToFileName`.
  - `rejectsMissingAnimation`.
  - `rejectsUnknownUnlock`.
  - `duplicateIdLastWinsWithWarning`.
  - `missingAnimationIsDroppedWithWarning`.
  - `hiddenOnlyVisibleWhenOwned`.
  - `unknownUnlockedIdsAreKeptButHidden` (punto 4 de «Puntos que revisar»): `visibleTo` con un `owns` que dice sí a un id inexistente no devuelve nada extra y no lanza nada.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*EmoteCatalogTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar.
- [ ] **Paso 4:** el mismo comando. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): catálogo de emotes`

### Tarea 5: datos del jugador

**Archivos:**
- Crear en `emotes/src/main/java/com/sack/rpgroll/emotes/player/`: `EmotePlayer.java` y `EmotePlayerStore.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/player/EmotePlayerStoreTest.java`

**Interfaces:**
- Produce `EmotePlayer`:
  - `Set<String> unlocked()` (vista), `boolean unlock(String id)`, `boolean lock(String id)`.
  - `List<String> favorites()` (tamaño fijo `max`, null = hueco vacío), `boolean toggleFavorite(String id)` (primer hueco libre; si no hay, false), `String favorite(int slot1Based)`.
  - `long lastUseMillis()` y `void lastUseMillis(long)`.
- `EmotePlayerStore(File dataFolder, int maxFavorites)`:
  - `EmotePlayer load(UUID)`, `save(UUID)`, `unload(UUID)`, `EmotePlayer get(UUID)` (carga si no está) y `saveAll()`.
  - Un YAML por jugador en `data/players/<uuid>.yml`, con las claves `unlocked`, `favorites` y `last-use`, como `PassPlayerStore`.

- [ ] **Paso 1: tests que fallan** (con `@TempDir`):
  - `roundTripsUnlockedAndFavorites`.
  - `favoritesCapAtMax` (con 7 llenos, el octavo da false).
  - `toggleFavoriteRemovesAndFreesSlot`.
  - `missingFileIsEmptyPlayer`.
  - `unknownIdsSurviveRoundTrip`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*EmotePlayerStoreTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar.
- [ ] **Paso 4:** el mismo comando. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): datos de jugador`

### Tarea 6: motor y servicio de emotes

**Archivos:**
- Crear en `emotes/src/main/java/com/sack/rpgroll/emotes/`:
  - `engine/EmoteEngine.java`, `engine/BetterModelEngine.java`
  - `service/EmoteSession.java`, `service/EmoteService.java`, `service/LaunchResult.java`, `service/PlayerState.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/service/EmoteServiceTest.java` y `emotes/src/test/java/com/sack/rpgroll/emotes/engine/FakeEmoteEngine.java`

**Interfaces:**
- Produce `interface EmoteEngine`:
  - `boolean play(Player player, String model, String animation, boolean loop, Runnable onEnd)`: `onEnd` se llama una vez, tanto si termina sola como si la cierra BetterModel por fuera.
  - `void stop(Player player)` y `float lengthSeconds(String model, String animation)` (-1 si no existe).
  - `boolean exists(String model, String animation)`.
- `BetterModelEngine` implementa la interfaz con la API comprobada en 3.6.0:
  - **Reproducir:**
    - `BetterModel.limb(model)` da un `ModelRenderer`.
    - `renderer.getOrCreate(new BukkitPlayer(player), TrackerModifier.DEFAULT, t -> {})` da un `EntityTracker`.
    - `tracker.animate(animation, AnimationModifier.builder().type(loop ? AnimationIterator.Type.LOOP : AnimationIterator.Type.PLAY_ONCE).build(), () -> { tracker.close(); })`.
    - `tracker.handleCloseEvent((t, reason) -> onEnd.run())`, con guarda de una sola vez.
    - Para `stop`, se guarda el tracker por jugador.
  - **Duración:** `BetterModel.limb(model).flatMap(r -> r.animation(animation)).map(BlueprintAnimation::length)`.
  - Si `TrackerModifier.DEFAULT` o `builder().type(...)` no existen con esos nombres en 3.6.0, mirar con `javap` el jar de `~/.gradle` y usar el equivalente. Anotar la decisión en el registro. No se prueba con unit tests: se prueba en la Tarea 13.
- `record PlayerState(boolean gliding, boolean swimming, boolean riding, boolean sleeping, boolean dead)`, con `static PlayerState of(Player)`.
- `enum LaunchResult { OK, UNKNOWN, LOCKED, COOLDOWN, BUSY_STATE, IN_COMBAT, ENGINE_FAILED, EMPTY_FAVORITE }`.
- `EmoteService(EmoteCatalog, EmotePlayerStore, EmoteEngine, Supplier<EmotesSettings>, LongSupplier clockMillis)`:
  - `LaunchResult launch(Player p, String idOrFavoriteSlot, PlayerState state, boolean bypassCooldown)`.
  - `LaunchResult force(Player p, String emoteId)`: sin desbloqueo, enfriamiento, combate ni estado.
  - `void stop(Player p)`, `Optional<EmoteSession> session(UUID)`.
  - `void markCombat(UUID, long nowMillis)` y `void clear(UUID)`.
  - Un jugador «tiene» un emote si se cumple cualquiera de estas condiciones: no es `locked`, lo tiene en `unlocked`, o tiene el permiso `rpgrollemotes.emote.<id>`, `rpgrollemotes.emote.*` o `rpgrollemotes.admin`.
- `record EmoteSession(String emoteId, boolean loop, Location start, long startMillis, boolean magic)`.

- [ ] **Paso 1: tests que fallan** (con `FakeEmoteEngine`, que guarda las llamadas y deja disparar `onEnd`):
  - `launchesFreeEmote`, `lockedWithoutUnlockFails`, `unlockedLockedEmoteLaunches` y `permissionUnlocks`.
  - `cooldownBlocksSecondLaunchWithin3s`, y que con bypass no bloquea.
  - `busyStatesFail` (cada campo de `PlayerState`).
  - `combatTagBlocksFor8s`.
  - `newEmoteReplacesRunning`: `stop` del motor antes de `play`.
  - `onEndClearsSession`.
  - `trackerClosedExternallyEndsSession` (punto 3): disparar `onEnd` desde el fake sin `stop` deja `session()` vacío.
  - `favoriteSlotLaunches`.
  - `emptyFavoriteSlotFails` (punto 5): devuelve `EMPTY_FAVORITE` y el motor no recibe nada.
  - `forceIgnoresLocksAndCooldown`.
  - `quitClosesSession` (punto 1, vía `clear`): el motor recibe `stop` y la sesión queda vacía.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*EmoteServiceTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar el servicio y `BetterModelEngine`. Al lanzar con éxito, el servicio también reproduce el `sound` del emote (en la posición del jugador) y sus `particles` (sobre la cabeza), si los tiene. `./gradlew :emotes:compileJava` debe pasar contra la API real.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): servicio y motor BetterModel`

### Tarea 7: cortes y combate

**Archivos:**
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/listener/CancelRules.java` y `CancelListener.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/listener/CancelRulesTest.java`

**Interfaces:**
- Consume (Tarea 6): `EmoteService.session`, `stop`, `markCombat` y `clear`.
- Produce `CancelRules`:
  - `static boolean movedTooFar(Location start, Location now, double max)`: solo X y Z, en el mismo mundo. Un mundo distinto cuenta como `true`.
  - `static boolean cancels(Emote emote, EmoteSession s, CancelReason r)`: false si `s.magic()`, si no `emote.cancel().contains(r)`.
- `CancelListener` escucha estos eventos:
  - `PlayerMoveEvent` (solo si hay sesión; si no, retorno inmediato).
  - `EntityDamageEvent` (daño recibido → DAMAGE y combate si el atacante es jugador o mob).
  - `EntityDamageByEntityEvent` (atacante jugador → ATTACK y combate).
  - `EntityMountEvent` y `VehicleEnterEvent` (MOUNT).
  - `EntityToggleGlideEvent`, `EntityToggleSwimEvent`, `PlayerBedEnterEvent`, `PlayerTeleportEvent` (se para siempre), `PlayerChangedWorldEvent` y `PlayerDeathEvent` (se para siempre).
  - `PlayerQuitEvent`: `clear` y `store.save` + `unload`.
  - `PlayerJoinEvent`: `store.load`.

- [ ] **Paso 1: tests que fallan**:
  - `headTurnIsNotMovement`: misma X y Z, otro yaw y pitch → false.
  - `jumpInPlaceIsNotMovement`: Y +1 → false.
  - `horizontalBeyond03Cancels`: 0.31 → true, 0.29 → false.
  - `otherWorldCancels`.
  - `magicSessionsIgnoreCancelReasons`.
  - `emoteWithoutMoveInCancelSurvivesMove`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*CancelRulesTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar y registrar el listener en `EmotesPlugin`.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): cortes por movimiento, daño y estado`

### Tarea 8: puente con Magic

**Archivos:**
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/magic/MagicBridge.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/magic/MagicBridgeTest.java`

**Interfaces:**
- Consume:
  - (Tarea 1) los tres eventos.
  - (Tarea 6) `EmoteService.force` y `stop`. Hace falta una variante interna `forceMagic(Player, String emoteId)` que cree la sesión con `magic=true`; se añade a `EmoteService` en esta tarea con su test.
  - (Tarea 3) `EmotesSettings.castEmoteFor` y `channelEmoteFor`.
- Produce: `MagicBridge(EmoteService, EmoteCatalog, Supplier<EmotesSettings>)` es un `Listener`, con `String chooseCast(String schoolId, double horizontalSpeed)`. Si la velocidad es mayor que `walkingSpeed` y existe `<id>_caminando`, usa esa variante; si no, `<id>`.

- [ ] **Paso 1: tests que fallan**:
  - `castUsesSchoolMappingOrDefault`.
  - `walkingVariantWhenMovingAndExists`: 0.06 → `conjurar_caminando`; 0.04 → `conjurar`.
  - `walkingVariantMissingFallsBack`.
  - `channelStartPlaysLoopAndEndStops`.
  - `castReplacesLoopAndLoopDoesNotResume` (punto 2): con meditar en marcha, un `SpellCastEvent` deja la sesión en conjurar, y al terminar conjurar no hay sesión.
  - `disabledBridgeDoesNothing` (`magic.enabled: false`).
  - `magicIgnoresLockAndCooldown`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*MagicBridgeTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar y registrar siempre en `onEnable`. Los eventos están en la Lib, así que no hay nada que comprobar.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): canalizar y conjurar con RPGRoll-Magic`

### Tarea 9: comandos

**Archivos:**
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/command/EmoteCommand.java` y `EmotesAdminCommand.java`
- Modificar: `emotes/src/main/resources/lang/*.yml` (mensajes de cada `LaunchResult` y de los comandos)
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/command/EmoteCommandsTest.java`

**Interfaces:**
- Consume (Tareas 4-6): el catálogo, el almacén y el servicio.
- Produce:
  - `/emote <id|1-7|stop>` (`rpgrollemotes.use`, alias `e`). Sin argumento abre el menú (Tarea 10).
  - `/emotes` (abre el menú).
  - `/emotesadmin give|take|list|play|reload` (`rpgrollemotes.admin`), según el spec. Funciona desde la consola usando `Senders.recipient` de la Lib donde aplique.
  - Mensajes por la barra de acción para `LaunchResult` y por el chat para admin.

- [ ] **Paso 1: tests que fallan**:
  - `suggestsOnlyOwnedVisibleEmotesPlusStopAndSlots`.
  - `adminSuggestsSubcommandsThenPlayersThenAllIds`.
  - `giveFromConsoleUnlocksAndSaves`.
  - `takeLocks`.
  - `playForcesForOnlinePlayer`.
  - `unknownIdGivesMessage`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*EmoteCommandsTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar y registrar con `BrigadierCommands.register`.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): comandos y autocompletado`

### Tarea 10: menú

**Archivos:**
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/gui/EmotesMenu.java`, `EmotesMenus.java` (skin y relleno, como `PassMenus`) y `MenuLayout.java`
- Modificar: `extras/src/main/resources/menus/servidor.yml` (botón «Emotes» con la acción de comando `emotes` en un hueco libre; si Emotes no está, el comando no existe y Extras ya muestra su error estándar)
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/gui/MenuLayoutTest.java`

**Interfaces:**
- Consume (Tareas 4-6 y 9): todo lo anterior.
- Produce `MenuLayout`, que es lógica pura:
  - `static List<Emote> page(List<Emote> list, int page, int perPage)`, con `perPage = 28`.
  - `static int pages(int size, int perPage)` (mínimo 1).
  - Huecos: pestañas de categoría en la fila 0, contenido en las filas 1-4 (columnas 1-7) y favoritos en la fila 5. Las constantes `FAVORITE_SLOTS` y `CATEGORY_SLOTS` están en `MenuLayout`.
- `EmotesMenu` extiende `InventoryGUI` (54 huecos) con fondo `MenuSkin` y el id de fondo `emotes_menu`.
  - **Emote:** clic izquierdo lo lanza y cierra; clic derecho lo pone o quita de favoritos y refresca.
  - **Bloqueado:** se ve en gris con la línea `howToUnlock`.
  - **Botones:** página anterior y siguiente, y «Parar emote».

- [ ] **Paso 1: tests que fallan**:
  - `pagesOf28`: 30 → 2 páginas; la página 1 tiene 2.
  - `zeroEmotesIsOnePage`.
  - `slotsDoNotOverlap`: los huecos de pestañas, contenido, favoritos y botones son disjuntos y están dentro de 0..53.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*MenuLayoutTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar el menú y el botón de Extras.
- [ ] **Paso 4:** `./gradlew :emotes:test :extras:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): menú de emotes y botón en el menú del servidor`

### Tarea 11: animaciones, catálogo de fábrica e instalador

**Archivos:**
- Crear:
  - `tools/emotes/construir_emotes.py`, a partir de `E:\Dev\scratchpad\emotes-20261119\emotes.py`.
  - `tools/emotes/plantilla/steve.bbmodel`: copia de la plantilla de BetterModel (MIT), con una nota de atribución en `tools/emotes/LEEME.md`.
- Generar:
  - `emotes/src/main/resources/animations/rpgroll_emotes.bbmodel`.
  - `emotes/src/main/resources/emotes/<categoría>/<id>.yml` para los 22 del spec más `canalizar`.
- Crear:
  - `emotes/src/main/java/com/sack/rpgroll/emotes/install/AnimationInstaller.java`.
  - Test: `emotes/src/test/java/com/sack/rpgroll/emotes/install/AnimationInstallerTest.java` y `emotes/src/test/java/com/sack/rpgroll/emotes/catalog/BundledCatalogTest.java`.

**Interfaces:**
- **Generador:** `python -I tools/emotes/construir_emotes.py [--salida <carpeta>]` escribe el `.bbmodel` (modelo `rpgroll_emotes`) y los `.yml`.
  - Usa las convenciones de eje del spec.
  - Duraciones: emotes cortos de 1,5 a 3 s; conjurar 0,8 s; los bucles con `loop: true` en el `.yml` y `"loop": "loop"` en la animación.
  - `conjurar` y `canalizar` llevan `hidden: true` y `category: magia`.
- **`AnimationInstaller(File pluginData, File betterModelPlayers, Consumer<String> warn)`**, con `void install()`:
  - Copia `animations/*.bbmodel` (del jar y de `plugins/RPGRoll-Emotes/animations/`, incluidos los de packs).
  - Guarda hashes en `data/installed-hashes.yml`. Reemplaza si el destino no existe o su hash es el que instaló el plugin. Si el admin lo editó, no lo toca y avisa.
  - Después: `BetterModelBukkit.platform().reload()` desde `EmotesPlugin`, fuera del instalador, para que el test no necesite BetterModel.

- [ ] **Paso 1: tests que fallan**:
  - `AnimationInstallerTest` (`@TempDir`):
    - `copiesWhenMissing`.
    - `replacesWhenUnchangedSinceLastInstall`.
    - `keepsAndWarnsWhenAdminEdited`.
    - `copiesPackAnimationsToo`.
  - `BundledCatalogTest`:
    - Lee los recursos reales y parsea los `.bbmodel` con Gson (ya en Paper).
    - Comprueba que hay 23 emotes (22 + canalizar).
    - Comprueba que cada `animation` existe en el `.bbmodel` y que los `loop` coinciden.
    - Comprueba que existen `conjurar_caminando` y `canalizar`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*AnimationInstallerTest" --tests "*BundledCatalogTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar el instalador, ampliar el generador con los 23 emotes y ejecutarlo. Engancharlo en `onEnable` en este orden:
  1. `ResourceCopier.copyDirectories(List.of("emotes", "animations"))`, para que los de fábrica queden en `plugins/RPGRoll-Emotes/` junto a los que añaden los packs.
  2. Instalar.
  3. Recargar BetterModel.
  4. Cargar el catálogo desde `plugins/RPGRoll-Emotes/emotes/`.
  5. `checkAnimations(engine::exists, ...)`.
- [ ] **Paso 4:** `./gradlew :emotes:test`. Esperado: PASA.
- [ ] **Paso 5: revisión en el juego** en el servidor `emotes-qa` (ver la Tarea 13 para montarlo). Mirar cada animación con el cliente de Sack en vista frontal y lateral, y corregir la que se vea mal. Sin jars nuevos todavía: se cargan los `.bbmodel` con `/bm play rpgroll_emotes <anim>`.
- [ ] **Paso 6: commit** `feat(emotes): 23 animaciones, catálogo e instalador`

### Tarea 12: emotes de los packs y pack de recursos en SRP

**Archivos:**
- Crear, en `packs/mareas-malditas/RPGRoll-Emotes/` y `packs/dinastia-de-jade/RPGRoll-Emotes/`: `animations/<pack>_emotes.bbmodel` y `emotes/*.yml`.
  - Mareas: `pirata_hornpipe` (bucle) y `mareas_catalejo`.
  - Jade: `jade_saludo_marcial` y `jade_postura_grulla` (bucle).
  - Todos `locked`, con `how-to-unlock`.
- Modificar: las recompensas de cada pack, por ejemplo el rango de cada pack con `command: emotesadmin give {player} <id>`. Se ponen en la misión o el rango más fiel al tema; anotar cuál en el registro. Modificar también `packs/<pack>/LEEME.md`.
- Modificar: `sackresourcepack/src/main/java/com/sack/rpgroll/sackresourcepack/api/AssetRegistrationService.java`, para añadir `boolean registerArchive(String moduleId, File zip, String namespace, int priority, String description)`.
- Crear: `emotes/src/main/java/com/sack/rpgroll/emotes/pack/SrpBridge.java`
- Test: `emotes/src/test/java/com/sack/rpgroll/emotes/ContentPacksEmotesTest.java` y `sackresourcepack/src/test/java/com/sack/rpgroll/sackresourcepack/api/RegisterArchiveTest.java`

**Interfaces:**
- **`registerArchive`:**
  - Vacía `content/<moduleId>/` y copia las entradas del zip, menos `pack.mcmeta` y `pack.png`.
  - Escribe `pack.yml` con `id`, `name` = `moduleId`, `version: '1.0'`, `author: 'BetterModel'`, `namespace`, `priority` y `description`, con el formato del `pack.yml` de Furniture.
  - Devuelve false si el zip no existe.
- **`SrpBridge(Plugin emotes)`** en `EmotesPlugin` si SRP está activo y `resource-pack.register-in-sackresourcepack: true`. Hace dos cosas:
  - `BetterModelBukkit.platform().addReloadEndHandler(r -> sync())`.
  - `sync()` lee `plugins/BetterModel/<build-folder-location>.zip` y llama a `registerArchive("bettermodel", zip, "bettermodel", 20, "Modelos y emotes de BetterModel")`.
  - La ruta sale de `BetterModel.config()`; si no se puede leer, `BetterModel/build.zip`.
  - Avisa en el log de aplicar con `/srp publish`. No se usa `srp rebuild`: deja el hash viejo y el cliente no descarga el pack.

- [ ] **Paso 1: tests que fallan**:
  - `ContentPacksEmotesTest`: todos los `packs/**/RPGRoll-Emotes/emotes/*.yml` se parsean con `EmoteParser`, y su `animation` existe en el `.bbmodel` de su pack.
  - `RegisterArchiveTest`:
    - `copiesAssetsAndWritesPackYml`.
    - `skipsMcmetaAndPng`.
    - `clearsOldFiles`.
    - `missingZipReturnsFalse`.
- [ ] **Paso 2:** `./gradlew :emotes:test --tests "*ContentPacksEmotesTest" :sackresourcepack:test --tests "*RegisterArchiveTest"`. Esperado: FALLA.
- [ ] **Paso 3:** implementar. Generar las animaciones de los packs con `construir_emotes.py --pack <pack>`.
- [ ] **Paso 4:** `./gradlew :emotes:test :sackresourcepack:test`. Esperado: PASA.
- [ ] **Paso 5: commit** `feat(emotes): emotes de Mareas y Jade, y pack de BetterModel en SackResourcePack`

### Tarea 13: prueba real en `emotes-qa`

**Archivos:**
- Crear: `E:\Dev\scratchpad\emotes-20261119\QA.md` (resultados con capturas)

- [ ] **Paso 1: pedir permiso a Sack para compilar** los jars de Lib (`:common`), Magic, SackResourcePack, Emotes, core y Extras, porque Magic exige core. Esperar su sí.
- [ ] **Paso 2: montar el servidor de prueba.**
  - Instalar en `emotes-qa` (Paper 26.1.2 + ViaVersion + BetterModel 3.6.0): esos jars y FMM 2.14.0, como en main.
  - Quitar `players/emotes.bbmodel` de la prueba anterior.
  - Arrancar. Esperado: `✔ RPGRoll-Emotes habilitado`, ningún aviso de animación faltante y BetterModel recargado con `rpgroll_emotes`.
- [ ] **Paso 3: probar con el cliente 26.3 de Sack, en modo online.**
  - El menú, con pestañas, favoritos y bloqueados.
  - `/emote` con sus sugerencias.
  - `give` y `take` desde la consola.
  - Cortes: caminar, recibir daño, montar y teletransportarse.
  - Enfriamiento.
  - Un hechizo instantáneo quieto (conjurar) y caminando (conjurar_caminando).
  - Meteor: canalizar, cancelar moviéndose, y completar para ver conjurar.
  - Un hechizo con meditar en marcha.
  - Ítems de FMM y muebles visibles a la vez, para descartar el choque del ítem base.
- [ ] **Paso 4: pack.** Con SRP instalado: `/bm reload`, `/srp publish`. El cliente descarga el pack y los emotes y modelos de RPGRoll se ven a la vez.
- [ ] **Paso 5: rendimiento.** Con 5 bots (los scripts de bots de las pruebas de RPGRoll) y emotes en bucle a la vez, `spark tps` y `spark profiler` durante 60 s. Esperado: TPS 20 y BetterModel por debajo del 5 % del tick.
- [ ] **Paso 6: Velocity.** Si se puede montar en local un Velocity con forwarding moderno delante de `emotes-qa`, comprobar la skin. Si no, anotarlo como pendiente para el despliegue en main.
- [ ] **Paso 7:** anotar los resultados en `QA.md`, corregir lo que falle (cada arreglo con su test, si se puede) y pasar `./gradlew test`. Esperado: suite completa en verde.

### Tarea 14: documentación

**Archivos:**
- Crear: `emotes/README.md` con la guía para el comprador:
  - instalar BetterModel;
  - el pack sin SRP;
  - formato del `.yml`;
  - comandos, permisos y desbloqueos desde pase, cajas, misiones y NPC;
  - integración con Magic;
  - límites: Bedrock y clientes anteriores a 1.21.4.
- Modificar: `README.md` de la raíz (módulo nuevo en la lista) y `tools/emotes/LEEME.md` (cómo añadir un emote y las convenciones de ejes).
- [ ] **Paso 1:** escribir los documentos.
- [ ] **Paso 2:** `./gradlew test`. Esperado: verde.
- [ ] **Paso 3: commit** `docs(emotes): guía del módulo`

Fuera de este plan, y de Sack: la subida de versión y la release (Lib, Magic, SRP, Extras, Emotes), la ficha y el precio en la tienda, la página en RPGRollDocs y el despliegue en main.
