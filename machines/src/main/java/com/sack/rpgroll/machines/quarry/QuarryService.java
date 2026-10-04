package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Las canteras: su ítem, su área, su cofre y su marco. */
public class QuarryService {

    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN};

    private final Plugin plugin;
    private final LangManager lang;
    private final Displays displays;
    private final QuarryStore store;
    private final Claims claims;
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;
    private QuarrySettings settings;

    public QuarryService(Plugin plugin, LangManager lang, Displays displays, QuarryStore store, Claims claims,
            QuarrySettings settings) {
        this.plugin = plugin;
        this.lang = lang;
        this.displays = displays;
        this.store = store;
        this.claims = claims;
        this.itemKey = new NamespacedKey(plugin, "quarry");
        this.recipeKey = new NamespacedKey(plugin, "quarry");
        this.settings = settings;
    }

    public QuarrySettings settings() {
        return settings;
    }

    public void settings(QuarrySettings settings) {
        this.settings = settings;
    }

    public QuarryStore store() {
        return store;
    }

    public Claims claims() {
        return claims;
    }

    public Optional<Quarry> at(Block block) {
        return store.at(Displays.key(block));
    }

    public int side(Quarry quarry) {
        return Math.max(1, (int) settings.track(Numeric.AREA).value(quarry.level(Numeric.AREA)));
    }

    /** ¿Cabe el área de este lado entera en un claim del dueño? (sin GriefPrevention o sin exigirlo, sí) */
    public boolean areaAllowed(Quarry quarry, World world, int side) {
        if (!settings.requireClaim()) {
            return true;
        }
        int minX = Quarry.min(quarry.x(), side);
        int minZ = Quarry.min(quarry.z(), side);
        return claims.owns(quarry.owner(), world, quarry.y(), minX, minZ, minX + side - 1, minZ + side - 1);
    }

    /** El cofre (o barril, tolva, caja de shulker) pegado a la cantera, o vacío. */
    public Optional<Inventory> output(Block block) {
        for (BlockFace face : FACES) {
            Block side = block.getRelative(face);
            var state = side.getState(false);
            if (state instanceof Chest chest) {
                return Optional.of(chest.getInventory());
            }
            if (state instanceof Barrel barrel) {
                return Optional.of(barrel.getInventory());
            }
            if (state instanceof ShulkerBox box) {
                return Optional.of(box.getInventory());
            }
            if (state instanceof Hopper hopper) {
                return Optional.of(hopper.getInventory());
            }
        }
        return Optional.empty();
    }

    public void refreshFrame(Block block) {
        NamespacedKey model = settings.frame() ? Ui.model(settings.frameModel()) : null;
        if (model == null) {
            displays.remove(block, Displays.FRAME);
        } else {
            displays.frame(block, model, settings.frameScale(), BlockFace.UP);
        }
    }

    public void removeFrame(Block block) {
        displays.removeAll(block);
    }

    /** Lo que guarda de una cantera rota tirado en el suelo, para que no se pierda nada. */
    public void spill(Quarry quarry, Location at) {
        for (ItemStack item : quarry.buffer()) {
            at.getWorld().dropItemNaturally(at, item);
        }
        quarry.buffer().clear();
    }

    // ---------------------------------------------------------------- ítem

    /** El ítem de la cantera; con {@code from}, conserva sus mejoras. */
    public ItemStack item(Quarry from, int amount) {

        ItemStack item = new ItemStack(settings.block(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.component("quarry.item_name").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("quarry.item_lore"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemKey, PersistentDataType.BYTE, (byte) 1);
        if (from != null) {
            for (Numeric numeric : Numeric.values()) {
                int level = from.level(numeric);
                pdc.set(new NamespacedKey(plugin, "quarry-" + numeric.id()), PersistentDataType.INTEGER, level);
                if (level > 0) {
                    lore.add(lang.component("quarry.item_level", "upgrade", lang.raw("quarry.upgrade." + numeric.id()),
                            "level", Ui.roman(level)));
                }
            }
            for (Unlock unlock : Unlock.values()) {
                if (from.unlocked(unlock)) {
                    pdc.set(new NamespacedKey(plugin, "quarry-" + unlock.id()), PersistentDataType.BYTE, (byte) 1);
                    lore.add(lang.component("quarry.item_unlock", "upgrade", lang.raw("quarry.unlock." + unlock.id())));
                }
            }
        }
        meta.lore(lore.stream().map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());
        NamespacedKey model = Ui.model(settings.itemModel());
        if (model != null) {
            meta.setItemModel(model);
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /** Pasa las mejoras guardadas en el ítem a una cantera recién puesta. */
    public void copyUpgrades(ItemStack item, Quarry quarry) {
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Numeric numeric : Numeric.values()) {
            Integer level = pdc.get(new NamespacedKey(plugin, "quarry-" + numeric.id()), PersistentDataType.INTEGER);
            if (level != null) {
                quarry.level(numeric, Math.min(level, settings.track(numeric).max()));
            }
        }
        for (Unlock unlock : Unlock.values()) {
            if (pdc.has(new NamespacedKey(plugin, "quarry-" + unlock.id()), PersistentDataType.BYTE)) {
                quarry.unlock(unlock, true);
            }
        }
    }

    // ---------------------------------------------------------------- receta

    public void registerRecipe() {

        Bukkit.removeRecipe(recipeKey);
        QuarrySettings.Recipe recipe = settings.recipe();
        if (!settings.enabled() || !recipe.enabled() || recipe.shape().isEmpty()) {
            return;
        }
        try {
            ShapedRecipe shaped = new ShapedRecipe(recipeKey, item(null, 1));
            shaped.shape(recipe.shape().toArray(String[]::new));
            recipe.ingredients().forEach(shaped::setIngredient);
            Bukkit.addRecipe(shaped);
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getLogger().warning("✘ quarries.yml: la receta de la cantera no es válida: " + e.getMessage());
        }
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }
}
