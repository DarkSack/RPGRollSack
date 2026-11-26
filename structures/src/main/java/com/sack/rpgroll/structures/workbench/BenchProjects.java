package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.build.ConstructionEngine;
import com.sack.rpgroll.structures.build.ConstructionEngine.StartResult;
import com.sack.rpgroll.structures.deposit.ReturnService;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.ProjectState;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Las obras de cada mesa, para {@link WorkbenchService}: romperla con {@code break-policy: pause|cancel} y volver a
 * colocarla. Hilo principal.
 */
public final class BenchProjects implements ProjectsView {

    private final ProjectService projects;
    private final ConstructionEngine engine;
    private final ReturnService returns;
    private final IntSupplier refundPercent;
    private final Function<UUID, Player> online;
    private final BiConsumer<Player, String> send;

    /**
     * @param refundPercent {@code cancel-refund} de performance.yml, leído en cada cancelación
     * @param online        jugador conectado por UUID, o null
     * @param send          mensaje (clave de lang) a un jugador
     */
    public BenchProjects(ProjectService projects, ConstructionEngine engine, ReturnService returns,
                         IntSupplier refundPercent, Function<UUID, Player> online, BiConsumer<Player, String> send) {
        this.projects = projects;
        this.engine = engine;
        this.returns = returns;
        this.refundPercent = refundPercent;
        this.online = online;
        this.send = send;
    }

    @Override
    public boolean hasActiveProject(UUID bench) {
        return projects.hasActive(bench);
    }

    /** BUILDING → PAUSED con {@code project.paused.bench}; WAITING_MATERIALS y READY siguen igual, atadas a la mesa. */
    @Override
    public void benchRemoved(Player by, UUID bench) {
        projects.forBench(bench).ifPresent(p -> {
            engine.pause(p, ConstructionEngine.PAUSED_BENCH);
            Player owner = online.apply(p.owner());
            if (p.state() == ProjectState.PAUSED && owner != null && !owner.getUniqueId().equals(by.getUniqueId())) {
                send.accept(owner, ConstructionEngine.PAUSED_BENCH);
            }
        });
    }

    /** La devolución va al dueño de la obra (lo hace {@link ReturnService#cancel}), no a quien rompe. */
    @Override
    public CompletableFuture<Boolean> cancelFor(Player by, UUID bench) {
        Optional<Project> p = projects.forBench(bench);
        return p.isEmpty() ? CompletableFuture.completedFuture(true)
                : returns.cancel(by, p.get(), refundPercent.getAsInt());
    }

    /**
     * Solo la obra pausada por su mesa: {@code resume} sin bypass mira la protección con el dueño de la obra, nunca con
     * quien coloca. Se avisa al dueño, o a quien coloca si el dueño no está.
     */
    @Override
    public void benchPlaced(Player by, UUID bench) {
        Optional<Project> found = projects.forBench(bench)
                .filter(p -> p.state() == ProjectState.PAUSED && ConstructionEngine.PAUSED_BENCH.equals(p.error()));
        if (found.isEmpty()) {
            return;
        }
        StartResult r = engine.resume(found.get());
        String key = switch (r) {
            case STARTED -> "build.resumed";
            case PROTECTED -> ConstructionEngine.PAUSED_PROTECTED;
            default -> null; // ocupada o sin estructura: sigue en pausa y se reanuda a mano
        };
        if (key != null) {
            Player owner = online.apply(found.get().owner());
            send.accept(owner != null ? owner : by, key);
        }
    }
}
