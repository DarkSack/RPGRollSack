package com.sack.rpgroll.extras.elevator;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * Elevadores: un bloque fabricable. Saltar encima lleva al siguiente elevador de la misma columna
 * hacia arriba; agacharse, al de abajo. Funciona igual en Bedrock (saltar y agacharse, sin comandos).
 * <p>
 * Los elevadores colocados se apuntan en el propio chunk (PDC), así un bloque igual puesto a mano
 * no es un elevador y no hace falta ningún archivo aparte.
 */
public final class Elevators implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey itemKey;
    private final NamespacedKey chunkKey;
    private final NamespacedKey recipeKey;

    private boolean enabled;
    private Material block = Material.LIGHT_BLUE_GLAZED_TERRACOTTA;
    private int maxDistance = 32;

    public Elevators(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.itemKey = new NamespacedKey(plugin, "elevator");
        this.chunkKey = new NamespacedKey(plugin, "elevators");
        this.recipeKey = new NamespacedKey(plugin, "elevator");
    }

    /** Lee la sección {@code elevator} de config.yml (sin ella, encendido con los valores de fábrica). */
    public void configure(ConfigurationSection section) {

        Bukkit.removeRecipe(recipeKey);
        enabled = section == null || section.getBoolean("enabled", true);
        if (section != null) {
            Material configured = Material.matchMaterial(section.getString("block", block.name()));
            if (configured != null && configured.isBlock() && configured.isSolid()) {
                block = configured;
            } else {
                plugin.getLogger().warning("config.yml elevator.block no es un bloque sólido; se usa " + block);
            }
            maxDistance = Math.max(3, Math.min(256, section.getInt("max-distance", maxDistance)));
        }
        if (!enabled) {
            return;
        }
        ItemStack result = item();
        result.setAmount(section == null ? 2 : Math.max(1, section.getInt("recipe-amount", 2)));
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("WWW", "WPW", "WWW");
        recipe.setIngredient('W', new RecipeChoice.MaterialChoice(Tag.WOOL));
        recipe.setIngredient('P', Material.ENDER_PEARL);
        Bukkit.addRecipe(recipe, true);
        Bukkit.getOnlinePlayers().forEach(player -> player.discoverRecipe(recipeKey));
    }

    public void shutdown() {
        Bukkit.removeRecipe(recipeKey);
    }

    public ItemStack item() {
        ItemStack item = ItemStack.of(block);
        var meta = item.getItemMeta();
        meta.displayName(lang.component("elevator.item_name"));
        meta.lore(List.of(lang.component("elevator.item_lore_1"), lang.component("elevator.item_lore_2")));
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isElevatorItem(ItemStack item) {
        return item != null && item.getType() == block && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    // ----- Registro por chunk: x, z locales y la altura empaquetados en un int -----

    static int pack(Block b) {
        return (b.getX() & 15) | ((b.getZ() & 15) << 4) | ((b.getY() + 4096) << 8);
    }

    private int[] stored(Chunk chunk) {
        int[] values = chunk.getPersistentDataContainer().get(chunkKey, PersistentDataType.INTEGER_ARRAY);
        return values == null ? new int[0] : values;
    }

    boolean isElevator(Block b) {
        if (b.getType() != block) {
            return false;
        }
        int key = pack(b);
        return Arrays.stream(stored(b.getChunk())).anyMatch(v -> v == key);
    }

    private void mark(Block b, boolean add) {
        Chunk chunk = b.getChunk();
        int key = pack(b);
        int[] current = Arrays.stream(stored(chunk)).filter(v -> v != key).toArray();
        if (add) {
            current = Arrays.copyOf(current, current.length + 1);
            current[current.length - 1] = key;
        }
        if (current.length == 0) {
            chunk.getPersistentDataContainer().remove(chunkKey);
        } else {
            chunk.getPersistentDataContainer().set(chunkKey, PersistentDataType.INTEGER_ARRAY, current);
        }
    }

    // ----- Colocar y quitar -----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (enabled && isElevatorItem(event.getItemInHand())) {
            mark(event.getBlockPlaced(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (!isElevator(b)) {
            return;
        }
        mark(b, false);
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), item());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().stream().filter(this::isElevator).forEach(b -> mark(b, false));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().stream().filter(this::isElevator).forEach(b -> mark(b, false));
    }

    /** Un pistón movería el bloque y dejaría la marca en el hueco: los elevadores no se empujan. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isElevator)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonBack(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isElevator)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            event.getPlayer().discoverRecipe(recipeKey);
        }
    }

    // ----- Viajar -----

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        if (enabled) {
            travel(event.getPlayer(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (enabled && event.isSneaking()) {
            travel(event.getPlayer(), -1);
        }
    }

    private void travel(Player player, int direction) {
        Block under = player.getLocation().getBlock().getRelative(0, -1, 0);
        if (!isElevator(under)) {
            return;
        }
        Block target = next(under, direction);
        if (target == null) {
            if (direction > 0) {
                player.sendActionBar(lang.component("elevator.none_up"));
            } else {
                player.sendActionBar(lang.component("elevator.none_down"));
            }
            return;
        }
        Location to = player.getLocation().clone();
        to.setY(target.getY() + 1);
        player.teleport(to);
        player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, direction > 0 ? 1.6f : 1.2f);
    }

    /** El siguiente elevador de la columna con dos bloques libres encima, o null. */
    Block next(Block from, int direction) {
        int min = from.getWorld().getMinHeight();
        int max = from.getWorld().getMaxHeight() - 3;
        for (int dy = 1; dy <= maxDistance; dy++) {
            int y = from.getY() + dy * direction;
            if (y < min || y > max) {
                return null;
            }
            Block candidate = from.getWorld().getBlockAt(from.getX(), y, from.getZ());
            if (isElevator(candidate) && candidate.getRelative(0, 1, 0).isPassable()
                    && candidate.getRelative(0, 2, 0).isPassable()) {
                return candidate;
            }
        }
        return null;
    }
}
