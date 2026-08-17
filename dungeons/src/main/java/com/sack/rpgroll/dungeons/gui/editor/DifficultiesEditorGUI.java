package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonDifficulty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Lista de modos de dificultad — click abre el editor de ese modo, shift-click lo elimina. */
public class DifficultiesEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int ADD_SLOT = 22;
    private static final int BACK_SLOT = 26;

    private final DungeonEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public DifficultiesEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.difficulties.title", "id", session.original.id())), SIZE);
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

        List<DungeonDifficulty> difficulties = session.difficulties;

        for (int i = 0; i < difficulties.size() && i < 18; i++) {

            DungeonDifficulty difficulty = difficulties.get(i);

            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(Component.text(difficulty.displayName(), NamedTextColor.YELLOW))
                    .setLore(
                            ComponentUtils.parse(lang.raw("gui.editor.difficulties.item.multipliers",
                                    "health", difficulty.healthMultiplier(), "damage", difficulty.damageMultiplier(),
                                    "loot", difficulty.lootMultiplier())),
                            ComponentUtils.parse(lang.raw("gui.editor.difficulties.item.modifiers",
                                    "count", difficulty.modifiers().size())),
                            ComponentUtils.parse(lang.raw("gui.editor.rooms.item.edit_remove_hint")))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.difficulties.add")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.difficulties.size() && slot < 18) {

            if (event.isShiftClick()) {
                List<DungeonDifficulty> updated = new ArrayList<>(session.difficulties);
                updated.remove(slot);
                session.difficulties = updated;
                build();
                return;
            }

            new DifficultyEditGUI(player, session, slot, this::reopen).open();
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
        session.chatPromptManager.prompt(player, "gui.editor.difficulties.prompt.new_id", value -> {

            String id = value.trim().toLowerCase().replace(' ', '_');

            List<DungeonDifficulty> updated = new ArrayList<>(session.difficulties);
            updated.add(new DungeonDifficulty(id, id, 1.0, 1.0, 1.0, List.of()));
            session.difficulties = updated;

            build();
        });
    }

    private void reopen() {
        new DifficultiesEditorGUI(player, session, onBack).open();
    }

}
