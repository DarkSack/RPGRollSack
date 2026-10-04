package com.sack.rpgroll.machines.furnace;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.furnace.FurnaceSettings.FurnaceTier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * El nivel de un horno: en el PDC del bloque (su TileState) mientras está puesto, y en el del
 * ítem cuando se recoge. Un horno sin nivel es vanilla.
 */
public class FurnaceService {

    private final NamespacedKey tierKey;
    private final LangManager lang;
    private final Displays displays;
    private FurnaceSettings settings;

    public FurnaceService(Plugin plugin, LangManager lang, Displays displays, FurnaceSettings settings) {
        this.tierKey = new NamespacedKey(plugin, "furnace-tier");
        this.lang = lang;
        this.displays = displays;
        this.settings = settings;
    }

    public FurnaceSettings settings() {
        return settings;
    }

    public void settings(FurnaceSettings settings) {
        this.settings = settings;
    }

    public boolean isFurnace(Block block) {
        return settings.kinds().contains(block.getType());
    }

    public Optional<FurnaceTier> tier(Block block) {
        if (!FurnaceSettings.FURNACES.contains(block.getType())
                || !(block.getState(false) instanceof TileState state)) {
            return Optional.empty();
        }
        return settings.tier(state.getPersistentDataContainer().get(tierKey, PersistentDataType.STRING));
    }

    /** El id guardado aunque ya no exista en furnaces.yml (para no perderlo al romper). */
    public Optional<String> rawTier(Block block) {
        if (!FurnaceSettings.FURNACES.contains(block.getType()) || !(block.getState(false) instanceof TileState state)) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.getPersistentDataContainer().get(tierKey, PersistentDataType.STRING));
    }

    public void setTier(Block block, FurnaceTier tier) {
        BlockState snapshot = block.getState();
        if (!(snapshot instanceof TileState state)) {
            return;
        }
        if (tier == null) {
            state.getPersistentDataContainer().remove(tierKey);
        } else {
            state.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.id());
        }
        state.update(true, false);
        refreshFrame(block, tier);
    }

    public void refreshFrame(Block block, FurnaceTier tier) {
        NamespacedKey model = tier == null ? null : settings.frameModel(block.getType(), tier);
        if (model == null) {
            displays.remove(block, Displays.FRAME);
            return;
        }
        BlockFace front = block.getBlockData() instanceof Directional directional ? directional.getFacing() : BlockFace.UP;
        displays.frame(block, model, settings.frameScale(), front);
    }

    // ---------------------------------------------------------------- ítems

    public ItemStack item(Material kind, FurnaceTier tier, int amount) {

        ItemStack item = new ItemStack(kind, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.component("furnace.item_name", "tier", tier.name(), "kind", lang.raw("furnace.kind." + FurnaceSettings.kindId(kind)))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(stats(tier).stream()
                .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList());
        NamespacedKey model = settings.itemModel(kind, tier);
        if (model != null) {
            meta.setItemModel(model);
        }
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.id());
        item.setItemMeta(meta);
        return item;
    }

    /** El ítem de un horno con un nivel que ya no está en furnaces.yml: conserva el id por si vuelve. */
    public ItemStack orphanItem(Material kind, String tierId) {
        ItemStack item = new ItemStack(kind);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tierId);
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> itemTier(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !FurnaceSettings.FURNACES.contains(item.getType())) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.STRING));
    }

    public List<Component> stats(FurnaceTier tier) {
        List<Component> lines = new ArrayList<>();
        if (tier == null) {
            lines.add(lang.component("furnace.vanilla"));
            return lines;
        }
        lines.add(lang.component("furnace.stat_speed", "value", Ui.times(tier.speed())));
        lines.add(lang.component("furnace.stat_fuel", "value", Ui.times(tier.fuel())));
        if (tier.doubleChance() > 0) {
            lines.add(lang.component("furnace.stat_double", "value", Ui.percent(tier.doubleChance())));
        }
        return lines;
    }
}
