package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

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
    private final LangManager lang;

    public DialoguesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.dialogues.title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
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
            Component association = line.phaseId() != null
                    ? lang.component("gui.dialogues.assoc_phase", "phase", line.phaseId())
                    : line.trigger() != null
                            ? lang.component("gui.dialogues.assoc_trigger", "trigger", line.trigger())
                            : lang.component("gui.dialogues.assoc_none");

            setItem(i, new ItemBuilder(Material.WRITABLE_BOOK)
                    .setName(association)
                    .setLore(Component.text(line.text(), NamedTextColor.GRAY),
                            lang.component("gui.common.shift_remove_dark"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.dialogues.add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
        session.chatPromptManager.prompt(player, "gui.dialogues.prompt_assoc", value -> {

                    String trimmed = value.trim();
                    MobTrigger trigger = null;
                    String phaseId = null;

                    if (trimmed.toLowerCase(Locale.ROOT).startsWith("trigger:")) {
                        try {
                            trigger = MobTrigger.valueOf(
                                    trimmed.substring("trigger:".length()).trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            lang.send(player, "gui.dialogues.invalid_trigger");
                            return;
                        }
                    } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("phase:")) {
                        phaseId = trimmed.substring("phase:".length()).trim();
                    } else if (!trimmed.equalsIgnoreCase("ninguno")) {
                        lang.send(player, "gui.common.invalid_format");
                        return;
                    }

                    promptText(trigger, phaseId);
                });
    }

    private void promptText(MobTrigger trigger, String phaseId) {
        session.chatPromptManager.prompt(player, "gui.dialogues.prompt_text", text -> {

            List<MobDialogueLine> updated = new ArrayList<>(session.dialogues);
            updated.add(new MobDialogueLine(trigger, phaseId, text));
            session.dialogues = updated;

            build();
        });
    }

}
