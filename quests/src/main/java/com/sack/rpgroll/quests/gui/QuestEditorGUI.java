package com.sack.rpgroll.quests.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestCategory;
import com.sack.rpgroll.quests.core.QuestDifficulty;
import com.sack.rpgroll.quests.core.QuestManager;
import com.sack.rpgroll.quests.core.QuestRequirements;
import com.sack.rpgroll.quests.core.QuestRewards;
import com.sack.rpgroll.quests.core.QuestStage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de campos de nivel superior de una quest (identidad, requisito de
 * nivel, recompensas básicas, lista de stages por id) + una lista de
 * stages (agregar/quitar por id — el contenido de cada stage, diálogos y
 * objetivos, se sigue autorando en YAML por ahora).
 */
public class QuestEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int CATEGORY_SLOT = 11;
    private static final int DIFFICULTY_SLOT = 12;
    private static final int REPEATABLE_SLOT = 13;
    private static final int COOLDOWN_SLOT = 14;
    private static final int LEVEL_REQ_SLOT = 15;

    private static final int REWARD_MONEY_SLOT = 19;
    private static final int REWARD_XP_SLOT = 20;

    private static final int STAGES_START = 27;
    private static final int ADD_STAGE_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final QuestManager questManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Quest current;

    public QuestEditorGUI(Player player, Quest quest, QuestManager questManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Quest: " + quest.id(), NamedTextColor.GOLD), SIZE);
        this.current = quest;
        this.questManager = questManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Quest updated) {
        current = updated;
        questManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("Nombre: " + current.displayName()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Categoría: " + current.category(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY))
                .build());

        setItem(DIFFICULTY_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(Component.text("Dificultad: " + current.difficulty(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY))
                .build());

        setItem(REPEATABLE_SLOT, new ItemBuilder(current.repeatable() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text("Repetible: " + (current.repeatable() ? "sí" : "no"),
                        current.repeatable() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Cooldown: " + (current.cooldownMillis() / 1000) + "s", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1h · Click derecho: -1h", NamedTextColor.GRAY))
                .build());

        setItem(LEVEL_REQ_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requirements().level(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(REWARD_MONEY_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text("Recompensa de oro: " + current.rewards().money(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +50 · Click derecho: -50", NamedTextColor.GRAY))
                .build());

        setItem(REWARD_XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Recompensa de XP: " + current.rewards().experience(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +50 · Click derecho: -50", NamedTextColor.GRAY))
                .build());

        List<QuestStage> stages = current.stages();

        for (int i = 0; i < stages.size() && i < 9; i++) {
            setItem(STAGES_START + i, new ItemBuilder(Material.MAP)
                    .setName(Component.text((i + 1) + ". " + stages.get(i).id(), NamedTextColor.WHITE))
                    .setLore(Component.text(stages.get(i).objectives().size() + " objetivo(s)", NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY),
                            Component.text("Los objetivos/diálogo se editan en el YAML", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_STAGE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar stage (por id)", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre de la quest:",
                    value -> replace(new Quest(current.id(), value, current.category(), current.difficulty(),
                            current.repeatable(), current.cooldownMillis(), current.requirements(), current.stages(),
                            current.rewards(), current.events())));
            return;
        }

        if (slot == CATEGORY_SLOT) {
            QuestCategory[] values = QuestCategory.values();
            QuestCategory next = values[(current.category().ordinal() + 1) % values.length];
            replace(new Quest(current.id(), current.displayName(), next, current.difficulty(), current.repeatable(),
                    current.cooldownMillis(), current.requirements(), current.stages(), current.rewards(),
                    current.events()));
            return;
        }

        if (slot == DIFFICULTY_SLOT) {
            QuestDifficulty[] values = QuestDifficulty.values();
            QuestDifficulty next = values[(current.difficulty().ordinal() + 1) % values.length];
            replace(new Quest(current.id(), current.displayName(), current.category(), next, current.repeatable(),
                    current.cooldownMillis(), current.requirements(), current.stages(), current.rewards(),
                    current.events()));
            return;
        }

        if (slot == REPEATABLE_SLOT) {
            replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                    !current.repeatable(), current.cooldownMillis(), current.requirements(), current.stages(),
                    current.rewards(), current.events()));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long deltaHours = click == ClickType.RIGHT ? -1 : 1;
            long updatedSeconds = Math.max(0, current.cooldownMillis() / 1000 + deltaHours * 3600);
            replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                    current.repeatable(), updatedSeconds * 1000, current.requirements(), current.stages(),
                    current.rewards(), current.events()));
            return;
        }

        if (slot == LEVEL_REQ_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            QuestRequirements req = current.requirements();
            QuestRequirements updated = new QuestRequirements(Math.max(0, req.level() + delta), req.race(),
                    req.playerClass(), req.profession(), req.trait(), req.permission(), req.money(), req.items(),
                    req.completedQuests(), req.world(), req.region(), req.biome(), req.weather(), req.hourMin(),
                    req.hourMax());
            replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                    current.repeatable(), current.cooldownMillis(), updated, current.stages(), current.rewards(),
                    current.events()));
            return;
        }

        if (slot == REWARD_MONEY_SLOT) {
            double delta = click == ClickType.RIGHT ? -50 : 50;
            QuestRewards rewards = current.rewards();
            QuestRewards updated = new QuestRewards(Math.max(0, rewards.money() + delta), rewards.experience(),
                    rewards.items(), rewards.commands(), rewards.quests(), rewards.extra());
            replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                    current.repeatable(), current.cooldownMillis(), current.requirements(), current.stages(),
                    updated, current.events()));
            return;
        }

        if (slot == REWARD_XP_SLOT) {
            int delta = click == ClickType.RIGHT ? -50 : 50;
            QuestRewards rewards = current.rewards();
            QuestRewards updated = new QuestRewards(rewards.money(), Math.max(0, rewards.experience() + delta),
                    rewards.items(), rewards.commands(), rewards.quests(), rewards.extra());
            replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                    current.repeatable(), current.cooldownMillis(), current.requirements(), current.stages(),
                    updated, current.events()));
            return;
        }

        if (slot >= STAGES_START && slot < STAGES_START + Math.min(current.stages().size(), 9)) {

            if (event.isShiftClick()) {

                if (current.stages().size() <= 1) {
                    player.sendMessage(Component.text("Una quest necesita al menos un stage.", NamedTextColor.RED));
                    return;
                }

                List<QuestStage> updated = new ArrayList<>(current.stages());
                updated.remove(slot - STAGES_START);
                replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                        current.repeatable(), current.cooldownMillis(), current.requirements(), updated,
                        current.rewards(), current.events()));
            }
            return;
        }

        if (slot == ADD_STAGE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del nuevo stage:", value -> {

                String stageId = value.trim().toLowerCase().replace(' ', '_');
                List<QuestStage> updated = new ArrayList<>(current.stages());
                updated.add(new QuestStage(stageId, List.of(), List.of(), null, null));

                replace(new Quest(current.id(), current.displayName(), current.category(), current.difficulty(),
                        current.repeatable(), current.cooldownMillis(), current.requirements(), updated,
                        current.rewards(), current.events()));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
