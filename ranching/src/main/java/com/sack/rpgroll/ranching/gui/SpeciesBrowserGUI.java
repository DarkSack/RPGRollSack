package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpeciesBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SpeciesManager speciesManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Species> species;

    public SpeciesBrowserGUI(Player player, SpeciesManager speciesManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.species"), NamedTextColor.GOLD), SIZE);
        this.speciesManager = speciesManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.species = List.copyOf(speciesManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < species.size() && i < 36; i++) {

            Species entry = species.get(i);

            setItem(i, new ItemBuilder(parseMaterial(entry.icon(), Material.COW_SPAWN_EGG))
                    .setName(Component.text(entry.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.browser.id_line", "id", entry.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.species.browser.entity_line", "entity", entry.entityType()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.species.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < species.size() && slot < 36) {
            new SpeciesEditorGUI(player, species.get(slot), speciesManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.species.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (speciesManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.species.browser.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            speciesManager.save(new Species(id, id, "COW_SPAWN_EGG", "", "COW", Set.of(), Map.of(), 100, 300, 6000,
                    12000, 240000, 24000, 1, 1, 0.5, Set.of()));
            reopen();
        });
    }

    static Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private void reopen() {
        this.species = List.copyOf(speciesManager.getAll());
        open();
    }

}
