package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonMobModifier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class MobModifiersEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int MODIFIERS_START = 0;
    private static final int MODIFIERS_MAX = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SeasonManager seasonManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Season current;

    public MobModifiersEditorGUI(Player player, Season season, SeasonManager seasonManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.mob_modifiers_editor.title", "id", season.id()), SIZE);
        this.current = season;
        this.seasonManager = seasonManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(List<SeasonMobModifier> modifiers) {
        current = new Season(current.id(), current.displayName(), current.icon(), current.color(),
                current.description(), current.durationAmount(), current.durationUnit(), current.climate(),
                current.subSeasons(), current.biomeTemperatureModifiers(), current.vegetationEffects(), modifiers,
                current.exclusiveBossId(), current.worldEventIds(), current.worldEventDailyChance(),
                current.tags());
        seasonManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        List<SeasonMobModifier> modifiers = current.mobModifiers();

        for (int i = 0; i < modifiers.size() && i < MODIFIERS_MAX; i++) {

            SeasonMobModifier modifier = modifiers.get(i);

            setItem(MODIFIERS_START + i, new ItemBuilder(Material.ZOMBIE_HEAD)
                    .setName(Component.text(modifier.mobId(), NamedTextColor.GREEN))
                    .setLore(lang.component("gui.mob_modifiers_editor.chance_label", "percent",
                            Math.round(modifier.extraSpawnChance() * 100)),
                            lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.mob_modifiers_editor.add"))
                .setLore(lang.component("gui.mob_modifiers_editor.add_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < current.mobModifiers().size() && slot < MODIFIERS_MAX) {
            if (event.isShiftClick()) {
                List<SeasonMobModifier> modifiers = new ArrayList<>(current.mobModifiers());
                modifiers.remove(slot);
                replace(modifiers);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.mob_modifiers_editor.prompt_add"), value -> {

                String[] parts = value.trim().split("\\s+");

                if (parts.length < 2) {
                    chatPromptManager.lang().send(player, "gui.common.invalid_format");
                    return;
                }

                double chance;

                try {
                    chance = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    chatPromptManager.lang().send(player, "gui.mob_modifiers_editor.invalid_chance", "value", parts[1]);
                    return;
                }

                List<SeasonMobModifier> modifiers = new ArrayList<>(current.mobModifiers());
                modifiers.add(new SeasonMobModifier(parts[0], chance));
                replace(modifiers);
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
