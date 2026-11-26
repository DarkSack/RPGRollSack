package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.structures.api.BuiltStructure;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectKind;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ciclo de vida de los módulos sobre las construcciones terminadas. Todo en el hilo principal; cada llamada a un
 * módulo va envuelta (también {@link LinkageError}: un módulo con una dependencia que falta, como MythicMobs, lanza
 * {@code NoClassDefFoundError}): si lanza, se avisa una vez por construcción y método y las demás siguen.
 *
 * <p>En memoria están todas las construcciones; su estructura y su módulo se buscan en cada llamada, así que una
 * recarga del catálogo (o una estructura que entra tarde) se aplica sola. Una construcción cuya estructura ya no está
 * en el catálogo, o sin {@code module:}, no hace nada.
 */
public final class ModuleService {

    /** Clave genérica cuando el módulo no está o su {@code precheck} falla. */
    public static final String DENIED = "build.module-denied";

    /** ¿Está cargado el chunk de esa posición? */
    @FunctionalInterface
    public interface Chunks {
        boolean loaded(String world, int x, int z);
    }

    private final ModuleRegistry registry;
    private final Function<String, Optional<StructureDefinition>> defs;
    private final ProjectRepository repo;
    private final Chunks chunks;
    private final Consumer<Runnable> main;
    private final Logger log;
    private final Map<UUID, BuildRecord> builds = new LinkedHashMap<>();
    /** "construcción:método" (o "estructura:precheck") ya avisados. */
    private final Set<String> warned = new HashSet<>();
    private long ticks;

    /** @param main vuelve al hilo principal desde el de la base de datos */
    public ModuleService(ModuleRegistry registry, Function<String, Optional<StructureDefinition>> defs,
                         ProjectRepository repo, Chunks chunks, Consumer<Runnable> main, Logger log) {
        this.registry = registry;
        this.defs = defs;
        this.repo = repo;
        this.chunks = chunks;
        this.main = main;
        this.log = log;
    }

    /** Al arrancar: todas las construcciones de la base de datos, sin repetir {@code onBuilt}. */
    public void load() {
        repo.allBuilds().whenComplete((list, e) -> main.accept(() -> {
            if (e != null) {
                log.log(Level.SEVERE, "No se pudieron leer las construcciones; sus módulos no funcionan hasta"
                        + " reiniciar", e);
                return;
            }
            list.forEach(r -> builds.putIfAbsent(r.id(), r));
        }));
    }

    /** Construcción recién registrada en la base de datos. */
    public void onBuilt(BuildRecord r) {
        builds.put(r.id(), r);
        call(r, "onBuilt", StructureModule::onBuilt);
    }

    /**
     * Aviso del motor con la obra terminada y su construcción ya en la base de datos. Una reparación ({@code built}
     * null) no hace nada: su módulo ya estaba. Una mejora saca de la memoria la construcción vieja
     * ({@link Project#buildId()}, ya borrada en la misma transacción), llama a su {@code onRemoved} y después al
     * {@code onBuilt} de la nueva.
     */
    public void completed(Project p, BuildRecord built) {
        if (built == null) {
            return;
        }
        if (p.kind() == ProjectKind.UPGRADE && p.buildId() != null) {
            BuildRecord old = builds.remove(p.buildId());
            if (old != null) {
                call(old, "onRemoved", StructureModule::onRemoved);
            }
        }
        onBuilt(built);
    }

    /** Cada tick del servidor: {@code onTick} según {@code tickEvery} y con el chunk del origen cargado. */
    public void tick() {
        ticks++;
        // Copia: un módulo puede llamar a remove/onBuilt desde onTick.
        for (BuildRecord r : List.copyOf(builds.values())) {
            if (!builds.containsKey(r.id())) {
                continue; // borrada en este mismo tick
            }
            try {
                Optional<StructureDefinition> def = defs.apply(r.structure());
                Optional<StructureModule> m = def.flatMap(d -> registry.get(d.module()));
                if (m.isEmpty()) {
                    continue;
                }
                int every = m.get().tickEvery();
                if (every <= 0 || ticks % every != 0 || !chunks.loaded(r.world(), r.x(), r.z())) {
                    continue;
                }
                m.get().onTick(new BuiltStructure(r, def.get(), def.get().moduleConfig()));
            } catch (RuntimeException | LinkageError ex) {
                warnOnce(r.id() + ":onTick", "El módulo de la construcción " + r.id() + " (" + r.structure()
                        + ") falló en onTick", ex);
            }
        }
    }

    /**
     * Borra la fila de la construcción y, si la había, llama a {@code onRemoved}; los bloques se quedan. El futuro se
     * completa en el hilo principal: true si había fila.
     */
    public CompletableFuture<Boolean> remove(UUID buildId) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        CompletableFuture<Boolean> f;
        try {
            f = repo.deleteBuild(buildId);
        } catch (RuntimeException e) {
            f = CompletableFuture.failedFuture(e);
        }
        f.whenComplete((ok, e) -> main.accept(() -> {
            if (e != null) {
                out.completeExceptionally(e);
                return;
            }
            BuildRecord r = builds.remove(buildId);
            if (Boolean.TRUE.equals(ok) && r != null) {
                call(r, "onRemoved", StructureModule::onRemoved);
            }
            out.complete(Boolean.TRUE.equals(ok));
        }));
        return out;
    }

    /**
     * Al confirmar y al arrancar una obra: clave de idioma si su módulo no deja construir ahí. Sin módulo, vacío; con
     * un módulo que no está o que lanza, {@link #DENIED} (lado seguro).
     */
    public Optional<String> precheck(StructureDefinition def, UUID owner, String world, Box box) {
        if (def.module() == null) {
            return Optional.empty();
        }
        Optional<StructureModule> m = registry.get(def.module());
        if (m.isEmpty()) {
            return Optional.of(DENIED);
        }
        try {
            Optional<String> why = m.get().precheck(owner, world, box, def.moduleConfig());
            return why == null ? Optional.empty() : why;
        } catch (RuntimeException | LinkageError e) {
            warnOnce(def.id() + ":precheck", "El módulo de la estructura " + def.id() + " falló en precheck", e);
            return Optional.of(DENIED);
        }
    }

    private void call(BuildRecord r, String method, BiConsumer<StructureModule, BuiltStructure> fn) {
        try {
            Optional<StructureDefinition> def = defs.apply(r.structure());
            Optional<StructureModule> m = def.flatMap(d -> registry.get(d.module()));
            if (m.isPresent()) {
                fn.accept(m.get(), new BuiltStructure(r, def.get(), def.get().moduleConfig()));
            }
        } catch (RuntimeException | LinkageError e) {
            warnOnce(r.id() + ":" + method, "El módulo de la construcción " + r.id() + " (" + r.structure()
                    + ") falló en " + method, e);
        }
    }

    private void warnOnce(String key, String msg, Throwable e) {
        if (warned.add(key)) {
            log.log(Level.WARNING, msg + " (no se vuelve a avisar)", e);
        }
    }
}
