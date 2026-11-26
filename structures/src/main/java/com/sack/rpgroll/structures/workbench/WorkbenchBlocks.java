package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.BreakRules.BreakDecision;
import com.sack.rpgroll.structures.workbench.WorkbenchService.PlaceResult;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Mesas como bloque: colocar, romper, usar, explosiones y pistones. Mismo esquema que las estaciones de
 * RPGRoll-Enchantments, pero la identidad viene del ítem firmado y la verdad está en la base de datos; el PDC
 * del chunk ({@link BenchChunks}, lista de {@code id@x,y,z:MATERIAL}) es la segunda fuente para huérfanas.
 */
public final class WorkbenchBlocks implements Listener {

    private final Plugin plugin;
    private final WorkbenchService service;
    private final WorkbenchItems items;
    private final WorkbenchDisplay display;
    private final LangManager lang;
    private final BenchChunks chunks;
    private volatile BenchOpener opener = BenchOpener.NONE;

    public WorkbenchBlocks(Plugin plugin, WorkbenchService service, WorkbenchItems items, WorkbenchDisplay display,
                           LangManager lang) {
        this.plugin = plugin;
        this.service = service;
        this.items = items;
        this.display = display;
        this.lang = lang;
        this.chunks = new BenchChunks(plugin);
    }

    /** La Task 6 enchufa aquí el menú principal. */
    public void opener(BenchOpener opener) {
        this.opener = opener == null ? BenchOpener.NONE : opener;
    }

