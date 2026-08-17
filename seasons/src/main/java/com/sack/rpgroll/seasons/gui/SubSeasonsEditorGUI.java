package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.DurationUnit;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SubSeason;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubSeasonsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int SUB_SEASONS_START = 0;
    private static final int SUB_SEASONS_MAX = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SeasonManager seasonManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Season current;

    public SubSeasonsEditorGUI(Player player, Season season, SeasonManager seasonManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.sub_seasons_editor.title", "id", season.id()), SIZE);
        this.current = season;
        this.seasonManager = seasonManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(List<SubSeason> subSeasons) {
        current = new Season(current.id(), current.displayName(), current.icon(), current.color(),
                current.description(), current.durationAmount(), current.durationUnit(), current.climate(),
                subSeasons, current.biomeTemperatureModifiers(), current.vegetationEffects(),
                current.mobModifiers(), current.exclusiveBossId(), current.worldEventIds(),
                current.worldEventDailyChance(), current.tags());
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

        List<SubSeason> subSeasons = current.subSeasons();

        for (int i = 0; i < subSeasons.size() && i < SUB_SEASONS_MAX; i++) {

            SubSeason sub = subSeasons.get(i);

            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("gui.common.id_label", "id", sub.id()));
            lore.add(lang.component("gui.sub_seasons_editor.duration_label", "amount", sub.durationAmount(), "unit",
                    sub.durationUnit()));
            if (sub.temperatureOverride() != null) {
                lore.add(lang.component("gui.sub_seasons_editor.temperature_label", "temperature",
                        sub.temperatureOverride()));
            }
            lore.add(lang.component("gui.common.shift_remove"));

            setItem(SUB_SEASONS_START + i, new ItemBuilder(Material.BOOK)
                    .setName(Component.text((i + 1) + ". " + sub.displayName(), NamedTextColor.GREEN))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.sub_seasons_editor.add"))
                .setLore(lang.component("gui.sub_seasons_editor.add_lore_1"),
                        lang.component("gui.sub_seasons_editor.add_lore_2"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < current.subSeasons().size() && slot < SUB_SEASONS_MAX) {
            if (event.isShiftClick()) {
                List<SubSeason> subSeasons = new ArrayList<>(current.subSeasons());
                subSeasons.remove(slot);
                replace(subSeasons);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.sub_seasons_editor.prompt_add"), value -> {

                String[] parts = value.trim().split("\\s+");

                if (parts.length < 4) {
                    chatPromptManager.lang().send(player, "gui.common.invalid_format");
                    return;
                }

                DurationUnit unit;

                try {
                    unit = DurationUnit.valueOf(parts[3].trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    chatPromptManager.lang().send(player, "gui.sub_seasons_editor.invalid_unit", "value", parts[3]);
                    return;
                }

                int amount = parseIntOr(parts[2], 1);
                Double temperature = parts.length >= 5 ? parseDoubleOrNull(parts[4]) : null;

                List<SubSeason> subSeasons = new ArrayList<>(current.subSeasons());
                subSeasons.add(new SubSeason(parts[0], parts[1], amount, unit, temperature));
                replace(subSeasons);
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private int parseIntOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Double parseDoubleOrNull(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
