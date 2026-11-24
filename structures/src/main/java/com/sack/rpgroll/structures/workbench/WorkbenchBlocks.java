package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.BreakRules.BreakDecision;
import com.sack.rpgroll.structures.workbench.WorkbenchService.PlaceResult;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Mesas como bloque: colocar, romper, usar, explosiones y pistones. Mismo esquema que las estaciones de
 * RPGRoll-Enchantments, pero la identidad viene del ítem firmado y la verdad está en la base de datos; el PDC
 * del chunk ({@code rpgroll-structures:benches}, lista de {@code id@x,y,z}) es la segunda fuente para huérfanas.
 */
public final class WorkbenchBlocks implements Listener {

    private final Plugin plugin;
    private final WorkbenchService service;
    private final WorkbenchItems items;
    private final WorkbenchDisplay display;
    private final LangManager lang;
    private final NamespacedKey chunkKey;
    private volatile BenchOpener opener = BenchOpener.NONE;

    public WorkbenchBlocks(Plugin plugin, WorkbenchService service, WorkbenchItems items, WorkbenchDisplay display,
                           LangManager lang) {
        this.plugin = plugin;
        this.service = service;
        this.items = items;
        this.display = display;
        this.lang = lang;
        this.chunkKey = new NamespacedKey(plugin, "benches");
    }

    /** La Task 6 enchufa aquí el menú principal. */
    public void opener(BenchOpener opener) {
        this.opener = opener == null ? BenchOpener.NONE : opener;
    }

    private void runMain(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    // ---- PDC del chunk ----

    private static String entry(WorkbenchRecord r) {
        return r.id() + "@" + r.x() + "," + r.y() + "," + r.z();
    }

    private List<String> stored(Chunk chunk) {
        List<String> l = chunk.getPersistentDataContainer().get(chunkKey, PersistentDataType.LIST.strings());
        return l == null ? new ArrayList<>() : new ArrayList<>(l);
    }

    private void store(Chunk chunk, List<String> entries) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (entries.isEmpty()) {
            pdc.remove(chunkKey);
        } else {
            pdc.set(chunkKey, PersistentDataType.LIST.strings(), entries);
        }
    }

    private void mark(Block b, WorkbenchRecord r, boolean add) {
        List<String> entries = stored(b.getChunk());
        String prefix = r.id() + "@";
        entries.removeIf(e -> e.startsWith(prefix));
        if (add) {
            entries.add(entry(r));
        }
        store(b.getChunk(), entries);
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
                    giveBack(p, back);
                    return;
                }
                mark(b, rec.get(), true);
                display.show(rec.get());
                return;
            }
            // La base de datos falló: se quita el bloque y se devuelve el ítem.
            b.setType(Material.AIR, false);
            giveBack(p, back);
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

    private static void giveBack(Player p, ItemStack item) {
        // En creativo colocar no gasta el ítem: devolverlo daría una copia con el mismo id.
        if (p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        p.getInventory().addItem(item).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
    }

    // ---- romper ----

    /** La barrera no se pica en supervivencia: el clic izquierdo lanza el evento de romper a mano. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(PlayerInteractEvent event) {
        Block b = event.getClickedBlock();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || b == null
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
            lang.send(p, rec.owner().equals(p.getUniqueId()) || p.hasPermission(WorkbenchService.PERM_BYPASS)
                    ? "workbench.break.has-project" : "workbench.break.denied");
            return;
        }
        // breakBench termina en el hilo principal: thenAccept quita el bloque en la misma tarea en que la mesa sale
        // de la caché, sin un tick en el que el bloque siga ahí desprotegido.
        service.breakBench(p, b).thenAccept(dropped -> {
            dropped.ifPresent(stack -> {
                display.remove(rec);
                mark(b, rec, false);
                b.setType(Material.AIR);
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), stack);
            });
        }).exceptionally(t -> {
            plugin.getLogger().log(Level.SEVERE, "No se pudo romper la mesa " + rec.id(), t);
            runMain(() -> lang.send(p, "workbench.break.error"));
            return null;
        });
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
        ensureDisplays(event.getChunk());
    }

    private void ensureDisplays(Chunk chunk) {
        for (WorkbenchRecord r : service.inChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
            display.ensure(r);
        }
    }

    /** Arranque: chunks ya cargados. Si sus entidades aún no cargaron, el evento llegará solo. */
    public void verify(Chunk chunk) {
        verifyBlocks(chunk);
        if (chunk.isEntitiesLoaded()) {
            ensureDisplays(chunk);
        }
    }

    /** Qué hacer con el bloque de una mesa al cargar su chunk. */
    enum BlockCheck { OK, NORMALIZE, ORPHAN }

    /**
     * Aire: la mesa ya no está (huérfana). El bloque esperado: bien. Otro bloque con la mesa apuntada en el PDC del
     * chunk: cambió {@code fallback-block} o el modelo, y se repone. Otro bloque sin la mesa en el PDC: huérfana.
     */
    static BlockCheck check(boolean air, boolean listed, Material actual, Material expected) {
        if (air) {
            return BlockCheck.ORPHAN;
        }
        if (actual == expected) {
            return BlockCheck.OK;
        }
        return listed ? BlockCheck.NORMALIZE : BlockCheck.ORPHAN;
    }

    /**
     * Cruza el chunk con la base de datos. Una fila sin su bloque pasa a huérfana (sin borrarse); una con el bloque
     * de otra configuración se repone; una entrada del chunk sin fila se avisa y se quita.
     */
    private void verifyBlocks(Chunk chunk) {
        World w = chunk.getWorld();
        List<WorkbenchRecord> benches = service.inChunk(w.getName(), chunk.getX(), chunk.getZ());
        List<String> entries = stored(chunk);
        for (WorkbenchRecord r : benches) {
            Block b = w.getBlockAt(r.x(), r.y(), r.z());
            Material type = b.getType();
            Material expected = display.expectedBlock(r);
            boolean listed = entries.stream().anyMatch(e -> e.startsWith(r.id() + "@"));
            switch (check(type.isAir(), listed, type, expected)) {
                case OK -> { }
                // Solo el bloque: el display lo pone o lo quita ensureDisplays cuando cargan las entidades.
                case NORMALIZE -> b.setType(expected, false);
                case ORPHAN -> {
                    plugin.getLogger().warning(lang.raw("workbench.warning.block-missing", "id", r.id(),
                            "world", r.world(), "x", r.x(), "y", r.y(), "z", r.z()));
                    service.markOrphan(r);
                }
            }
        }
        List<String> kept = new ArrayList<>();
        for (String e : entries) {
            if (known(e)) {
                kept.add(e);
            } else {
                plugin.getLogger().warning(lang.raw("workbench.warning.chunk-orphan", "id", id(e),
                        "x", coords(e)[0], "y", coords(e)[1], "z", coords(e)[2]));
            }
        }
        for (WorkbenchRecord r : benches) {
            if (service.byId(r.id()).filter(x -> !x.orphan()).isPresent() && kept.stream().noneMatch(e -> e.startsWith(r.id() + "@"))) {
                kept.add(entry(r));
            }
        }
        if (!kept.equals(entries)) {
            store(chunk, kept);
        }
    }

    private boolean known(String entry) {
        try {
            return service.byId(UUID.fromString(id(entry))).filter(r -> !r.orphan()).isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String id(String entry) {
        return entry.substring(0, Math.max(0, entry.indexOf('@')));
    }

    private static String[] coords(String entry) {
        String[] c = entry.substring(entry.indexOf('@') + 1).split(",");
        return c.length == 3 ? c : new String[] {"?", "?", "?"};
    }
}
