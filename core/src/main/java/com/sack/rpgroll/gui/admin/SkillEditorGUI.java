package com.sack.rpgroll.gui.admin;

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
    private final Runnable onBack;
    private Skill current;

    public SkillEditorGUI(Player player, Skill skill, SkillManager skillManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Skill: " + skill.id(), NamedTextColor.GOLD), SIZE);
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
                .setName(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("Nombre: " + current.name()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)"
                        : current.description()))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requiredLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(MANA_SLOT, new ItemBuilder(Material.LAPIS_LAZULI)
                .setName(Component.text("Costo de maná: " + current.manaCost(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Cooldown: " + current.cooldownSeconds() + "s", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1s · Click derecho: -1s", NamedTextColor.GRAY))
                .build());

        setItem(DAMAGE_SLOT, new ItemBuilder(Material.DIAMOND_SWORD)
                .setName(Component.text("Multiplicador de daño: " + current.damageMultiplier(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Skill(current.id(),
                    value, current.description(), current.requiredLevel(), current.manaCost(),
                    current.cooldownSeconds(), current.damageMultiplier())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Skill(
                    current.id(), current.name(), value, current.requiredLevel(), current.manaCost(),
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
