package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.runtime.FishRecord;
import com.sack.rpgroll.fishing.runtime.FishingProfileManager;
import com.sack.rpgroll.fishing.runtime.PlayerFishingProfile;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Colección de capturas del jugador — especies conocidas con su mejor marca, desconocidas como silueta. */
public class EncyclopediaGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int SPECIES_MAX = 45;
    private static final int BACK_SLOT = 49;

    private final FishSpeciesManager speciesManager;
    private final FishingProfileManager profileManager;
    private final LangManager lang;
    private final List<FishSpecies> species;

    public EncyclopediaGUI(Player player, FishSpeciesManager speciesManager, FishingProfileManager profileManager,
            LangManager lang) {
        super(player, lang.component("gui.encyclopedia.title"), SIZE);
        this.speciesManager = speciesManager;
        this.profileManager = profileManager;
        this.lang = lang;
        this.species = List.copyOf(speciesManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        PlayerFishingProfile profile = profileManager.getOrLoad(player);

        for (int i = 0; i < species.size() && i < SPECIES_MAX; i++) {

            FishSpecies fish = species.get(i);
            var recordOpt = profile.recordFor(fish.id());

            if (recordOpt.isEmpty()) {
                setItem(i, new ItemBuilder(Material.GRAY_DYE)
                        .setName(lang.component("gui.encyclopedia.undiscovered_name"))
                        .setLore(lang.component("gui.encyclopedia.undiscovered_lore"))
                        .build());
                continue;
            }

            FishRecord record = recordOpt.get();

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(fish.icon()))
                    .setName(Component.text((fish.legendary() ? "★ " : "") + fish.displayName(),
                            fish.legendary() ? NamedTextColor.GOLD : NamedTextColor.AQUA))
                    .setLore(
                            Component.text(fish.category() + " · " + fish.rarity(), NamedTextColor.GRAY),
                            lang.component("gui.encyclopedia.caught", "count", record.caughtCount()),
                            lang.component("gui.encyclopedia.best_weight", "weight",
                                    String.format(Locale.ROOT, "%.2f", record.bestWeight())),
                            lang.component("gui.encyclopedia.best_length", "length",
                                    String.format(Locale.ROOT, "%.1f", record.bestLength())),
                            lang.component("gui.encyclopedia.best_quality", "quality", record.bestQuality()))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (event.getSlot() == BACK_SLOT) {
            close();
        }
    }

}
