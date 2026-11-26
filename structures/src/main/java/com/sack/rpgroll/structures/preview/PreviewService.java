package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.config.PerformanceSettings;
import com.sack.rpgroll.structures.integration.BedrockCheck;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vista previa de una estructura antes de construir: bloques falsos (como mucho {@code previewMaxBlocks}, los más
 * bajos primero) o solo contorno de partículas, validación del terreno y corte por distancia o tiempo.
 */
public final class PreviewService implements Listener {

    /** Posición de bloque en el mundo. */
    public record Pos(int x, int y, int z) {
    }

    /** Qué enviar (con el bloque de la estructura) y qué reponer. */
    public record Diff(Map<Pos, SourceBlock> send, Set<Pos> restore) {
    }

    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.WHITE, 1f);
    private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.LIME, 1.5f);
    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.RED, 1.5f);

    private final Supplier<PerformanceSettings> perf;
    private final LangManager lang;
    private final ProjectRepository repo;
    private final AreaGuard guard;
    private final Function<UUID, Optional<WorkbenchRecord>> benches;
    private final BedrockCheck bedrock;
    private final Logger log;
    private final Map<UUID, PreviewSession> sessions = new ConcurrentHashMap<>();
    /** Cajas de obras y construcciones por mundo, para el choque entre proyectos; se refresca fuera del hilo. */
    private final Map<String, List<Footprint>> boxes = new ConcurrentHashMap<>();
    /** Bloques que mira por tick la validación completa al confirmar. */
    static final int SCAN_BLOCKS_PER_TICK = 4096;

    /** Resultado de la validación completa con el sitio, el giro y la estructura que se validaron. */
    public record Checked(CheckResult result, PreviewSession session, Location origin, Rotation rotation,
                          String structure) {
    }

    /** Validación completa en curso (al confirmar), de una sesión en un sitio y giro concretos. */
    private record PendingScan(PreviewSession session, Location origin, Rotation rotation, TerrainCheck.Scan scan,
                               CompletableFuture<Checked> done) {
        boolean at(PreviewSession s) {
            return s.minCorner().equals(origin) && s.rotation() == rotation;
        }

        void finish(CheckResult r) {
            done.complete(new Checked(r, session, origin, rotation, session.def().id()));
        }
    }

    /** Por jugador; las avanza {@link #scanTick} (hilo principal). */
    private final Map<UUID, PendingScan> scans = new ConcurrentHashMap<>();
    private static final CheckResult GONE = new CheckResult(List.of(), "preview.expired");
    /** La vista se movió o giró mientras se validaba: lo validado no es donde está ahora. */
    private static final CheckResult MOVED = new CheckResult(List.of(), "preview.moved");

    public PreviewService(Supplier<PerformanceSettings> perf, LangManager lang, ProjectRepository repo,
                          AreaGuard guard, Function<UUID, Optional<WorkbenchRecord>> benches, BedrockCheck bedrock, Logger log) {
        this.perf = perf;
        this.lang = lang;
        this.repo = repo;
        this.guard = guard;
        this.benches = benches;
        this.bedrock = bedrock;
        this.log = log;
    }

    // ---- lógica pura (con pruebas) ----

    /**
     * Esquina mínima de la caja a 2 bloques delante de la mesa, hacia donde mira el jugador, al nivel de la mesa y
     * centrada en el eje. {@code w} y {@code l} ya girados.
     */
    static int[] initialOrigin(int bx, int by, int bz, BlockFace facing, int w, int l) {
        return switch (facing) {
            case NORTH -> new int[] { bx - w / 2, by, bz - 2 - (l - 1) };
            case SOUTH -> new int[] { bx - w / 2, by, bz + 2 };
            case EAST -> new int[] { bx + 2, by, bz - l / 2 };
            case WEST -> new int[] { bx - 2 - (w - 1), by, bz - l / 2 };
            default -> throw new IllegalArgumentException("Solo orientaciones cardinales: " + facing);
        };
    }

    /** Orientación cardinal horizontal según el yaw (0 = sur, 90 = oeste). */
    static BlockFace cardinal(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        if (y >= 315 || y < 45) {
            return BlockFace.SOUTH;
        }
        if (y < 135) {
            return BlockFace.WEST;
        }
        return y < 225 ? BlockFace.NORTH : BlockFace.EAST;
    }

    /**
     * Bloques falsos a enviar y posiciones previas a reponer. {@code blocks} ya girados y ORDENADOS de abajo arriba (lo hace la sesión);
     * solo se envían los primeros {@code cap}.
     */
    static Diff diff(List<SourceBlock> blocks, int ox, int oy, int oz, int cap, Set<Pos> sent) {
        Map<Pos, SourceBlock> send = new LinkedHashMap<>();
        for (SourceBlock b : blocks.subList(0, Math.min(cap, blocks.size()))) {
            send.put(new Pos(ox + b.x(), oy + b.y(), oz + b.z()), b);
        }
        Set<Pos> restore = new HashSet<>(sent);
        restore.removeAll(send.keySet());
        return new Diff(send, restore);
    }

    // ---- sesiones ----

    public void start(Player p, UUID bench, StructureDefinition def) {
        stop(p);
        Location at = benchLocation(p, bench);
        int[] o = initialOrigin(at.getBlockX(), at.getBlockY(), at.getBlockZ(), cardinal(p.getLocation().getYaw()),
                def.blocks().width(), def.blocks().length());
        sessions.put(p.getUniqueId(), new PreviewSession(def, bench,
                new Location(p.getWorld(), o[0], o[1], o[2])));
        refreshBoxes(p.getWorld().getName());
    }

    /** Vuelve a leer las cajas de un mundo (al crear un proyecto); las vistas de ese mundo se revalidan. */
    public void invalidate(String world) {
        // Se conserva la lista anterior hasta que llegue la nueva: así un choque no se cuela mientras se lee.
        refreshBoxes(world);
    }

    void refreshBoxes(String world) {
        refresh(world).exceptionally(err -> {
            log.log(Level.WARNING, "No se pudieron leer las cajas de " + world, err);
            return null;
        });
    }

    /**
     * Lee las cajas de un mundo; el futuro termina cuando ya están en la caché (confirmar espera esto antes de su
     * {@link #check} para que no se cuele una obra recién creada). Si falla, la lista anterior se queda.
     */
    public CompletableFuture<Void> refresh(String world) {
        return repo.boxesIn(world).thenAccept(list -> {
            boxes.put(world, list.stream().map(Footprint::of).toList());
            sessions.values().forEach(s -> s.dirty = true);
        });
    }

    /** Dónde está la mesa; si ya no existe, el bloque del jugador. */
    private Location benchLocation(Player p, UUID bench) {
        return benches.apply(bench).map(b -> new Location(p.getWorld(), b.x(), b.y(), b.z()))
                .orElseGet(() -> p.getLocation().getBlock().getLocation());
    }

    /** Mover o girar con una validación completa en curso la termina con {@code preview.moved}. */
    public void rotate(Player p) {
        session(p).ifPresent(PreviewSession::rotate);
        endScan(p.getUniqueId(), MOVED);
    }

    public void move(Player p, int dx, int dy, int dz) {
        session(p).ifPresent(s -> s.move(dx, dy, dz));
        endScan(p.getUniqueId(), MOVED);
    }

    public void toggleMode(Player p) {
        session(p).ifPresent(PreviewSession::toggleMode);
    }

    List<Footprint> boxesFor(String world) {
        return boxes.getOrDefault(world, List.of());
    }

    public Optional<PreviewSession> session(Player p) {
        return Optional.ofNullable(sessions.get(p.getUniqueId()));
    }

    /**
     * Validación barata, para cada movimiento o giro: mundo, bioma y cajas, y choques y suelo solo en los bloques que
     * se envían (los {@code previewMaxBlocks} más bajos). Sin vista, se puede. La completa es {@link #fullCheck}.
     */
    public CheckResult check(Player p) {
        PreviewSession s = sessions.get(p.getUniqueId());
        if (s == null) {
            return new CheckResult(List.of(), null);
        }
        List<SourceBlock> all = s.rotatedBlocks();
        return scan(p, s, all.subList(0, Math.min(perf.get().previewMaxBlocks(), all.size())), false)
                .step(Integer.MAX_VALUE);
    }

    /**
     * Validación de todos los bloques al confirmar, repartida en ticks ({@value #SCAN_BLOCKS_PER_TICK} por tick) por
     * {@link #scanTick}; el futuro termina en el hilo principal. Sin vista, se puede. Si la vista se cierra antes,
     * termina con {@code preview.expired}.
     */
    public CompletableFuture<Checked> fullCheck(Player p) {
        PreviewSession s = sessions.get(p.getUniqueId());
        if (s == null) {
            return CompletableFuture.completedFuture(new Checked(new CheckResult(List.of(), null), null, null, null,
                    null));
        }
        PendingScan running = scans.get(p.getUniqueId());
        if (running != null && running.session() == s && running.at(s)) {
            return running.done(); // segundo clic en Confirmar sin mover: la misma validación
        }
        if (running != null) {
            running.finish(MOVED);
        }
        PendingScan scan = new PendingScan(s, s.minCorner(), s.rotation(), scan(p, s, s.rotatedBlocks(), true),
                new CompletableFuture<>());
        scans.put(p.getUniqueId(), scan);
        return scan.done();
    }

    /**
     * La vista sigue siendo la misma, en el mismo sitio y con el mismo giro que cuando se validó: solo entonces se
     * crea la obra con ese resultado (si no, hay que confirmar otra vez).
     */
    public boolean unchanged(Player p, Checked c) {
        PreviewSession s = sessions.get(p.getUniqueId());
        return c.session() != null && s == c.session() && s.def().id().equals(c.structure())
                && s.minCorner().equals(c.origin()) && s.rotation() == c.rotation();
    }

    /** Hilo principal, cada tick: avanza las validaciones completas en curso. */
    public void scanTick() {
        for (Map.Entry<UUID, PendingScan> en : scans.entrySet()) {
            PendingScan ps = en.getValue();
            try {
                if (sessions.get(en.getKey()) != ps.session()) {
                    ps.finish(GONE);
                } else if (!ps.at(ps.session())) {
                    ps.finish(MOVED);
                } else {
                    CheckResult r = ps.scan().step(SCAN_BLOCKS_PER_TICK);
                    if (r == null) {
                        continue;
                    }
                    ps.finish(r);
                }
            } catch (RuntimeException ex) {
                ps.done().completeExceptionally(ex);
            }
            scans.remove(en.getKey(), ps);
        }
    }

    /** {@code protect}: solo la validación completa pregunta a las protecciones (una vez, por la caja entera). */
    private TerrainCheck.Scan scan(Player p, PreviewSession s, List<SourceBlock> blocks, boolean protect) {
        Location o = s.minCorner();
        World w = o.getWorld();
        Footprint f = s.footprint();
        return TerrainCheck.scan(w.getName(), w.getMinHeight(), w.getMaxHeight(), o.getBlockX(), o.getBlockY(),
                o.getBlockZ(), s.width(), s.height(), s.length(), blocks, s.def().placement(),
                (x, y, z) -> {
                    Block b = w.getBlockAt(x, y, z);
                    return new TerrainCheck.BlockInfo(b.getType(), b.isReplaceable(), b.isLiquid(),
                            b.isSolid(), () -> w.getBiome(x, y, z).getKey().toString());
                }, boxes.getOrDefault(w.getName(), List.of()),
                () -> !protect || p.hasPermission(WorkbenchService.PERM_BYPASS) ? null
                        : guard.check(p.getUniqueId(), w.getName(), new Box(f.minX(), f.minY(), f.minZ(), f.maxX(),
                                f.maxY(), f.maxZ())).map(AreaGuard.Denial::langKey).orElse(null));
    }

    /** Quita la vista: repone el bloque real de cada posición enviada. */
    public void stop(Player p) {
        PreviewSession s = sessions.remove(p.getUniqueId());
        endScan(p.getUniqueId());
        if (s != null) {
            restore(p, s.minCorner().getWorld(), s.sent);
            s.sent.clear();
        }
    }

    public void stopAll() {
        for (UUID id : List.copyOf(sessions.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                stop(p);
            }
        }
        sessions.clear();
        scans.keySet().forEach(this::endScan);
    }

    private void endScan(UUID player) {
        endScan(player, GONE);
    }

    private void endScan(UUID player, CheckResult why) {
        PendingScan ps = scans.remove(player);
        if (ps != null) {
            ps.finish(why);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sessions.remove(e.getPlayer().getUniqueId());
        endScan(e.getPlayer().getUniqueId());
    }

    // ---- tarea periódica (cada 10 ticks) ----

    public void tick() {
        PerformanceSettings cfg = perf.get();
        for (Map.Entry<UUID, PreviewSession> en : sessions.entrySet()) {
            Player p = Bukkit.getPlayer(en.getKey());
            PreviewSession s = en.getValue();
            if (p == null || !p.isOnline()) {
                sessions.remove(en.getKey());
                continue;
            }
            try {
                Location o = s.minCorner();
                boolean far = !p.getWorld().equals(o.getWorld())
                        || p.getLocation().distanceSquared(o) > (double) cfg.previewMaxDistance() * cfg.previewMaxDistance();
                if (far || System.currentTimeMillis() - s.startedAt() > cfg.previewSeconds() * 1000L) {
                    stop(p);
                    lang.send(p, "preview.expired");
                    continue;
                }
                if (s.dirty) {
                    s.last = check(p);
                    s.dirty = false;
                    sendBlocks(p, s, cfg);
                }
                particles(p, s);
            } catch (RuntimeException ex) {
                log.log(Level.WARNING, "Vista previa de " + p.getName() + " falló; se cierra.", ex);
                stop(p);
            }
        }
    }

    void sendBlocks(Player p, PreviewSession s, PerformanceSettings cfg) {
        Location o = s.minCorner();
        // Bedrock (Floodgate) no ve bien los bloques falsos: solo contorno, con un aviso por sesión.
        boolean outlineOnly = s.mode() == PreviewSession.Mode.OUTLINE || bedrock.isBedrock(p.getUniqueId());
        if (outlineOnly && s.mode() != PreviewSession.Mode.OUTLINE && !s.bedrockNotified) {
            s.bedrockNotified = true;
            lang.send(p, "preview.bedrock");
        }
        // En contorno no se envía nada: todo lo enviado antes se repone.
        Diff d = diff(s.rotatedBlocks(), o.getBlockX(), o.getBlockY(), o.getBlockZ(),
                outlineOnly ? 0 : cfg.previewMaxBlocks(), s.sent);
        restore(p, o.getWorld(), d.restore());
        Map<Location, BlockData> fake = new LinkedHashMap<>();
        for (Map.Entry<Pos, SourceBlock> e : d.send().entrySet()) {
            fake.put(new Location(o.getWorld(), e.getKey().x(), e.getKey().y(), e.getKey().z()), e.getValue().data());
        }
        if (!fake.isEmpty()) {
            p.sendMultiBlockChange(fake);
        }
        s.sent.clear();
        s.sent.addAll(d.send().keySet());
    }

    private void restore(Player p, World w, Set<Pos> positions) {
        // En otro mundo el cliente ya descartó esos bloques falsos.
        if (!p.getWorld().equals(w)) {
            return;
        }
        for (Pos pos : positions) {
            Location l = new Location(w, pos.x(), pos.y(), pos.z());
            p.sendBlockChange(l, l.getBlock().getBlockData());
        }
    }

    void particles(Player p, PreviewSession s) {
        World w = s.minCorner().getWorld();
        for (int[] e : s.footprint().edges()) {
            if ((e[0] + e[1] + e[2]) % 2 == 0) {
                dust(p, w, e, WHITE);
            }
        }
        Location o = s.minCorner();
        dust(p, w, new int[] { o.getBlockX(), o.getBlockY(), o.getBlockZ() }, GREEN);
        List<int[]> hits = new ArrayList<>(s.last.collisions());
        for (int[] c : hits.subList(0, Math.min(64, hits.size()))) {
            dust(p, w, c, RED);
        }
    }

    private static void dust(Player p, World w, int[] pos, Particle.DustOptions color) {
        p.spawnParticle(Particle.DUST, new Location(w, pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5), 1, 0, 0, 0, 0,
                color);
    }
}
