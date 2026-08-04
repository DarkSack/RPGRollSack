package com.sack.rpgroll.seasons.gui;

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
        super(player, Component.text("Subestaciones: " + season.id(), NamedTextColor.GOLD), SIZE);
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

        List<SubSeason> subSeasons = current.subSeasons();

        for (int i = 0; i < subSeasons.size() && i < SUB_SEASONS_MAX; i++) {

            SubSeason sub = subSeasons.get(i);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("id: " + sub.id(), NamedTextColor.GRAY));
            lore.add(Component.text("Duración: " + sub.durationAmount() + " " + sub.durationUnit(), NamedTextColor.GRAY));
            if (sub.temperatureOverride() != null) {
                lore.add(Component.text("Temperatura fija: " + sub.temperatureOverride() + "°C", NamedTextColor.AQUA));
            }
            lore.add(Component.text("Shift-click para quitar", NamedTextColor.RED));

            setItem(SUB_SEASONS_START + i, new ItemBuilder(Material.BOOK)
                    .setName(Component.text((i + 1) + ". " + sub.displayName(), NamedTextColor.GREEN))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar subestación al final", NamedTextColor.GREEN))
                .setLore(Component.text("ID NOMBRE CANTIDAD UNIDAD [TEMP]", NamedTextColor.GRAY),
                        Component.text("ej. temprana Temprana 2 MINECRAFT_DAYS", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
            chatPromptManager.prompt(player, "Escribí: ID NOMBRE CANTIDAD UNIDAD [TEMPERATURA]:", value -> {

                String[] parts = value.trim().split("\\s+");

                if (parts.length < 4) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                    return;
                }

                DurationUnit unit;

                try {
                    unit = DurationUnit.valueOf(parts[3].trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("Unidad inválida: " + parts[3], NamedTextColor.RED));
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
