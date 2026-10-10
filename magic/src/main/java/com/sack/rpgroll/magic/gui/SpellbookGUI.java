package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.api.MagicAPI;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;
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
        super(player, chatPromptManager.lang().component("gui.spellbook.title"), SIZE);
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
        LangManager lang = chatPromptManager.lang();
        long now = System.currentTimeMillis();

        for (int i = 0; i < learnedSpells.size() && i < SPELLS_MAX; i++) {

            Spell spell = learnedSpells.get(i);
            boolean selected = spell.id().equals(spellbook.selectedSpellId());

            List<Component> lore = new ArrayList<>();

            if (!spell.description().isBlank()) {
                lore.addAll(MagicItemFactory.description(spell.description()));
                lore.add(Component.empty());
            }

            lore.add(lang.component("gui.spellbook.school_label", "schoolId", schoolName(spell.schoolId())));
            lore.add(lang.component("gui.spellbook.trigger_label", "trigger",
                    lang.raw("gui.spellbook.trigger." + spell.trigger().name(),
                            "seconds", seconds(spell.castTimeTicks()))));

            if (spell.cost().mana() > 0) {
                lore.add(lang.component("gui.spellbook.mana_label", "mana", spell.cost().mana()));
            }

            if (spell.cooldownTicks() > 0) {
                lore.add(lang.component("gui.spellbook.cooldown_label", "seconds", seconds(spell.cooldownTicks())));
            }

            if (spellbook.isOnCooldown(spell.id(), now)) {
                double secondsLeft = spellbook.remainingCooldownMillis(spell.id(), now) / 1000.0;
                lore.add(lang.component("gui.spellbook.on_cooldown_label", "seconds",
                        String.format(java.util.Locale.ROOT, "%.1f", secondsLeft)));
            } else {
                lore.add(lang.component("gui.spellbook.ready"));
            }

            List<String> runes = spellbook.runesFor(spell.id()).stream()
                    .map(runeId -> runeManager.get(runeId).map(Rune::displayName).orElse(runeId))
                    .toList();
            lore.add(lang.component("gui.spellbook.runes_label", "value",
                    runes.isEmpty() ? lang.raw("gui.spellbook.no_runes") : String.join("&r, ", runes)));
            lore.add(Component.empty());
            lore.add(lang.component("gui.spellbook.click_select"));
            lore.add(lang.component("gui.spellbook.shift_click_sockets"));

            var builder = new ItemBuilder(SchoolBrowserGUI.parseMaterial(spell.icon()))
                    .setName(ComponentUtils.parseWithDefault((selected ? "★ " : "") + spell.displayName(),
                            selected ? NamedTextColor.GOLD : SchoolBrowserGUI.parseColor(spell.color())))
                    .setLore(lore);

            // Solo se fuerza la negrita del seleccionado: forzarla a «false» en los demás
            // borraba la de los nombres que la traen («&4&lMeteoro»).
            if (selected) {
                builder.setName(ComponentUtils.parseWithDefault("★ " + spell.displayName(), NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
            }

            setItem(i, builder.build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    /** Ticks a segundos sin «.0» cuando son enteros (60 → «3», 50 → «2.5»). */
    private static String seconds(int ticks) {
        double value = ticks / 20.0;
        return value == Math.rint(value) ? String.valueOf((int) value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** El nombre de la escuela («&6Fuego»), o su id si Magic aún no está listo o no existe. */
    private static String schoolName(String schoolId) {
        if (!MagicAPI.isReady()) {
            return schoolId;
        }
        return MagicAPI.get().getSchoolManager().get(schoolId).map(MagicSchool::displayName).orElse(schoolId);
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
            chatPromptManager.lang().send(player, "gui.spellbook.selected", "spell", spell.displayName());
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
