package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class GrimoireEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int SCHOOL_SLOT = 13;
    private static final int LEVEL_SLOT = 14;

    private static final int SPELLS_START = 18;
    private static final int SPELLS_MAX = 27;
    private static final int GIVE_SLOT = 48;
    private static final int ADD_SPELL_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Grimoire current;

    public GrimoireEditorGUI(Player player, Grimoire grimoire, GrimoireManager grimoireManager,
            SpellManager spellManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Grimorio: " + grimoire.id(), NamedTextColor.GOLD), SIZE);
        this.current = grimoire;
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Grimoire updated) {
        current = updated;
        grimoireManager.save(current);
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

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(SCHOOL_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(Component.text("Escuela: " + (current.schoolId() == null ? "(cualquiera)" : current.schoolId()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Puramente informativo — escribí 'ninguna' para quitarlo", NamedTextColor.GRAY))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requiredLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        List<String> spellIds = current.spellIds();

        for (int i = 0; i < spellIds.size() && i < SPELLS_MAX; i++) {

            String spellId = spellIds.get(i);
            String displayName = spellManager.get(spellId).map(spell -> spell.displayName()).orElse(spellId);

            setItem(SPELLS_START + i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(displayName, NamedTextColor.LIGHT_PURPLE))
                    .setLore(Component.text("id: " + spellId, NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.RED))
                    .build());
        }

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("▶ Darme uno", NamedTextColor.GREEN))
                .build());

        setItem(ADD_SPELL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar hechizo", NamedTextColor.GREEN))
                .setLore(Component.text("Escribí el id de un hechizo existente", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int sign = click == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Grimoire(current.id(),
                    value, current.icon(), current.description(), current.schoolId(), current.requiredLevel(),
                    current.spellIds())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new Grimoire(
                    current.id(), current.displayName(), value, current.description(), current.schoolId(),
                    current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Grimoire(
                    current.id(), current.displayName(), current.icon(), value, current.schoolId(),
                    current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == SCHOOL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la escuela (o 'ninguna'):",
                    value -> replace(new Grimoire(current.id(), current.displayName(), current.icon(),
                            current.description(), value.equalsIgnoreCase("ninguna") ? null : value.trim(),
                            current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                    current.schoolId(), Math.max(0, current.requiredLevel() + sign), current.spellIds()));
            return;
        }

        if (slot >= SPELLS_START && slot < SPELLS_START + Math.min(current.spellIds().size(), SPELLS_MAX)) {
            if (event.isShiftClick()) {
                List<String> spellIds = new ArrayList<>(current.spellIds());
                spellIds.remove(slot - SPELLS_START);
                replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                        current.schoolId(), current.requiredLevel(), spellIds));
            }
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(MagicItemFactory.createGrimoire(current));
            player.sendMessage(Component.text("✔ Te diste un grimorio '" + current.displayName() + "'.",
                    NamedTextColor.GREEN));
            return;
        }

        if (slot == ADD_SPELL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del hechizo a agregar:", value -> {

                String spellId = value.trim().toLowerCase(java.util.Locale.ROOT);

                if (!spellManager.exists(spellId)) {
                    player.sendMessage(Component.text("No existe un hechizo con ese id.", NamedTextColor.RED));
                    return;
                }

                if (current.spellIds().contains(spellId)) {
                    player.sendMessage(Component.text("Ese hechizo ya está en el grimorio.", NamedTextColor.RED));
                    return;
                }

                List<String> spellIds = new ArrayList<>(current.spellIds());
                spellIds.add(spellId);
                replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                        current.schoolId(), current.requiredLevel(), spellIds));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
