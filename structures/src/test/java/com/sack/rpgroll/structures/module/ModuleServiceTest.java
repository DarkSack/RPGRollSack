package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleServiceTest {

    /** Módulo de prueba: apunta cada llamada; lanza en onTick para las construcciones de {@code throwFor}. */
    static class FakeModule implements StructureModule {
        final String type;
        final int every;
        final List<String> calls = new ArrayList<>();
        final Set<UUID> throwFor = new HashSet<>();

        FakeModule(String type, int every) {
            this.type = type;
            this.every = every;
        }

        @Override public String type() { return type; }
        @Override public List<Problem> validate(ConfigurationSection config) { return List.of(); }
        @Override public void onBuilt(BuiltStructure b) { calls.add("built:" + b.record().id()); }
        @Override public int tickEvery() { return every; }

        @Override public void onTick(BuiltStructure b) {
            if (throwFor.contains(b.record().id())) {
                throw new IllegalStateException("roto");
            }
            calls.add("tick:" + b.record().id());
        }

        @Override public void onRemoved(BuiltStructure b) { calls.add("removed:" + b.record().id()); }

        long count(String prefix) {
            return calls.stream().filter(c -> c.startsWith(prefix)).count();
        }
    }

    final ModuleRegistry registry = new ModuleRegistry();
    final Map<String, StructureDefinition> defs = new HashMap<>();
    final ProjectRepository repo = mock(ProjectRepository.class);
    /** Chunks descargados, por "mundo:cx,cz". */
    final Set<String> unloaded = new HashSet<>();
    final List<LogRecord> logs = new ArrayList<>();
    final Logger log = Logger.getAnonymousLogger();

    ModuleServiceTest() {
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { logs.add(r); }
            @Override public void flush() { }
            @Override public void close() { }
        });
    }

    ModuleService service() {
        return new ModuleService(registry, id -> Optional.ofNullable(defs.get(id)), repo,
                (w, x, z) -> !unloaded.contains(w + ":" + (x >> 4) + "," + (z >> 4)), Runnable::run, log);
    }

    StructureDefinition def(String id, String module) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("type", module);
        StructureDefinition d = new StructureDefinition(id, Map.of(), "granjas", null, null, null, null, List.of(), 0,
                false, null, List.of(), 1, 1, null, "rpgrollstructures.build." + id, null, module,
                module == null ? null : cfg);
        defs.put(id, d);
        return d;
    }

    static BuildRecord build(String structure, int x) {
        return new BuildRecord(UUID.randomUUID(), UUID.randomUUID(), structure, UUID.randomUUID(), "world", x, 64, 0,
                Rotation.NONE, new Box(x, 64, 0, x + 3, 66, 3), 1L);
    }

    void ticks(ModuleService s, int n) {
        for (int i = 0; i < n; i++) {
            s.tick();
        }
    }

    long warnings() {
        return logs.stream().filter(r -> r.getLevel() == Level.WARNING).count();
    }

    @Test void onBuiltIsCalledAfterRegistration() {
        FakeModule m = new FakeModule("test", 1);
        registry.register(m);
        StructureDefinition d = def("granja", "test");
        def("casa", null);
        ModuleService s = service();
        BuildRecord b = build("granja", 0);
        s.onBuilt(b);
        s.onBuilt(build("casa", 100)); // sin módulo: nada
        assertEquals(List.of("built:" + b.id()), m.calls);
        s.tick();
        assertEquals(List.of("built:" + b.id(), "tick:" + b.id()), m.calls, "queda registrada para onTick");
        assertEquals(Optional.empty(), s.precheck(d, b.owner(), "world", b.box()));
    }

    @Test void throwingModuleDoesNotBreakOthers() {
        FakeModule m = new FakeModule("test", 1);
        registry.register(m);
        def("granja", "test");
        ModuleService s = service();
        BuildRecord bad = build("granja", 0), good = build("granja", 100);
        m.throwFor.add(bad.id());
        s.onBuilt(bad);
        s.onBuilt(good);
        ticks(s, 3);
        assertEquals(3, m.count("tick:" + good.id()), "la otra construcción sigue");
        assertEquals(0, m.count("tick:" + bad.id()));
        assertEquals(1, warnings(), "un solo aviso por construcción y método");
    }

    @Test void onTickRespectsTickEveryAndLoadedChunks() {
        FakeModule every3 = new FakeModule("lento", 3);
        FakeModule never = new FakeModule("quieto", 0);
        registry.register(every3);
        registry.register(never);
        def("lenta", "lento");
        def("quieta", "quieto");
        ModuleService s = service();
        BuildRecord near = build("lenta", 0), far = build("lenta", 1000);
        s.onBuilt(near);
        s.onBuilt(far);
        s.onBuilt(build("quieta", 2000));
        unloaded.add("world:" + (1000 >> 4) + ",0");
        ticks(s, 6);
        assertEquals(2, every3.count("tick:" + near.id()));
        assertEquals(0, every3.count("tick:" + far.id()), "chunk del origen sin cargar: sin onTick");
        assertEquals(0, never.count("tick:"), "tickEvery 0: sin onTick");
    }

    @Test void removeDeletesRowAndCallsOnRemoved() {
        FakeModule m = new FakeModule("test", 1);
        registry.register(m);
        def("granja", "test");
        ModuleService s = service();
        BuildRecord b = build("granja", 0);
        s.onBuilt(b);
        when(repo.deleteBuild(b.id())).thenReturn(CompletableFuture.completedFuture(true));
        assertTrue(s.remove(b.id()).join());
        assertEquals(1, m.count("removed:" + b.id()));
        s.tick();
        assertEquals(0, m.count("tick:"), "fuera de la memoria: ya no hay onTick");
        when(repo.deleteBuild(b.id())).thenReturn(CompletableFuture.completedFuture(false));
        assertFalse(s.remove(b.id()).join());
        assertEquals(1, m.count("removed:"), "sin fila no se vuelve a llamar");
    }

    @Test void loadRegistersExistingBuilds() {
        FakeModule m = new FakeModule("test", 1);
        registry.register(m);
        def("granja", "test");
        BuildRecord b = build("granja", 0);
        BuildRecord orphan = build("borrada", 100); // su estructura ya no está en el catálogo
        when(repo.allBuilds()).thenReturn(CompletableFuture.completedFuture(List.of(b, orphan)));
        ModuleService s = service();
        s.load();
        s.tick();
        assertEquals(List.of("tick:" + b.id()), m.calls, "al cargar no se repite onBuilt");
    }

    @Test void throwingPrecheckDenies() {
        FakeModule m = new FakeModule("test", 0) {
            @Override public Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection c) {
                throw new IllegalStateException("roto");
            }
        };
        registry.register(m);
        StructureDefinition d = def("granja", "test");
        ModuleService s = service();
        Box box = new Box(0, 0, 0, 1, 1, 1);
        assertEquals(Optional.of(ModuleService.DENIED), s.precheck(d, UUID.randomUUID(), "world", box));
        assertEquals(Optional.empty(), s.precheck(def("casa", null), UUID.randomUUID(), "world", box));
    }

    @Test void moduleThatRemovesItsBuildInOnTickDoesNotStopTheOthers() {
        ModuleService[] holder = new ModuleService[1];
        BuildRecord self = build("granja", 0), other = build("granja", 100);
        FakeModule m = new FakeModule("test", 1) {
            @Override public void onTick(BuiltStructure b) {
                super.onTick(b);
                if (b.record().id().equals(self.id())) {
                    holder[0].remove(self.id()); // con main directo, borra de memoria en pleno recorrido
                    holder[0].onBuilt(build("granja", 200));
                }
            }
        };
        registry.register(m);
        def("granja", "test");
        when(repo.deleteBuild(self.id())).thenReturn(CompletableFuture.completedFuture(true));
        ModuleService s = service();
        holder[0] = s;
        s.onBuilt(self);
        s.onBuilt(other);
        s.tick();
        assertEquals(1, m.count("tick:" + other.id()), "la otra construcción hace su onTick en el mismo tick");
        assertEquals(1, m.count("removed:" + self.id()));
        assertEquals(0, warnings());
        s.tick();
        assertEquals(2, m.count("tick:" + other.id()));
        assertEquals(1, m.count("tick:" + self.id()), "la borrada ya no hace onTick");
    }

    @Test void linkageErrorIsCaughtAndWarnedOnce() {
        BuildRecord bad = build("granja", 0), good = build("granja", 100);
        FakeModule m = new FakeModule("test", 1) {
            @Override public void onTick(BuiltStructure b) {
                if (b.record().id().equals(bad.id())) {
                    throw new NoClassDefFoundError("io/lumine/mythic/api/MythicProvider");
                }
                super.onTick(b);
            }

            @Override public Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection c) {
                throw new NoClassDefFoundError("io/lumine/mythic/api/MythicProvider");
            }
        };
        registry.register(m);
        StructureDefinition d = def("granja", "test");
        ModuleService s = service();
        s.onBuilt(bad);
        s.onBuilt(good);
        ticks(s, 3);
        assertEquals(3, m.count("tick:" + good.id()));
        assertEquals(1, warnings(), "un aviso por construcción y método");
        assertEquals(Optional.of(ModuleService.DENIED), s.precheck(d, UUID.randomUUID(), "world", bad.box()));
    }

    @Test void registryRejectsDuplicateTypes() {
        registry.register(new FakeModule("test", 0));
        assertThrows(IllegalStateException.class, () -> registry.register(new FakeModule("TEST", 0)));
        assertTrue(registry.get("Test").isPresent());
        assertTrue(registry.get("otro").isEmpty());
    }
}
