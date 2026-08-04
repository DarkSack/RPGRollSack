package com.sack.rpgroll.quests.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestManager;
import com.sack.rpgroll.quests.core.QuestRequirements;
import com.sack.rpgroll.quests.core.QuestRewards;
import com.sack.rpgroll.quests.core.QuestStage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class QuestBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final QuestManager questManager;
    private final ChatPromptManager chatPromptManager;
    private List<Quest> quests;

    public QuestBrowserGUI(Player player, QuestManager questManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Quests RPGRoll", NamedTextColor.GOLD), SIZE);
        this.questManager = questManager;
        this.chatPromptManager = chatPromptManager;
        this.quests = List.copyOf(questManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < quests.size() && i < 36; i++) {

            Quest quest = quests.get(i);

            setItem(i, new ItemBuilder(Material.WRITTEN_BOOK)
                    .setName(Component.text(quest.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(quest.category() + " · " + quest.difficulty(), NamedTextColor.GRAY),
                            Component.text(quest.stages().size() + " stage(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear quest nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < quests.size() && slot < 36) {
            new QuestEditorGUI(player, quests.get(slot), questManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva quest:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (questManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una quest con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Quest quest = new Quest(id, id, null, null, false, 0, QuestRequirements.none(),
                    List.of(new QuestStage("inicio", List.of(), List.of(), null, null)), QuestRewards.none(),
                    null);

            questManager.save(quest);
            reopen();
        });
    }

    private void reopen() {
        this.quests = List.copyOf(questManager.getAll());
        build();
    }

}
