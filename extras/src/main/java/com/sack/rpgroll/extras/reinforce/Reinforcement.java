package com.sack.rpgroll.extras.reinforce;

import com.sack.rpgroll.common.block.Protection;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bloques reforzados. Con el Reforzador (una herramienta con usos) un clic derecho en cualquier
 * bloque ya puesto donde puedas construir lo deja a tu nombre: desde entonces nada lo rompe, ni
 * jugadores, ni explosiones, pistones, fuego, endermans, withers, el agua ni los desastres
 * (RPGRoll-Disasters pregunta por las explosiones). Para quitarlo, su dueño (o un admin) hace
 * agachado + clic derecho con el Reforzador y el bloque vuelve a ser normal.
 * <p>
 * El dueño de cada bloque se guarda en el propio chunk (PDC), sin archivo aparte.
 */
public final class Reinforcement implements Listener {

    public static final String ADMIN_PERMISSION = "rpgrollextras.reinforce.admin";
    private static final Set<Material> NEVER = Set.of(Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.JIGSAW,
            Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.NETHER_PORTAL, Material.LIGHT);

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey toolKey;
    private final NamespacedKey toolRecipe;
    private boolean enabled;

    public Reinforcement(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.toolKey = new NamespacedKey(plugin, "reinforcer");
        this.toolRecipe = new NamespacedKey(plugin, "reinforcer");
    }

    /** Lee la sección {@code reinforcement} de config.yml (sin ella, encendido). */
    public void configure(ConfigurationSection section) {

        shutdown();
        enabled = section == null || section.getBoolean("enabled", true);
        if (!enabled) {
            return;
        }
        // Se fabrica una vez (como un pincel: obsidiana, hierro y un palo) y sirve para 64 bloques.
        ShapedRecipe tool = new ShapedRecipe(toolRecipe, tool());
        tool.shape("O", "I", "S");
        tool.setIngredient('O', Material.OBSIDIAN);
        tool.setIngredient('I', Material.IRON_INGOT);
        tool.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(tool, true);
        Bukkit.getOnlinePlayers().forEach(this::discover);
    }

    public void shutdown() {
        Bukkit.removeRecipe(toolRecipe);
    }

    private void discover(Player player) {
        player.discoverRecipes(List.of(toolRecipe));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            discover(event.getPlayer());
        }
    }

    // ----- Ítems -----

    /** El Reforzador: un pincel (64 de durabilidad, y no es ingrediente de nada) marcado en su PDC. */
    public ItemStack tool() {
        return tagged(Material.BRUSH, toolKey, "reinforce.tool_name", "reinforce.tool_lore_1", "reinforce.tool_lore_2",
                "reinforce.tool_lore_3");
    }

    private ItemStack tagged(Material material, NamespacedKey key, String name, String... lore) {
        ItemStack item = ItemStack.of(material);
        var meta = item.getItemMeta();
        meta.displayName(lang.component(name));
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(lang.component(line));
        }
        meta.lore(lines);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean is(ItemStack item, NamespacedKey key) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    // ----- Registro por chunk -----

    static int pack(Block b) {
        return (b.getX() & 15) | ((b.getZ() & 15) << 4) | ((b.getY() + 4096) << 8);
    }

    private NamespacedKey keyOf(Block b) {
        return new NamespacedKey(plugin, "rf" + pack(b));
    }

    /** El dueño del refuerzo, o null si el bloque no está reforzado. */
    public UUID owner(Block b) {
        if (!b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
            return null;
        }
        String value = b.getChunk().getPersistentDataContainer().get(keyOf(b), PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isReinforced(Block b) {
        return owner(b) != null;
    }

    private void set(Block b, UUID owner) {
        var data = b.getChunk().getPersistentDataContainer();
        if (owner == null) {
            data.remove(keyOf(b));
        } else {
            data.set(keyOf(b), PersistentDataType.STRING, owner.toString());
        }
    }

    /** El bloque y, si es una puerta, una cama o una planta alta, su otra mitad: si no, se rompería por ahí. */
    private static List<Block> withHalves(Block b) {
        List<Block> blocks = new ArrayList<>(2);
        blocks.add(b);
        var data = b.getBlockData();
        if (data instanceof Bed bed) {
            BlockFace toOther = bed.getPart() == Bed.Part.FOOT ? bed.getFacing() : bed.getFacing().getOppositeFace();
            blocks.add(b.getRelative(toOther));
        } else if (data instanceof Bisected half) {
            blocks.add(b.getRelative(half.getHalf() == Bisected.Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN));
        }
        return blocks;
    }

    private static String name(UUID owner) {
        String name = Bukkit.getOfflinePlayer(owner).getName();
        return name == null ? "?" : name;
    }

    // ----- Reforzar y quitar el refuerzo -----

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {

        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || !is(event.getItem(), toolKey) || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        UUID current = owner(block);

        if (player.isSneaking()) {
            if (current == null) {
                player.sendActionBar(lang.component("reinforce.not_reinforced"));
            } else if (player.getUniqueId().equals(current) || player.hasPermission(ADMIN_PERMISSION)) {
                for (Block part : withHalves(block)) {
                    set(part, null);
                }
                block.getWorld().spawnParticle(Particle.WAX_OFF, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.4, 0.4, 0.4, 0);
                player.playSound(block.getLocation(), Sound.ITEM_AXE_WAX_OFF, 0.8f, 1f);
                player.sendActionBar(lang.component("reinforce.removed"));
            } else {
                player.sendActionBar(lang.component("reinforce.protected", "owner", name(current)));
            }
            return;
        }

        if (current != null) {
            player.sendActionBar(lang.component("reinforce.already", "owner", name(current)));
            return;
        }
        if (NEVER.contains(block.getType()) || block.getType().isAir() || block.isLiquid()) {
            player.sendActionBar(lang.component("reinforce.cannot"));
            return;
        }
        // Solo donde podrías romperlo tú: no se refuerza el bloque de otro para estorbarle.
        if (!Protection.canBreak(player, block)) {
            player.sendActionBar(lang.component("reinforce.not_here"));
            return;
        }
        for (Block part : withHalves(block)) {
            set(part, player.getUniqueId());
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.getInventory().setItemInMainHand(event.getItem().damage(1, player));
        }
        block.getWorld().spawnParticle(Particle.WAX_ON, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.4, 0.4, 0.4, 0);
        player.playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.6f);
        player.sendActionBar(lang.component("reinforce.done"));
    }

    // ----- Romper: nadie, ni el dueño (antes le quita el refuerzo) -----

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        UUID owner = owner(event.getBlock());
        if (owner == null) {
            return;
        }
        event.setCancelled(true);
        if (!Protection.testing()) {
            event.getPlayer().sendActionBar(message(event.getPlayer(), owner));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(BlockDamageEvent event) {
        UUID owner = owner(event.getBlock());
        if (owner != null) {
            event.getPlayer().sendActionBar(message(event.getPlayer(), owner));
        }
    }

    private net.kyori.adventure.text.Component message(Player player, UUID owner) {
        return lang.component(player.getUniqueId().equals(owner) ? "reinforce.yours" : "reinforce.protected",
                "owner", name(owner));
    }

    // ----- Nada más lo rompe -----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isReinforced);
    }

    /** También la explosión de prueba con la que RPGRoll-Disasters pregunta qué puede romper. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isReinforced);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isReinforced)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonBack(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isReinforced)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isReinforced(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Endermans, withers, ravagers, aldeanos, la arena que cae... */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent event) {
        if (isReinforced(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (isReinforced(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (isReinforced(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        if (isReinforced(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
