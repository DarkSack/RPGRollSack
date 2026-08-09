package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.ClimateProfile;
import com.sack.rpgroll.seasons.core.DurationUnit;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SeasonBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SeasonManager seasonManager;
    private final WorldEventManager worldEventManager;
    private final ChatPromptManager chatPromptManager;
    private List<Season> seasons;

    public SeasonBrowserGUI(Player player, SeasonManager seasonManager, WorldEventManager worldEventManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Estaciones", NamedTextColor.GOLD), SIZE);
        this.seasonManager = seasonManager;
        this.worldEventManager = worldEventManager;
        this.chatPromptManager = chatPromptManager;
        this.seasons = List.copyOf(seasonManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < seasons.size() && i < 36; i++) {

            Season season = seasons.get(i);
            NamedTextColor color = parseColor(season.color());

            setItem(i, new ItemBuilder(parseMaterial(season.icon()))
                    .setName(Component.text(season.displayName(), color))
                    .setLore(Component.text("id: " + season.id(), NamedTextColor.GRAY),
                            Component.text(season.durationAmount() + " " + season.durationUnit(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear estación nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < seasons.size() && slot < 36) {
            new SeasonEditorHubGUI(player, seasons.get(slot), seasonManager, worldEventManager, chatPromptManager,
                    this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva estación:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (seasonManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una estación con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Season season = new Season(id, id, "SUNFLOWER", "WHITE", "", 7, DurationUnit.MINECRAFT_DAYS,
                    ClimateProfile.temperate(), List.of(), Map.of(), Set.of(), List.of(), null, List.of(), 0.0,
                    Set.of());

            seasonManager.save(season);
            reopen();
        });
    }

    private void reopen() {
        this.seasons = List.copyOf(seasonManager.getAll());
        open();
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.SUNFLOWER;
        }
    }

    static NamedTextColor parseColor(String raw) {
        NamedTextColor color = NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
        return color != null ? color : NamedTextColor.WHITE;
    }

}
