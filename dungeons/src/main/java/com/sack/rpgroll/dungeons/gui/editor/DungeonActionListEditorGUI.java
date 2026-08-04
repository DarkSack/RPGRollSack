package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonAction;
import com.sack.rpgroll.dungeons.gui.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor genérico de una lista de {@link DungeonAction} — reutilizado
 * por Triggers y por los eventos de sala. Sintaxis de alta:
 * {@code TIPO clave=valor clave2=valor2} (ej. {@code SOUND sound=ENTITY_WITHER_SPAWN}),
 * o solo {@code TIPO} para una acción sin parámetros.
 */
public class DungeonActionListEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final List<DungeonAction> actions;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public DungeonActionListEditorGUI(Player player, String title, List<DungeonAction> actions,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.actions = actions;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < actions.size() && i < 36; i++) {

            DungeonAction action = actions.get(i);

            List<Component> lore = new ArrayList<>();
            for (var entry : action.params().entrySet()) {
                lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.GRAY));
            }
            lore.add(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY));

            setItem(i, new ItemBuilder(Material.COMMAND_BLOCK)
                    .setName(Component.text(action.type(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar acción", NamedTextColor.GREEN))
                .setLore(Component.text("TIPO clave=valor clave2=valor2", NamedTextColor.GRAY),
                        Component.text("ej. SOUND sound=ENTITY_WITHER_SPAWN", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < actions.size() && slot < 36) {
            if (event.isShiftClick()) {
                actions.remove(slot);
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        chatPromptManager.prompt(player, "Escribí: TIPO clave=valor clave2=valor2 (ej. TELEPORT x=100 y=65 z=0):",
                value -> {

                    String[] tokens = value.trim().split("\\s+");

                    if (tokens.length == 0 || tokens[0].isBlank()) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        return;
                    }

                    String type = tokens[0].toUpperCase(Locale.ROOT);
                    Map<String, String> params = new HashMap<>();

                    for (int i = 1; i < tokens.length; i++) {

                        String[] kv = tokens[i].split("=", 2);
                        if (kv.length == 2) {
                            params.put(kv[0], kv[1]);
                        }
                    }

                    actions.add(new DungeonAction(type, params));
                    build();
                });
    }

}
