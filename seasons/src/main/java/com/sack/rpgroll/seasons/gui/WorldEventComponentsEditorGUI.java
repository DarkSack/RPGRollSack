package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.WorldEvent;
import com.sack.rpgroll.seasons.core.WorldEventComponent;
import com.sack.rpgroll.seasons.core.WorldEventComponentType;
import com.sack.rpgroll.seasons.core.WorldEventManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorldEventComponentsEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int COMPONENTS_START = 0;
    private static final int COMPONENTS_MAX = 45;
    private static final int ADD_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final WorldEventManager worldEventManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private WorldEvent current;

    public WorldEventComponentsEditorGUI(Player player, WorldEvent event, WorldEventManager worldEventManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Componentes: " + event.id(), NamedTextColor.GOLD), SIZE);
        this.current = event;
        this.worldEventManager = worldEventManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(List<WorldEventComponent> components) {
        current = new WorldEvent(current.id(), current.displayName(), current.icon(), current.description(),
                current.durationTicks(), components);
        worldEventManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<WorldEventComponent> components = current.components();

        for (int i = 0; i < components.size() && i < COMPONENTS_MAX; i++) {
            setItem(COMPONENTS_START + i, componentItem(components.get(i), i));
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar componente", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO clave=valor,clave2=valor2", NamedTextColor.GRAY),
                        Component.text("ej. PARTICLE particle=FLAME,count=30", NamedTextColor.DARK_GRAY),
                        Component.text("ej. SPAWN_MOB mob-id=winter_yeti,chance=0.5", NamedTextColor.DARK_GRAY),
                        Component.text("Orden importa — se ejecutan de arriba a abajo", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack componentItem(WorldEventComponent component, int index) {

        List<Component> lore = new ArrayList<>();

        for (var entry : component.params().entrySet()) {
            lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.DARK_GRAY));
        }

        lore.add(Component.text("Shift-click para quitar", NamedTextColor.RED));

        return new ItemBuilder(iconFor(component.type()))
                .setName(Component.text("#" + (index + 1) + " " + component.type(), NamedTextColor.AQUA))
                .setLore(lore)
                .build();
    }

    private Material iconFor(WorldEventComponentType type) {
        return switch (type) {
            case PARTICLE -> Material.BLAZE_POWDER;
            case SOUND -> Material.NOTE_BLOCK;
            case VISUAL -> Material.NETHER_STAR;
            case APPLY_EFFECT -> Material.POTION;
            case SPAWN_MOB -> Material.ZOMBIE_HEAD;
            case MESSAGE -> Material.PAPER;
            case COMMAND -> Material.COMMAND_BLOCK;
            case SET_WEATHER -> Material.WATER_BUCKET;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < current.components().size() && slot < COMPONENTS_MAX) {
            if (event.isShiftClick()) {
                List<WorldEventComponent> components = new ArrayList<>(current.components());
                components.remove(slot);
                replace(components);
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAddComponent();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAddComponent() {
        chatPromptManager.prompt(player, "Escribí: TIPO clave=valor,clave2=valor2:", value -> {

            String[] parts = value.trim().split("\\s+", 2);
            WorldEventComponentType type;

            try {
                type = WorldEventComponentType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
                return;
            }

            Map<String, String> params = new LinkedHashMap<>();

            if (parts.length == 2) {
                for (String pair : parts[1].split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            List<WorldEventComponent> components = new ArrayList<>(current.components());
            components.add(new WorldEventComponent(type, params));
            replace(components);
        });
    }

}
