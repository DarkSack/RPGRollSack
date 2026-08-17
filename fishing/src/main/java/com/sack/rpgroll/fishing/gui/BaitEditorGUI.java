package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.item.FishingItemFactory;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BaitEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int MATERIAL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int TAGS_SLOT = 13;
    private static final int QUALITY_BONUS_SLOT = 14;
    private static final int LEGENDARY_MULT_SLOT = 15;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final BaitManager baitManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Bait current;

    public BaitEditorGUI(Player player, Bait bait, BaitManager baitManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.bait.editor_title", "id", bait.id()), SIZE);
        this.current = bait;
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(Bait updated) {
        current = updated;
        baitManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.bait.field_name", "name", current.displayName())).build());

        setItem(MATERIAL_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.material()))
                .setName(lang.component("gui.bait.field_material", "value", current.material())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description") : current.description()))
                .build());

        setItem(TAGS_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.bait.field_tags", "value", String.join(", ", current.tags())))
                .setLore(lang.component("gui.bait.tags_hint")).build());

        setItem(QUALITY_BONUS_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.bait.field_quality_bonus", "value", current.qualityBonus()))
                .setLore(lang.component("gui.common.plusminus_05")).build());

        setItem(LEGENDARY_MULT_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.bait.field_legendary_mult", "value",
                        String.format(java.util.Locale.ROOT, "%.1f", current.legendaryWeightMultiplier())))
                .setLore(lang.component("gui.common.plusminus_05")).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.bait.give_button")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.5 : 0.5;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.bait.prompt_name"), value -> replace(new Bait(current.id(),
                    value, current.material(), current.description(), current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.bait.prompt_material"), value -> replace(new Bait(current.id(),
                    current.displayName(), value, current.description(), current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.bait.prompt_description"), value -> replace(new Bait(current.id(),
                    current.displayName(), current.material(), value, current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == TAGS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.bait.prompt_tags"), value -> {

                Set<String> tags = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        tags.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Bait(current.id(), current.displayName(), current.material(), current.description(), tags,
                        current.qualityBonus(), current.legendaryWeightMultiplier()));
            });
            return;
        }

        if (slot == QUALITY_BONUS_SLOT) {
            replace(new Bait(current.id(), current.displayName(), current.material(), current.description(),
                    current.tags(), Math.max(0, current.qualityBonus() + sign), current.legendaryWeightMultiplier()));
            return;
        }

        if (slot == LEGENDARY_MULT_SLOT) {
            replace(new Bait(current.id(), current.displayName(), current.material(), current.description(),
                    current.tags(), current.qualityBonus(), Math.max(0.1, current.legendaryWeightMultiplier() + sign)));
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(FishingItemFactory.createBait(current, lang));
            lang.send(player, "gui.bait.given", "name", current.displayName());
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
