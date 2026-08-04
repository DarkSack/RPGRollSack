package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.LegacyTier;
import com.sack.rpgroll.ascension.deferred.LegacyManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class LegacyEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int PRESTIGE_SLOT = 11;
    private static final int BONUS_SLOT = 13;
    private static final int STAT_POINTS_SLOT = 15;
    private static final int BACK_SLOT = 26;

    private final LegacyManager manager;
    private final Runnable onBack;
    private LegacyTier current;

    public LegacyEditorGUI(Player player, LegacyTier tier, LegacyManager manager, Runnable onBack) {
        super(player, Component.text("Legado: " + tier.id(), NamedTextColor.GOLD), SIZE);
        this.current = tier;
        this.manager = manager;
        this.onBack = onBack;
    }

    private void replace(LegacyTier updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PRESTIGE_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Prestigio requerido: " + current.requiredPrestige(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(BONUS_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Bono de XP permanente: " + current.permanentExpBonusPercent() + "%",
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(STAT_POINTS_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Puntos de stat de bono: " + current.bonusStatPoints(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int delta = click == ClickType.RIGHT ? -1 : 1;

        if (slot == PRESTIGE_SLOT) {
            replace(new LegacyTier(current.id(), Math.max(0, current.requiredPrestige() + delta),
                    current.permanentExpBonusPercent(), current.bonusStatPoints()));
            return;
        }

        if (slot == BONUS_SLOT) {
            replace(new LegacyTier(current.id(), current.requiredPrestige(),
                    Math.max(0, current.permanentExpBonusPercent() + delta), current.bonusStatPoints()));
            return;
        }

        if (slot == STAT_POINTS_SLOT) {
            replace(new LegacyTier(current.id(), current.requiredPrestige(), current.permanentExpBonusPercent(),
                    Math.max(0, current.bonusStatPoints() + delta)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
