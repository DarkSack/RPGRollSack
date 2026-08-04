package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobDialogueLine;
import com.sack.rpgroll.mobs.core.MobTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de líneas de diálogo. Alta en dos pasos: primero a qué se asocia
 * ({@code trigger:SPAWN}, {@code phase:enraged} o {@code ninguno}), luego
 * el texto.
 */
public class DialoguesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;

    public DialoguesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Diálogos: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<MobDialogueLine> dialogues = session.dialogues;

        for (int i = 0; i < dialogues.size() && i < 36; i++) {

            MobDialogueLine line = dialogues.get(i);
            String association = line.phaseId() != null ? "fase: " + line.phaseId()
                    : line.trigger() != null ? "trigger: " + line.trigger() : "(sin asociar)";

            setItem(i, new ItemBuilder(Material.WRITABLE_BOOK)
                    .setName(Component.text(association, NamedTextColor.YELLOW))
                    .setLore(Component.text(line.text(), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar línea", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.dialogues.size() && slot < 36) {
            if (event.isShiftClick()) {
                List<MobDialogueLine> updated = new ArrayList<>(session.dialogues);
                updated.remove(slot);
                session.dialogues = updated;
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAssociation();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAssociation() {
        session.chatPromptManager.prompt(player,
                "Asociá esta línea: trigger:SPAWN, phase:enraged, o 'ninguno':", value -> {

                    String trimmed = value.trim();
                    MobTrigger trigger = null;
                    String phaseId = null;

                    if (trimmed.toLowerCase(Locale.ROOT).startsWith("trigger:")) {
                        try {
                            trigger = MobTrigger.valueOf(
                                    trimmed.substring("trigger:".length()).trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Component.text("Trigger inválido.", NamedTextColor.RED));
                            return;
                        }
                    } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("phase:")) {
                        phaseId = trimmed.substring("phase:".length()).trim();
                    } else if (!trimmed.equalsIgnoreCase("ninguno")) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        return;
                    }

                    promptText(trigger, phaseId);
                });
    }

    private void promptText(MobTrigger trigger, String phaseId) {
        session.chatPromptManager.prompt(player, "Escribí el texto del diálogo:", text -> {

            List<MobDialogueLine> updated = new ArrayList<>(session.dialogues);
            updated.add(new MobDialogueLine(trigger, phaseId, text));
            session.dialogues = updated;

            build();
        });
    }

}
