package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.skill.Skill;
import com.sack.rpgroll.workers.core.skill.SkillManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public class SkillEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int PROFESSION_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int ATTRIBUTE_KEY_SLOT = 13;
    private static final int MAX_LEVEL_SLOT = 14;
    private static final int VALUE_PER_LEVEL_SLOT = 15;
    private static final int BACK_SLOT = 40;

    private final SkillManager skillManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Skill current;

    public SkillEditorGUI(Player player, Skill skill, SkillManager skillManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Habilidad: " + skill.id(), NamedTextColor.GOLD), SIZE);
        this.current = skill;
        this.skillManager = skillManager;
        this.chatPromptManager = chatPromptManager;
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
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(PROFESSION_SLOT, new ItemBuilder(Material.VILLAGER_SPAWN_EGG)
                .setName(Component.text("Profesión: " + current.professionId(), NamedTextColor.AQUA)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(ATTRIBUTE_KEY_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("attribute-key: " + current.attributeKey(), NamedTextColor.GOLD)).build());

        setItem(MAX_LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel máximo: " + current.maxLevel(), NamedTextColor.GREEN))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(VALUE_PER_LEVEL_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Valor por nivel: " + current.valuePerLevel(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click: +0.5 · Click derecho: -0.5", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int intSign = event.getClick() == ClickType.RIGHT ? -1 : 1;
        double doubleSign = event.getClick() == ClickType.RIGHT ? -0.5 : 0.5;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Skill(current.id(), value,
                    current.description(), current.professionId(), current.maxLevel(), current.attributeKey(),
                    current.valuePerLevel())));
        } else if (slot == PROFESSION_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la profesión:", value -> replace(new Skill(current.id(),
                    current.displayName(), current.description(), value.trim().toLowerCase(Locale.ROOT), current.maxLevel(),
                    current.attributeKey(), current.valuePerLevel())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Skill(current.id(),
                    current.displayName(), value, current.professionId(), current.maxLevel(), current.attributeKey(),
                    current.valuePerLevel())));
        } else if (slot == ATTRIBUTE_KEY_SLOT) {
            chatPromptManager.prompt(player, "Escribí el attribute-key:", value -> replace(new Skill(current.id(),
                    current.displayName(), current.description(), current.professionId(), current.maxLevel(), value,
                    current.valuePerLevel())));
        } else if (slot == MAX_LEVEL_SLOT) {
            replace(new Skill(current.id(), current.displayName(), current.description(), current.professionId(),
                    Math.max(1, current.maxLevel() + intSign * 5), current.attributeKey(), current.valuePerLevel()));
        } else if (slot == VALUE_PER_LEVEL_SLOT) {
            replace(new Skill(current.id(), current.displayName(), current.description(), current.professionId(),
                    current.maxLevel(), current.attributeKey(), Math.max(0, current.valuePerLevel() + doubleSign)));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
