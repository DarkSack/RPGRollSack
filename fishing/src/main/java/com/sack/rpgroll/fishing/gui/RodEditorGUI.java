package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.FishingRodManager;
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

public class RodEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int MATERIAL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int DURABILITY_SLOT = 13;
    private static final int CAST_POWER_SLOT = 14;
    private static final int REEL_SPEED_SLOT = 15;
    private static final int PRECISION_SLOT = 16;
    private static final int RESISTANCE_SLOT = 19;
    private static final int LUCK_SLOT = 20;
    private static final int CATEGORIES_SLOT = 21;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final FishingRodManager rodManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private FishingRod current;

    public RodEditorGUI(Player player, FishingRod rod, FishingRodManager rodManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.rod.editor_title", "id", rod.id()), SIZE);
        this.current = rod;
        this.rodManager = rodManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(FishingRod updated) {
        current = updated;
        rodManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.rod.field_name", "name", current.displayName())).build());

        setItem(MATERIAL_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.material()))
                .setName(lang.component("gui.rod.field_material", "value", current.material())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description") : current.description()))
                .build());

        setItem(DURABILITY_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(lang.component("gui.rod.field_durability", "value", current.durability()))
                .setLore(lang.component("gui.common.plusminus_8")).build());

        setItem(CAST_POWER_SLOT, new ItemBuilder(Material.FISHING_ROD)
                .setName(lang.component("gui.rod.field_cast_power", "value",
                        String.format(Locale.ROOT, "%.2f", current.castPower())))
                .setLore(lang.component("gui.common.plusminus_01")).build());

        setItem(REEL_SPEED_SLOT, new ItemBuilder(Material.STRING)
                .setName(lang.component("gui.rod.field_reel_speed", "value",
                        String.format(Locale.ROOT, "%.2f", current.reelSpeed())))
                .setLore(lang.component("gui.common.plusminus_01")).build());

        setItem(PRECISION_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("gui.rod.field_precision", "value",
                        String.format(Locale.ROOT, "%.1f", current.precision())))
                .setLore(lang.component("gui.common.plusminus_05")).build());

        setItem(RESISTANCE_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(lang.component("gui.rod.field_resistance", "value",
                        String.format(Locale.ROOT, "%.2f", current.resistance())))
                .setLore(lang.component("gui.rod.resistance_hint"))
                .build());

        setItem(LUCK_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.rod.field_luck", "value",
                        String.format(Locale.ROOT, "%.2f", current.luckBonus())))
                .setLore(lang.component("gui.rod.luck_hint"))
                .build());

        setItem(CATEGORIES_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(lang.component("gui.rod.field_categories", "value",
                        String.join(", ", current.preferredCategories())))
                .setLore(lang.component("gui.rod.categories_hint")).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.rod.give_button")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.1 : 0.1;
        int intSign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rod.prompt_name"), value -> replace(new FishingRod(current.id(),
                    value, current.material(), current.description(), current.durability(), current.castPower(),
                    current.reelSpeed(), current.precision(), current.resistance(), current.luckBonus(),
                    current.preferredCategories())));
            return;
        }

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rod.prompt_material"), value -> replace(new FishingRod(current.id(),
                    current.displayName(), value, current.description(), current.durability(), current.castPower(),
                    current.reelSpeed(), current.precision(), current.resistance(), current.luckBonus(),
                    current.preferredCategories())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rod.prompt_description"), value -> replace(new FishingRod(
                    current.id(), current.displayName(), current.material(), value, current.durability(),
                    current.castPower(), current.reelSpeed(), current.precision(), current.resistance(),
                    current.luckBonus(), current.preferredCategories())));
            return;
        }

        if (slot == DURABILITY_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    Math.max(1, current.durability() + intSign * 8), current.castPower(), current.reelSpeed(),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == CAST_POWER_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), Math.max(0.1, current.castPower() + sign), current.reelSpeed(),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == REEL_SPEED_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), Math.max(0.1, current.reelSpeed() + sign),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == PRECISION_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(),
                    Math.max(0, current.precision() + sign * 5), current.resistance(), current.luckBonus(),
                    current.preferredCategories()));
            return;
        }

        if (slot == RESISTANCE_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                    Math.max(0.1, current.resistance() + sign), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == LUCK_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                    current.resistance(), Math.max(0.1, current.luckBonus() + sign), current.preferredCategories()));
            return;
        }

        if (slot == CATEGORIES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rod.prompt_categories"), value -> {

                Set<String> categories = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        categories.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                        current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                        current.resistance(), current.luckBonus(), categories));
            });
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(FishingItemFactory.createRod(current, lang));
            lang.send(player, "gui.rod.given", "name", current.displayName());
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
