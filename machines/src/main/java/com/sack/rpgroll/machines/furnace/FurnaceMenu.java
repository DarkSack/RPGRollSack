package com.sack.rpgroll.machines.furnace;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.furnace.FurnaceSettings.FurnaceTier;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Las mejoras de un horno: el nivel que tiene, el siguiente y lo que cuesta. */
public class FurnaceMenu extends InventoryGUI {

    private static final int CURRENT = 11;
    private static final int NEXT = 15;

    private final FurnaceService service;
    private final LangManager lang;
    private final Block block;
    private final Material kind;

    public FurnaceMenu(Player player, Block block, FurnaceService service, LangManager lang) {
        super(player, lang.component("furnace.menu_title", "kind", lang.raw("furnace.kind." + FurnaceSettings.kindId(block.getType()))), 27);
        this.service = service;
        this.lang = lang;
        this.block = block;
        this.kind = block.getType();
    }

    @Override
    public void build() {

        clear();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            setItem(slot, Ui.filler());
        }

        FurnaceTier current = service.tier(block).orElse(null);
        FurnaceSettings settings = service.settings();

        List<Component> now = new ArrayList<>(service.stats(current));
        setItem(CURRENT, Ui.button(kind, current == null ? null : settings.itemModel(kind, current),
                lang.component("furnace.current", "tier", current == null ? lang.raw("furnace.vanilla_name") : current.name()),
                now));

        var next = settings.next(current);
        if (next.isEmpty()) {
            setItem(NEXT, Ui.button(Material.NETHER_STAR, lang.component("furnace.max"), List.of()));
            return;
        }

        FurnaceTier tier = next.get();
        List<Component> lore = new ArrayList<>(service.stats(tier));
        lore.add(Component.empty());
        lore.addAll(Payment.lore(player, tier.cost(), lang));
        lore.add(Component.empty());
        boolean affordable = Payment.has(player, tier.cost());
        lore.add(lang.component(affordable ? "menu.click_upgrade" : "menu.cant_afford"));
        setItem(NEXT, Ui.button(kind, settings.itemModel(kind, tier),
                lang.component("furnace.next", "tier", tier.name(), "level", Ui.roman(tier.level())), lore));
        setItem(13, Ui.button(affordable ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                lang.component("menu.arrow"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        if (event.getRawSlot() != NEXT) {
            return;
        }
        // Que el horno siga ahí y siga siendo el mismo (otro jugador pudo romperlo o mejorarlo).
        if (block.getType() != kind) {
            close();
            return;
        }
        FurnaceTier current = service.tier(block).orElse(null);
        var next = service.settings().next(current);
        if (next.isEmpty()) {
            build();
            return;
        }
        if (!Payment.take(player, next.get().cost())) {
            lang.send(player, "menu.cant_afford_message");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            build();
            return;
        }
        service.setTier(block, next.get());
        player.playSound(block.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        lang.send(player, "furnace.upgraded", "tier", next.get().name());
        build();
    }
}