    private void runMain(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    // ---- colocar ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (items.read(item).isEmpty()) {
            return;
        }
        Player p = event.getPlayer();
        Block b = event.getBlockPlaced();
        PlaceResult v = service.validate(p, item, b);
        if (v != PlaceResult.OK) {
            event.setCancelled(true);
            lang.send(p, "workbench.place." + messageKey(v));
        }
    }

    /**
     * El registro se escribe solo cuando la colocación es firme: MONITOR, sin cancelar y con canBuild (la
     * protección de spawn revierte sin cancelar). Al confirmar la base se comprueba que el bloque siga siendo
     * el del ítem; si no, se deshace el registro.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!event.canBuild() || items.read(item).isEmpty()) {
            return;
        }
        Player p = event.getPlayer();
        Block b = event.getBlockPlaced();
        Material expected = item.getType();
        ItemStack back = item.asOne();
        service.place(p, item, b).thenAcceptAsync(result -> {
            if (result == PlaceResult.OK) {
                Optional<WorkbenchRecord> rec = service.at(b);
                if (rec.isEmpty()) {
                    return;
                }
                if (b.getType() != expected) {
                    // La colocación no se quedó (revertida o cambiada por otro plugin): la mesa no existe.
                    service.cancelPlacement(rec.get().id()).exceptionally(failed("deshacer el registro de " + rec.get().id()));
                    giveBack(p, back, b);
                    return;
                }
                display.show(rec.get()); // también la apunta en el PDC del chunk, con su bloque
                return;
            }
            // La base de datos falló: se quita el bloque y se devuelve el ítem.
            b.setType(Material.AIR, false);
            giveBack(p, back, b);
            lang.send(p, "workbench.place." + messageKey(result));
        }, this::runMain).exceptionally(failed("colocar una mesa"));
    }

    /** Registra el fallo de una continuación asíncrona; sin esto una excepción se pierde en silencio. */
    private <T> java.util.function.Function<Throwable, T> failed(String what) {
        return t -> {
            plugin.getLogger().log(Level.SEVERE, "Error al " + what, t);
            return null;
        };
    }

    private static String messageKey(PlaceResult r) {
        return switch (r) {
            case NOT_GENUINE -> "not-genuine";
            case NO_PERMISSION -> "no-permission";
            case OCCUPIED -> "occupied";
            default -> "error";
        };
    }

    /** Devuelve el ítem; si el jugador ya se desconectó, lo suelta en la mesa en vez de perderlo. */
    private static void giveBack(Player p, ItemStack item, Block at) {
        // En creativo colocar no gasta el ítem: devolverlo daría una copia con el mismo id.
        if (p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!p.isOnline()) {
            at.getWorld().dropItemNaturally(at.getLocation().add(0.5, 0.5, 0.5), item);
            return;
        }
        p.getInventory().addItem(item).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
    }

    // ---- romper ----

    /**
     * La barrera no se pica en supervivencia: el clic izquierdo lanza el evento de romper a mano. En aventura no:
     * ahí no se rompen bloques, y el evento sintético se saltaría esa regla.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || b == null
                || event.getPlayer().getGameMode() == GameMode.ADVENTURE
                || b.getType() != Material.BARRIER || service.at(b).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        new BlockBreakEvent(b, event.getPlayer()).callEvent();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        Optional<WorkbenchRecord> bench = service.at(b);
        if (bench.isEmpty()) {
            return;
        }
        // El bloque se queda hasta que la base de datos confirme; solo entonces se quita y se suelta el ítem.
        event.setCancelled(true);
        Player p = event.getPlayer();
        WorkbenchRecord rec = bench.get();
        BreakDecision decision = service.breakDecision(p, rec);
        if (decision == BreakDecision.DENY) {
            lang.send(p, service.breakDeniedKey(p, rec));
            return;
        }
        // breakBench termina en el hilo principal: thenAccept quita el bloque en la misma tarea en que la mesa sale
        // de la caché, sin un tick en el que el bloque siga ahí desprotegido.
        service.breakBench(p, b).thenAccept(result -> {
            switch (result.status()) {
                // pause y cancel sueltan el ítem igual: con pause la fila queda huérfana y la obra espera a la mesa.
                case DROPPED, PAUSED, CANCELLED -> {
                    removeFromWorld(b, rec);
                    result.dropped().ifPresent(stack ->
                            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), stack));
                    if (result.status() == WorkbenchService.BreakStatus.PAUSED) {
                        lang.send(p, "workbench.break.paused");
                    } else if (result.status() == WorkbenchService.BreakStatus.CANCELLED) {
                        lang.send(p, "build.cancelled");
                    }
                }
                case CANCEL_FAILED -> lang.send(p, "build.cancel-error");
                // La fila la borró otro proceso: la mesa ya no existe. Se quita sin ítem para no duplicarla.
                case GONE -> {
                    removeFromWorld(b, rec);
                    lang.send(p, "workbench.break.gone");
                }
                case UPGRADING -> lang.send(p, "workbench.break.upgrading");
                case NOTHING -> { }
            }
        }).exceptionally(t -> {
            plugin.getLogger().log(Level.SEVERE, "No se pudo romper la mesa " + rec.id(), t);
            runMain(() -> lang.send(p, "workbench.break.error"));
            return null;
        });
    }

    /** Display, entrada del chunk y bloque fuera. */
    private void removeFromWorld(Block b, WorkbenchRecord rec) {
        display.remove(rec);
        chunks.mark(b.getChunk(), rec, null);
        b.setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> service.at(b).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> service.at(b).isPresent());
    }

    /** El Wither, los devastadores y demás entidades que cambian bloques. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (service.at(event.getBlock()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> service.at(b).isPresent())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonBack(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(b -> service.at(b).isPresent())) {
            event.setCancelled(true);
        }
    }

    // ---- usar ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || b == null
                || event.getPlayer().isSneaking()) {
            return;
        }
        Optional<WorkbenchRecord> bench = service.at(b);
        if (bench.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player p = event.getPlayer();
        WorkbenchRecord rec = bench.get();
        service.canUse(p, rec).thenAcceptAsync(ok -> {
            if (ok) {
                opener.open(p, rec);
            } else {
                lang.send(p, "workbench.use.denied");
            }
        }, this::runMain).exceptionally(failed("abrir la mesa " + rec.id()));
    }

    // ---- chunks ----

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        verifyBlocks(event.getChunk());
    }

    /**
     * WorldForge (en ServerLoadEvent) y Multiverse (en su onEnable) abren mundos después de nuestro arranque: sus
     * mesas se indexan ahora y se revisan los chunks que ya cargó.
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        service.worldLoaded(world.getName());
        for (Chunk chunk : world.getLoadedChunks()) {
            verify(chunk);
        }
    }

    /** Desde 1.17 las entidades cargan aparte y después del chunk: solo aquí se puede saber si falta el display. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        // Displays de mesas que pasaron a huérfanas (o se borraron) mientras sus entidades no estaban cargadas.
        display.removeStray(event.getEntities(), id -> service.byId(id).filter(r -> !r.orphan()).isPresent());
        ensureDisplays(event.getChunk());
    }

    private void ensureDisplays(Chunk chunk) {
        for (WorkbenchRecord r : service.inChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
            display.ensure(r);
        }
    }

    /** Arranque y {@code reload}: chunks ya cargados. Si sus entidades aún no cargaron, el evento llegará solo. */
    public void verify(Chunk chunk) {
        verifyBlocks(chunk);
        if (chunk.isEntitiesLoaded()) {
            ensureDisplays(chunk);
        }
    }

    /** Qué hacer con el bloque de una mesa al cargar su chunk. */
    enum BlockCheck { OK, NORMALIZE, ORPHAN }

    /**
     * Aire: la mesa ya no está (huérfana). El bloque esperado: bien. Si la mesa está apuntada en el PDC del chunk y
     * el bloque es el que se puso ({@code stored}) o una barrera, cambió {@code fallback-block} o el modelo y se
     * repone. Cualquier otro bloque (alguien construyó en el hueco tras un /setblock o WorldEdit) no se pisa:
     * huérfana. {@code stored} es null en las entradas del formato viejo: ahí solo se repone la barrera.
     * <p>
     * shortcut: un bloque vanilla igual al que se puso no se distingue del de la mesa. Si la mesa se quitó desde
     * fuera (WorldEdit), alguien puso ese mismo bloque en el hueco y luego cambia {@code fallback-block}, se
     * sustituye. Pasa muy poco; para cerrarlo haría falta marcar el bloque (PDC de bloque o tile entity).
     */
    static BlockCheck check(boolean air, boolean listed, Material actual, Material stored, Material expected) {
        if (air) {
            return BlockCheck.ORPHAN;
        }
        if (actual == expected) {
            return BlockCheck.OK;
        }
        if (listed && (actual == Material.BARRIER || actual == stored)) {
            return BlockCheck.NORMALIZE;
        }
        return BlockCheck.ORPHAN;
    }

    /**
     * Cruza el chunk con la base de datos. Una fila sin su bloque pasa a huérfana (sin borrarse, y sin su display);
     * una con el bloque de otra configuración se repone; una entrada del chunk sin fila se avisa una vez y se quita.
     * Las entradas se reescriben con el bloque actual (así las del formato viejo pasan al nuevo).
     */
    private void verifyBlocks(Chunk chunk) {
        World w = chunk.getWorld();
        List<String> raw = chunks.stored(chunk);
        Map<String, BenchChunks.Entry> entries = new LinkedHashMap<>();
        for (String e : raw) {
            BenchChunks.Entry parsed = BenchChunks.parse(e);
            entries.putIfAbsent(parsed.id(), parsed);
        }
        Set<String> handled = new HashSet<>();
        List<String> kept = new ArrayList<>();
        for (WorkbenchRecord r : service.inChunk(w.getName(), chunk.getX(), chunk.getZ())) {
            String id = r.id().toString();
            handled.add(id);
            Block b = w.getBlockAt(r.x(), r.y(), r.z());
            Material type = b.getType();
            Material expected = display.expectedBlock(r);
            BenchChunks.Entry entry = entries.get(id);
            switch (check(type.isAir(), entry != null, type, entry == null ? null : entry.material(), expected)) {
                case OK -> kept.add(BenchChunks.entry(r, expected));
                case NORMALIZE -> {
                    // Solo el bloque: el display lo pone o lo quita ensureDisplays cuando cargan las entidades.
                    b.setType(expected, false);
                    kept.add(BenchChunks.entry(r, expected));
                }
                case ORPHAN -> {
                    plugin.getLogger().warning(lang.raw("workbench.warning.block-missing", "id", r.id(),
                            "world", r.world(), "x", r.x(), "y", r.y(), "z", r.z(), "block", type.name()));
                    display.remove(r);
                    service.markOrphan(r);
                }
            }
        }
        for (BenchChunks.Entry e : entries.values()) {
            if (handled.contains(e.id())) {
                continue;
            }
            Optional<WorkbenchRecord> row = row(e.id());
            if (row.isPresent() && !row.get().orphan()) {
                kept.add(e.raw()); // activa pero aún sin posición aquí (mundo sin indexar): se conserva
            } else if (row.isEmpty()) {
                plugin.getLogger().warning(lang.raw("workbench.warning.chunk-orphan", "id", e.id(),
                        "x", e.x(), "y", e.y(), "z", e.z()));
            }
            // Fila huérfana: ya se avisó al marcarla; la entrada se quita sin repetir el aviso.
        }
        if (!kept.equals(raw)) {
            chunks.store(chunk, kept);
        }
    }

    private Optional<WorkbenchRecord> row(String id) {
        try {
            return service.byId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
