package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.WorldEvent;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class WorldEventBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final WorldEventManager worldEventManager;
    private final WorldEventEngine engine;
    private final ChatPromptManager chatPromptManager;
    private List<WorldEvent> events;

    public WorldEventBrowserGUI(Player player, WorldEventManager worldEventManager, WorldEventEngine engine,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.world_event_browser.title"), SIZE);
        this.worldEventManager = worldEventManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.events = List.copyOf(worldEventManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        for (int i = 0; i < events.size() && i < 36; i++) {

            WorldEvent event = events.get(i);

            setItem(i, new ItemBuilder(SeasonBrowserGUI.parseMaterial(event.icon()))
                    .setName(Component.text(event.displayName(), NamedTextColor.LIGHT_PURPLE))
                    .setLore(lang.component("gui.common.id_label", "id", event.id()),
                            lang.component("gui.world_event_browser.component_count", "count",
                                    event.components().size()),
                            lang.component("gui.common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.world_event_browser.new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < events.size() && slot < 36) {
            new WorldEventEditorGUI(player, events.get(slot), worldEventManager, engine, chatPromptManager,
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.world_event_browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (worldEventManager.exists(id)) {
                chatPromptManager.lang().send(player, "gui.world_event_browser.already_exists");
                reopen();
                return;
            }

            worldEventManager.save(new WorldEvent(id, id, "NETHER_STAR", "", 200, List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.events = List.copyOf(worldEventManager.getAll());
        open();
    }

}
