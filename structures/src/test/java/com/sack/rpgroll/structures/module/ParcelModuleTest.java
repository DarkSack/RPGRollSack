package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ParcelModuleTest {

    /** Terrenos de prueba: apunta lo creado y lo borrado; {@code deny} es lo que devuelve canCreate. */
    static class FakeClaims implements ParcelClaims {
        Optional<String> deny = Optional.empty();
        RuntimeException createError, removeError;
        final List<String> created = new ArrayList<>();
        final List<String> removed = new ArrayList<>();

        @Override public Optional<String> canCreate(UUID owner, String world, Box box) { return deny; }

        @Override public Optional<String> create(UUID owner, String world, Box box, String name) {
            if (createError != null) {
                throw createError;
            }
            created.add(name);
            return Optional.of("42");
        }

        @Override public void remove(String world, String createdId) {
            if (removeError != null) {
                throw removeError;
            }
            removed.add(world + "/" + createdId);
        }
    }

    final Set<Hook> active = EnumSet.of(Hook.GRIEFPREVENTION, Hook.WORLDGUARD);
    final Map<Hook, FakeClaims> claims = new EnumMap<>(Map.of(Hook.GRIEFPREVENTION, new FakeClaims(),
            Hook.WORLDGUARD, new FakeClaims()));
    final List<String> broken = new ArrayList<>();
    final List<String> told = new ArrayList<>();
    final ProjectRepository repo = mock(ProjectRepository.class);
    final List<String> warnings = new ArrayList<>();
    final Logger log = Logger.getAnonymousLogger();
    final ParcelModule module = new ParcelModule(active::contains, claims::get, (h, why) -> broken.add(h.key()),
            repo, Runnable::run, (who, key) -> told.add(who + ":" + key), log);

    ParcelModuleTest() {
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
    }
    final UUID owner = UUID.randomUUID();
    final Box box = new Box(0, 64, 0, 9, 70, 4);

    static ConfigurationSection config(String claim) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("type", "parcel");
        y.set("claim", claim);
        return y;
    }

    BuiltStructure built(String claim) {
        BuildRecord r = new BuildRecord(UUID.fromString("abcdef12-0000-0000-0000-000000000000"), UUID.randomUUID(),
                "parcela", owner, "world", 0, 64, 0, Rotation.NONE, box, 1L);
        return new BuiltStructure(r, null, config(claim));
    }

    @Test void validatesClaimValue() {
        assertTrue(module.validate(config("griefprevention")).isEmpty());
        assertTrue(module.validate(config("worldguard")).isEmpty());
        assertTrue(module.validate(config("none")).isEmpty());
        assertEquals("claim", module.validate(config("towny")).get(0).key());
    }

    @Test void enoughBlocksComparesWithTheXzArea() {
        assertTrue(ParcelClaims.enoughBlocks(50, box)); // 10 × 5
        assertFalse(ParcelClaims.enoughBlocks(49, box));
    }

    @Test void precheckRejectsNotEnoughClaimBlocks() {
        claims.get(Hook.GRIEFPREVENTION).deny = Optional.of(ParcelModule.NOT_ENOUGH_BLOCKS);
        assertEquals(Optional.of(ParcelModule.NOT_ENOUGH_BLOCKS),
                module.precheck(owner, "world", box, config("griefprevention")));
        assertEquals(Optional.empty(), module.precheck(owner, "world", box, config("worldguard")));
        assertEquals(Optional.empty(), module.precheck(owner, "world", box, config("none")));
    }

    @Test void precheckRejectsWhenIntegrationInactive() {
        active.remove(Hook.WORLDGUARD);
        assertEquals(Optional.of(ParcelModule.INACTIVE), module.precheck(owner, "world", box, config("worldguard")));
        assertEquals(Optional.empty(), module.precheck(owner, "world", box, config("griefprevention")));
    }

    @Test void precheckMarksBrokenWhenTheAdapterThrows() {
        ParcelClaims boom = new FakeClaims() {
            @Override public Optional<String> canCreate(UUID o, String w, Box b) { throw new IllegalStateException(); }
        };
        ParcelModule m = new ParcelModule(active::contains, h -> boom, (h, why) -> broken.add(h.key()), repo,
                Runnable::run, (who, key) -> told.add(key), Logger.getAnonymousLogger());
        assertEquals(Optional.of(ParcelModule.ERROR), m.precheck(owner, "world", box, config("worldguard")));
        assertEquals(List.of("worldguard"), broken);
    }

    @Test void onBuiltStoresCreatedId() {
        when(repo.saveModuleData(any(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        BuiltStructure b = built("griefprevention");
        module.onBuilt(b);
        assertEquals(List.of("rpgroll_parcel_abcdef12"), claims.get(Hook.GRIEFPREVENTION).created);
        verify(repo).saveModuleData(b.record().id(), "griefprevention:42");
        assertTrue(told.isEmpty());
    }

    @Test void onBuiltWithClaimNoneDoesNothing() {
        module.onBuilt(built("none"));
        verifyNoInteractions(repo);
        assertTrue(claims.get(Hook.GRIEFPREVENTION).created.isEmpty());
    }

    @Test void onBuiltFailureKeepsBuild() {
        claims.get(Hook.WORLDGUARD).createError = new IllegalStateException("roto");
        assertDoesNotThrow(() -> module.onBuilt(built("worldguard")));
        assertEquals(List.of(owner + ":" + ParcelModule.FAILED), told);
        assertEquals(List.of("worldguard"), broken);
        verify(repo, never()).deleteBuild(any());
        verify(repo, never()).saveModuleData(any(), anyString());
    }

    @Test void onBuiltDeniedTellsTheReasonAndCreatesNothing() {
        claims.get(Hook.GRIEFPREVENTION).deny = Optional.of(ParcelModule.OVERLAPS_CLAIM);
        module.onBuilt(built("griefprevention"));
        assertEquals(List.of(owner + ":" + ParcelModule.OVERLAPS_CLAIM), told);
        assertTrue(claims.get(Hook.GRIEFPREVENTION).created.isEmpty());
        verify(repo, never()).saveModuleData(any(), anyString());
    }

    @Test void onRemovedDeletesStoredClaim() {
        BuiltStructure b = built("griefprevention");
        // Guardado con WorldGuard: manda lo guardado, no el YAML de ahora.
        when(repo.moduleData(b.record().id()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of("worldguard:rpgroll_parcel_abcdef12")));
        when(repo.deleteModuleData(any())).thenReturn(CompletableFuture.completedFuture(null));
        module.onRemoved(b);
        assertEquals(List.of("world/rpgroll_parcel_abcdef12"), claims.get(Hook.WORLDGUARD).removed);
        assertTrue(claims.get(Hook.GRIEFPREVENTION).removed.isEmpty());
        verify(repo).deleteModuleData(b.record().id());
    }

    @Test void onRemovedWithIntegrationInactiveWarnsAndRemovesNothing() {
        active.remove(Hook.GRIEFPREVENTION);
        BuiltStructure b = built("griefprevention");
        when(repo.moduleData(b.record().id()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of("griefprevention:42")));
        module.onRemoved(b);
        assertTrue(claims.get(Hook.GRIEFPREVENTION).removed.isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("griefprevention:42") && w.contains("a mano")),
                warnings.toString());
        verify(repo, never()).deleteModuleData(any());
    }

    @Test void onBuiltRefusedByThePluginFailsOnlyThisParcel() {
        claims.get(Hook.GRIEFPREVENTION).createError = new ParcelClaims.Refused(new RuntimeException("evento"));
        module.onBuilt(built("griefprevention"));
        assertEquals(List.of(owner + ":" + ParcelModule.FAILED), told);
        assertTrue(broken.isEmpty(), "no se apaga la integración entera");
        verify(repo, never()).saveModuleData(any(), anyString());
    }

    @Test void onRemovedRefusedKeepsTheIntegrationAndTheRow() {
        claims.get(Hook.WORLDGUARD).removeError = new ParcelClaims.Refused(new RuntimeException("no"));
        BuiltStructure b = built("worldguard");
        when(repo.moduleData(b.record().id()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of("worldguard:rpgroll_parcel_abcdef12")));
        module.onRemoved(b);
        assertTrue(broken.isEmpty());
        verify(repo, never()).deleteModuleData(any());
        assertFalse(warnings.isEmpty());
    }

    @Test void onRemovedReflectionFailureMarksBroken() {
        claims.get(Hook.WORLDGUARD).removeError = new IllegalStateException(new NoSuchMethodException("x"));
        BuiltStructure b = built("worldguard");
        when(repo.moduleData(b.record().id()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of("worldguard:rpgroll_parcel_abcdef12")));
        module.onRemoved(b);
        assertEquals(List.of("worldguard"), broken);
        verify(repo, never()).deleteModuleData(any());
    }

    static void boom() {
        throw new IllegalArgumentException("el plugin no quiere");
    }

    @Test void callTellsRefusalsFromReflectionFailures() throws Exception {
        java.lang.reflect.Method m = ParcelModuleTest.class.getDeclaredMethod("boom");
        ParcelClaims.Refused r = assertThrows(ParcelClaims.Refused.class,
                () -> ParcelClaims.call(() -> m.invoke(null)));
        assertInstanceOf(IllegalArgumentException.class, r.getCause());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ParcelClaims.call(() -> { throw new NoSuchMethodException("x"); }));
        assertInstanceOf(NoSuchMethodException.class, e.getCause());
        assertEquals("ok", ParcelClaims.call(() -> "ok"));
    }

    @Test void gpRulesFollowGriefPreventionByHand() {
        // Por defecto todo permite: mundo con claims, sin límites, sin cruces, bloques de sobra. La caja es 10 x 5.
        assertEquals(Optional.empty(), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 1, 1, false, 1000), box));
        assertEquals(Optional.of(ParcelModule.CLAIMS_DISABLED), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(false, 0, 3, 1, 1, false, 1000), box));
        assertEquals(Optional.of(ParcelModule.TOO_MANY_CLAIMS), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 3, 3, 1, 1, false, 1000), box));
        assertEquals(Optional.empty(), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 4, 3, 1, 1, false, 1000), box), "por debajo del límite");
        assertEquals(Optional.empty(), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 5, 50, false, 1000), box));
        assertEquals(Optional.of(ParcelModule.TOO_NARROW), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 6, 1, false, 1000), box));
        assertEquals(Optional.of(ParcelModule.TOO_SMALL), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 1, 51, false, 1000), box));
        assertEquals(Optional.of(ParcelModule.OVERLAPS_CLAIM), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 1, 1, true, 1000), box));
        assertEquals(Optional.of(ParcelModule.NOT_ENOUGH_BLOCKS), ParcelClaims.gpRules(
                new ParcelClaims.GpFacts(true, 0, 3, 1, 1, false, 49), box));
    }

    @Test void onRemovedWithoutStoredDataDoesNothing() {
        BuiltStructure b = built("griefprevention");
        when(repo.moduleData(b.record().id())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        module.onRemoved(b);
        assertTrue(claims.get(Hook.GRIEFPREVENTION).removed.isEmpty());
    }
}
