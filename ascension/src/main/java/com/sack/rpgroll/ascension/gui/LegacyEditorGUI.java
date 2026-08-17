package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.LegacyTier;
import com.sack.rpgroll.ascension.deferred.LegacyManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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
    private final LangManager lang;
    private LegacyTier current;

    public LegacyEditorGUI(Player player, LegacyTier tier, LegacyManager manager, Runnable onBack,
            LangManager lang) {
        super(player, lang.component("gui.legacy.editor_title", "id", tier.id()), SIZE);
        this.current = tier;
        this.manager = manager;
        this.onBack = onBack;
        this.lang = lang;
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
                .setName(lang.component("gui.legacy.prestige_label", "prestige", current.requiredPrestige()))
                .setLore(lang.component("gui.common.click_plusminus1"))
                .build());

        setItem(BONUS_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.legacy.bonus_label", "percent", current.permanentExpBonusPercent()))
                .setLore(lang.component("gui.common.click_plusminus1"))
                .build());

        setItem(STAT_POINTS_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.legacy.stat_points_label", "points", current.bonusStatPoints()))
                .setLore(lang.component("gui.common.click_plusminus1"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
