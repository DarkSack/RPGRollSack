package com.sack.rpgroll.quests.gui;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;
    private Quest current;

    public QuestEditorGUI(Player player, Quest quest, QuestManager questManager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("editor.title", "id", quest.id()), SIZE);
        this.current = quest;
        this.questManager = questManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
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
                .setName(lang.component("editor.name_label", "name", current.displayName())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("editor.click_to_rename"))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("editor.category_label", "category", current.category()))
                .setLore(lang.component("editor.click_next"))
                .build());

        setItem(DIFFICULTY_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(lang.component("editor.difficulty_label", "difficulty", current.difficulty()))
                .setLore(lang.component("editor.click_next"))
                .build());

        setItem(REPEATABLE_SLOT, new ItemBuilder(current.repeatable() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(lang.component(current.repeatable() ? "editor.repeatable_yes" : "editor.repeatable_no"))
                .setLore(lang.component("editor.click_to_toggle"))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("editor.cooldown_label", "seconds", current.cooldownMillis() / 1000))
                .setLore(lang.component("editor.cooldown_hint"))
                .build());

        setItem(LEVEL_REQ_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("editor.level_label", "level", current.requirements().level()))
                .setLore(lang.component("editor.click_plus_minus_1"))
                .build());

        setItem(REWARD_MONEY_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(lang.component("editor.reward_money_label", "money", current.rewards().money()))
                .setLore(lang.component("editor.click_plus_minus_50"))
                .build());

        setItem(REWARD_XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("editor.reward_xp_label", "xp", current.rewards().experience()))
                .setLore(lang.component("editor.click_plus_minus_50"))
                .build());

        List<QuestStage> stages = current.stages();

        for (int i = 0; i < stages.size() && i < 9; i++) {
            setItem(STAGES_START + i, new ItemBuilder(Material.MAP)
                    .setName(lang.component("editor.stage_entry", "index", i + 1, "id", stages.get(i).id())
                            .colorIfAbsent(NamedTextColor.WHITE))
                    .setLore(lang.component("editor.stage_objectives", "count", stages.get(i).objectives().size()),
                            lang.component("editor.shift_click_remove"),
                            lang.component("editor.stage_yaml_hint"))
                    .build());
        }

        setItem(ADD_STAGE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.add_stage"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "editor.prompt_name",
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
                    lang.send(player, "editor.stage_min_required");
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
            chatPromptManager.prompt(player, "editor.prompt_add_stage", value -> {

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
