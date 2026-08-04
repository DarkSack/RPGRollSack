package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SpeciesBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final FishSpeciesManager speciesManager;
    private final BaitManager baitManager;
    private final ChatPromptManager chatPromptManager;
    private List<FishSpecies> species;

    public SpeciesBrowserGUI(Player player, FishSpeciesManager speciesManager, BaitManager baitManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Especies de Pesca", NamedTextColor.GOLD), SIZE);
        this.speciesManager = speciesManager;
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
        this.species = List.copyOf(speciesManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < species.size() && i < 36; i++) {

            FishSpecies fish = species.get(i);

            setItem(i, new ItemBuilder(parseMaterial(fish.icon()))
                    .setName(Component.text((fish.legendary() ? "★ " : "") + fish.displayName(),
                            fish.legendary() ? NamedTextColor.GOLD : NamedTextColor.AQUA))
                    .setLore(Component.text("id: " + fish.id(), NamedTextColor.GRAY),
                            Component.text(fish.category() + " · " + fish.rarity(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear especie nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < species.size() && slot < 36) {
            new SpeciesEditorGUI(player, species.get(slot), speciesManager, baitManager, chatPromptManager,
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva especie:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (speciesManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una especie con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            FishSpecies fish = new FishSpecies(id, id, "COD", 0, "",
                    com.sack.rpgroll.fishing.core.FishCategory.FRESHWATER,
                    com.sack.rpgroll.fishing.core.FishRarity.COMMON, Set.of(), Set.of(), Set.of(), 0.5, 2.0, 10, 40,
                    5, 5, com.sack.rpgroll.fishing.core.FishBehaviorType.SLOW, Set.of(), Set.of(), Set.of(), Set.of(),
                    false, 0, false, null, null, null);

            speciesManager.save(fish);
            reopen();
        });
    }

    private void reopen() {
        this.species = List.copyOf(speciesManager.getAll());
        build();
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.COD;
        }
    }

}
