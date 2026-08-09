package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista del jugador de su propia magia: qué hechizos sabe, cuál tiene
 * seleccionado (el que dispara al sostener un catalizador), cooldowns
 * activos, y acceso a socketear runas en cada uno.
 */
public class SpellbookGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int SPELLS_MAX = 45;
    private static final int BACK_SLOT = 49;

    private final SpellbookManager spellbookManager;
    private final SpellManager spellManager;
    private final RuneManager runeManager;
    private final ChatPromptManager chatPromptManager;
    private List<Spell> learnedSpells;

    public SpellbookGUI(Player player, SpellbookManager spellbookManager, SpellManager spellManager,
            RuneManager runeManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Tu Grimorio", NamedTextColor.GOLD), SIZE);
        this.spellbookManager = spellbookManager;
        this.spellManager = spellManager;
        this.runeManager = runeManager;
        this.chatPromptManager = chatPromptManager;
        this.learnedSpells = resolveLearned();
    }

    private List<Spell> resolveLearned() {

        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        List<Spell> spells = new ArrayList<>();

        for (String spellId : spellbook.allLearned()) {
            spellManager.get(spellId).ifPresent(spells::add);
        }

        return spells;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        long now = System.currentTimeMillis();

        for (int i = 0; i < learnedSpells.size() && i < SPELLS_MAX; i++) {

            Spell spell = learnedSpells.get(i);
            boolean selected = spell.id().equals(spellbook.selectedSpellId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Escuela: " + spell.schoolId(), NamedTextColor.GRAY));
            lore.add(Component.text("Trigger: " + spell.trigger(), NamedTextColor.GRAY));

            if (spellbook.isOnCooldown(spell.id(), now)) {
                double secondsLeft = spellbook.remainingCooldownMillis(spell.id(), now) / 1000.0;
                lore.add(Component.text(String.format(java.util.Locale.ROOT, "En cooldown: %.1fs", secondsLeft),
                        NamedTextColor.RED));
            } else {
                lore.add(Component.text("Listo para lanzar", NamedTextColor.GREEN));
            }

            List<String> runes = spellbook.runesFor(spell.id());
            lore.add(Component.text("Runas: " + (runes.isEmpty() ? "(ninguna)" : String.join(", ", runes)),
                    NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.empty());
            lore.add(Component.text("Click para seleccionar", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift-click para socketear runas", NamedTextColor.YELLOW));

            var builder = new ItemBuilder(SchoolBrowserGUI.parseMaterial(spell.icon()))
                    .setName(Component.text((selected ? "★ " : "") + spell.displayName(),
                            selected ? NamedTextColor.GOLD : SchoolBrowserGUI.parseColor(spell.color()))
                            .decoration(TextDecoration.BOLD, selected))
                    .setLore(lore);

            setItem(i, builder.build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < learnedSpells.size() && slot < SPELLS_MAX) {

            Spell spell = learnedSpells.get(slot);
            PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);

            if (event.isShiftClick()) {
                new RuneSocketGUI(player, spell, spellbook, runeManager, chatPromptManager, this::reopen).open();
                return;
            }

            spellbook.select(spell.id());
            player.sendMessage(Component.text("✔ Seleccionaste '" + spell.displayName() + "'.", NamedTextColor.GREEN));
            build();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void reopen() {
        this.learnedSpells = resolveLearned();
        open();
    }

}
