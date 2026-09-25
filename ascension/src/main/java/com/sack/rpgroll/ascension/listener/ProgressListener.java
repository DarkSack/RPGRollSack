package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.api.event.PlayerJobLevelUpEvent;
import com.sack.rpgroll.api.event.PlayerLeaveJobEvent;
import com.sack.rpgroll.api.event.PlayerLevelUpEvent;
import com.sack.rpgroll.ascension.engine.ProgressService;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;

import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Traduce eventos de Bukkit y de RPGRoll a progreso de Ascension. Los que
 * se pueden cancelar se ignoran si lo están: una acción que una protección
 * impidió no cuenta.
 */
public class ProgressListener implements Listener {

    private final Plugin plugin;
    private final ProgressService progress;

    public ProgressListener(Plugin plugin, ProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null || killer.equals(entity)) {
            return;
        }

        if (entity instanceof Player) {
            progress.handle(killer, new ProgressEvent(TriggerType.KILL_PLAYER, "PLAYER", held(killer)));
        } else {
            progress.handle(killer, new ProgressEvent(TriggerType.KILL_ENTITY, entity.getType().name(), held(killer)));
        }
    }

    /**
     * A HIGHEST y no a MONITOR: tiene que preguntar si el bloque lo puso un
     * jugador antes de que el listener del minero, a MONITOR, borre la marca.
     * Poner y quitar el mismo bloque no cuenta.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        String material = event.getBlock().getType().name();

        // La consulta del bloque colocado va a SQLite: solo si a alguien le
        // importa este bloque.
        if (!progress.isTracked(TriggerType.BREAK_BLOCK, material)) {
            return;
        }

        if (RPGRollAPI.isReady() && RPGRollAPI.get().getPlacedBlockTracker().isPlayerPlaced(event.getBlock())) {
            return;
        }

        progress.handle(event.getPlayer(), new ProgressEvent(TriggerType.BREAK_BLOCK, material, held(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        progress.handle(event.getPlayer(),
                new ProgressEvent(TriggerType.PLACE_BLOCK, event.getBlockPlaced().getType().name(), null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) {
            return;
        }

        progress.handle(event.getPlayer(),
                new ProgressEvent(TriggerType.FISH, item.getItemStack().getType().name(), held(event.getPlayer())));
    }

    /** Cuenta una vez por clic, también con shift-clic (que fabrica varios). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        progress.handle(player,
                new ProgressEvent(TriggerType.CRAFT_ITEM, event.getRecipe().getResult().getType().name(), null));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(PlayerLevelUpEvent event) {
        progress.refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJobLevelUp(PlayerJobLevelUpEvent event) {
        progress.refresh(event.getPlayer());
    }

    /** Al dejar un oficio se van los permisos de sus evoluciones. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJobLeave(PlayerLeaveJobEvent event) {
        progress.jobEvolutions().check(event.getPlayer());
    }

    /** Un segundo después de entrar, cuando el core ya cargó el personaje. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                progress.refresh(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        progress.jobEvolutions().onQuit(event.getPlayer());
    }

    private static String held(Player player) {
        return player.getInventory().getItemInMainHand().getType().name();
    }

}
