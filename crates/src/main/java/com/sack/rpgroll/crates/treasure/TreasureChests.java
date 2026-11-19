package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.crates.lucky.LuckyExecutor;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * El cofre de un tesoro desenterrado: un bloque de barrera (sólido e invisible) con un ItemDisplay encima
 * que lleva el modelo. El display es persistente y lleva el id del tesoro en su PDC, así que el cofre
 * sigue ahí tras un reinicio. Se abre una sola vez; al cerrarlo, lo que quede cae al suelo y el cofre se va.
 */
public class TreasureChests {

    /** El inventario de un cofre abierto: sabe a qué display y bloque pertenece. */
    static final class Holder implements InventoryHolder {

        private final UUID display;
        private final Block block;
        private Inventory inventory;

        Holder(UUID display, Block block) {
            this.display = display;
            this.block = block;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        UUID display() {
            return display;
        }

        Block block() {
            return block;
        }
    }

    private final Plugin plugin;
    private final LangManager lang;
    private final TreasureManager manager;
    private final LuckyExecutor executor;
    private final NamespacedKey chestKey;
    private final ChestClaims claims = new ChestClaims();

    public TreasureChests(Plugin plugin, LangManager lang, TreasureManager manager, LuckyExecutor executor) {
        this.plugin = plugin;
        this.lang = lang;
        this.manager = manager;
        this.executor = executor;
        this.chestKey = new NamespacedKey(plugin, "treasure_chest");
    }

    /** Hace aparecer el cofre de {@code treasure} en {@code block} (ya vacío) y sus guardianes. */
    public void spawn(Player finder, Treasure treasure, Block block) {

        block.setType(Material.BARRIER, false);
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        // Los modelos miran al norte (-z), como los muebles; girarlo -yaw deja el frente hacia quien cava.
        float yaw = Math.round(finder.getLocation().getYaw() / 90f) * 90f;

        block.getWorld().spawn(center, ItemDisplay.class, display -> {
            display.setItemStack(displayItem(treasure));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.FIXED);
            float scale = (float) treasure.chest().scale();
            display.setTransformation(new Transformation(new Vector3f(),
                    new AxisAngle4f((float) Math.toRadians(-yaw), 0, 1, 0), new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(chestKey, PersistentDataType.STRING, treasure.id());
        });

        block.getWorld().playSound(center, Sound.BLOCK_CHEST_LOCKED, 1f, 0.7f);
        block.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.6f);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 0.6, 0), 30, 0.5, 0.4, 0.5);
        finder.sendMessage(lang.component("treasure.found", "treasure", treasure.displayName()));

        executor.runActions(finder, block, treasure.chest().guardians(), "Tesoro '" + treasure.id() + "', guardianes");
    }

    private static ItemStack displayItem(Treasure treasure) {
        if (treasure.chest().model() == null) {
            return new ItemStack(Material.CHEST);
        }
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey model = NamespacedKey.fromString(treasure.chest().model().toLowerCase(Locale.ROOT));
        if (model != null) {
            meta.setItemModel(model);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** El display de cofre de tesoro que hay en {@code block}, si lo hay. */
    public Optional<ItemDisplay> displayAt(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        return block.getWorld().getNearbyEntitiesByType(ItemDisplay.class, center, 0.6).stream()
                .filter(display -> display.getPersistentDataContainer().has(chestKey, PersistentDataType.STRING))
                .findFirst();
    }

    /** Clic derecho en el cofre: la primera vez tira el botín y lo abre. */
    public void open(Player player, Block block, ItemDisplay display) {

        String id = display.getPersistentDataContainer().get(chestKey, PersistentDataType.STRING);
        Optional<Treasure> treasure = manager.get(id == null ? "" : id);
        if (treasure.isEmpty()) {
            // Tesoro borrado de la configuración: el cofre se quita sin botín.
            remove(block, display);
            lang.send(player, "treasure.unknown");
            return;
        }
        if (!claims.claim(display.getUniqueId())) {
            lang.send(player, "treasure.already_open");
            return;
        }

        Treasure t = treasure.get();
        Holder holder = new Holder(display.getUniqueId(), block);
        String title = t.chest().title() == null ? t.displayName() : t.chest().title();
        Inventory inventory = Bukkit.createInventory(holder, t.chest().rows() * 9, ComponentUtils.parse(title));
        holder.inventory = inventory;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ItemStack> items = TreasureLoot.resolve(TreasureLoot.roll(t.loot(), random), TreasureChests::item,
                warning -> plugin.getLogger().warning("✘ Tesoro '" + t.id() + "': " + warning));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        for (int i = 0; i < items.size(); i++) {
            if (i < slots.size()) {
                inventory.setItem(slots.get(i), items.get(i));
            } else {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), items.get(i));
            }
        }

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().playSound(center, Sound.BLOCK_CHEST_OPEN, 1f, 0.8f);
        player.openInventory(inventory);
        executor.runActions(player, block, t.actions(), "Tesoro '" + t.id() + "'");
    }

    /** Al cerrar: lo que quede cae al suelo y el cofre desaparece. */
    public void close(Holder holder) {

        Block block = holder.block();
        Location drop = block.getLocation().add(0.5, 0.7, 0.5);
        for (ItemStack item : holder.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                block.getWorld().dropItemNaturally(drop, item);
            }
        }
        holder.getInventory().clear();

        if (Bukkit.getEntity(holder.display()) instanceof ItemDisplay display) {
            remove(block, display);
        } else if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }
    }

    /** Quita barrera y display, con un poco de polvo. */
    public void remove(Block block, ItemDisplay display) {
        display.remove();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.CLOUD, center, 15, 0.3, 0.3, 0.3, 0.02);
        block.getWorld().playSound(center, Sound.BLOCK_WOOD_BREAK, 1f, 0.8f);
    }

    /** Material vanilla o, si no, id de RPGRoll-Items. */
    private static Optional<ItemStack> item(String id, int amount) {
        Material material = Material.matchMaterial(id);
        if (material != null && material.isItem() && !material.isAir()) {
            return Optional.of(new ItemStack(material, Math.min(amount, material.getMaxStackSize())));
        }
        if (Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Items")) {
            return ItemsBridge.create(id, amount);
        }
        return Optional.empty();
    }

}
