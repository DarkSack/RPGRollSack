package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.WorldEvent;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WorldEventEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int DURATION_SLOT = 13;
    private static final int TEST_SLOT = 14;
    private static final int COMPONENTS_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final WorldEventManager worldEventManager;
    private final WorldEventEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private WorldEvent current;

    public WorldEventEditorGUI(Player player, WorldEvent event, WorldEventManager worldEventManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        this(player, event, worldEventManager, null, chatPromptManager, onBack);
    }

    public WorldEventEditorGUI(Player player, WorldEvent event, WorldEventManager worldEventManager,
            WorldEventEngine engine, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Evento: " + event.id(), NamedTextColor.GOLD), SIZE);
        this.current = event;
        this.worldEventManager = worldEventManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(WorldEvent updated) {
        current = updated;
        worldEventManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SeasonBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Duración: " + current.durationTicks() + " ticks", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +100 · Click derecho: -100", NamedTextColor.GRAY)).build());

        setItem(TEST_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("▶ Probar en tu mundo", NamedTextColor.GREEN))
                .setLore(engine == null
                        ? Component.text("(no disponible desde acá)", NamedTextColor.DARK_GRAY)
                        : Component.text("Dispara el evento en tu mundo actual", NamedTextColor.GRAY))
                .build());

        setItem(COMPONENTS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(Component.text("▶ Componentes (" + current.components().size() + ")", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(
                    new WorldEvent(current.id(), value, current.icon(), current.description(),
                            current.durationTicks(), current.components())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(
                    new WorldEvent(current.id(), current.displayName(), value, current.description(),
                            current.durationTicks(), current.components())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(
                    new WorldEvent(current.id(), current.displayName(), current.icon(), value,
                            current.durationTicks(), current.components())));
            return;
        }

        if (slot == DURATION_SLOT) {
            replace(new WorldEvent(current.id(), current.displayName(), current.icon(), current.description(),
                    Math.max(0, current.durationTicks() + sign * 100), current.components()));
            return;
        }

        if (slot == TEST_SLOT) {
            if (engine != null) {
                engine.trigger(current, player.getWorld());
            }
            return;
        }

        if (slot == COMPONENTS_SLOT) {
            new WorldEventComponentsEditorGUI(player, current, worldEventManager, chatPromptManager, this::onComponentsBack)
                    .open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void onComponentsBack() {
        current = worldEventManager.get(current.id()).orElse(current);
        build();
    }

}
