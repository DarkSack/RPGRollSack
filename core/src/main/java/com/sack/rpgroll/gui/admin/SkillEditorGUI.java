package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.skill.Skill;
import com.sack.rpgroll.gameplay.skill.SkillManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SkillEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int LEVEL_SLOT = 11;
    private static final int MANA_SLOT = 12;
    private static final int COOLDOWN_SLOT = 13;
    private static final int DAMAGE_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final SkillManager skillManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Skill current;

    public SkillEditorGUI(Player player, Skill skill, SkillManager skillManager, ChatPromptManager chatPromptManager,
            LangManager lang, Runnable onBack) {
        super(player, lang.component("skill_editor_gui.title", "id", skill.id()), SIZE);
        this.current = skill;
        this.skillManager = skillManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(Skill updated) {
        current = updated;
        skillManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("skill_editor_gui.name_slot_name", "name", current.name()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("skill_editor_gui.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("skill_editor_gui.description_slot_name"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang.raw("skill_editor_gui.no_description")
                        : current.description()))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("skill_editor_gui.level_slot_name", "level", current.requiredLevel()))
                .setLore(lang.component("skill_editor_gui.level_slot_lore"))
                .build());

        setItem(MANA_SLOT, new ItemBuilder(Material.LAPIS_LAZULI)
                .setName(lang.component("skill_editor_gui.mana_slot_name", "mana", current.manaCost()))
                .setLore(lang.component("skill_editor_gui.mana_slot_lore"))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("skill_editor_gui.cooldown_slot_name", "cooldown", current.cooldownSeconds()))
                .setLore(lang.component("skill_editor_gui.cooldown_slot_lore"))
                .build());

        setItem(DAMAGE_SLOT, new ItemBuilder(Material.DIAMOND_SWORD)
                .setName(lang.component("skill_editor_gui.damage_slot_name", "multiplier",
                        current.damageMultiplier()))
                .setLore(lang.component("skill_editor_gui.damage_slot_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("skill_editor_gui.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("skill_editor_gui.prompt_name"), value -> replace(new Skill(
                    current.id(), value, current.description(), current.requiredLevel(), current.manaCost(),
                    current.cooldownSeconds(), current.damageMultiplier())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("skill_editor_gui.prompt_description"), value -> replace(
                    new Skill(current.id(), current.name(), value, current.requiredLevel(), current.manaCost(),
                            current.cooldownSeconds(), current.damageMultiplier())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new Skill(current.id(), current.name(), current.description(),
                    Math.max(1, current.requiredLevel() + delta), current.manaCost(), current.cooldownSeconds(),
                    current.damageMultiplier()));
            return;
        }

        if (slot == MANA_SLOT) {
            int delta = click == ClickType.RIGHT ? -5 : 5;
            replace(new Skill(current.id(), current.name(), current.description(), current.requiredLevel(),
                    Math.max(0, current.manaCost() + delta), current.cooldownSeconds(),
                    current.damageMultiplier()));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new Skill(current.id(), current.name(), current.description(), current.requiredLevel(),
                    current.manaCost(), Math.max(0, current.cooldownSeconds() + delta),
                    current.damageMultiplier()));
            return;
        }

        if (slot == DAMAGE_SLOT) {
            double delta = click == ClickType.RIGHT ? -0.1 : 0.1;
            replace(new Skill(current.id(), current.name(), current.description(), current.requiredLevel(),
                    current.manaCost(), current.cooldownSeconds(), Math.max(0, current.damageMultiplier() + delta)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
