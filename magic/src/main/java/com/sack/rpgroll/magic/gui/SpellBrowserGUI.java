package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCastTrigger;
import com.sack.rpgroll.magic.core.SpellCost;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.core.SchoolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SpellBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SpellManager spellManager;
    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private List<Spell> spells;

    public SpellBrowserGUI(Player player, SpellManager spellManager, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Hechizos", NamedTextColor.GOLD), SIZE);
        this.spellManager = spellManager;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.spells = List.copyOf(spellManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < spells.size() && i < 36; i++) {

            Spell spell = spells.get(i);
            NamedTextColor color = SchoolBrowserGUI.parseColor(spell.color());

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(spell.icon()))
                    .setName(Component.text(spell.displayName(), color))
                    .setLore(Component.text("id: " + spell.id(), NamedTextColor.GRAY),
                            Component.text("Escuela: " + spell.schoolId(), NamedTextColor.GRAY),
                            Component.text(spell.rarity() + " · Nivel " + spell.level(), NamedTextColor.GRAY),
                            Component.text(spell.components().size() + " componente(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear hechizo nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < spells.size() && slot < 36) {
            new SpellEditorHubGUI(player, spells.get(slot), spellManager, schoolManager, chatPromptManager,
                    this::reopen).open();
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

        if (schoolManager.count() == 0) {
            player.sendMessage(Component.text("Primero creá al menos una escuela.", NamedTextColor.RED));
            return;
        }

        chatPromptManager.prompt(player, "Escribí: id escuela (ej. fireball fire):", value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length < 2) {
                player.sendMessage(Component.text("Formato inválido — hacen falta id y escuela.", NamedTextColor.RED));
                return;
            }

            String id = parts[0].toLowerCase(Locale.ROOT).replace(' ', '_');
            String schoolId = parts[1].toLowerCase(Locale.ROOT);

            if (spellManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un hechizo con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            if (!schoolManager.exists(schoolId)) {
                player.sendMessage(Component.text("No existe una escuela con id: " + schoolId, NamedTextColor.RED));
                reopen();
                return;
            }

            Spell spell = new Spell(id, id, "BLAZE_POWDER", "WHITE", schoolId, null, 1, SpellCost.none(), 0, 20,
                    SpellCastTrigger.RIGHT_CLICK, null, 0, Set.of(), "", List.of());

            spellManager.save(spell);
            reopen();
        });
    }

    private void reopen() {
        this.spells = List.copyOf(spellManager.getAll());
        open();
    }

}
