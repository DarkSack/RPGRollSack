package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.Claims;
import com.sack.rpgroll.machines.quarry.Quarry;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Recipe;

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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lo común de las máquinas de área: tipos, ítem, cofre, claim, marco y recetas. */
public class AreaService {

    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN};

    private final Plugin plugin;
    private final LangManager lang;
    private final Displays displays;
    private final AreaStore store;
    private final Claims claims;
    private final NamespacedKey kindKey;
    private final Map<String, AreaKind> kinds = new LinkedHashMap<>();

    public AreaService(Plugin plugin, LangManager lang, Displays displays, AreaStore store, Claims claims,
            List<AreaKind> kinds) {
        this.plugin = plugin;
        this.lang = lang;
        this.displays = displays;
        this.store = store;
        this.claims = claims;
        this.kindKey = new NamespacedKey(plugin, "area-kind");
        kinds.forEach(kind -> this.kinds.put(kind.id(), kind));
    }

    public Collection<AreaKind> kinds() {
        return kinds.values();
    }

    public Optional<AreaKind> kind(String id) {
        return Optional.ofNullable(kinds.get(id));
    }

    public Optional<AreaKind> kind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        return kind(item.getItemMeta().getPersistentDataContainer().getOrDefault(kindKey, PersistentDataType.STRING, ""));
    }

    public AreaStore store() {
        return store;
    }

    public Optional<AreaMachine> at(Block block) {
        return store.at(Displays.key(block));
    }

    /** ¿Está el cuadrado de lado {@code side} entero en un claim del dueño? (Si el tipo no lo pide, sí.) */
    public boolean areaAllowed(AreaMachine machine, World world, int side) {
        AreaKind kind = kinds.get(machine.kind());
        if (kind == null || !kind.settings().requireClaim()) {
            return true;
        }
        int minX = Quarry.min(machine.x(), side);
        int minZ = Quarry.min(machine.z(), side);
        return claims.owns(machine.owner(), world, machine.y(), minX, minZ, minX + side - 1, minZ + side - 1);
    }

    public Claims claims() {
        return claims;
    }

    public Optional<Inventory> output(Block block) {
        for (BlockFace face : FACES) {
            var state = block.getRelative(face).getState(false);
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

    /** Mete el búfer en el cofre; true si cupo todo. */
    public boolean flush(AreaMachine machine, Inventory output) {
        if (machine.buffer().isEmpty()) {
            return true;
        }
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack item : machine.buffer()) {
            left.addAll(output.addItem(item.clone()).values());
        }
        machine.buffer().clear();
        machine.buffer().addAll(left);
        store.dirty();
        return left.isEmpty();
    }

    public void spill(AreaMachine machine, Location at) {
        for (ItemStack item : machine.buffer()) {
            at.getWorld().dropItemNaturally(at, item);
        }
        machine.buffer().clear();
    }

    public void refreshFrame(Block block, AreaKind kind) {
        AreaSettings settings = kind.settings();
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

    // ---------------------------------------------------------------- ítem

    private NamespacedKey levelKey(AreaKind kind, String upgrade) {
        return new NamespacedKey(plugin, "area-" + kind.id() + "-" + upgrade);
    }

    public ItemStack item(AreaKind kind, AreaMachine from, int amount) {
        AreaSettings settings = kind.settings();
        ItemStack item = new ItemStack(settings.block(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        String prefix = "area." + kind.id();
        meta.displayName(lang.component(prefix + ".item_name").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component(prefix + ".item_lore"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(kindKey, PersistentDataType.STRING, kind.id());
        if (from != null) {
            for (String upgrade : kind.upgrades()) {
                int level = from.level(upgrade);
                pdc.set(levelKey(kind, upgrade), PersistentDataType.INTEGER, level);
                if (level > 0) {
                    lore.add(lang.component("area.item_level", "upgrade", lang.raw(prefix + ".upgrade." + upgrade),
                            "level", Ui.roman(level)));
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

    public void copyUpgrades(ItemStack item, AreaKind kind, AreaMachine machine) {
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (String upgrade : kind.upgrades()) {
            Integer level = pdc.get(levelKey(kind, upgrade), PersistentDataType.INTEGER);
            if (level != null) {
                machine.level(upgrade, Math.min(level, kind.settings().track(upgrade).max()));
            }
        }
    }

    // ---------------------------------------------------------------- recetas

    public void registerRecipes() {
        for (AreaKind kind : kinds.values()) {
            NamespacedKey key = new NamespacedKey(plugin, kind.id());
            Bukkit.removeRecipe(key);
            Recipe recipe = kind.settings().recipe();
            if (!kind.settings().enabled() || !recipe.enabled() || recipe.shape().isEmpty()) {
                continue;
            }
            try {
                ShapedRecipe shaped = new ShapedRecipe(key, item(kind, null, 1));
                shaped.shape(recipe.shape().toArray(String[]::new));
                recipe.ingredients().forEach(shaped::setIngredient);
                Bukkit.addRecipe(shaped);
            } catch (IllegalArgumentException | IllegalStateException e) {
                plugin.getLogger().warning("✘ " + kind.file() + ": la receta no es válida: " + e.getMessage());
            }
        }
    }

    public void unregisterRecipes() {
        kinds.keySet().forEach(id -> Bukkit.removeRecipe(new NamespacedKey(plugin, id)));
    }
}
