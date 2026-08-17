package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class BreedBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final BreedManager breedManager;
    private final SpeciesManager speciesManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Breed> breeds;

    public BreedBrowserGUI(Player player, BreedManager breedManager, SpeciesManager speciesManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.breeds"), NamedTextColor.GOLD), SIZE);
        this.breedManager = breedManager;
        this.speciesManager = speciesManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.breeds = List.copyOf(breedManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < breeds.size() && i < 36; i++) {

            Breed breed = breeds.get(i);

            setItem(i, new ItemBuilder(Material.NAME_TAG)
                    .setName(Component.text(breed.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.browser.id_line", "id", breed.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.breed.browser.species_line", "species", breed.speciesId()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.breed.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < breeds.size() && slot < 36) {
            new BreedEditorGUI(player, breeds.get(slot), breedManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.breed.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (breedManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.breed.browser.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            if (speciesManager.getAll().isEmpty()) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.breed.browser.need_species_first"), NamedTextColor.RED));
                reopen();
                return;
            }

            String speciesId = speciesManager.getAll().iterator().next().id();
            breedManager.save(new Breed(id, id, "", speciesId, 1.0, 1.0, 1.0, 1.0, "Neutral",
                    com.sack.rpgroll.common.reskin.EntityReskin.NONE));
            reopen();
        });
    }

    private void reopen() {
        this.breeds = List.copyOf(breedManager.getAll());
        open();
    }

}
