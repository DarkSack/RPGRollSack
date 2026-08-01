package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcAction;
import com.sack.rpgroll.npcs.core.NpcEditSession;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Muestra las acciones actuales del NPC (click para eliminar) y un botón
 * para agregar una nueva vía chat, en formato "TIPO|valor"
 * (ej. "MESSAGE|Hola {player}" o "COMMAND|give {player} diamond 1").
 */
public class NpcActionsGUI extends InventoryGUI {

    private static final int SLOT_ADD = 22;
    private static final int SLOT_BACK = 26;

    private final NpcEditSession session;
    private final NpcAdminGUI parent;
    private final ChatPromptManager chatPromptManager;
    private final Map<Integer, Integer> slotToActionIndex = new HashMap<>();

    public NpcActionsGUI(Player player, NpcEditSession session, NpcAdminGUI parent,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Acciones del NPC", NamedTextColor.GOLD).decorate(TextDecoration.BOLD), 36);
        this.session = session;
        this.parent = parent;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();
        slotToActionIndex.clear();

        var actions = session.getActions();

        for (int i = 0; i < actions.size() && i < 18; i++) {

            NpcAction action = actions.get(i);

            setItem(i, new ItemBuilder(action.type() == NpcAction.NpcActionType.MESSAGE
                    ? Material.PAPER
                    : Material.COMMAND_BLOCK)
                    .setName(Component.text(action.type().name(), NamedTextColor.GOLD))
                    .setLore(Component.text(action.value(), NamedTextColor.WHITE),
                            Component.text("Click para eliminar", NamedTextColor.RED))
                    .build());

            slotToActionIndex.put(i, i);
        }

        setItem(SLOT_ADD, ItemBuilder.createConfirmButton("Agregar acción"));
        setItem(SLOT_BACK, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            close();
            parent.reopen();
            return;
        }

        if (slot == SLOT_ADD) {
            close();
            chatPromptManager.prompt(player,
                    "Escribe: TIPO|valor (ej: MESSAGE|Hola {player}, o COMMAND|give {player} diamond 1)",
                    input -> {
                        String[] parts = input.split("\\|", 2);

                        if (parts.length != 2) {
                            player.sendMessage(Component.text("Formato inválido. Usa TIPO|valor", NamedTextColor.RED));
                            reopen();
                            return;
                        }

                        try {
                            NpcAction.NpcActionType type = NpcAction.NpcActionType
                                    .valueOf(parts[0].trim().toUpperCase());
                            session.addAction(new NpcAction(type, parts[1].trim()));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Component.text("Tipo inválido: " + parts[0], NamedTextColor.RED));
                        }

                        reopen();
                    });
            return;
        }

        Integer index = slotToActionIndex.get(slot);
        if (index != null) {
            session.removeAction(index);
            close();
            reopen();
        }
    }

    private void reopen() {
        new NpcActionsGUI(player, session, parent, chatPromptManager).open();
    }

}